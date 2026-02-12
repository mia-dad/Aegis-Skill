package com.mia.aegis.skill.dsl.model;

/**
 * 引用文件描述。
 *
 * <p>表示 Skill Markdown 中通过 <!-- reference: path --> 指令引用的外部文件。</p>
 *
 * <p>引用文件内容会被加载到 Skill 的 context 中，可在模板中通过
 * {{context.filename}} 方式访问。</p>
 */
public class Reference {

    private final String path;
    private final FileType fileType;
    private final String name;
    private final String content;

    /**
     * 创建 Reference（不含内容）。
     *
     * @param path 文件路径
     * @param fileType 文件类型
     */
    public Reference(String path, FileType fileType) {
        this(path, fileType, null, null);
    }

    /**
     * 创建 Reference（完整信息）。
     *
     * @param path 文件路径
     * @param fileType 文件类型
     * @param name 引用名称（用于 context 访问）
     * @param content 文件内容
     */
    public Reference(String path, FileType fileType, String name, String content) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Reference path cannot be null or empty");
        }
        this.path = path.trim();
        this.fileType = fileType != null ? fileType : FileType.fromPath(path);
        this.name = name != null ? name : extractName(this.path);
        this.content = content;
    }

    /**
     * 获取文件路径。
     *
     * @return 文件路径
     */
    public String getPath() {
        return path;
    }

    /**
     * 获取文件类型。
     *
     * @return 文件类型
     */
    public FileType getFileType() {
        return fileType;
    }

    /**
     * 获取引用名称。
     *
     * <p>名称用于在 context 中访问引用内容，如 {{context.system_prompt}}</p>
     *
     * @return 引用名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取文件内容。
     *
     * @return 文件内容，可能为 null（未加载时）
     */
    public String getContent() {
        return content;
    }

    /**
     * 创建带内容的新 Reference。
     *
     * @param content 文件内容
     * @return 包含内容的新 Reference
     */
    public Reference withContent(String content) {
        return new Reference(this.path, this.fileType, this.name, content);
    }

    /**
     * 从文件路径提取引用名称。
     *
     * <p>提取规则：取文件名（不含扩展名），将特殊字符替换为下划线。</p>
     *
     * @param path 文件路径
     * @return 引用名称
     */
    private static String extractName(String path) {
        // 获取文件名部分
        String fileName = path;
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            fileName = path.substring(lastSlash + 1);
        }
        int lastBackslash = fileName.lastIndexOf('\\');
        if (lastBackslash >= 0 && lastBackslash < fileName.length() - 1) {
            fileName = fileName.substring(lastBackslash + 1);
        }

        // 移除扩展名
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            fileName = fileName.substring(0, lastDot);
        }

        // 替换特殊字符为下划线
        return fileName.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    @Override
    public String toString() {
        return "Reference{" +
                "path='" + path + '\'' +
                ", fileType=" + fileType +
                ", name='" + name + '\'' +
                ", hasContent=" + (content != null) +
                '}';
    }
}
