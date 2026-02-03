package com.mia.aegis.skill.executor.step;


import com.mia.aegis.skill.dsl.model.Step;
import com.mia.aegis.skill.exception.SkillExecutionException;
import com.mia.aegis.skill.executor.context.ExecutionContext;
import com.mia.aegis.skill.executor.context.StepResult;

/**
 * Step 执行器接口。
 * 定义单个 Step 的执行逻辑。
 */
public interface StepExecutor {

    /**
     * 执行 Step。
     *
     * @param step    要执行的 Step
     * @param context 执行上下文
     * @return Step 执行结果
     * @throws SkillExecutionException 执行失败时抛出
     */
    StepResult execute(Step step, ExecutionContext context) throws SkillExecutionException;

    /**
     * 检查此执行器是否支持指定的 Step。
     *
     * @param step 要检查的 Step
     * @return 是否支持
     */
    boolean supports(Step step);
}

