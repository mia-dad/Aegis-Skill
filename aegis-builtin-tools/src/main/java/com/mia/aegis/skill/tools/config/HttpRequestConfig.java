package com.mia.aegis.skill.tools.config;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * HTTP 请求配置。
 *
 * <p>管理 HTTP 请求的安全限制和默认配置。</p>
 *
 * <h3>安全机制</h3>
 * <ul>
 *   <li>域名白名单：限制可访问的域名</li>
 *   <li>协议限制：默认只允许 HTTPS</li>
 *   <li>超时控制：防止请求挂起</li>
 *   <li>大小限制：防止响应过大</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * HttpRequestConfig config = new HttpRequestConfig();
 * config.addAllowedDomain("api.example.com");
 * config.addAllowedDomain("data.example.com");
 * config.setAllowHttp(false); // 只允许 HTTPS
 * config.setDefaultTimeout(30000);
 * }</pre>
 */
@Component

public class HttpRequestConfig {

    /** 默认连接超时（毫秒） */
    public static final int DEFAULT_CONNECT_TIMEOUT = 10000;

    /** 默认读取超时（毫秒） */
    public static final int DEFAULT_READ_TIMEOUT = 30000;

    /** 默认最大响应大小（字节）：10MB */
    public static final int DEFAULT_MAX_RESPONSE_SIZE = 10 * 1024 * 1024;

    private final Set<String> allowedDomains;
    private final Map<String, String> defaultHeaders;
    private int connectTimeout;
    private int readTimeout;
    private int maxResponseSize;
    private boolean allowHttp;
    private boolean allowAnyDomain;

    /**
     * 构造 HTTP 请求配置（默认设置）。
     */
    public HttpRequestConfig() {
        this.allowedDomains = new HashSet<String>();
        this.defaultHeaders = new HashMap<String, String>();
        this.connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        this.readTimeout = DEFAULT_READ_TIMEOUT;
        this.maxResponseSize = DEFAULT_MAX_RESPONSE_SIZE;
        this.allowHttp = false;
        this.allowAnyDomain = false;
    }

    /**
     * 添加允许的域名。
     *
     * @param domain 域名（如 "api.example.com"）
     */
    public void addAllowedDomain(String domain) {
        if (domain != null && !domain.trim().isEmpty()) {
            allowedDomains.add(domain.trim().toLowerCase());
        }
    }

    /**
     * 移除允许的域名。
     *
     * @param domain 域名
     */
    public void removeAllowedDomain(String domain) {
        if (domain != null) {
            allowedDomains.remove(domain.trim().toLowerCase());
        }
    }

    /**
     * 检查域名是否允许访问。
     *
     * @param domain 域名
     * @return 如果允许返回 true
     */
    public boolean isDomainAllowed(String domain) {
        if (allowAnyDomain) {
            return true;
        }
        if (domain == null || allowedDomains.isEmpty()) {
            return allowAnyDomain;
        }
        return allowedDomains.contains(domain.trim().toLowerCase());
    }

    /**
     * 检查 URL 是否允许访问。
     *
     * @param url 完整 URL
     * @return 如果允许返回 true
     */
    public boolean isUrlAllowed(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String lowerUrl = url.trim().toLowerCase();

        // 检查协议
        if (lowerUrl.startsWith("http://") && !allowHttp) {
            return false;
        }
        if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
            return false;
        }

        // 检查域名
        if (allowAnyDomain) {
            return true;
        }

        String domain = extractDomain(url);
        return isDomainAllowed(domain);
    }

    /**
     * 从 URL 提取域名。
     *
     * @param url URL
     * @return 域名
     */
    private String extractDomain(String url) {
        try {
            String noProtocol = url.replaceFirst("^https?://", "");
            int pathStart = noProtocol.indexOf('/');
            String hostPort = pathStart > 0 ? noProtocol.substring(0, pathStart) : noProtocol;
            int portStart = hostPort.indexOf(':');
            return portStart > 0 ? hostPort.substring(0, portStart) : hostPort;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 设置默认请求头。
     *
     * @param name  头名称
     * @param value 头值
     */
    public void setDefaultHeader(String name, String value) {
        if (name != null && !name.trim().isEmpty()) {
            defaultHeaders.put(name.trim(), value);
        }
    }

    /**
     * 获取默认请求头。
     *
     * @return 不可变的默认请求头 Map
     */
    public Map<String, String> getDefaultHeaders() {
        return Collections.unmodifiableMap(new HashMap<String, String>(defaultHeaders));
    }

    /**
     * 获取连接超时时间。
     *
     * @return 连接超时（毫秒）
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 设置连接超时时间。
     *
     * @param connectTimeout 连接超时（毫秒），必须 > 0
     */
    public void setConnectTimeout(int connectTimeout) {
        if (connectTimeout <= 0) {
            throw new IllegalArgumentException("connectTimeout must be positive");
        }
        this.connectTimeout = connectTimeout;
    }

    /**
     * 获取读取超时时间。
     *
     * @return 读取超时（毫秒）
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * 设置读取超时时间。
     *
     * @param readTimeout 读取超时（毫秒），必须 > 0
     */
    public void setReadTimeout(int readTimeout) {
        if (readTimeout <= 0) {
            throw new IllegalArgumentException("readTimeout must be positive");
        }
        this.readTimeout = readTimeout;
    }

    /**
     * 获取最大响应大小。
     *
     * @return 最大响应大小（字节）
     */
    public int getMaxResponseSize() {
        return maxResponseSize;
    }

    /**
     * 设置最大响应大小。
     *
     * @param maxResponseSize 最大响应大小（字节），必须 > 0
     */
    public void setMaxResponseSize(int maxResponseSize) {
        if (maxResponseSize <= 0) {
            throw new IllegalArgumentException("maxResponseSize must be positive");
        }
        this.maxResponseSize = maxResponseSize;
    }

    /**
     * 是否允许 HTTP（非 HTTPS）请求。
     *
     * @return 如果允许 HTTP 返回 true
     */
    public boolean isAllowHttp() {
        return allowHttp;
    }

    /**
     * 设置是否允许 HTTP 请求。
     *
     * @param allowHttp true 允许 HTTP，false 只允许 HTTPS
     */
    public void setAllowHttp(boolean allowHttp) {
        this.allowHttp = allowHttp;
    }

    /**
     * 是否允许任意域名。
     *
     * @return 如果允许任意域名返回 true
     */
    public boolean isAllowAnyDomain() {
        return allowAnyDomain;
    }

    /**
     * 设置是否允许任意域名。
     *
     * <p>警告：在生产环境中应该保持为 false</p>
     *
     * @param allowAnyDomain true 允许任意域名
     */
    public void setAllowAnyDomain(boolean allowAnyDomain) {
        this.allowAnyDomain = allowAnyDomain;
    }

    /**
     * 获取所有允许的域名。
     *
     * @return 不可变的域名集合
     */
    public Set<String> getAllowedDomains() {
        return Collections.unmodifiableSet(new HashSet<String>(allowedDomains));
    }
}

