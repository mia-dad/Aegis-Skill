package com.mia.aegis.skill.executor.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Skill 执行结果。
 *
 * <p>记录 Skill 整体执行状态、最终输出和所有 Step 的执行结果。</p>
 */
public class SkillResult {

    private final boolean success;
    private final Object output;
    private final String error;
    private final List<StepResult> stepResults;
    private final long totalDuration;

    /**
     * 创建 Skill 执行结果。
     *
     * @param success 是否成功
     * @param output 最终输出
     * @param error 错误信息
     * @param stepResults Step 执行结果列表
     * @param totalDuration 总执行耗时
     */
    public SkillResult(boolean success, Object output, String error,
                       List<StepResult> stepResults, long totalDuration) {
        this.success = success;
        this.output = output;
        this.error = error;
        this.stepResults = stepResults != null
                ? Collections.unmodifiableList(new ArrayList<StepResult>(stepResults))
                : Collections.<StepResult>emptyList();
        this.totalDuration = totalDuration;
    }

    /**
     * 创建成功结果。
     *
     * @param output 最终输出
     * @param stepResults Step 执行结果列表
     * @param totalDuration 总执行耗时
     * @return SkillResult 实例
     */
    public static SkillResult success(Object output, List<StepResult> stepResults, long totalDuration) {
        return new SkillResult(true, output, null, stepResults, totalDuration);
    }

    /**
     * 创建失败结果。
     *
     * @param error 错误信息
     * @param stepResults Step 执行结果列表
     * @param totalDuration 总执行耗时
     * @return SkillResult 实例
     */
    public static SkillResult failure(String error, List<StepResult> stepResults, long totalDuration) {
        return new SkillResult(false, null, error, stepResults, totalDuration);
    }

    /**
     * 判断是否成功。
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 获取最终输出。
     *
     * @return 最终输出
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
     * 获取所有 Step 执行结果。
     *
     * @return Step 执行结果列表
     */
    public List<StepResult> getStepResults() {
        return stepResults;
    }

    /**
     * 获取总执行耗时。
     *
     * @return 总执行耗时（毫秒）
     */
    public long getTotalDuration() {
        return totalDuration;
    }

    /**
     * 获取成功执行的 Step 数量。
     *
     * @return 成功 Step 数量
     */
    public int getSuccessfulStepCount() {
        int count = 0;
        for (StepResult result : stepResults) {
            if (result.isSuccess()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取失败的 Step 数量。
     *
     * @return 失败 Step 数量
     */
    public int getFailedStepCount() {
        int count = 0;
        for (StepResult result : stepResults) {
            if (result.isFailed()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        return "SkillResult{" +
                "success=" + success +
                ", steps=" + stepResults.size() +
                ", duration=" + totalDuration + "ms" +
                (error != null ? ", error='" + error + '\'' : "") +
                '}';
    }
}

