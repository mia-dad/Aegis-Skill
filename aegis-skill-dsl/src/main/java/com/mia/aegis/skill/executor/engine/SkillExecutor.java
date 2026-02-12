package com.mia.aegis.skill.executor.engine;

import com.mia.aegis.skill.dsl.model.Skill;
import com.mia.aegis.skill.exception.ExecutionNotFoundException;
import com.mia.aegis.skill.exception.SkillExecutionException;
import com.mia.aegis.skill.executor.context.ExecutionContext;
import com.mia.aegis.skill.executor.context.SkillResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Skill 执行器接口。
 *
 * <p>负责执行完整的 Skill，包括所有 Step 的顺序执行、
 * 状态管理和结果收集。</p>
 *
 * <p>支持 await 步骤的暂停和恢复执行。</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * SkillExecutor executor = new DefaultSkillExecutor(toolRegistry, llmAdapter);
 * Map<String, Object> input = new HashMap<>();
 * input.put("company", "ABC Corp");
 *
 * // 执行 Skill
 * SkillResult result = executor.execute(skill, input);
 *
 * // 如果需要等待用户输入
 * if (result.isAwaiting()) {
 *     String executionId = result.getExecutionId();
 *     // 获取用户输入后恢复执行
 *     Map<String, Object> userInput = getUserInput();
 *     result = executor.resume(skill, executionId, userInput);
 * }
 *
 * if (result.isSuccess()) {
 *     Object output = result.getOutput();
 * }
 * }</pre>
 */
public interface SkillExecutor {

    /**
     * 同步执行 Skill。
     *
     * @param skill Skill 定义
     * @param input 输入参数
     * @return Skill 执行结果
     * @throws SkillExecutionException 执行失败时抛出
     */
    SkillResult execute(Skill skill, Map<String, Object> input) throws SkillExecutionException;

    /**
     * 使用预建上下文同步执行 Skill。
     *
     * @param skill Skill 定义
     * @param context 执行上下文
     * @return Skill 执行结果
     * @throws SkillExecutionException 执行失败时抛出
     */
    SkillResult execute(Skill skill, ExecutionContext context) throws SkillExecutionException;

    /**
     * 恢复暂停的 Skill 执行。
     *
     * @param skill Skill 定义
     * @param executionId 执行ID
     * @param userInput 用户提供的输入
     * @return Skill 执行结果
     * @throws SkillExecutionException 执行失败时抛出
     * @throws ExecutionNotFoundException 如果执行ID不存在
     * @throws com.mia.aegis.skill.executor.store.ExecutionAlreadyCompletedException 如果执行已完成或不可恢复
     */
    SkillResult resume(Skill skill, String executionId, Map<String, Object> userInput)
            throws SkillExecutionException;

    /**
     * 异步执行 Skill。
     *
     * @param skill Skill 定义
     * @param input 输入参数
     * @return 执行结果的 Future
     */
    CompletableFuture<SkillResult> executeAsync(Skill skill, Map<String, Object> input);

    /**
     * 使用预建上下文异步执行 Skill。
     *
     * @param skill Skill 定义
     * @param context 执行上下文
     * @return 执行结果的 Future
     */
    CompletableFuture<SkillResult> executeAsync(Skill skill, ExecutionContext context);

    /**
     * 异步恢复暂停的 Skill 执行。
     *
     * @param skill Skill 定义
     * @param executionId 执行ID
     * @param userInput 用户提供的输入
     * @return 执行结果的 Future
     */
    CompletableFuture<SkillResult> resumeAsync(Skill skill, String executionId, Map<String, Object> userInput);
}
