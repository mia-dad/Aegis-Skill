package com.mia.aegis.skill.executor.step;

import com.mia.aegis.skill.dsl.model.Step;
import com.mia.aegis.skill.dsl.model.StepType;
import com.mia.aegis.skill.dsl.model.TemplateStepConfig;
import com.mia.aegis.skill.exception.SkillExecutionException;
import com.mia.aegis.skill.exception.TemplateRenderException;
import com.mia.aegis.skill.executor.context.ExecutionContext;
import com.mia.aegis.skill.executor.context.StepResult;
import com.mia.aegis.skill.template.TemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Template 类型 Step 执行器。
 *
 * <p>负责执行 Template 类型的 Step：纯文本模板渲染，
 * 做变量替换、表达式求值和循环渲染，不调用 LLM 也不调用 Tool。
 * 渲染结果直接作为 step 输出。</p>
 */
public class TemplateStepExecutor implements StepExecutor {

    private static final Logger logger = LoggerFactory.getLogger(TemplateStepExecutor.class);

    private final TemplateRenderer templateRenderer;

    /**
     * 创建 TemplateStepExecutor。
     *
     * @param templateRenderer 模板渲染器
     */
    public TemplateStepExecutor(TemplateRenderer templateRenderer) {
        if (templateRenderer == null) {
            throw new IllegalArgumentException("TemplateRenderer cannot be null");
        }
        this.templateRenderer = templateRenderer;
    }

    @Override
    public StepResult execute(Step step, ExecutionContext context) throws SkillExecutionException {
        if (!supports(step)) {
            throw new SkillExecutionException("TemplateStepExecutor only supports TEMPLATE type steps");
        }

        long startTime = System.currentTimeMillis();
        String stepName = step.getName();

        try {
            TemplateStepConfig config = step.getTemplateConfig();
            String template = config.getTemplate();

            logger.info("===== [Template Step 开始执行] =====");
            logger.info("Step 名称: {}", stepName);
            logger.debug("模板内容: {}", template);

            // 构建变量上下文并渲染模板
            Map<String, Object> variableContext = context.buildVariableContext();
            String rendered = templateRenderer.render(template, variableContext);

            logger.info("模板渲染成功，输出长度: {} 字符", rendered.length());
            logger.debug("渲染结果: {}", rendered);
            logger.info("===== [Template Step 执行完成] =====");

            long duration = System.currentTimeMillis() - startTime;
            return StepResult.success(stepName, rendered, duration);

        } catch (TemplateRenderException e) {
            logger.error("模板渲染失败: {}", e.getMessage(), e);
            long duration = System.currentTimeMillis() - startTime;
            throw new SkillExecutionException("Template render failed: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Template Step 执行异常: {}", e.getMessage(), e);
            long duration = System.currentTimeMillis() - startTime;
            throw new SkillExecutionException("Failed to execute template step: " + stepName, e);
        }
    }

    @Override
    public boolean supports(Step step) {
        return step != null && step.getType() == StepType.TEMPLATE;
    }
}
