package com.mia.aegis.skill.tools;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.mia.aegis.skill.exception.ToolExecutionException;
import com.mia.aegis.skill.tools.config.HttpRequestConfig;
import com.mia.aegis.skill.tools.ToolOutputContext;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP 请求 Tool。
 *
 * <p>执行 HTTP GET/POST 请求，支持 headers、query 参数和请求体。</p>
 *
 * <h3>输入参数</h3>
 * <ul>
 *   <li><b>url</b> (String, 必填): 请求 URL</li>
 *   <li><b>method</b> (String, 可选): HTTP 方法，默认 GET</li>
 *   <li><b>headers</b> (Map, 可选): 请求头</li>
 *   <li><b>query</b> (Map, 可选): 查询参数</li>
 *   <li><b>body</b> (Object, 可选): 请求体（POST 时）</li>
 *   <li><b>timeout</b> (Integer, 可选): 超时毫秒数</li>
 * </ul>
 *
 * <h3>输出</h3>
 * <pre>{@code
 * {
 *   "statusCode": 200,
 *   "headers": { ... },
 *   "body": { ... } // 或 String
 * }
 * }</pre>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * Map<String, Object> input = new HashMap<>();
 * input.put("url", "https://api.example.com/data");
 * input.put("method", "GET");
 * input.put("headers", Collections.singletonMap("Authorization", "Bearer token"));
 *
 * Object result = httpRequestTool.execute(input);
 * }</pre>
 */
@Component
public class HttpRequestTool extends BuiltInTool {

    private static final String NAME = "http_request";
    private static final String DESCRIPTION = "Execute HTTP GET/POST requests";

    private final HttpRequestConfig config;
    private final ObjectMapper objectMapper;

    /**
     * 构造 HTTP 请求 Tool（默认配置）。
     */
    public HttpRequestTool() {
        this(new HttpRequestConfig());
    }

    /**
     * 构造 HTTP 请求 Tool。
     *
     * @param config HTTP 请求配置
     */
    public HttpRequestTool(HttpRequestConfig config) {
        super(NAME, DESCRIPTION, Category.DATA_ACCESS);
        this.config = config != null ? config : new HttpRequestConfig();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ValidationResult validateInput(Map<String, Object> input) {
        if (input == null) {
            return ValidationResult.failure("Input cannot be null");
        }

        Object urlObj = input.get("url");
        if (urlObj == null || urlObj.toString().trim().isEmpty()) {
            return ValidationResult.failure("url is required");
        }

        String url = urlObj.toString();
        if (!config.isUrlAllowed(url)) {
            return ValidationResult.failure("URL not allowed by security policy: " + url);
        }

        Object methodObj = input.get("method");
        if (methodObj != null) {
            String method = methodObj.toString().toUpperCase();
            if (!method.equals("GET") && !method.equals("POST")) {
                return ValidationResult.failure("method must be GET or POST");
            }
        }

        return ValidationResult.success();
    }

    @Override
    public void execute(Map<String, Object> input, ToolOutputContext output) throws ToolExecutionException {
        ValidationResult validation = validateInput(input);
        if (!validation.isValid()) {
            throw new ToolExecutionException(NAME, validation.getErrorMessage());
        }

        String url = input.get("url").toString();
        String method = getMethod(input);
        Map<String, String> headers = getHeaders(input);
        Map<String, String> query = getQueryParams(input);
        Object body = input.get("body");
        int timeout = getTimeout(input);

        try {
            // 构建完整 URL（带 query 参数）
            String fullUrl = buildUrlWithQuery(url, query);

            // 创建连接
            URL urlObj = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();

            try {
                // 设置请求方法
                conn.setRequestMethod(method);

                // 设置超时
                conn.setConnectTimeout(timeout);
                conn.setReadTimeout(timeout);

                // 设置默认请求头
                for (Map.Entry<String, String> entry : config.getDefaultHeaders().entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }

                // 设置自定义请求头
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }

                // POST 请求体
                if ("POST".equals(method) && body != null) {
                    conn.setDoOutput(true);
                    if (conn.getRequestProperty("Content-Type") == null) {
                        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    }

                    String bodyStr = body instanceof String ? (String) body : objectMapper.writeValueAsString(body);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(bodyStr.getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    }
                }

                // 获取响应
                int statusCode = conn.getResponseCode();
                Map<String, String> responseHeaders = extractResponseHeaders(conn);
                String responseBody = readResponse(conn);

                // 写入输出上下文
                output.put("statusCode", statusCode);
                // 序列化 headers 为 JSON 字符串
                try {
                    output.put("headers", objectMapper.writeValueAsString(responseHeaders));
                } catch (Exception e) {
                    output.put("headers", responseHeaders.toString());
                }
                output.put("body", responseBody);

            } finally {
                conn.disconnect();
            }

        } catch (IOException e) {
            throw new ToolExecutionException(NAME, "HTTP request failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getErrorDescriptions() {
        Map<String, String> errors = new LinkedHashMap<String, String>();
        errors.put("URL_NOT_ALLOWED", "URL not allowed by security policy");
        errors.put("CONNECTION_TIMEOUT", "Connection timed out");
        errors.put("RESPONSE_TOO_LARGE", "Response exceeds maximum size limit");
        errors.put("INVALID_METHOD", "HTTP method must be GET or POST");
        return Collections.unmodifiableMap(errors);
    }

    @Override
    public ToolSchema getInputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<String, ToolSchema.ParameterSpec>();
        params.put("url", ToolSchema.ParameterSpec.builder("string", "Request URL")
                .required()
                .example("https://api.example.com/data")
                .build());
        params.put("method", ToolSchema.ParameterSpec.builder("string", "HTTP method")
                .defaultValue("GET")
                .options("GET", "POST")
                .build());
        params.put("headers", ToolSchema.ParameterSpec.optional("object", "Request headers"));
        params.put("query", ToolSchema.ParameterSpec.optional("object", "Query parameters"));
        params.put("body", ToolSchema.ParameterSpec.optional("object", "Request body (POST)"));
        params.put("timeout", ToolSchema.ParameterSpec.builder("integer", "Timeout in milliseconds")
                .defaultValue(30000)
                .constraint("min", 100)
                .constraint("max", 300000)
                .build());
        return new ToolSchema(params);
    }

    @Override
    public ToolSchema getOutputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<String, ToolSchema.ParameterSpec>();
        params.put("statusCode", ToolSchema.ParameterSpec.required("integer", "HTTP status code"));
        params.put("headers", ToolSchema.ParameterSpec.required("object", "Response headers"));
        params.put("body", ToolSchema.ParameterSpec.required("object", "Response body"));
        return new ToolSchema(params);
    }

    private String getMethod(Map<String, Object> input) {
        Object methodObj = input.get("method");
        return methodObj != null ? methodObj.toString().toUpperCase() : "GET";
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getHeaders(Map<String, Object> input) {
        Object headersObj = input.get("headers");
        if (headersObj instanceof Map) {
            Map<String, String> headers = new HashMap<String, String>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) headersObj).entrySet()) {
                headers.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            return headers;
        }
        return new HashMap<String, String>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getQueryParams(Map<String, Object> input) {
        Object queryObj = input.get("query");
        if (queryObj instanceof Map) {
            Map<String, String> query = new HashMap<String, String>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) queryObj).entrySet()) {
                query.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            return query;
        }
        return new HashMap<String, String>();
    }

    private int getTimeout(Map<String, Object> input) {
        Object timeoutObj = input.get("timeout");
        if (timeoutObj instanceof Number) {
            return ((Number) timeoutObj).intValue();
        }
        return config.getReadTimeout();
    }

    private String buildUrlWithQuery(String url, Map<String, String> query) throws UnsupportedEncodingException {
        if (query == null || query.isEmpty()) {
            return url;
        }

        StringBuilder sb = new StringBuilder(url);
        boolean hasQuery = url.contains("?");

        for (Map.Entry<String, String> entry : query.entrySet()) {
            sb.append(hasQuery ? "&" : "?");
            sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            sb.append("=");
            sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            hasQuery = true;
        }

        return sb.toString();
    }

    private Map<String, String> extractResponseHeaders(HttpURLConnection conn) {
        Map<String, String> headers = new HashMap<String, String>();
        for (int i = 0; ; i++) {
            String name = conn.getHeaderFieldKey(i);
            String value = conn.getHeaderField(i);
            if (name == null && value == null) {
                break;
            }
            if (name != null) {
                headers.put(name, value);
            }
        }
        return headers;
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        InputStream is;
        try {
            is = conn.getInputStream();
        } catch (IOException e) {
            is = conn.getErrorStream();
        }

        if (is == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            int totalBytes = 0;
            while ((line = reader.readLine()) != null) {
                totalBytes += line.length();
                if (totalBytes > config.getMaxResponseSize()) {
                    throw new IOException("Response exceeds maximum size: " + config.getMaxResponseSize());
                }
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        }
    }

    private boolean isJsonResponse(Map<String, String> headers) {
        String contentType = headers.get("Content-Type");
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }
}

