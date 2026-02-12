package com.mia.skill.api.dashscope;

import com.mia.aegis.skill.exception.LLMInvocationException;
import com.mia.aegis.skill.llm.LLMAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 阿里云百炼平台（DashScope）LLM 适配器。
 *
 * <p>通过 OpenAI 兼容模式 HTTP API 调用 DashScope 模型服务。</p>
 *
 * <h3>配置方式</h3>
 * <p>API Key 通过以下方式配置（优先级从高到低）：</p>
 * <ol>
 *   <li><strong>构造函数注入</strong>（最高优先级）：通过构造函数直接传入 API Key</li>
 *   <li><strong>JVM 系统属性</strong>：{@code -Ddashscope.api.key=sk-xxx}</li>
 *   <li><strong>环境变量</strong>：{@code DASHSCOPE_API_KEY=sk-xxx}</li>
 * </ol>
 *
 * <p><strong>优先级说明</strong>：当同时设置多个配置源时，按上述优先级顺序选择。
 * 空字符串或仅含空白的配置会被忽略并回退到下一优先级。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 配置 API Key（三种方式）
 * // 方式1: 构造函数注入 - new DashScopeLLMAdapter("sk-your-api-key")
 * // 方式2: 环境变量 - export DASHSCOPE_API_KEY=sk-your-api-key
 * // 方式3: JVM 参数 - java -Ddashscope.api.key=sk-your-api-key ...
 *
 * // 创建实例并调用
 * LLMAdapter adapter = new DashScopeLLMAdapter("sk-your-api-key");
 * String response = adapter.invoke("分析这段财务数据", Collections.emptyMap());
 * System.out.println(response);
 * }</pre>
 *
 * <h3>支持的模型</h3>
 * <ul>
 *   <li>{@code qwen3-max} - 通义千问最强模型（默认）</li>
 * </ul>
 *
 * <h3>调用选项</h3>
 * <p>invoke() 方法的 options 参数支持以下键：</p>
 * <ul>
 *   <li>{@code model} - 模型名称，默认 "qwen3-max"</li>
 *   <li>{@code temperature} - 温度参数 (0.0-2.0)</li>
 *   <li>{@code max_tokens} - 最大生成 token 数</li>
 *   <li>{@code top_p} - Top-P 采样参数 (0.0-1.0)</li>
 * </ul>
 *
 * <h3>错误处理</h3>
 * <p>所有 API 调用错误统一封装为 {@link LLMInvocationException}：</p>
 * <ul>
 *   <li>401: Authentication failed（API Key 无效）</li>
 *   <li>429: Rate limit exceeded（请求频率限制）</li>
 *   <li>5xx: Server error（服务端错误）</li>
 *   <li>IOException: Network error（网络错误）</li>
 * </ul>
 *
 * <h3>超时配置</h3>
 * <ul>
 *   <li>连接超时：30 秒 ({@link #CONNECT_TIMEOUT_MS})</li>
 *   <li>读取超时：60 秒 ({@link #READ_TIMEOUT_MS})</li>
 * </ul>
 *
 * <h3>线程安全</h3>
 * <p>本类设计为线程安全，可在多线程环境中共享使用。每次调用创建独立的 HTTP 连接。</p>
 *
 * @see LLMAdapter
 * @see LLMInvocationException
 */
public class DashScopeLLMAdapter implements LLMAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DashScopeLLMAdapter.class);

    /** 系统属性名：API Key */
    public static final String SYSTEM_PROPERTY_API_KEY = "dashscope.api.key";

    /** 环境变量名：API Key */
    public static final String ENV_VAR_API_KEY = "DASHSCOPE_API_KEY";

    /** DashScope OpenAI 兼容模式 API 端点 */
    public static final String API_ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    /** 默认模型 */
    public static final String DEFAULT_MODEL = "qwen3-max";

    /** 连接超时（毫秒） */
    public static final int CONNECT_TIMEOUT_MS = 30_000;

    /** 读取超时（毫秒） */
    public static final int READ_TIMEOUT_MS = 60_000;

    /** 注入的 API Key（最高优先级） */
    private final String injectedApiKey;

    /**
     * 默认构造函数，不从构造函数注入 API Key。
     * <p>API Key 将从系统属性或环境变量中读取。</p>
     */
    public DashScopeLLMAdapter() {
        this(null);
    }

    /**
     * 带参数的构造函数，注入 API Key。
     *
     * @param apiKey API Key，如果为 null 或空，则从系统属性或环境变量读取
     */
    public DashScopeLLMAdapter(String apiKey) {
        this.injectedApiKey = apiKey;
    }

    /**
     * 获取适配器名称。
     *
     * @return 固定返回 "dashscope"
     */
    @Override
    public String getName() {
        return "dashscope";
    }

    /**
     * 同步调用 DashScope LLM。
     *
     * @param prompt 用户输入的 Prompt 文本
     * @param options 调用选项
     * @return 模型生成的响应文本
     * @throws LLMInvocationException 调用失败时抛出
     * @throws IllegalArgumentException 当 prompt 为 null 或空时
     */
    @Override
    public String invoke(String prompt, Map<String, Object> options) throws LLMInvocationException {
        // 1. 输入验证
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }

        // 2. 获取 API Key
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new LLMInvocationException("dashscope",
                    "API Key not configured. Set system property '" + SYSTEM_PROPERTY_API_KEY +
                            "' or environment variable '" + ENV_VAR_API_KEY + "'");
        }

        // 3. 构建请求 JSON
        String requestBody = buildRequestJson(prompt, options);

        logger.info("==================== [DashScope LLM 调用开始] ====================");
        logger.info("请求模型: {}", options != null && options.containsKey("model") ? options.get("model") : DEFAULT_MODEL);
        logger.info("请求 Prompt (前500字符): {}", prompt.length() > 500 ? prompt.substring(0, 500) + "..." : prompt);
        logger.debug("完整请求体: {}", requestBody);

        HttpURLConnection connection = null;
        try {
            // 4. 创建 HTTP 连接
            URL url = new URL(API_ENDPOINT);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setDoOutput(true);

            // 5. 设置请求头
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);

            // 6. 发送请求体
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 7. 获取响应状态码
            int statusCode = connection.getResponseCode();
            logger.info("响应状态码: {}", statusCode);

            // 8. 读取响应
            String responseBody;
            if (statusCode >= 200 && statusCode < 300) {
                responseBody = readStream(connection.getInputStream());
            } else {
                responseBody = readStream(connection.getErrorStream());
                logger.error("错误响应: {}", responseBody);
                handleErrorResponse(statusCode, responseBody);
                return null; // 不会到达这里，handleErrorResponse 会抛出异常
            }

            logger.debug("完整响应体: {}", responseBody);

            // 9. 提取并返回 content
            String content = extractContent(responseBody);
            logger.info("模型响应 (前500字符): {}", content.length() > 500 ? content.substring(0, 500) + "..." : content);
            logger.info("==================== [DashScope LLM 调用结束] ====================");
            return content;

        } catch (IOException e) {
            logger.error("DashScope LLM 调用异常: {}", e.getMessage(), e);
            throw new LLMInvocationException("dashscope", "Network error: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 读取输入流内容为字符串。
     *
     * @param inputStream 输入流
     * @return 读取的字符串内容
     * @throws IOException 读取失败时
     */
    private String readStream(java.io.InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * 异步调用 DashScope LLM。
     *
     * @param prompt 用户输入的 Prompt 文本
     * @param options 调用选项
     * @return 包含响应文本的 CompletableFuture
     */
    @Override
    public CompletableFuture<String> invokeAsync(String prompt, Map<String, Object> options) {
        return CompletableFuture.supplyAsync(() -> invoke(prompt, options));
    }

    /**
     * 检查适配器是否可用。
     *
     * @return 如果 API Key 已配置，返回 true；否则返回 false
     */
    @Override
    public boolean isAvailable() {
        String apiKey = getApiKey();
        return apiKey != null && !apiKey.isEmpty();
    }

    /**
     * 获取支持的模型列表。
     *
     * @return 当前版本支持的模型数组：["qwen3-max"]
     */
    @Override
    public String[] getSupportedModels() {
        return new String[]{DEFAULT_MODEL};
    }

    // ========== 内部私有方法 ==========

    /**
     * 获取 API Key。
     *
     * <p>优先级：构造函数注入 > 系统属性 > 环境变量</p>
     *
     * @return API Key 字符串，未配置时返回 null
     */
    private String getApiKey() {
        // 1. 优先使用注入的 API Key
        if (injectedApiKey != null && !injectedApiKey.trim().isEmpty()) {
            return injectedApiKey.trim();
        }

        // 2. 从系统属性读取
        String apiKey = System.getProperty(SYSTEM_PROPERTY_API_KEY);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return apiKey.trim();
        }

        // 3. 从环境变量读取
        apiKey = System.getenv(ENV_VAR_API_KEY);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return apiKey.trim();
        }

        return null;
    }

    /**
     * 构建请求 JSON 字符串。
     *
     * @param prompt 用户 Prompt
     * @param options 调用选项
     * @return JSON 格式的请求体
     */
    private String buildRequestJson(String prompt, Map<String, Object> options) {
        String model = DEFAULT_MODEL;
        if (options != null && options.containsKey("model")) {
            Object modelObj = options.get("model");
            if (modelObj != null) {
                model = modelObj.toString();
            }
        }

        // 转义 prompt 中的特殊字符
        String escapedPrompt = escapeJsonString(prompt);

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"model\":\"").append(model).append("\",");
        json.append("\"messages\":[{\"role\":\"user\",\"content\":\"").append(escapedPrompt).append("\"}]");

        // 可选参数
        if (options != null) {
            if (options.containsKey("temperature")) {
                json.append(",\"temperature\":").append(options.get("temperature"));
            }
            if (options.containsKey("max_tokens")) {
                json.append(",\"max_tokens\":").append(options.get("max_tokens"));
            }
            if (options.containsKey("top_p")) {
                json.append(",\"top_p\":").append(options.get("top_p"));
            }
        }

        json.append("}");
        return json.toString();
    }

    /**
     * 转义 JSON 字符串中的特殊字符。
     *
     * @param str 原始字符串
     * @return 转义后的字符串
     */
    private String escapeJsonString(String str) {
        if (str == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * 从响应 JSON 中提取 content。
     *
     * @param responseJson API 响应 JSON
     * @return 提取的 content 字段值
     * @throws LLMInvocationException 当响应格式异常时
     */
    private String extractContent(String responseJson) throws LLMInvocationException {
        // 查找 "choices" 数组
        int choicesIndex = responseJson.indexOf("\"choices\"");
        if (choicesIndex == -1) {
            throw new LLMInvocationException("dashscope", "Invalid response: missing 'choices' field");
        }

        // 查找 "content" 字段
        int contentIndex = responseJson.indexOf("\"content\"", choicesIndex);
        if (contentIndex == -1) {
            throw new LLMInvocationException("dashscope", "Empty response: no content in choices");
        }

        // 定位 content 值的起始位置
        int colonIndex = responseJson.indexOf(":", contentIndex);
        if (colonIndex == -1) {
            throw new LLMInvocationException("dashscope", "Invalid response: malformed content field");
        }

        // 跳过空白和引号
        int start = colonIndex + 1;
        while (start < responseJson.length() && Character.isWhitespace(responseJson.charAt(start))) {
            start++;
        }

        if (start >= responseJson.length() || responseJson.charAt(start) != '"') {
            throw new LLMInvocationException("dashscope", "Invalid response: content value not a string");
        }
        start++; // 跳过开始引号

        // 查找结束引号（处理转义）
        StringBuilder content = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < responseJson.length(); i++) {
            char c = responseJson.charAt(i);
            if (escaped) {
                switch (c) {
                    case '"':
                        content.append('"');
                        break;
                    case '\\':
                        content.append('\\');
                        break;
                    case 'n':
                        content.append('\n');
                        break;
                    case 'r':
                        content.append('\r');
                        break;
                    case 't':
                        content.append('\t');
                        break;
                    case 'b':
                        content.append('\b');
                        break;
                    case 'f':
                        content.append('\f');
                        break;
                    default:
                        content.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return content.toString();
            } else {
                content.append(c);
            }
        }

        throw new LLMInvocationException("dashscope", "Invalid response: unterminated content string");
    }

    /**
     * 处理 HTTP 错误响应。
     *
     * @param statusCode HTTP 状态码
     * @param responseBody 响应体
     * @throws LLMInvocationException 根据错误类型抛出相应异常
     */
    private void handleErrorResponse(int statusCode, String responseBody) throws LLMInvocationException {
        String errorMessage;

        switch (statusCode) {
            case 401:
                errorMessage = "Authentication failed: Invalid API Key";
                break;
            case 429:
                errorMessage = "Rate limit exceeded: Too many requests";
                break;
            case 400:
                errorMessage = "Bad request: " + extractErrorMessage(responseBody);
                break;
            case 500:
            case 502:
            case 503:
            case 504:
                errorMessage = "Server error: DashScope service unavailable (HTTP " + statusCode + ")";
                break;
            default:
                errorMessage = "API error (HTTP " + statusCode + "): " + extractErrorMessage(responseBody);
        }

        throw new LLMInvocationException("dashscope", errorMessage);
    }

    /**
     * 从错误响应 JSON 中提取错误消息。
     *
     * @param responseBody 响应体
     * @return 错误消息
     */
    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return "Unknown error";
        }

        // 尝试提取 "message" 字段
        int messageIndex = responseBody.indexOf("\"message\"");
        if (messageIndex != -1) {
            int colonIndex = responseBody.indexOf(":", messageIndex);
            if (colonIndex != -1) {
                int start = colonIndex + 1;
                while (start < responseBody.length() && Character.isWhitespace(responseBody.charAt(start))) {
                    start++;
                }
                if (start < responseBody.length() && responseBody.charAt(start) == '"') {
                    start++;
                    int end = responseBody.indexOf("\"", start);
                    if (end != -1) {
                        return responseBody.substring(start, end);
                    }
                }
            }
        }

        // 返回原始响应（截断）
        return responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
    }
}