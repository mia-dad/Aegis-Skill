package com.mia.aegis.skill.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mia.aegis.skill.exception.ToolExecutionException;
import com.mia.aegis.skill.tools.BuiltInTool;
import com.mia.aegis.skill.tools.ToolOutputContext;
import com.mia.aegis.skill.tools.ToolSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Web 搜索工具 - 基于百度千帆 Web Search API。
 */
@Component
public class WebSearchTool extends BuiltInTool {

    private static final Logger logger = LoggerFactory.getLogger(WebSearchTool.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSearchConfig config;

    @Autowired(required = false)
    public WebSearchTool(WebSearchConfig config) {
        super("builtin_web_search", "执行网络搜索并返回结果", Category.DATA_ACCESS);
        this.config = config != null ? config : createDefaultConfig();

        // 启动时记录配置信息
        logger.info("WebSearchTool initialized - enabled: {}, endpoint: {}, apiKey: {}",
            this.config.isEnabled(),
            this.config.getEndpoint(),
            maskApiKey(this.config.getApiKey()));
    }

    public WebSearchTool() {
        this(null);
    }

    private WebSearchConfig createDefaultConfig() {
        WebSearchConfig defaultConfig = new WebSearchConfig();
        defaultConfig.setEnabled(false);
        return defaultConfig;
    }

    @Override
    public ToolSchema getInputSchema() {
        Map<String, ToolSchema.ParameterSpec> parameters = new HashMap<String, ToolSchema.ParameterSpec>();
        parameters.put("query", ToolSchema.ParameterSpec.required(
                "string", "搜索查询关键词"));
        return new ToolSchema(parameters);
    }

    @Override
    public ToolSchema getOutputSchema() {
        Map<String, ToolSchema.ParameterSpec> parameters = new HashMap<String, ToolSchema.ParameterSpec>();
        parameters.put("query", ToolSchema.ParameterSpec.required(
                "string", "搜索查询词"));
        parameters.put("resultCount", ToolSchema.ParameterSpec.required(
                "integer", "结果数量"));
        parameters.put("executionTime", ToolSchema.ParameterSpec.required(
                "integer", "执行时间（毫秒）"));
        parameters.put("results", ToolSchema.ParameterSpec.required(
                "array", "搜索结果列表"));
        return new ToolSchema(parameters);
    }

    @Override
    public void execute(Map<String, Object> parameters, ToolOutputContext output) {
        String query = (String) parameters.get("query");
        if (query == null || query.trim().isEmpty()) {
            throw new ToolExecutionException("builtin_web_search", "查询参数不能为空");
        }

        // 如果配置未启用，抛出异常
        if (!config.isEnabled()) {
            throw new ToolExecutionException("builtin_web_search", "WebSearch 功能未启用，请在配置中设置 aegis.websearch.enabled=true");
        }

        // 如果 API Key 未设置，抛出异常
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            throw new ToolExecutionException("builtin_web_search", "WebSearch API Key 未配置，请在配置中设置 aegis.websearch.api-key");
        }

        logger.info("Executing web search for query: {}", query);
        long startTime = System.currentTimeMillis();

        // 调用百度千帆 Web Search API（失败时直接抛出异常）
        Map<String, Object> apiResponse = callBaiduWebSearchAPI(query);
        long executionTime = System.currentTimeMillis() - startTime;

        // 转换 API 响应为统一格式并写入上下文
        Map<String, Object> result = convertToStandardFormat(query, apiResponse, executionTime);
        output.put("query", result.get("query"));
        output.put("resultCount", result.get("resultCount"));
        output.put("executionTime", result.get("executionTime"));
        output.put("results", result.get("results"));
    }

    /**
     * 调用百度千帆 Web Search API
     */
    private Map<String, Object> callBaiduWebSearchAPI(String query) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(config.getEndpoint());
            logger.debug("Calling Baidu Web Search API: {}", config.getEndpoint());

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Appbuilder-Authorization", "Bearer " + config.getApiKey());
            conn.setConnectTimeout(config.getTimeout());
            conn.setReadTimeout(config.getTimeout());
            conn.setDoOutput(true);

            // 构建请求体（新版 API 格式）
            Map<String, Object> requestBody = new HashMap<>();

            // messages 数组
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("content", query);
            message.put("role", "user");
            messages.add(message);
            requestBody.put("messages", messages);

            // search_source
            requestBody.put("search_source", "baidu_search_v2");

            // resource_type_filter
            List<Map<String, Object>> resourceTypeFilter = new ArrayList<>();
            Map<String, Object> filter = new HashMap<>();
            filter.put("type", "web");
            filter.put("top_k", config.getMaxResults());
            resourceTypeFilter.add(filter);
            requestBody.put("resource_type_filter", resourceTypeFilter);

            String jsonRequest = objectMapper.writeValueAsString(requestBody);
            logger.debug("Request body: {}", jsonRequest);
            logger.debug("Request headers: Content-Type=application/json, X-Appbuilder-Authorization=Bearer {}", maskApiKey(config.getApiKey()));

            // 发送请求
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // 读取响应
            int statusCode = conn.getResponseCode();
            String responseMessage = conn.getResponseMessage();

            logger.info("API response status: {} {}", statusCode, responseMessage);

            // 读取响应体（成功或失败）
            String responseBody = readResponse(conn, statusCode);

            if (statusCode != 200) {
                logger.error("API call failed with status {}: {}", statusCode, responseBody);
                throw new ToolExecutionException("builtin_web_search",
                    String.format("百度 Web Search API 调用失败 [HTTP %d]: %s", statusCode, responseBody));
            }

            logger.debug("API response body: {}", responseBody);
            return objectMapper.readValue(responseBody, Map.class);

        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Exception during API call: {}", e.getMessage(), e);
            throw new ToolExecutionException("builtin_web_search",
                "调用百度 Web Search API 时发生异常: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 读取响应体（支持成功和错误响应）
     */
    private String readResponse(HttpURLConnection conn, int statusCode) {
        try {
            BufferedReader reader;
            if (statusCode >= 200 && statusCode < 300) {
                reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();
        } catch (Exception e) {
            logger.error("Failed to read response body: {}", e.getMessage());
            return "Failed to read response: " + e.getMessage();
        }
    }

    /**
     * 将百度 API 响应转换为统一格式
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToStandardFormat(String query, Map<String, Object> apiResponse, long executionTime) {
        Map<String, Object> result = new HashMap<>();
        result.put("executionTime", executionTime);
        result.put("query", query);

        List<Map<String, Object>> standardResults = new ArrayList<>();

        // 提取搜索结果 - 百度千帆新版 API 使用 "references" 字段
        if (apiResponse.containsKey("references")) {
            List<Map<String, Object>> apiResults = (List<Map<String, Object>>) apiResponse.get("references");
            if (apiResults != null) {
                for (Map<String, Object> item : apiResults) {
                    Map<String, Object> standardItem = new HashMap<>();
                    // 直接使用 API 返回的 id，如果没有则使用递增 id
                    standardItem.put("id", item.getOrDefault("id", standardResults.size() + 1));
                    standardItem.put("type", item.getOrDefault("type", "web"));
                    standardItem.put("title", item.getOrDefault("title", ""));
                    standardItem.put("url", item.getOrDefault("url", ""));
                    // 新 API 使用 "content" 作为描述字段
                    standardItem.put("description", item.getOrDefault("content", ""));
                    // 新 API 使用 "date" 作为日期字段
                    standardItem.put("published_date", item.getOrDefault("date", ""));
                    standardItem.put("web_anchor", item.getOrDefault("web_anchor", ""));
                    standardItem.put("icon", item.get("icon"));
                    standardItem.put("image", item.get("image"));
                    standardItem.put("video", item.get("video"));
                    standardResults.add(standardItem);
                }
            }
        }

        result.put("resultCount", standardResults.size());
        result.put("results", standardResults);

        logger.info("Search completed: {} results found in {}ms", standardResults.size(), executionTime);
        return result;
    }

    /**
     * 隐藏 API Key 的敏感部分（仅用于日志）
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 20) {
            return "***";
        }
        return apiKey.substring(0, 10) + "..." + apiKey.substring(apiKey.length() - 5);
    }
}
