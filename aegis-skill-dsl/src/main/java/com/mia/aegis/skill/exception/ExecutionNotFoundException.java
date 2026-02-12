package com.mia.aegis.skill.exception;

/**
 * 执行未找到异常。
 *
 * <p>当尝试恢复一个不存在的执行时抛出。</p>
 */
public class ExecutionNotFoundException extends SkillExecutionException {

    private final String executionId;

    /**
     * 创建异常实例。
     *
     * @param executionId 执行ID
     */
    public ExecutionNotFoundException(String executionId) {
        super(null, executionId, ErrorCode.STORE_EXECUTION_NOT_FOUND, executionId);
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
