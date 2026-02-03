package com.mia.aegis.skill.executor.step;



import com.mia.aegis.skill.dsl.model.Step;
import com.mia.aegis.skill.dsl.model.StepType;
import com.mia.aegis.skill.dsl.model.ToolStepConfig;
import com.mia.aegis.skill.exception.SkillExecutionException;
import com.mia.aegis.skill.exception.TemplateRenderException;
import com.mia.aegis.skill.exception.ToolExecutionException;
import com.mia.aegis.skill.executor.context.ExecutionContext;
import com.mia.aegis.skill.executor.context.StepResult;
import com.mia.aegis.skill.i18n.MessageUtil;
import com.mia.aegis.skill.template.TemplateRenderer;
import com.mia.aegis.skill.tools.ToolProvider;
import com.mia.aegis.skill.tools.ToolRegistry;
import com.mia.aegis.skill.tools.ValidationResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tool 类型 Step 执行器。
 *
 * <p>负责执行 Tool 类型的 Step：
 * <ol>
 *   <li>查找 Tool Provider</li>
 *   <li>渲染输入模板</li>
 *   <li>验证输入参数</li>
 *   <li>执行 Tool 并返回结果</li>
 * </ol>
 * </p>
 */
public class ToolStepExecutor implements StepExecutor {

    private final ToolRegistry toolRegistry;


    private final TemplateRenderer templateRenderer;

    /**
     * 创建 ToolStepExecutor。
     *
     * @param toolRegistry Tool 注册表
     * @param templateRenderer 模板渲染器
     */
    public ToolStepExecutor(ToolRegistry toolRegistry, TemplateRenderer templateRenderer) {
        if (toolRegistry == null) {
            throw new IllegalArgumentException(MessageUtil.getMessage("toolstepexecutor.registry.null"));
        }
        if (templateRenderer == null) {
            throw new IllegalArgumentException(MessageUtil.getMessage("toolstepexecutor.renderer.null"));
        }
        this.toolRegistry = toolRegistry;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public StepResult execute(Step step, ExecutionContext context) throws SkillExecutionException {
        if (!supports(step)) {
            throw new SkillExecutionException(step.getName(),
                    new IllegalArgumentException("不支持的工具类型,目前仅支持TOOL类型"));
        }

        long startTime = System.currentTimeMillis();
        String stepName = step.getName();

        try {
            ToolStepConfig config = step.getToolConfig();
            String toolName = config.getToolName();

            // 1. 查找 Tool Provider
            Optional<ToolProvider> optionalTool = toolRegistry.find(toolName);
            if (!optionalTool.isPresent()) {
                throw new SkillExecutionException(stepName,
                        new IllegalStateException("工具执行时,没有发现: " + toolName)+"此工具服务");
            }
            ToolProvider tool = optionalTool.get();

            // 2. 渲染输入模板
            Map<String, Object> renderedInput = renderInputTemplate(config.getInputTemplate(), context);

            // 3. 验证输入参数
            ValidationResult validationResult = tool.validateInput(renderedInput);
            if (!validationResult.isValid()) {
                throw new SkillExecutionException(stepName,
                        new IllegalArgumentException("工具执行时,入参校验失败: " +
                                String.join(", ", validationResult.getErrors())));
            }

            // 4. 执行 Tool
            Object output = tool.execute(renderedInput);

            long duration = System.currentTimeMillis() - startTime;
            return StepResult.success(stepName, output, duration);

        } catch (ToolExecutionException e) {
            long duration = System.currentTimeMillis() - startTime;
            return StepResult.failed(stepName, "工具执行时,发生异常: " + e.getMessage(), duration);
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
        return step != null && step.getType() == StepType.TOOL;
    }

    /**
     * 渲染输入模板。
     *
     * @param inputTemplate 输入模板（可能包含 {{variable}} 语法）
     * @param context 执行上下文
     * @return 渲染后的输入参数
     * @throws TemplateRenderException 渲染失败时抛出
     */
    private Map<String, Object> renderInputTemplate(Map<String, String> inputTemplate,
                                                    ExecutionContext context) throws TemplateRenderException {
        if (inputTemplate == null || inputTemplate.isEmpty()) {
            return new HashMap<String, Object>();
        }

        Map<String, Object> variableContext = context.buildVariableContext();
        Map<String, Object> renderedInput = new HashMap<String, Object>();

        for (Map.Entry<String, String> entry : inputTemplate.entrySet()) {
            String key = entry.getKey();
            String template = entry.getValue();

            if (template == null) {
                renderedInput.put(key, null);
            } else if (containsTemplate(template)) {
                // 模板渲染
                String rendered = templateRenderer.render(template, variableContext);
                renderedInput.put(key, parseValue(rendered));
            } else {
                // 直接使用值
                renderedInput.put(key, parseValue(template));
            }
        }

        return renderedInput;
    }

    /**
     * 检查字符串是否包含模板语法。
     *
     * @param value 要检查的字符串
     * @return 是否包含模板语法
     */
    private boolean containsTemplate(String value) {
        return value != null && value.contains("{{") && value.contains("}}");
    }

    /**
     * 解析值，尝试转换为适当的类型。
     *
     * @param value 字符串值
     * @return 解析后的值
     */
    private Object parseValue(String value) {
        if (value == null) {
            return null;
        }

        // 尝试解析为数字
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            // 不是数字，继续
        }

        // 布尔值
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }

        // 默认返回字符串
        return value;
    }
}

