package com.mia.aegis.skill.dsl.model;

/**
 * 引用文件类型枚举。
 *
 * <p>用于标识 Reference 指令引用的文件类型，影响内容解析方式。</p>
 */
public enum FileType {
    /**
     * Markdown 文件 (.md)
     */
    MARKDOWN("md"),

    /**
     * 纯文本文件 (.txt)
     */
    TEXT("txt"),

    /**
     * JSON 文件 (.json)
     */
    JSON("json"),

    /**
     * YAML 文件 (.yaml, .yml)
     */
    YAML("yaml");

    private final String extension;

    FileType(String extension) {
        this.extension = extension;
    }

    /**
     * 获取文件扩展名。
     *
     * @return 扩展名（不含点号）
     */
    public String getExtension() {
        return extension;
    }

    /**
     * 根据文件路径推断文件类型。
     *
     * @param path 文件路径
     * @return 文件类型，未知类型返回 TEXT
     */
    public static FileType fromPath(String path) {
        if (path == null || path.isEmpty()) {
            return TEXT;
        }

        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith(".md")) {
            return MARKDOWN;
        } else if (lowerPath.endsWith(".json")) {
            return JSON;
        } else if (lowerPath.endsWith(".yaml") || lowerPath.endsWith(".yml")) {
            return YAML;
        } else if (lowerPath.endsWith(".txt")) {
            return TEXT;
        }

        // 默认按文本处理
        return TEXT;
    }
}
