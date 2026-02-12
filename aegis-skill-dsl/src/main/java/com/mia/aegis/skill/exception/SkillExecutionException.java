package com.mia.aegis.skill.exception;

/**
 * Skill 执行异常。
 *
 * <p>当 Skill 执行过程中发生错误时抛出。</p>
 */
public class SkillExecutionException extends SkillException {

    private final String skillName;
    private final String executionId;

    /**
     * 创建执行异常（简单消息）。
     *
     * <p>向后兼容的构造器。</p>
     *
     * @param message 错误信息
     */
    public SkillExecutionException(String message) {
        super(message, ErrorCode.EXEC_FAILED);
        this.skillName = null;
        this.executionId = null;
    }

    /**
     * 创建执行异常（带原因）。
     *
     * <p>向后兼容的构造器。</p>
     *
     * @param message 错误信息
     * @param cause 原始异常
     */
    public SkillExecutionException(String message, Throwable cause) {
        super(message, ErrorCode.EXEC_FAILED, cause);
        this.skillName = null;
        this.executionId = null;
    }

    /**
     * 创建执行异常。
     *
     * @param skillName Skill 名称
     * @param executionId 执行ID
     * @param message 错误信息
     */
    public SkillExecutionException(String skillName, String executionId, String message) {
        super(message, ErrorCode.EXEC_FAILED);
        this.skillName = skillName;
        this.executionId = executionId;
    }

    /**
     * 创建执行异常（带原因）。
     *
     * @param skillName Skill 名称
     * @param executionId 执行ID
     * @param message 错误信息
     * @param cause 原始异常
     */
    public SkillExecutionException(String skillName, String executionId, String message, Throwable cause) {
        super(message, ErrorCode.EXEC_FAILED, cause);
        this.skillName = skillName;
        this.executionId = executionId;
    }

    /**
     * 创建执行异常（带错误代码）。
     *
     * @param skillName Skill 名称
     * @param executionId 执行ID
     * @param errorCode 错误代码
     * @param args 消息参数
     */
    public SkillExecutionException(String skillName, String executionId,
                                   ErrorCode errorCode, Object... args) {
        super(errorCode, args);
        this.skillName = skillName;
        this.executionId = executionId;
    }

    /**
     * 获取 Skill 名称。
     *
     * @return Skill 名称
     */
    public String getSkillName() {
        return skillName;
    }

    /**
     * 获取执行ID。
     *
     * @return 执行ID
     */
    public String getExecutionId() {
        return executionId;
    }

    @Override
    public String toString() {
        return "SkillExecutionException{" +
                "errorCode='" + getErrorCode().getCode() + '\'' +
                ", skillName='" + skillName + '\'' +
                ", executionId='" + executionId + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
