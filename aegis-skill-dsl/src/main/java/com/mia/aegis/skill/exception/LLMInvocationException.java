package com.mia.aegis.skill.exception;

/**
 * LLM 调用异常。
 *
 * <p>当 LLM Adapter 调用过程中发生错误时抛出。</p>
 */
public class LLMInvocationException extends RuntimeException {

    private final String adapterName;

    /**
     * 创建 LLM 调用异常。
     *
     * @param adapterName Adapter 名称
     * @param message 错误信息
     */
    public LLMInvocationException(String adapterName, String message) {
        super("大模型 '" + adapterName + "' 执行失败: " + message);
        this.adapterName = adapterName;
    }

    /**
     * 创建 LLM 调用异常（带原因）。
     *
     * @param adapterName Adapter 名称
     * @param message 错误信息
     * @param cause 原始异常
     */
    public LLMInvocationException(String adapterName, String message, Throwable cause) {
        super("大模型 '" + adapterName + "' 执行失败: " + message, cause);
        this.adapterName = adapterName;
    }

    /**
     * 获取 Adapter 名称。
     *
     * @return Adapter 名称
     */
    public String getAdapterName() {
        return adapterName;
    }
}

