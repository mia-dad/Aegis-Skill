package com.mia.aegis.skill.executor.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Skill 执行结果。
 *
 * <p>记录 Skill 整体执行状态、最终输出和所有 Step 的执行结果。</p>
 *
 * <p>支持三种执行状态：</p>
 * <ul>
 *   <li>成功 - Skill 完整执行完成</li>
 *   <li>失败 - 执行过程中出现错误</li>
 *   <li>等待 - 遇到 await 步骤，暂停执行等待用户输入</li>
 * </ul>
 */
public class SkillResult {

    private final boolean success;
    private final boolean awaiting;
    private final Object output;
    private final String error;
    private final String executionId;
    private final List<StepResult> stepResults;
    private final long totalDuration;

    /**
     * 创建 Skill 执行结果。
     *
     * @param success 是否成功
     * @param awaiting 是否等待用户输入
     * @param output 最终输出
     * @param error 错误信息
     * @param executionId 执行ID（await 状态时使用）
     * @param stepResults Step 执行结果列表
     * @param totalDuration 总执行耗时
     */
    private SkillResult(boolean success, boolean awaiting, Object output, String error,
                       String executionId, List<StepResult> stepResults, long totalDuration) {
        this.success = success;
        this.awaiting = awaiting;
        this.output = output;
        this.error = error;
        this.executionId = executionId;
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
        return new SkillResult(true, false, output, null, null, stepResults, totalDuration);
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
        return new SkillResult(false, false, null, error, null, stepResults, totalDuration);
    }

    /**
     * 创建等待用户输入结果。
     *
     * @param executionId 执行ID
     * @param awaitRequest Await 请求信息
     * @param stepResults Step 执行结果列表
     * @param totalDuration 总执行耗时
     * @return SkillResult 实例
     */
    public static SkillResult awaiting(String executionId, Object awaitRequest,
                                       List<StepResult> stepResults, long totalDuration) {
        return new SkillResult(false, true, awaitRequest, null, executionId, stepResults, totalDuration);
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
     * 判断是否等待用户输入。
     *
     * @return 是否等待
     */
    public boolean isAwaiting() {
        return awaiting;
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
     * 获取执行ID（await 状态时有效）。
     *
     * @return executionId，非 await 状态返回 null
     */
    public String getExecutionId() {
        return executionId;
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
                ", awaiting=" + awaiting +
                (executionId != null ? ", executionId='" + executionId + '\'' : "") +
                ", steps=" + stepResults.size() +
                ", duration=" + totalDuration + "ms" +
                (error != null ? ", error='" + error + '\'' : "") +
                '}';
    }
}
