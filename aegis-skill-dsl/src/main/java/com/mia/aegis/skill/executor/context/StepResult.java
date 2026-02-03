package com.mia.aegis.skill.executor.context;


import com.mia.aegis.skill.dsl.model.StepStatus;

/**
 * Step 执行结果。
 * 不可变类
 *
 * <p>记录单个 Step 的执行状态、输出、错误和耗时。</p>
 */
public class StepResult {

    private final String stepName;
    private final StepStatus status;
    private final Object output;
    private final String error;
    private final long duration;

    /**
     * 创建 Step 执行结果。
     *
     * @param stepName Step 名称
     * @param status 执行状态
     * @param output 执行输出
     * @param error 错误信息
     * @param duration 执行耗时（毫秒）
     */
    public StepResult(String stepName, StepStatus status, Object output, String error, long duration) {
        this.stepName = stepName;
        this.status = status;
        this.output = output;
        this.error = error;
        this.duration = duration;
    }

    /**
     * 创建成功结果。
     *
     * @param stepName Step 名称
     * @param output 执行输出
     * @param duration 执行耗时
     * @return StepResult 实例
     */
    public static StepResult success(String stepName, Object output, long duration) {
        return new StepResult(stepName, StepStatus.SUCCESS, output, null, duration);
    }

    /**
     * 创建失败结果。
     *
     * @param stepName Step 名称
     * @param error 错误信息
     * @param duration 执行耗时
     * @return StepResult 实例
     */
    public static StepResult failed(String stepName, String error, long duration) {
        return new StepResult(stepName, StepStatus.FAILED, null, error, duration);
    }

    /**
     * 创建跳过结果。
     *
     * @param stepName Step 名称
     * @return StepResult 实例
     */
    public static StepResult skipped(String stepName) {
        return new StepResult(stepName, StepStatus.SKIPPED, null, null, 0);
    }

    /**
     * 获取 Step 名称。
     *
     * @return Step 名称
     */
    public String getStepName() {
        return stepName;
    }

    /**
     * 获取执行状态。
     *
     * @return 执行状态
     */
    public StepStatus getStatus() {
        return status;
    }

    /**
     * 获取执行输出。
     *
     * @return 执行输出
     */
    public Object getOutput() {
        return output;
    }

    /**
     * 获取错误信息。
     *
     * @return 错误信息
     */
    public String getError() {
        return error;
    }

    /**
     * 获取执行耗时。
     *
     * @return 执行耗时（毫秒）
     */
    public long getDuration() {
        return duration;
    }

    /**
     * 判断是否成功。
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return status == StepStatus.SUCCESS;
    }

    /**
     * 判断是否失败。
     *
     * @return 是否失败
     */
    public boolean isFailed() {
        return status == StepStatus.FAILED;
    }

    @Override
    public String toString() {
        return "StepResult{" +
                "stepName='" + stepName + '\'' +
                ", status=" + status +
                ", duration=" + duration + "ms" +
                (error != null ? ", error='" + error + '\'' : "") +
                '}';
    }
}

