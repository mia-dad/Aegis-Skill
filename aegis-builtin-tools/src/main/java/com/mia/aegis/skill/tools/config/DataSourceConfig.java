package com.mia.aegis.skill.tools.config;

import org.springframework.stereotype.Component;

/**
 * 数据源配置实体。
 *
 * <p>对应单个数据库连接配置，包含连接信息和连接池参数。</p>
 */
@Component

public class DataSourceConfig {

    /** 数据库类型：mysql, postgresql 等 */
    private String type = "mysql";

    /** JDBC URL */
    private String url;

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 连接池大小（默认 10） */
    private int poolSize = 10;

    /** 最小空闲连接数（默认 2） */
    private int minIdle = 2;

    /** 连接超时时间（毫秒，默认 30000） */
    private long connectionTimeout = 30000;

    /** 空闲超时时间（毫秒，默认 600000） */
    private long idleTimeout = 600000;

    /** 最大生命周期（毫秒，默认 1800000） */
    private long maxLifetime = 1800000;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public long getMaxLifetime() {
        return maxLifetime;
    }

    public void setMaxLifetime(long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    @Override
    public String toString() {
        return "DataSourceConfig{" +
                "type='" + type + '\'' +
                ", url='" + (url != null ? url.replaceAll("password=[^&]*", "password=***") : null) + '\'' +
                ", username='" + username + '\'' +
                ", poolSize=" + poolSize +
                '}';
    }
}

