package com.mia.aegis.skill.executor.engine;

import com.mia.aegis.skill.dsl.model.Skill;
import com.mia.aegis.skill.dsl.model.Step;
import com.mia.aegis.skill.executor.context.SkillResult;
import com.mia.aegis.skill.executor.context.StepResult;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 日志执行监听器。
 *
 * <p>记录 Skill 和 Step 执行的详细日志，包括：
 * <ul>
 *   <li>执行开始/结束事件</li>
 *   <li>耗时统计</li>
 *   <li>输入/输出摘要</li>
 *   <li>错误信息</li>
 * </ul>
 * </p>
 */
public class LoggingExecutionListener implements ExecutionListener {

    private static final Logger logger = Logger.getLogger(LoggingExecutionListener.class.getName());

    private final boolean includeOutputSummary;
    private final int maxOutputLength;

    /**
     * 创建日志监听器（默认设置）。
     */
    public LoggingExecutionListener() {
        this(true, 200);
    }

    /**
     * 创建日志监听器。
     *
     * @param includeOutputSummary 是否包含输出摘要
     * @param maxOutputLength 输出摘要最大长度
     */
    public LoggingExecutionListener(boolean includeOutputSummary, int maxOutputLength) {
        this.includeOutputSummary = includeOutputSummary;
        this.maxOutputLength = maxOutputLength;
    }

    @Override
    public void onSkillStart(Skill skill) {
        logger.info(String.format("[Skill] Starting '%s' (%d steps)",
                skill.getId(), skill.getSteps().size()));
    }

    @Override
    public void onSkillComplete(Skill skill, SkillResult result) {
        if (result.isSuccess()) {
            logger.info(String.format("[Skill] Completed '%s' successfully in %dms",
                    skill.getId(), result.getTotalDuration()));
        } else {
            logger.warning(String.format("[Skill] Failed '%s' in %dms: %s",
                    skill.getId(), result.getTotalDuration(), result.getError()));
        }

        // 输出 Step 耗时汇总
        if (logger.isLoggable(Level.FINE)) {
            StringBuilder summary = new StringBuilder();
            summary.append("[Skill] Step timing summary for '").append(skill.getId()).append("':\n");
            for (StepResult stepResult : result.getStepResults()) {
                summary.append(String.format("  - %s: %s (%dms)\n",
                        stepResult.getStepName(),
                        stepResult.getStatus(),
                        stepResult.getDuration()));
            }
            logger.fine(summary.toString());
        }
    }

    @Override
    public void onStepStart(Step step, int stepIndex, int totalSteps) {
        logger.info(String.format("[Step %d/%d] Starting '%s' (type: %s)",
                stepIndex + 1, totalSteps, step.getName(), step.getType()));
    }

    @Override
    public void onStepComplete(Step step, StepResult result, int stepIndex, int totalSteps) {
        if (result.isSuccess()) {
            String message = String.format("[Step %d/%d] Completed '%s' in %dms",
                    stepIndex + 1, totalSteps, step.getName(), result.getDuration());

            if (includeOutputSummary && result.getOutput() != null) {
                String outputSummary = summarizeOutput(result.getOutput());
                message += " - Output: " + outputSummary;
            }

            logger.info(message);
        } else {
            logger.warning(String.format("[Step %d/%d] Failed '%s' in %dms: %s",
                    stepIndex + 1, totalSteps, step.getName(), result.getDuration(), result.getError()));
        }
    }

    /**
     * 生成输出摘要。
     *
     * @param output 输出对象
     * @return 摘要字符串
     */
    private String summarizeOutput(Object output) {
        if (output == null) {
            return "null";
        }

        String str = output.toString();
        if (str.length() <= maxOutputLength) {
            return str;
        }

        return str.substring(0, maxOutputLength) + "... (truncated)";
    }
}