package com.mia.aegis.skill.dsl.model.io;

/**
 * 输出格式枚举。
 *
 * <p>定义 Skill 输出契约支持的格式类型。</p>
 */
public enum OutputFormat {

    /**
     * JSON 格式输出。
     * 输出必须是有效的 JSON 对象或数组。
     */
    JSON,

    /**
     * 纯文本格式输出。
     * 输出为普通字符串，不做结构化校验。
     */
    TEXT;

    /**
     * 从字符串解析 OutputFormat。
     *
     * @param value 格式字符串（不区分大小写）
     * @return 对应的 OutputFormat，默认返回 JSON
     */
    public static OutputFormat fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return JSON; // 默认 JSON 格式
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return JSON; // 无法识别时默认 JSON
        }
    }
}

