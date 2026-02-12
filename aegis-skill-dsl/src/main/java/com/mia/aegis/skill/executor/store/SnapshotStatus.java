package com.mia.aegis.skill.executor.store;

/**
 * 快照状态枚举。
 *
 * <p>表示 ExecutionContextSnapshot 的生命周期状态：</p>
 * <ul>
 *   <li>{@link #ACTIVE} - 活跃状态，等待用户输入</li>
 *   <li>{@link #RESUMED} - 已恢复执行</li>
 *   <li>{@link #EXPIRED} - 已过期</li>
 *   <li>{@link #CANCELLED} - 已取消</li>
 * </ul>
 */
public enum SnapshotStatus {

    /**
     * 活跃状态，等待用户输入。
     * 执行暂停，等待用户提供输入后恢复。
     */
    ACTIVE,

    /**
     * 已恢复执行。
     * 用户已提供输入，执行已恢复继续。
     */
    RESUMED,

    /**
     * 已过期。
     * 等待时间超过配置的超时阈值，快照被标记为过期。
     */
    EXPIRED,

    /**
     * 已取消。
     * 执行被手动取消，不再接受恢复请求。
     */
    CANCELLED
}
