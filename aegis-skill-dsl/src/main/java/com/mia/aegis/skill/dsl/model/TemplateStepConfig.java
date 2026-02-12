package com.mia.aegis.skill.dsl.model;

import com.mia.aegis.skill.i18n.MessageUtil;

/**
 * Template 类型 Step 的配置。
 *
 * <p>包含纯文本模板内容，支持变量替换、表达式求值和循环渲染。
 * 不调用 LLM 也不调用 Tool，渲染结果直接作为 step 输出。</p>
 */
public class TemplateStepConfig implements StepConfig {

    private final String template;

    /**
     * 创建 Template Step 配置。
     *
     * @param template 模板内容（支持 {{var}} 和 {{step.value}} 语法）
     */
    public TemplateStepConfig(String template) {
        if (template == null || template.trim().isEmpty()) {
            throw new IllegalArgumentException(MessageUtil.getMessage("templatestepconfig.template.null"));
        }
        this.template = template;
    }

    /**
     * 获取模板内容。
     *
     * @return 模板字符串
     */
    public String getTemplate() {
        return template;
    }

    @Override
    public StepType getStepType() {
        return StepType.TEMPLATE;
    }

    @Override
    public String toString() {
        return "TemplateStepConfig{" +
                "template='" + (template.length() > 50 ? template.substring(0, 50) + "..." : template) + '\'' +
                '}';
    }
}
