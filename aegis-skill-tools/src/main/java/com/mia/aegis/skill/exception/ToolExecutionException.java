package com.mia.aegis.skill.exception;

/**
 * Tool 执行异常。
 *
 * <p>当 Tool 执行过程中发生错误时抛出。</p>
 */
public class ToolExecutionException extends RuntimeException {

    private final String toolName;

    /**
     * 创建 Tool 执行异常。
     *
     * @param toolName Tool 名称
     * @param message 错误信息
     */
    public ToolExecutionException(String toolName, String message) {
        super("Tool '" + toolName + "' execution error: " + message);
        this.toolName = toolName;
    }

    /**
     * 创建 Tool 执行异常（带原因）。
     *
     * @param toolName Tool 名称
     * @param message 错误信息
     * @param cause 原始异常
     */
    public ToolExecutionException(String toolName, String message, Throwable cause) {
        super("Tool '" + toolName + "' execution error: " + message, cause);
        this.toolName = toolName;
    }

    /**
     * 获取 Tool 名称。
     *
     * @return Tool 名称
     */
    public String getToolName() {
        return toolName;
    }
}
