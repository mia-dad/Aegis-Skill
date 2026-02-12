package com.mia.aegis.skill.executor.step;

import com.mia.aegis.skill.dsl.model.AwaitStepConfig;
import com.mia.aegis.skill.dsl.model.StepType;
import com.mia.aegis.skill.dsl.model.io.InputSchema;
import com.mia.aegis.skill.dsl.model.Step;
import com.mia.aegis.skill.exception.SkillExecutionException;
import com.mia.aegis.skill.executor.context.ExecutionContext;
import com.mia.aegis.skill.executor.context.StepResult;
import com.mia.aegis.skill.executor.store.AwaitRequest;

/**
 * Await 类型 Step 执行器。
 *
 * <p>执行 await 步骤时，生成 AwaitRequest 返回给调用方。
 * 不直接保存到 ExecutionStore，由 DefaultSkillExecutor 统一处理暂停逻辑。</p>
 *
 * <p>执行流程：</p>
 * <ol>
 *   <li>从 Step 配置中获取 message 和 inputSchema</li>
 *   <li>创建 AwaitRequest 对象</li>
 *   <li>返回 AWAITING 状态的 StepResult</li>
 * </ol>
 */
public class AwaitStepExecutor implements StepExecutor {


    /**
     * 创建 AwaitStepExecutor 实例。
     */
    public AwaitStepExecutor() {
        // 无依赖，默认构造函数
    }

    @Override
    public StepResult execute(Step step, ExecutionContext context) throws SkillExecutionException {
        long startTime = System.currentTimeMillis();

        if (!supports(step)) {
            throw new SkillExecutionException("AwaitStepExecutor does not support step type: " + step.getType());
        }

        try {
            AwaitStepConfig config = step.getAwaitConfig();

            // 创建 AwaitRequest
            AwaitRequest awaitRequest = new AwaitRequest(
                    config.getMessage(),
                    config.getInputSchema()
            );

            long duration = System.currentTimeMillis() - startTime;

            // 返回 AWAITING 状态，将 AwaitRequest 作为输出
            return StepResult.awaiting(step.getName(), awaitRequest, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return StepResult.failed(step.getName(), "Await step execution failed: " + e.getMessage(), duration);
        }
    }

    @Override
    public boolean supports(Step step) {
        return step != null && step.getType() == StepType.AWAIT;
    }
}
