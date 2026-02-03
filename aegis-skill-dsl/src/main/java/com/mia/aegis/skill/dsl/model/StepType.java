package com.mia.aegis.skill.dsl.model;

import com.mia.aegis.skill.i18n.MessageUtil;

/**
 * Step 类型枚举。
 *
 * <p>定义 Skill 中 Step 的三种执行类型：</p>
 * <ul>
 *   <li>{@link #TOOL} - 调用外部 Tool（HTTP API、数据库、Java Service）</li>
 *   <li>{@link #PROMPT} - 调用 LLM 模型生成响应</li>
 *   <li>{@link #COMPOSE} - 组合多个前置 Step 的输出</li>
 * </ul>
 */
public enum StepType {

    /**
     * Tool 类型 Step。
     * 调用已注册的外部 Tool，获取结构化数据。
     */
    TOOL,

    /**
     * Prompt 类型 Step。
     * 将 Prompt 模板发送至 LLM Adapter，获取生成结果。
     */
    PROMPT,

    /**
     * Compose 类型 Step。
     * 将多个前置 Step 的输出组合为单一结构化输出。
     */
    COMPOSE;

    /**
     * 从字符串解析 StepType。
     *
     * @param value 类型字符串（不区分大小写）
     * @return 对应的 StepType
     * @throws IllegalArgumentException 如果类型字符串无效
     */
    public static StepType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(MessageUtil.getMessage("steptype.value.null"));
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(MessageUtil.getMessage("steptype.value.unknown", value));
        }
    }
}

