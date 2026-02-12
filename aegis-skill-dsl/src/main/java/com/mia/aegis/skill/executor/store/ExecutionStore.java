package com.mia.aegis.skill.executor.store;

import java.util.List;

/**
 * 执行上下文快照存储接口。
 *
 * <p>定义 await 暂停执行的持久化操作：保存、查询、删除和过期清理。</p>
 *
 * <p>V1 实现使用内存存储，后续版本可扩展为 Redis 或数据库存储。</p>
 */
public interface ExecutionStore {

    /**
     * 保存执行上下文快照。
     *
     * @param snapshot 执行上下文快照实例
     * @throws IllegalArgumentException 如果 snapshot 为 null 或 executionId 为空
     */
    void save(ExecutionContextSnapshot snapshot);

    /**
     * 根据 executionId 查找快照。
     *
     * @param executionId 执行唯一标识符
     * @return 执行上下文快照实例，不存在返回 null
     */
    ExecutionContextSnapshot findById(String executionId);

    /**
     * 根据 executionId 删除快照。
     *
     * @param executionId 执行唯一标识符
     * @return 被删除的快照实例，不存在返回 null
     */
    ExecutionContextSnapshot removeById(String executionId);

    /**
     * 查找所有已过期的快照。
     *
     * <p>过期判定：createdAt + timeout &lt; beforeTimestamp</p>
     *
     * @param beforeTimestamp 时间戳阈值（epoch 毫秒）
     * @return 已过期的快照列表，无过期则返回空列表
     */
    List<ExecutionContextSnapshot> findExpired(long beforeTimestamp);

    /**
     * 更新快照的状态。
     *
     * @param executionId 执行唯一标识符
     * @param newStatus 新状态
     * @return 更新是否成功
     */
    boolean updateStatus(String executionId, SnapshotStatus newStatus);

    /**
     * 检查 executionId 是否存在。
     *
     * @param executionId 执行唯一标识符
     * @return 是否存在
     */
    boolean exists(String executionId);

    /**
     * 获取当前存储的快照数量。
     *
     * @return 快照数量
     */
    int size();

    /**
     * 清空所有存储的快照。
     *
     * <p>主要用于测试目的。</p>
     */
    void clear();
}
