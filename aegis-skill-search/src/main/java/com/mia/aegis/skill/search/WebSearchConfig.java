package com.mia.aegis.skill.search;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Web 搜索配置类。
 *
 * <p>配置百度千帆 Web Search API</p>
 */
@Component
@ConfigurationProperties(prefix = "aegis.websearch")
public class WebSearchConfig {

    /**
     * API Key (从百度千帆平台获取)
     */
    private String apiKey;

    /**
     * API Endpoint
     */
    private String endpoint = "https://qianfan.baidubce.com/v2/ai_search/web_search";

    /**
     * 请求超时时间（毫秒）
     */
    private int timeout = 30000;

    /**
     * 最大返回结果数
     */
    private int maxResults = 10;

    /**
     * 是否启用
     */
    private boolean enabled = true;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
