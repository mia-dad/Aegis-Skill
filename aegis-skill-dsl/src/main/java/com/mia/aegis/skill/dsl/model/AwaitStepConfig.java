package com.mia.aegis.skill.dsl.model;

import com.mia.aegis.skill.dsl.model.io.InputSchema;


/**
 * Await 类型 Step 的配置。
 *
 * <p>用于定义 Skill 执行过程中的暂停点，等待用户提供输入后继续执行。</p>
 *
 * <p>DSL 示例：</p>
 * <pre>{@code
 * ### step: confirm_order
 * **type**: await
 *
 * ```yaml
 * message: "请确认订单信息"
 * input_schema:
 *   confirm:
 *     type: boolean
 *     required: true
 *     description: "是否确认订单"
 *   notes:
 *     type: string
 *     required: false
 *     description: "备注信息"
 * ```
 * }</pre>
 */
public class AwaitStepConfig implements StepConfig {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final String message;
    private final InputSchema inputSchema;

    /**
     * 创建 Await Step 配置。
     *
     * @param message 向用户展示的提示信息
     * @param inputSchema 定义用户需提供的输入结构
     * @throws IllegalArgumentException 如果参数无效
     */
    public AwaitStepConfig(String message, InputSchema inputSchema) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Await message cannot be null or empty");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Await message exceeds maximum length of " + MAX_MESSAGE_LENGTH);
        }
        if (inputSchema == null) {
            throw new IllegalArgumentException("Await inputSchema cannot be null");
        }
        if (inputSchema.isEmpty()) {
            throw new IllegalArgumentException("Await inputSchema must have at least one field");
        }
        this.message = message.trim();
        this.inputSchema = inputSchema;
    }

    /**
     * 获取提示信息。
     *
     * @return 向用户展示的提示文本
     */
    public String getMessage() {
        return message;
    }

    /**
     * 获取输入 Schema。
     *
     * @return 定义用户需提供的输入结构
     */
    public InputSchema getInputSchema() {
        return inputSchema;
    }

    @Override
    public StepType getStepType() {
        return StepType.AWAIT;
    }

    @Override
    public String toString() {
        return "AwaitStepConfig{" +
                "message='" + message + '\'' +
                ", inputSchema=" + inputSchema +
                '}';
    }
}