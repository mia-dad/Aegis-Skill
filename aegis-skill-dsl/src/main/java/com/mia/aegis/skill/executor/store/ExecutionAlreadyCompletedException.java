package com.mia.aegis.skill.executor.store;

import com.mia.aegis.skill.exception.ErrorCode;
import com.mia.aegis.skill.exception.SkillExecutionException;

/**
 * 执行已完成异常。
 *
 * <p>当尝试恢复一个已完成或不可恢复的执行时抛出。</p>
 */
public class ExecutionAlreadyCompletedException extends SkillExecutionException {

    private final String executionId;
    private final SnapshotStatus status;

    /**
     * 创建异常实例。
     *
     * @param executionId 执行ID
     * @param status 当前状态
     */
    public ExecutionAlreadyCompletedException(String executionId, SnapshotStatus status) {
        super(null, executionId, ErrorCode.STORE_EXECUTION_COMPLETED, executionId, status);
        this.executionId = executionId;
        this.status = status;
    }

    /**
     * 获取执行ID。
     *
     * @return executionId
     */
    public String getExecutionId() {
        return executionId;
    }

    /**
     * 获取状态。
     *
     * @return status
     */
    public SnapshotStatus getStatus() {
        return status;
    }
}
