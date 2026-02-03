package com.mia.aegis.skill.dsl.model;


/**
 * Step 配置接口。
 *
 * <p>作为不同类型 Step 配置的公共接口。</p>
 *
 * @see ToolStepConfig
 * @see PromptStepConfig
 * @see ComposeStepConfig
 */
public interface StepConfig {

    /**
     * 获取此配置对应的 Step 类型。
     *
     * @return Step 类型
     */
    StepType getStepType();
}

