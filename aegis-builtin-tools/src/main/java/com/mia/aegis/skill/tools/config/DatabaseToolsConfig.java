package com.mia.aegis.skill.tools.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据库工具全局配置。
 *
 * <p>使用 Spring Boot ConfigurationProperties 注入配置，支持多数据源。</p>
 *
 * <p>配置示例（application.properties）：</p>
 * <pre>{@code
 * aegis.datasource.sources.main_db.type=mysql
 * aegis.datasource.sources.main_db.url=jdbc:mysql://localhost:3306/aegis
 * aegis.datasource.sources.main_db.username=root
 * aegis.datasource.sources.main_db.password=secret
 * aegis.datasource.sources.main_db.pool-size=10
 *
 * aegis.datasource.max-rows=10000
 * aegis.datasource.query-timeout=30
 * }</pre>
 */
@Component
@ConfigurationProperties(prefix = "aegis.datasource")
public class DatabaseToolsConfig {

    /** 数据源映射：name -> config */
    private Map<String, DataSourceConfig> sources = new HashMap<String, DataSourceConfig>();

    /** 最大返回行数限制（默认 10000） */
    private int maxRows = 10000;

    /** SQL 执行超时时间（秒，默认 30） */
    private int queryTimeout = 30;

    public Map<String, DataSourceConfig> getSources() {
        return sources;
    }

    public void setSources(Map<String, DataSourceConfig> sources) {
        this.sources = sources;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    public int getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    /**
     * 获取指定名称的数据源配置。
     *
     * @param name 数据源名称
     * @return 数据源配置，不存在返回 null
     */
    public DataSourceConfig getDataSource(String name) {
        return sources.get(name);
    }

    /**
     * 检查数据源是否存在。
     *
     * @param name 数据源名称
     * @return 是否存在
     */
    public boolean hasDataSource(String name) {
        return sources.containsKey(name);
    }

    @Override
    public String toString() {
        return "DatabaseToolsConfig{" +
                "sources=" + sources.keySet() +
                ", maxRows=" + maxRows +
                ", queryTimeout=" + queryTimeout +
                '}';
    }
}
