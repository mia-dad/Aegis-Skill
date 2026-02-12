package com.mia.aegis.skill.dsl.model;

/**
 * Step 执行状态枚举。
 * 也是一个技能的生命周期
 * <p>Step 状态转换：</p>
 * <pre>
 * PENDING → RUNNING → SUCCESS | FAILED | SKIPPED | AWAITING
 * </pre>
 */
public enum StepStatus {

    /**
     * 等待执行。
     * Step 初始状态，尚未开始执行。
     */
    PENDING,

    /**
     * 执行中。
     * Step 正在执行，等待完成。
     */
    RUNNING,

    /**
     * 执行成功。
     * Step 执行完成，输出可用。
     */
    SUCCESS,

    /**
     * 执行失败。
     * Step 执行过程中发生错误。
     */
    FAILED,

    /**
     * 被跳过。
     * Step 因策略或条件被跳过执行（v2 预留）。
     */
    SKIPPED,

    /**
     * 等待用户输入。
     * Step 遇到 await，暂停执行等待用户输入。
     */
    AWAITING;

    /**
     * 判断是否为终态。
     *
     * @return 如果状态为 SUCCESS、FAILED、SKIPPED 或 AWAITING 返回 true
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == SKIPPED || this == AWAITING;
    }

    /**
     * 判断是否执行成功。
     *
     * @return 如果状态为 SUCCESS 返回 true
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 判断是否执行失败。
     *
     * @return 如果状态为 FAILED 返回 true
     */
    public boolean isFailed() {
        return this == FAILED;
    }
}

