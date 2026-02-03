package com.mia.aegis.skill.executor.step;



import com.mia.aegis.skill.dsl.model.PromptStepConfig;
import com.mia.aegis.skill.dsl.model.Step;
import com.mia.aegis.skill.dsl.model.StepType;
import com.mia.aegis.skill.exception.LLMInvocationException;
import com.mia.aegis.skill.exception.SkillExecutionException;
import com.mia.aegis.skill.exception.TemplateRenderException;
import com.mia.aegis.skill.executor.context.ExecutionContext;
import com.mia.aegis.skill.executor.context.StepResult;
import com.mia.aegis.skill.i18n.MessageUtil;
import com.mia.aegis.skill.llm.LLMAdapter;
import com.mia.aegis.skill.llm.LLMAdapterRegistry;
import com.mia.aegis.skill.template.TemplateRenderer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Prompt 类型 Step 执行器。
 *
 * <p>负责执行 Prompt 类型的 Step：
 * <ol>
 *   <li>渲染 Prompt 模板（包含跨 Step 变量引用）</li>
 *   <li>查找 LLM Adapter</li>
 *   <li>调用 LLM 并返回结果</li>
 * </ol>
 * </p>
 */
public class PromptStepExecutor implements StepExecutor {

    private final LLMAdapterRegistry llmRegistry;

    private final TemplateRenderer templateRenderer;

    private final Map<String, Object> defaultOptions;

    /**
     * 创建 PromptStepExecutor。
     *
     * @param llmRegistry LLM Adapter 注册表
     * @param templateRenderer 模板渲染器
     */
    public PromptStepExecutor(LLMAdapterRegistry llmRegistry, TemplateRenderer templateRenderer) {
        if (llmRegistry == null) {
            throw new IllegalArgumentException(MessageUtil.getMessage("promptstepexecutor.llmregistry.null"));
        }
        if (templateRenderer == null) {
            throw new IllegalArgumentException(MessageUtil.getMessage("promptstepexecutor.renderer.null"));
        }
        this.llmRegistry = llmRegistry;
        this.templateRenderer = templateRenderer;
        this.defaultOptions = new HashMap<String, Object>();
    }

    /**
     * 创建 PromptStepExecutor（带默认选项）。
     *
     * @param llmRegistry LLM Adapter 注册表
     * @param templateRenderer 模板渲染器
     * @param defaultOptions 默认 LLM 调用选项
     */
    public PromptStepExecutor(LLMAdapterRegistry llmRegistry, TemplateRenderer templateRenderer,
                              Map<String, Object> defaultOptions) {
        this(llmRegistry, templateRenderer);
        if (defaultOptions != null) {
            this.defaultOptions.putAll(defaultOptions);
        }
    }

    @Override
    public StepResult execute(Step step, ExecutionContext context) throws SkillExecutionException {
        if (!supports(step)) {
            throw new SkillExecutionException(step.getName(),
                    new IllegalArgumentException("PromptStepExecutor only supports PROMPT type steps"));
        }

        long startTime = System.currentTimeMillis();
        String stepName = step.getName();

        try {
            PromptStepConfig config = step.getPromptConfig();
            String template = config.getTemplate();

            // 1. 渲染 Prompt 模板（支持 {{variable}} 和 {{step.output}} 语法）
            String renderedPrompt = renderPromptTemplate(template, context);

            // 2. 查找 LLM Adapter
            Optional<LLMAdapter> optionalAdapter = llmRegistry.getDefault();
            if (!optionalAdapter.isPresent()) {
                throw new SkillExecutionException(stepName,
                        new IllegalStateException("大模型实例不存在"));
            }
            LLMAdapter adapter = optionalAdapter.get();

            // 3. 调用 LLM
            Map<String, Object> options = buildOptions(context);
            String response = adapter.invoke(renderedPrompt, options);

            // 处理空响应
            if (response == null || response.trim().isEmpty()) {
                long duration = System.currentTimeMillis() - startTime;
                return StepResult.failed(stepName, "LLM returned empty response", duration);
            }

            long duration = System.currentTimeMillis() - startTime;
            return StepResult.success(stepName, response, duration);

        } catch (LLMInvocationException e) {
            long duration = System.currentTimeMillis() - startTime;
            return StepResult.failed(stepName, "LLM invocation failed: " + e.getMessage(), duration);
        } catch (TemplateRenderException e) {
            long duration = System.currentTimeMillis() - startTime;
            throw new SkillExecutionException(stepName, e);
        } catch (SkillExecutionException e) {
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            throw new SkillExecutionException(stepName, e);
        }
    }

    @Override
    public boolean supports(Step step) {
        return step != null && step.getType() == StepType.PROMPT;
    }

    /**
     * 渲染 Prompt 模板。
     *
     * <p>支持以下变量引用：
     * <ul>
     *   <li>{@code {{variable}}} - 引用输入参数</li>
     *   <li>{@code {{step_name.output}}} - 引用前置 Step 输出</li>
     *   <li>{@code {{context.key}}} - 引用运行时上下文</li>
     * </ul>
     * </p>
     *
     * @param template Prompt 模板
     * @param context 执行上下文
     * @return 渲染后的 Prompt
     * @throws TemplateRenderException 渲染失败时抛出
     */
    private String renderPromptTemplate(String template, ExecutionContext context) throws TemplateRenderException {
        Map<String, Object> variableContext = context.buildVariableContext();
        return templateRenderer.render(template, variableContext);
    }

    /**
     * 构建 LLM 调用选项。
     *
     * @param context 执行上下文
     * @return 调用选项
     */
    private Map<String, Object> buildOptions(ExecutionContext context) {
        Map<String, Object> options = new HashMap<String, Object>(defaultOptions);

        // 可以从 context 的 metadata 中读取特定选项
        Map<String, Object> metadata = context.getMetadata();
        if (metadata.containsKey("llm_temperature")) {
            options.put("temperature", metadata.get("llm_temperature"));
        }
        if (metadata.containsKey("llm_max_tokens")) {
            options.put("max_tokens", metadata.get("llm_max_tokens"));
        }
        if (metadata.containsKey("llm_model")) {
            options.put("model", metadata.get("llm_model"));
        }

        return options;
    }

    /**
     * 设置默认 LLM 调用选项。
     *
     * @param key 选项键
     * @param value 选项值
     */
    public void setDefaultOption(String key, Object value) {
        defaultOptions.put(key, value);
    }

    /**
     * 获取默认 LLM 调用选项。
     *
     * @return 默认选项映射
     */
    public Map<String, Object> getDefaultOptions() {
        return new HashMap<String, Object>(defaultOptions);
    }
}

