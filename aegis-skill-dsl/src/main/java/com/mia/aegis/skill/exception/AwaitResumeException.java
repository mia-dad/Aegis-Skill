package com.mia.aegis.skill.exception;

/**
 * Await 恢复异常。
 *
 * <p>await 执行恢复过程中发生的错误。</p>
 */
public class AwaitResumeException extends SkillExecutionException {

    private final String executionId;

    /**
     * 创建异常实例。
     *
     * @param executionId 执行ID
     * @param message 错误信息
     */
    public AwaitResumeException(String executionId, String message) {
        super(message);
        this.executionId = executionId;
    }

    /**
     * 创建异常实例。
     *
     * @param executionId 执行ID
     * @param message 错误信息
     * @param cause 原因
     */
    public AwaitResumeException(String executionId, String message, Throwable cause) {
        super(message, cause);
        this.executionId = executionId;
    }

    /**
     * 获取执行ID。
     *
     * @return executionId
     */
    public String getExecutionId() {
        return executionId;
    }
}
