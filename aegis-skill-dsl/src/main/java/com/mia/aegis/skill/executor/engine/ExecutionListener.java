package com.mia.aegis.skill.executor.engine;

import com.mia.aegis.skill.dsl.model.Skill;
import com.mia.aegis.skill.dsl.model.Step;
import com.mia.aegis.skill.executor.context.SkillResult;
import com.mia.aegis.skill.executor.context.StepResult;

/**
 * 执行监听器接口。
 *
 * <p>用于在 Skill 和 Step 执行过程中接收通知，
 * 支持日志记录、性能监控和调试等场景。</p>
 */
public interface ExecutionListener {

    /**
     * Skill 开始执行时调用。
     *
     * @param skill 正在执行的 Skill
     */
    void onSkillStart(Skill skill);

    /**
     * Skill 执行完成时调用。
     *
     * @param skill 已执行的 Skill
     * @param result 执行结果
     */
    void onSkillComplete(Skill skill, SkillResult result);

    /**
     * Step 开始执行时调用。
     *
     * @param step 正在执行的 Step
     * @param stepIndex 当前 Step 索引（从 0 开始）
     * @param totalSteps 总 Step 数量
     */
    void onStepStart(Step step, int stepIndex, int totalSteps);

    /**
     * Step 执行完成时调用。
     *
     * @param step 已执行的 Step
     * @param result Step 执行结果
     * @param stepIndex 当前 Step 索引
     * @param totalSteps 总 Step 数量
     */
    void onStepComplete(Step step, StepResult result, int stepIndex, int totalSteps);

    /**
     * 空实现，用于只需部分回调的场景。
     */
    class Adapter implements ExecutionListener {
        @Override
        public void onSkillStart(Skill skill) {}

        @Override
        public void onSkillComplete(Skill skill, SkillResult result) {}

        @Override
        public void onStepStart(Step step, int stepIndex, int totalSteps) {}

        @Override
        public void onStepComplete(Step step, StepResult result, int stepIndex, int totalSteps) {}
    }
}
