package com.mia.aegis.skill.executor.engine;

import com.mia.aegis.skill.dsl.model.Skill;
import com.mia.aegis.skill.dsl.model.Step;
import com.mia.aegis.skill.dsl.model.StepStatus;
import com.mia.aegis.skill.dsl.model.StepType;
import com.mia.aegis.skill.dsl.model.io.OutputContract;
import com.mia.aegis.skill.dsl.model.WhenCondition;
import com.mia.aegis.skill.exception.SkillExecutionException;
import com.mia.aegis.skill.executor.context.ExecutionContext;
import com.mia.aegis.skill.executor.context.SkillResult;
import com.mia.aegis.skill.executor.context.StepResult;
import com.mia.aegis.skill.executor.io.OutputValidationResult;
import com.mia.aegis.skill.executor.io.OutputValidator;
import com.mia.aegis.skill.executor.io.SimpleOutputValidator;
import com.mia.aegis.skill.executor.store.AwaitRequest;
import com.mia.aegis.skill.executor.store.ExecutionContextSnapshot;
import com.mia.aegis.skill.exception.ExecutionNotFoundException;
import com.mia.aegis.skill.executor.store.ExecutionStore;
import com.mia.aegis.skill.executor.store.InMemoryExecutionStore;
import com.mia.aegis.skill.executor.step.AwaitStepExecutor;
import com.mia.aegis.skill.executor.step.PromptStepExecutor;
import com.mia.aegis.skill.executor.step.StepExecutor;
import com.mia.aegis.skill.executor.step.TemplateStepExecutor;
import com.mia.aegis.skill.executor.step.ToolStepExecutor;
import com.mia.aegis.skill.dsl.condition.ConditionEvaluator;
import com.mia.aegis.skill.dsl.condition.DefaultConditionEvaluator;
import com.mia.aegis.skill.llm.LLMAdapterRegistry;
import com.mia.aegis.skill.template.TemplateRenderer;
import com.mia.aegis.skill.tools.ToolRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 默认 Skill 执行器实现。
 *
 * <p>实现 Skill 的完整执行流程：
 * <ol>
 *   <li>初始化执行上下文</li>
 *   <li>串行执行每个 Step</li>
 *   <li>管理 Step 状态转换</li>
 *   <li>收集执行结果</li>
 *   <li>支持 await 步骤的暂停和恢复</li>
 * </ol>
 * </p>
 */
public class DefaultSkillExecutor implements SkillExecutor {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSkillExecutor.class);

    private final Map<StepType, StepExecutor> stepExecutors;
    private final OutputValidator outputValidator;
    private final ExecutionStore executionStore;
    private final ConditionEvaluator conditionEvaluator;
    private ExecutionListener listener;

    /**
     * 创建 DefaultSkillExecutor（仅 Tool 支持）。
     *
     * @param toolRegistry Tool 注册表
     * @param templateRenderer 模板渲染器
     */
    public DefaultSkillExecutor(ToolRegistry toolRegistry, TemplateRenderer templateRenderer) {
        this(toolRegistry, null, templateRenderer, null);
    }

    /**
     * 创建 DefaultSkillExecutor（完整支持）。
     *
     * @param toolRegistry Tool 注册表
     * @param llmRegistry LLM Adapter 注册表（可选）
     * @param templateRenderer 模板渲染器
     */
    public DefaultSkillExecutor(ToolRegistry toolRegistry, LLMAdapterRegistry llmRegistry,
                                TemplateRenderer templateRenderer) {
        this(toolRegistry, llmRegistry, templateRenderer, null);
    }

    /**
     * 创建 DefaultSkillExecutor（完整支持 + 自定义 ExecutionStore）。
     *
     * @param toolRegistry Tool 注册表
     * @param llmRegistry LLM Adapter 注册表（可选）
     * @param templateRenderer 模板渲染器
     * @param executionStore 执行存储（可选，默认使用内存存储）
     */
    public DefaultSkillExecutor(ToolRegistry toolRegistry, LLMAdapterRegistry llmRegistry,
                                TemplateRenderer templateRenderer, ExecutionStore executionStore) {
        if (toolRegistry == null) {
            throw new IllegalArgumentException("ToolRegistry cannot be null");
        }
        if (templateRenderer == null) {
            throw new IllegalArgumentException("TemplateRenderer cannot be null");
        }

        this.stepExecutors = new HashMap<StepType, StepExecutor>();
        this.outputValidator = new SimpleOutputValidator();
        this.executionStore = executionStore != null ? executionStore : new InMemoryExecutionStore();
        this.conditionEvaluator = new DefaultConditionEvaluator();

        // 注册 Tool Step 执行器
        stepExecutors.put(StepType.TOOL, new ToolStepExecutor(toolRegistry, templateRenderer));

        // 注册 Prompt Step 执行器（如果 LLM Registry 可用）
        if (llmRegistry != null) {
            stepExecutors.put(StepType.PROMPT, new PromptStepExecutor(llmRegistry, templateRenderer));
        }

        // 注册 Template Step 执行器
        stepExecutors.put(StepType.TEMPLATE, new TemplateStepExecutor(templateRenderer));

        // 注册 Await Step 执行器
        stepExecutors.put(StepType.AWAIT, new AwaitStepExecutor());
    }

    /**
     * 注册 Step 执行器。
     *
     * @param type Step 类型
     * @param executor Step 执行器
     */
    public void registerStepExecutor(StepType type, StepExecutor executor) {
        if (type == null || executor == null) {
            throw new IllegalArgumentException("StepType and StepExecutor cannot be null");
        }
        stepExecutors.put(type, executor);
    }

    @Override
    public SkillResult execute(Skill skill, Map<String, Object> input) throws SkillExecutionException {
        ExecutionContext context = new ExecutionContext(input);
        return execute(skill, context);
    }

    @Override
    public SkillResult execute(Skill skill, ExecutionContext context) throws SkillExecutionException {
        if (skill == null) {
            throw new IllegalArgumentException("Skill cannot be null");
        }
        if (context == null) {
            context = ExecutionContext.empty();
        }

        long startTime = System.currentTimeMillis();
        List<StepResult> stepResults = new ArrayList<StepResult>();
        Object lastOutput = null;
        String error = null;
        boolean success = true;

        // 通知监听器 Skill 开始
        if (listener != null) {
            listener.onSkillStart(skill);
        }

        try {
            List<Step> steps = skill.getSteps();
            int totalSteps = steps.size();
            int stepIndex = 0;

            // 串行执行每个 Step
            for (Step step : steps) {
                // 注册 varName 映射（Tool 步骤不需要 varName，其输出通过 ToolOutputContext 直接写入）
                if (step.hasVarName() && step.getType() != StepType.TOOL) {
                    context.registerVarName(step.getName(), step.getVarName());
                }

                // 通知监听器 Step 开始
                if (listener != null) {
                    listener.onStepStart(step, stepIndex, totalSteps);
                }

                StepResult result = executeStep(step, context);
                stepResults.add(result);
                context.addStepResult(result);

                // 输出上下文变量快照
                logContextVariables(step.getName(), context);

                // 通知监听器 Step 完成
                if (listener != null) {
                    listener.onStepComplete(step, result, stepIndex, totalSteps);
                }

                if (result.isSuccess()) {
                    // 更新 Step 状态和输出
                    step.setStatus(StepStatus.SUCCESS);
                    step.setOutput(result.getOutput());
                    lastOutput = result.getOutput();
                    stepIndex++;
                } else if (result.isSkipped()) {
                    // 步骤被跳过（条件不满足）
                    step.setStatus(StepStatus.SKIPPED);
                    // 跳过的步骤不影响执行流程，继续执行下一个步骤
                    stepIndex++;
                } else if (result.isAwaiting()) {
                    // Await Step: 暂停执行，保存快照
                    step.setStatus(StepStatus.AWAITING);
                    long totalDuration = System.currentTimeMillis() - startTime;

                    // 从 StepResult 获取 AwaitRequest
                    AwaitRequest awaitRequest = (AwaitRequest) result.getOutput();

                    // 保存快照
                    ExecutionContextSnapshot snapshot = ExecutionContextSnapshot.createActive(
                        context,
                        skill.getId(),
                        stepIndex,
                        awaitRequest
                    );
                    executionStore.save(snapshot);

                    // 返回 WAITING 状态
                    SkillResult waitingResult = SkillResult.awaiting(
                        context.getExecutionId(),
                        awaitRequest,
                        stepResults,
                        totalDuration
                    );

                    // 通知监听器 Skill 完成（await 状态）
                    if (listener != null) {
                        listener.onSkillComplete(skill, waitingResult);
                    }

                    return waitingResult;
                } else {
                    // Step 执行失败
                    step.setStatus(StepStatus.FAILED);
                    success = false;
                    error = "Step '" + step.getName() + "' failed: " + result.getError();

                    // 标记后续 Step 为 SKIPPED
                    markRemainingStepsSkipped(skill.getSteps(), step, stepResults);
                    break;
                }
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            SkillResult skillResult;

            if (success) {
                // 从执行上下文中根据 output_schema 构建输出
                Object finalOutput = buildOutputFromContext(context, skill.getOutputContract());

                // 执行输出契约校验
                OutputValidationResult validationResult = validateOutput(finalOutput, skill.getOutputContract());
                if (validationResult.isFailed()) {
                    skillResult = SkillResult.failure(validationResult.getErrorMessage(), stepResults, totalDuration);
                } else {
                    skillResult = SkillResult.success(finalOutput, stepResults, totalDuration);
                }
            } else {
                // 执行失败时直接返回错误，不需要构建输出
                skillResult = SkillResult.failure(error, stepResults, totalDuration);
            }

            // 通知监听器 Skill 完成
            if (listener != null) {
                listener.onSkillComplete(skill, skillResult);
            }

            return skillResult;

        } catch (SkillExecutionException e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            SkillResult skillResult = SkillResult.failure(e.getMessage(), stepResults, totalDuration);
            if (listener != null) {
                listener.onSkillComplete(skill, skillResult);
            }
            return skillResult;
        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            SkillResult skillResult = SkillResult.failure("Unexpected error: " + e.getMessage(), stepResults, totalDuration);
            if (listener != null) {
                listener.onSkillComplete(skill, skillResult);
            }
            return skillResult;
        }
    }

    @Override
    public SkillResult resume(Skill skill, String executionId, Map<String, Object> userInput)
            throws SkillExecutionException {
        if (skill == null) {
            throw new IllegalArgumentException("Skill cannot be null");
        }
        if (executionId == null || executionId.isEmpty()) {
            throw new IllegalArgumentException("executionId cannot be null or empty");
        }

        // 1. 查找快照
        ExecutionContextSnapshot snapshot = executionStore.findById(executionId);
        if (snapshot == null) {
            throw new ExecutionNotFoundException(executionId);
        }

        // 2. 检查快照状态
        if (!snapshot.isResumable()) {
            throw new com.mia.aegis.skill.executor.store.ExecutionAlreadyCompletedException(
                executionId, snapshot.getStatus());
        }

        // 3. 更新状态为 RESUMED
        executionStore.updateStatus(executionId, com.mia.aegis.skill.executor.store.SnapshotStatus.RESUMED);

        // 4. 恢复执行上下文并注入用户输入
        ExecutionContext savedContext = snapshot.getContext();
        int resumeStepIndex = snapshot.getCurrentStepIndex();

        String awaitStepName = skill.getSteps().get(resumeStepIndex).getName();

        // 创建新的上下文，包含已有的 step 结果和 await 输入
        ExecutionContext resumeContext = ExecutionContext.forResume(
            savedContext.getInput(),
            savedContext.getStepResults(),
            savedContext.getAwaitInputs(),
            executionId
        );
        resumeContext.addAwaitInput(awaitStepName, userInput);

        // 5. 将 await step 标记为成功（输出为用户输入）
        StepResult awaitResult = StepResult.success(awaitStepName, userInput, 0);
        resumeContext.addStepResult(awaitResult);

        // 6. 从下一个 step 继续执行
        int nextStepIndex = resumeStepIndex + 1;
        return executeFromStep(skill, resumeContext, nextStepIndex);
    }

    @Override
    public CompletableFuture<SkillResult> executeAsync(Skill skill, Map<String, Object> input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(skill, input);
            } catch (SkillExecutionException e) {
                return SkillResult.failure(e.getMessage(), new ArrayList<StepResult>(), 0);
            }
        });
    }

    @Override
    public CompletableFuture<SkillResult> executeAsync(Skill skill, ExecutionContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(skill, context);
            } catch (SkillExecutionException e) {
                return SkillResult.failure(e.getMessage(), new ArrayList<StepResult>(), 0);
            }
        });
    }

    @Override
    public CompletableFuture<SkillResult> resumeAsync(Skill skill, String executionId, Map<String, Object> userInput) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return resume(skill, executionId, userInput);
            } catch (ExecutionNotFoundException | com.mia.aegis.skill.executor.store.ExecutionAlreadyCompletedException e) {
                return SkillResult.failure(e.getMessage(), new ArrayList<StepResult>(), 0);
            } catch (SkillExecutionException e) {
                return SkillResult.failure(e.getMessage(), new ArrayList<StepResult>(), 0);
            }
        });
    }

    /**
     * 从指定 step 开始继续执行 Skill。
     *
     * @param skill Skill 定义
     * @param context 执行上下文
     * @param startStepIndex 开始执行的 step 索引
     * @return Skill 执行结果
     * @throws SkillExecutionException 执行失败时抛出
     */
    private SkillResult executeFromStep(Skill skill, ExecutionContext context, int startStepIndex)
            throws SkillExecutionException {
        long startTime = System.currentTimeMillis();
        List<StepResult> stepResults = new ArrayList<StepResult>(context.getStepResults().values());
        Object lastOutput = null;
        String error = null;
        boolean success = true;

        // 获取之前成功的最后一个输出
        for (StepResult result : stepResults) {
            if (result.isSuccess()) {
                lastOutput = result.getOutput();
            }
        }

        try {
            List<Step> steps = skill.getSteps();
            int totalSteps = steps.size();

            // 恢复执行时，需要为 resume 前的步骤重新注册 varName 映射
            // 这些步骤已经执行完毕，但其 varName 映射在新的 ExecutionContext 中丢失了
            for (int i = 0; i < startStepIndex; i++) {
                Step preStep = steps.get(i);
                if (preStep.hasVarName() && preStep.getType() != StepType.TOOL) {
                    context.registerVarName(preStep.getName(), preStep.getVarName());
                }
            }

            // 从指定索引开始执行
            for (int stepIndex = startStepIndex; stepIndex < totalSteps; stepIndex++) {
                Step step = steps.get(stepIndex);

                // 注册 varName 映射（Tool 步骤不需要 varName）
                if (step.hasVarName() && step.getType() != StepType.TOOL) {
                    context.registerVarName(step.getName(), step.getVarName());
                }

                // 通知监听器 Step 开始
                if (listener != null) {
                    listener.onStepStart(step, stepIndex, totalSteps);
                }

                StepResult result = executeStep(step, context);
                stepResults.add(result);
                context.addStepResult(result);

                // 输出上下文变量快照
                logContextVariables(step.getName(), context);

                // 通知监听器 Step 完成
                if (listener != null) {
                    listener.onStepComplete(step, result, stepIndex, totalSteps);
                }

                if (result.isSuccess()) {
                    step.setStatus(StepStatus.SUCCESS);
                    step.setOutput(result.getOutput());
                    lastOutput = result.getOutput();
                } else if (result.isSkipped()) {
                    // 步骤被跳过（条件不满足）
                    step.setStatus(StepStatus.SKIPPED);
                    // 跳过的步骤不影响执行流程，继续执行下一个步骤
                } else if (result.isAwaiting()) {
                    // 再次遇到 await step
                    step.setStatus(StepStatus.AWAITING);
                    long totalDuration = System.currentTimeMillis() - startTime;

                    AwaitRequest newAwaitRequest = (AwaitRequest) result.getOutput();
                    ExecutionContextSnapshot newSnapshot = ExecutionContextSnapshot.createActive(
                        context,
                        skill.getId(),
                        stepIndex,
                        newAwaitRequest
                    );
                    executionStore.save(newSnapshot);

                    return SkillResult.awaiting(
                        context.getExecutionId(),
                        newAwaitRequest,
                        stepResults,
                        totalDuration
                    );
                } else {
                    step.setStatus(StepStatus.FAILED);
                    success = false;
                    error = "Step '" + step.getName() + "' failed: " + result.getError();
                    markRemainingStepsSkipped(steps, step, stepResults);
                    break;
                }
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            SkillResult skillResult;

            if (success) {
                // 从执行上下文中根据 output_schema 构建输出
                Object finalOutput = buildOutputFromContext(context, skill.getOutputContract());

                // 执行输出契约校验
                OutputValidationResult validationResult = validateOutput(finalOutput, skill.getOutputContract());
                if (validationResult.isFailed()) {
                    skillResult = SkillResult.failure(validationResult.getErrorMessage(), stepResults, totalDuration);
                } else {
                    skillResult = SkillResult.success(finalOutput, stepResults, totalDuration);
                }
            } else {
                // 执行失败时直接返回错误，不需要构建输出
                skillResult = SkillResult.failure(error, stepResults, totalDuration);
            }

            // 通知监听器 Skill 完成
            if (listener != null) {
                listener.onSkillComplete(skill, skillResult);
            }

            return skillResult;

        } catch (SkillExecutionException e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            SkillResult skillResult = SkillResult.failure(e.getMessage(), stepResults, totalDuration);
            if (listener != null) {
                listener.onSkillComplete(skill, skillResult);
            }
            return skillResult;
        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            SkillResult skillResult = SkillResult.failure("Unexpected error: " + e.getMessage(), stepResults, totalDuration);
            if (listener != null) {
                listener.onSkillComplete(skill, skillResult);
            }
            return skillResult;
        }
    }

    /**
     * 执行单个 Step。
     *
     * @param step 要执行的 Step
     * @param context 执行上下文
     * @return Step 执行结果
     * @throws SkillExecutionException 执行失败时抛出
     */
    private StepResult executeStep(Step step, ExecutionContext context) throws SkillExecutionException {
        String stepName = step.getName();
        StepType stepType = step.getType();

        // 检查 when 条件
        if (step.hasWhenCondition()) {
            WhenCondition whenCondition = step.getWhenCondition();
            boolean conditionMet = conditionEvaluator.evaluate(
                whenCondition.getParsedExpression(), context);

            if (!conditionMet) {
                // 条件不满足时，跳过步骤
                step.setStatus(StepStatus.SKIPPED);
                String skipReason = "条件不满足: " + whenCondition.getRawExpression();
                return StepResult.skipped(stepName, skipReason);
            }
        }

        // 设置状态为 RUNNING
        step.setStatus(StepStatus.RUNNING);

        // 查找对应的 Step 执行器
        StepExecutor executor = stepExecutors.get(stepType);
        if (executor == null) {
            long duration = 0;
            String error = "No executor found for step type: " + stepType;
            step.setStatus(StepStatus.FAILED);
            return StepResult.failed(stepName, error, duration);
        }

        // 执行 Step
        return executor.execute(step, context);
    }

    /**
     * 输出当前上下文中所有变量的 debug 日志。
     *
     * @param stepName 刚执行完的步骤名称
     * @param context 执行上下文
     */
    private void logContextVariables(String stepName, ExecutionContext context) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        Map<String, Object> variableContext = context.buildVariableContext();
        StringBuilder sb = new StringBuilder();
        sb.append("[上下文变量快照] 步骤 '").append(stepName).append("' 执行后，当前所有变量:\n");
        for (Map.Entry<String, Object> entry : variableContext.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String valueStr;
            if (value == null) {
                valueStr = "null";
            } else {
                String raw = value.toString();
                valueStr = raw.length() > 200 ? raw.substring(0, 200) + "...(truncated)" : raw;
            }
            sb.append("  ").append(key).append(" = ").append(valueStr)
              .append("  [").append(value != null ? value.getClass().getSimpleName() : "null").append("]\n");
        }
        logger.debug(sb.toString());
    }

    /**
     * 标记剩余 Step 为 SKIPPED。
     *
     * @param allSteps 所有 Step 列表
     * @param failedStep 失败的 Step
     * @param stepResults 结果列表（用于添加跳过结果）
     */
    private void markRemainingStepsSkipped(List<Step> allSteps, Step failedStep,
                                           List<StepResult> stepResults) {
        boolean foundFailed = false;
        for (Step step : allSteps) {
            if (step == failedStep) {
                foundFailed = true;
                continue;
            }
            if (foundFailed) {
                step.setStatus(StepStatus.SKIPPED);
                stepResults.add(StepResult.skipped(step.getName()));
            }
        }
    }

    /**
     * 校验输出是否符合契约。
     *
     * @param output 执行输出
     * @param contract 输出契约（可为 null）
     * @return 校验结果
     */
    private OutputValidationResult validateOutput(Object output, OutputContract contract) {
        if (contract == null || contract.isEmpty()) {
            return OutputValidationResult.success();
        }
        return outputValidator.validate(output, contract);
    }

    /**
     * 从执行上下文中根据 output_schema 构建输出。
     *
     * <p>从执行上下文的所有步骤输出中提取 output_schema 定义的字段，
     * 如果某个字段在上下文中找不到，则使用 null。</p>
     *
     * @param context 执行上下文
     * @param contract 输出契约（可为 null）
     * @return 构建的输出对象
     */
    private Object buildOutputFromContext(ExecutionContext context, OutputContract contract) {
        // 如果没有定义 output_schema，返回空 Map
        if (contract == null || contract.isEmpty()) {
            return new HashMap<String, Object>();
        }

        // 构建变量上下文（包含所有步骤的输出）
        Map<String, Object> variableContext = context.buildVariableContext();

        // 根据 output_schema 提取字段
        Map<String, Object> output = new HashMap<String, Object>();

        if (contract.getFields() != null) {
            for (String fieldName : contract.getFields().keySet()) {
                // 从上下文中查找字段值
                Object value = findValueInContext(fieldName, variableContext);
                output.put(fieldName, value);
            }
        }

        return output;
    }

    /**
     * 在变量上下文中查找字段值。
     *
     * <p>支持两种查找方式：</p>
     * <ul>
     *   <li>直接字段名：fieldName</li>
     *   <li>嵌套字段：stepName.fieldName</li>
     * </ul>
     *
     * @param fieldName 字段名
     * @param variableContext 变量上下文
     * @return 字段值，找不到返回 null
     */
    private Object findValueInContext(String fieldName, Map<String, Object> variableContext) {
        return variableContext.get(fieldName);
    }

    /**
     * 获取已注册的 Step 执行器数量。
     *
     * @return 执行器数量
     */
    public int getRegisteredExecutorCount() {
        return stepExecutors.size();
    }

    /**
     * 检查是否有指定类型的 Step 执行器。
     *
     * @param type Step 类型
     * @return 是否已注册
     */
    public boolean hasExecutor(StepType type) {
        return stepExecutors.containsKey(type);
    }

    /**
     * 设置执行监听器。
     *
     * @param listener 执行监听器（可为 null 以移除）
     */
    public void setExecutionListener(ExecutionListener listener) {
        this.listener = listener;
    }

    /**
     * 获取当前执行监听器。
     *
     * @return 执行监听器
     */
    public ExecutionListener getExecutionListener() {
        return listener;
    }

    /**
     * 获取执行存储。
     *
     * @return ExecutionStore
     */
    public ExecutionStore getExecutionStore() {
        return executionStore;
    }
}
