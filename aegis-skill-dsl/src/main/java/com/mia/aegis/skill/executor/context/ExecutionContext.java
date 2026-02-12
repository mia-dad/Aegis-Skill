package com.mia.aegis.skill.executor.context;

import com.mia.aegis.skill.tools.ToolOutputContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 执行上下文。
 *
 * <p>管理 Skill 执行过程中的输入参数、Step 执行结果和运行时信息。</p>
 */
public class ExecutionContext implements ToolOutputContext {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionContext.class);

    private final String executionId;
    private final Map<String, Object> input;
    private final Map<String, StepResult> stepResults;
    private final Map<String, Object> metadata;
    private final RuntimeInfo runtime;
    private final Map<String, Object> awaitInputs;
    /** stepName → varName 映射，用于将步骤输出以变量别名存入上下文 */
    private final Map<String, String> varNameMappings;
    /** Tool 步骤通过 ToolOutputContext.put() 直接写入的变量 */
    private final Map<String, Object> toolVariables;

    /**
     * 创建执行上下文。
     *
     * @param input 用户输入参数
     */
    public ExecutionContext(Map<String, Object> input) {
        this.executionId = ExecutionIdGenerator.generate();
        this.input = input != null
                ? Collections.unmodifiableMap(new HashMap<String, Object>(input))
                : Collections.<String, Object>emptyMap();
        this.stepResults = new LinkedHashMap<String, StepResult>();
        this.metadata = new HashMap<String, Object>();
        this.runtime = RuntimeInfo.now();
        this.awaitInputs = new HashMap<String, Object>();
        this.varNameMappings = new HashMap<String, String>();
        this.toolVariables = new LinkedHashMap<String, Object>();

        logger.debug("创建执行上下文 - executionId: {}, inputKeys: {}",
            executionId, this.input.keySet());
    }

    /**
     * 创建执行上下文（用于恢复快照）。
     *
     * @param input 用户输入参数
     * @param existingStepResults 已完成的 step 结果
     * @param existingAwaitInputs 已收集的 await 输入
     * @param executionId 执行ID
     */
    ExecutionContext(Map<String, Object> input,
                     Map<String, StepResult> existingStepResults,
                     Map<String, Object> existingAwaitInputs,
                     String executionId) {
        this.executionId = executionId;
        this.input = input != null
                ? Collections.unmodifiableMap(new HashMap<String, Object>(input))
                : Collections.<String, Object>emptyMap();
        this.stepResults = existingStepResults != null
                ? new LinkedHashMap<String, StepResult>(existingStepResults)
                : new LinkedHashMap<String, StepResult>();
        this.metadata = new HashMap<String, Object>();
        this.runtime = RuntimeInfo.now();
        this.awaitInputs = existingAwaitInputs != null
                ? new HashMap<String, Object>(existingAwaitInputs)
                : new HashMap<String, Object>();
        this.varNameMappings = new HashMap<String, String>();
        this.toolVariables = new LinkedHashMap<String, Object>();
    }

    /**
     * 创建空的执行上下文。
     *
     * @return ExecutionContext 实例
     */
    public static ExecutionContext empty() {
        return new ExecutionContext(null);
    }

    /**
     * 创建用于恢复执行的上下文。
     *
     * <p>用于从暂停状态恢复执行时重建上下文。</p>
     *
     * @param input 用户输入参数
     * @param existingStepResults 已完成的 step 结果
     * @param existingAwaitInputs 已收集的 await 输入
     * @param executionId 执行ID
     * @return ExecutionContext 实例
     */
    public static ExecutionContext forResume(Map<String, Object> input,
                                           Map<String, StepResult> existingStepResults,
                                           Map<String, Object> existingAwaitInputs,
                                           String executionId) {
        return new ExecutionContext(input, existingStepResults, existingAwaitInputs, executionId);
    }

    /**
     * 获取执行ID。
     *
     * @return 执行ID
     */
    public String getExecutionId() {
        return executionId;
    }

    /**
     * 获取用户输入参数。
     *
     * @return 不可变的输入参数映射
     */
    public Map<String, Object> getInput() {
        return input;
    }

    /**
     * 获取输入参数值。
     *
     * @param key 参数名
     * @return 参数值
     */
    public Object getInputValue(String key) {
        return input.get(key);
    }

    /**
     * 获取输入参数值（带默认值）。
     *
     * @param key 参数名
     * @param defaultValue 默认值
     * @return 参数值或默认值
     */
    public Object getInputValue(String key, Object defaultValue) {
        Object value = input.get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取所有 Step 执行结果。
     *
     * @return 不可变的 Step 结果映射
     */
    public Map<String, StepResult> getStepResults() {
        return Collections.unmodifiableMap(stepResults);
    }

    /**
     * 获取指定 Step 的执行结果。
     *
     * @param stepName Step 名称
     * @return Step 执行结果，不存在返回 null
     */
    public StepResult getStepResult(String stepName) {
        return stepResults.get(stepName);
    }

    /**
     * 获取指定 Step 的输出。
     *
     * @param stepName Step 名称
     * @return Step 输出，不存在或未成功返回 null
     */
    public Object getStepOutput(String stepName) {
        StepResult result = stepResults.get(stepName);
        return result != null && result.isSuccess() ? result.getOutput() : null;
    }

    /**
     * 通过变量名（包括 varName 别名）获取步骤输出。
     *
     * <p>查找顺序：
     * <ol>
     *   <li>直接按步骤名查找</li>
     *   <li>反向查找 varName 映射（name 作为 varName，找到对应的 stepName）</li>
     * </ol>
     *
     * @param name 步骤名或变量别名
     * @return 步骤输出，找不到返回 null
     */
    public Object getOutputByVarName(String name) {
        // 1. 直接按步骤名查找
        Object directOutput = getStepOutput(name);
        if (directOutput != null) {
            return directOutput;
        }

        // 2. 反向查找：name 是某个步骤的 varName
        for (Map.Entry<String, String> entry : varNameMappings.entrySet()) {
            if (entry.getValue().equals(name)) {
                return getStepOutput(entry.getKey());
            }
        }

        return null;
    }

    /**
     * 添加 Step 执行结果。
     *
     * @param result Step 执行结果
     */
    public void addStepResult(StepResult result) {
        stepResults.put(result.getStepName(), result);

        logger.debug("添加步骤结果 - executionId: {}, step: {}, status: {}, duration: {}ms",
            executionId, result.getStepName(), result.getStatus(), result.getDuration());
    }

    /**
     * 检查 Step 是否已执行。
     *
     * @param stepName Step 名称
     * @return 是否已执行
     */
    public boolean hasStepResult(String stepName) {
        return stepResults.containsKey(stepName);
    }

    /**
     * 获取元数据。
     *
     * @return 不可变的元数据映射
     */
    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * 设置元数据。
     *
     * @param key 键
     * @param value 值
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * 获取运行时信息。
     *
     * @return 运行时信息
     */
    public RuntimeInfo getRuntime() {
        return runtime;
    }

    /**
     * 获取所有 await 收集的用户输入。
     *
     * @return 不可变的 await 输入映射
     */
    public Map<String, Object> getAwaitInputs() {
        return Collections.unmodifiableMap(awaitInputs);
    }

    /**
     * 获取指定 await step 收集的用户输入。
     *
     * @param stepName await step 名称
     * @return 用户输入数据，不存在返回 null
     */
    public Object getAwaitInput(String stepName) {
        return awaitInputs.get(stepName);
    }

    /**
     * 添加 await step 收集的用户输入。
     *
     * <p>在恢复执行时调用，将用户输入注入上下文。</p>
     *
     * @param stepName await step 名称
     * @param userInput 用户提供的输入数据
     */
    public void addAwaitInput(String stepName, Map<String, Object> userInput) {
        awaitInputs.put(stepName, userInput);

        logger.info("注入 await 输入 - executionId: {}, step: {}, fields: {}",
            executionId, stepName, userInput.keySet());
    }

    /**
     * 检查是否有指定 await step 的输入。
     *
     * @param stepName await step 名称
     * @return 是否存在输入
     */
    public boolean hasAwaitInput(String stepName) {
        return awaitInputs.containsKey(stepName);
    }

    /**
     * 注册步骤的变量别名映射。
     *
     * <p>设置后，该步骤的输出将以 varName 为键直接存入变量上下文，
     * 而不使用 StepOutputWrapper 包装。</p>
     *
     * @param stepName 步骤名称
     * @param varName 变量别名
     */
    public void registerVarName(String stepName, String varName) {
        varNameMappings.put(stepName, varName);
        logger.debug("注册变量别名 - stepName: {}, varName: {}", stepName, varName);
    }

    /**
     * Tool 步骤通过此方法将输出变量直接写入上下文。
     *
     * <p>实现 {@link ToolOutputContext} 接口。</p>
     *
     * @param key   变量名
     * @param value 变量值
     */
    @Override
    public void put(String key, Object value) {
        toolVariables.put(key, value);
        logger.debug("Tool 写入变量 - key: {}, value type: {}", key,
            value != null ? value.getClass().getSimpleName() : "null");
    }

    /**
     * 获取 Tool 步骤直接写入的所有变量。
     *
     * @return 不可变的 tool 变量映射
     */
    public Map<String, Object> getToolVariables() {
        return Collections.unmodifiableMap(toolVariables);
    }

    /**
     * 构建变量上下文用于模板渲染。
     *
     * <p>合并输入参数、Step 输出和 await 收集的用户输入，
     * 支持 {{variable}}、{{step.value}} 和 {{awaitStepName.fieldName}} 语法。</p>
     *
     * @return 变量上下文
     */
    public Map<String, Object> buildVariableContext() {
        logger.debug("构建变量上下文 - executionId: {}, variables: {}",
            executionId, input.keySet());

        Map<String, Object> context = new HashMap<String, Object>(input);

        // 将 await 输入平铺到全局变量（覆盖 input）
        for (Map.Entry<String, Object> entry : awaitInputs.entrySet()) {
            Object userInput = entry.getValue();
            if (userInput instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) userInput;
                context.putAll(map);
            }
        }

        // 添加 Step 输出
        for (Map.Entry<String, StepResult> entry : stepResults.entrySet()) {
            String stepName = entry.getKey();
            StepResult result = entry.getValue();
            if (result.isSuccess()) {
                Object output = result.getOutput();
                logger.debug("[buildVariableContext] Step: {}, 输出类型: {}, 值: {}",
                        stepName, output != null ? output.getClass().getName() : "null", output);

                String varName = varNameMappings.get(stepName);
                if (varName != null) {
                    // 有 varName：直接以 varName 为键存入，不包装 StepOutputWrapper
                    context.put(varName, output);
                    logger.debug("[buildVariableContext] 使用 varName '{}' 替代 stepName '{}'", varName, stepName);
                } else {
                    // 无 varName：保持原逻辑，使用 StepOutputWrapper 包装
                    context.put(stepName, new StepOutputWrapper(output));
                }
            }
        }

        // 添加 Tool 步骤直接写入的变量（覆盖同名 step 输出）
        context.putAll(toolVariables);

        // 添加 context 命名空间
        Map<String, Object> contextNamespace = new HashMap<String, Object>();
        contextNamespace.put("startTime", runtime.getStartTime());
        contextNamespace.put("elapsed", runtime.getElapsedTime());
        contextNamespace.putAll(metadata);
        context.put("context", contextNamespace);

        return context;
    }

    /**
     * Step 输出包装器，用于支持 {{step.value}} 语法。
     */
    public static class StepOutputWrapper {
        private final Object value;

        public StepOutputWrapper(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value != null ? value.toString() : "";
        }
    }

    @Override
    public String toString() {
        return "ExecutionContext{" +
                "executionId='" + executionId + '\'' +
                ", inputKeys=" + input.keySet() +
                ", completedSteps=" + stepResults.size() +
                ", awaitInputs=" + awaitInputs.size() +
                ", runtime=" + runtime +
                '}';
    }
}

