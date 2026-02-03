package com.mia.aegis.skill.llm;

import com.mia.aegis.skill.exception.LLMInvocationException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * LLM 适配器 SPI 接口。
 *
 * <p>用于将 Prompt Step 发送至不同的 LLM 提供商。
 * 实现类通过 Java ServiceLoader 机制发现。</p>
 *
 * <p>Registration:</p>
 * <p>在 META-INF/services/com.aegis.skill.spi.LLMAdapter 中注册实现类。</p>
 *
 * <p>Example Implementation:</p>
 * <pre>{@code
 * public class ClaudeAdapter implements LLMAdapter {
 *     @Override
 *     public String getName() { return "claude"; }
 *
 *     @Override
 *     public String invoke(String prompt, Map<String, Object> options) {
 *         // Call Claude API
 *         return response;
 *     }
 * }
 * }</pre>
 */
public interface LLMAdapter {

    /**
     * 获取适配器名称。
     *
     * @return 适配器名称（如 "claude", "openai", "qwen"）
     */
    String getName();

    /**
     * 同步调用 LLM。
     *
     * @param prompt 渲染后的 Prompt 内容
     * @param options 调用选项（如 temperature, max_tokens）
     * @return LLM 生成的响应
     * @throws LLMInvocationException 调用失败时抛出
     */
    String invoke(String prompt, Map<String, Object> options) throws LLMInvocationException;

    /**
     * 异步调用 LLM。
     *
     * @param prompt 渲染后的 Prompt 内容
     * @param options 调用选项
     * @return 响应的 Future
     */
    CompletableFuture<String> invokeAsync(String prompt, Map<String, Object> options);

    /**
     * 检查适配器是否可用。
     *
     * @return 如果配置正确且服务可达，返回 true
     */
    boolean isAvailable();

    /**
     * 获取支持的模型列表。
     *
     * @return 模型名称数组
     */
    String[] getSupportedModels();
}

