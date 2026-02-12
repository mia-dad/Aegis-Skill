package com.mia.aegis.skill.executor.store;

import com.mia.aegis.skill.executor.context.ExecutionContext;

/**
 * 执行上下文快照。
 *
 * <p>用于持久化 await 暂停时的执行上下文，支持后续恢复执行。</p>
 *
 * <p>设计理念：</p>
 * <ul>
 *   <li>快照是 ExecutionContext 的持久化包装器，避免重复存储数据</li>
 *   <li>只包含快照特有的元数据（skillName, stepIndex, awaitRequest 等）</li>
 *   <li>通过 {@link #context} 引用访问完整的执行上下文</li>
 * </ul>
 *
 * <p>核心数据：</p>
 * <ul>
 *   <li>context - 执行上下文（包含 executionId, input, stepResults, awaitInputs 等）</li>
 *   <li>skillName - Skill 名称</li>
 *   <li>currentStepIndex - 当前暂停的步骤索引</li>
 *   <li>awaitRequest - 用户输入请求信息</li>
 *   <li>createdAt - 创建时间戳</li>
 *   <li>status - 快照状态</li>
 * </ul>
 */
public class ExecutionContextSnapshot {

    private final ExecutionContext context;
    private final String skillName;
    private final int currentStepIndex;
    private final AwaitRequest awaitRequest;
    private final long createdAt;
    private volatile SnapshotStatus status;

    /**
     * 创建快照实例。
     *
     * @param context 执行上下文
     * @param skillName Skill 名称
     * @param currentStepIndex 当前暂停的步骤索引
     * @param awaitRequest 用户输入请求信息
     * @param createdAt 创建时间戳（毫秒）
     * @param status 快照状态
     */
    public ExecutionContextSnapshot(ExecutionContext context, String skillName, int currentStepIndex,
                                    AwaitRequest awaitRequest, long createdAt, SnapshotStatus status) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        if (skillName == null || skillName.trim().isEmpty()) {
            throw new IllegalArgumentException("skillName cannot be null or empty");
        }
        if (currentStepIndex < 0) {
            throw new IllegalArgumentException("currentStepIndex cannot be negative");
        }
        if (awaitRequest == null) {
            throw new IllegalArgumentException("awaitRequest cannot be null");
        }
        if (createdAt <= 0) {
            throw new IllegalArgumentException("createdAt must be positive");
        }
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }

        this.context = context;
        this.skillName = skillName;
        this.currentStepIndex = currentStepIndex;
        this.awaitRequest = awaitRequest;
        this.createdAt = createdAt;
        this.status = status;
    }

    /**
     * 创建新的 ACTIVE 状态的快照。
     *
     * @param context 执行上下文
     * @param skillName Skill 名称
     * @param currentStepIndex 当前暂停的步骤索引
     * @param awaitRequest 用户输入请求信息
     * @return ExecutionContextSnapshot 实例
     */
    public static ExecutionContextSnapshot createActive(ExecutionContext context, String skillName,
                                                       int currentStepIndex, AwaitRequest awaitRequest) {
        return new ExecutionContextSnapshot(context, skillName, currentStepIndex,
            awaitRequest, System.currentTimeMillis(), SnapshotStatus.ACTIVE);
    }

    /**
     * 获取执行 ID（从上下文中获取）。
     *
     * @return executionId
     */
    public String getExecutionId() {
        return context.getExecutionId();
    }

    /**
     * 获取执行上下文。
     *
     * @return ExecutionContext
     */
    public ExecutionContext getContext() {
        return context;
    }

    /**
     * 获取 Skill 名称。
     *
     * @return skillName
     */
    public String getSkillName() {
        return skillName;
    }

    /**
     * 获取当前暂停的步骤索引。
     *
     * @return currentStepIndex
     */
    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    /**
     * 获取用户输入请求信息。
     *
     * @return AwaitRequest
     */
    public AwaitRequest getAwaitRequest() {
        return awaitRequest;
    }

    /**
     * 获取创建时间戳。
     *
     * @return 创建时间戳（毫秒）
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取快照状态。
     *
     * @return SnapshotStatus
     */
    public SnapshotStatus getStatus() {
        return status;
    }

    /**
     * 设置快照状态。
     *
     * @param status 新状态
     */
    public void setStatus(SnapshotStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        this.status = status;
    }

    /**
     * 检查快照是否处于活跃状态。
     *
     * @return 是否活跃
     */
    public boolean isActive() {
        return status == SnapshotStatus.ACTIVE;
    }

    /**
     * 检查快照是否已恢复。
     *
     * @return 是否已恢复
     */
    public boolean isResumed() {
        return status == SnapshotStatus.RESUMED;
    }

    /**
     * 检查快照是否已过期。
     *
     * @return 是否已过期
     */
    public boolean isExpired() {
        return status == SnapshotStatus.EXPIRED;
    }

    /**
     * 检查快照是否已取消。
     *
     * @return 是否已取消
     */
    public boolean isCancelled() {
        return status == SnapshotStatus.CANCELLED;
    }

    /**
     * 检查快照是否可恢复。
     *
     * <p>只有处于 ACTIVE 状态的快照才可恢复。</p>
     *
     * @return 是否可恢复
     */
    public boolean isResumable() {
        return status == SnapshotStatus.ACTIVE;
    }

    /**
     * 检查快照是否已过期（基于超时时间）。
     *
     * @param timeoutMillis 超时时间（毫秒）
     * @return 是否已过期
     */
    public boolean isExpiredAt(long timeoutMillis) {
        return System.currentTimeMillis() > createdAt + timeoutMillis;
    }

    @Override
    public String toString() {
        return "ExecutionContextSnapshot{" +
            "executionId='" + getExecutionId() + '\'' +
            ", skillName='" + skillName + '\'' +
            ", stepIndex=" + currentStepIndex +
            ", status=" + status +
            ", createdAt=" + createdAt +
            '}';
    }
}
