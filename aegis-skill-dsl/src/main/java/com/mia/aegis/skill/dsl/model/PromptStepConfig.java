package com.mia.aegis.skill.dsl.model;

import com.mia.aegis.skill.i18n.MessageUtil;

/**
 * Prompt 类型 Step 的配置。
 *
 * <p>包含 Prompt 模板内容。</p>
 */
public class PromptStepConfig implements StepConfig {

    private final String template;

    /**
     * 创建 Prompt Step 配置。
     *
     * @param template Prompt 模板（支持 {{var}} 和 {{step.value}} 语法）
     */
    public PromptStepConfig(String template) {
        if (template == null || template.trim().isEmpty()) {
            throw new IllegalArgumentException(MessageUtil.getMessage("promptstepconfig.template.null"));
        }
        this.template = template;
    }

    /**
     * 获取 Prompt 模板。
     *
     * @return Prompt 模板字符串
     */
    public String getTemplate() {
        return template;
    }

    @Override
    public StepType getStepType() {
        return StepType.PROMPT;
    }

    @Override
    public String toString() {
        return "PromptStepConfig{" +
                "template='" + (template.length() > 50 ? template.substring(0, 50) + "..." : template) + '\'' +
                '}';
    }
}

