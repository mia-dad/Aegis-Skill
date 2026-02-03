package com.mia.aegis.skill.exception;

/**
        * 模板渲染异常。
        *
        * <p>当模板渲染过程中发生错误时抛出。</p>
        */
public class TemplateRenderException extends RuntimeException {

    private final String template;
    private final String variableName;

    /**
     * 创建模板渲染异常。
     *
     * @param message 错误信息
     */
    public TemplateRenderException(String message) {
        super(message);
        this.template = null;
        this.variableName = null;
    }

    /**
     * 创建模板渲染异常（带模板信息）。
     *
     * @param message 错误信息
     * @param template 模板字符串
     */
    public TemplateRenderException(String message, String template) {
        super(message);
        this.template = template;
        this.variableName = null;
    }

    /**
     * 创建模板渲染异常（带变量信息）。
     *
     * @param message 错误信息
     * @param template 模板字符串
     * @param variableName 问题变量名
     */
    public TemplateRenderException(String message, String template, String variableName) {
        super(message);
        this.template = template;
        this.variableName = variableName;
    }

    /**
     * 创建模板渲染异常（带原因）。
     *
     * @param message 错误信息
     * @param cause 原始异常
     */
    public TemplateRenderException(String message, Throwable cause) {
        super(message, cause);
        this.template = null;
        this.variableName = null;
    }

    /**
     * 创建变量未找到异常。
     *
     * @param variableName 变量名
     * @param template 模板字符串
     * @return TemplateRenderException 实例
     */
    public static TemplateRenderException variableNotFound(String variableName, String template) {
        return new TemplateRenderException(
                "Variable '" + variableName + "' not found in context",
                template,
                variableName
        );
    }

    /**
     * 获取模板字符串。
     *
     * @return 模板字符串
     */
    public String getTemplate() {
        return template;
    }

    /**
     * 获取问题变量名。
     *
     * @return 变量名
     */
    public String getVariableName() {
        return variableName;
    }
}
