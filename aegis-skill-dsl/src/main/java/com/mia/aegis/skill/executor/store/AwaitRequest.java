package com.mia.aegis.skill.executor.store;

import com.mia.aegis.skill.dsl.model.io.InputSchema;

/**
 * Await 步骤返回给调用方的请求信息。
 *
 * <p>包含向用户展示的提示信息和需要用户提供的输入结构定义。</p>
 */
public class AwaitRequest {

    private final String message;
    private final InputSchema inputSchema;

    /**
     * 创建 AwaitRequest 实例。
     *
     * @param message 向用户展示的提示信息
     * @param inputSchema 定义用户需要提供的输入结构
     */
    public AwaitRequest(String message, InputSchema inputSchema) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("message cannot be null or empty");
        }
        if (inputSchema == null) {
            throw new IllegalArgumentException("inputSchema cannot be null");
        }
        this.message = message;
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
    public String toString() {
        return "AwaitRequest{" +
            "message='" + message + '\'' +
            ", inputSchema=" + inputSchema +
            '}';
    }
}
