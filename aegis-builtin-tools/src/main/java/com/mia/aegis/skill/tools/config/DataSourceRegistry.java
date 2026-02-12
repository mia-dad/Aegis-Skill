package com.mia.aegis.skill.tools.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源注册表。
 *
 * <p>管理多个 HikariCP 连接池，提供按名称获取数据源的能力。</p>
 */
@Component
public class DataSourceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceRegistry.class);

    private final DatabaseToolsConfig config;
    private final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<String, HikariDataSource>();

    /**
     * 构造函数。
     *
     * @param config 数据库工具配置
     */
    @Autowired
    public DataSourceRegistry(DatabaseToolsConfig config) {
        this.config = config;
    }

    /**
     * 初始化所有配置的数据源。
     *
     * <p>在 Spring 容器启动后自动调用。</p>
     */
    @PostConstruct
    public void initialize() {
        if (config.getSources() == null || config.getSources().isEmpty()) {
            logger.info("No datasources configured");
            return;
        }

        for (Map.Entry<String, DataSourceConfig> entry : config.getSources().entrySet()) {
            String name = entry.getKey();
            DataSourceConfig dsConfig = entry.getValue();
            try {
                HikariDataSource ds = createHikariDataSource(name, dsConfig);
                dataSources.put(name, ds);
                logger.info("Initialized datasource: {}", name);
            } catch (Exception e) {
                logger.error("Failed to initialize datasource: {}", name, e);
            }
        }
    }

    /**
     * 关闭所有数据源连接池。
     *
     * <p>在 Spring 容器关闭前自动调用。</p>
     */
    @PreDestroy
    public void shutdown() {
        for (Map.Entry<String, HikariDataSource> entry : dataSources.entrySet()) {
            String name = entry.getKey();
            HikariDataSource ds = entry.getValue();
            try {
                ds.close();
                logger.info("Closed datasource: {}", name);
            } catch (Exception e) {
                logger.error("Failed to close datasource: {}", name, e);
            }
        }
        dataSources.clear();
    }

    /**
     * 获取数据源。
     *
     * @param name 数据源名称
     * @return DataSource 实例
     * @throws IllegalArgumentException 如果数据源不存在
     */
    public DataSource getDataSource(String name) {
        DataSource ds = dataSources.get(name);
        if (ds == null) {
            throw new IllegalArgumentException("Datasource not found: " + name);
        }
        return ds;
    }

    /**
     * 获取数据库连接。
     *
     * @param name 数据源名称
     * @return Connection 实例
     * @throws SQLException 如果获取连接失败
     * @throws IllegalArgumentException 如果数据源不存在
     */
    public Connection getConnection(String name) throws SQLException {
        return getDataSource(name).getConnection();
    }

    /**
     * 检查数据源是否存在。
     *
     * @param name 数据源名称
     * @return 是否存在
     */
    public boolean hasDataSource(String name) {
        return dataSources.containsKey(name);
    }

    /**
     * 检查数据源健康状态。
     *
     * @param name 数据源名称
     * @return 是否健康
     */
    public boolean isHealthy(String name) {
        if (!hasDataSource(name)) {
            return false;
        }
        try {
            Connection conn = getConnection(name);
            boolean valid = conn.isValid(5);
            conn.close();
            return valid;
        } catch (SQLException e) {
            logger.warn("Health check failed for datasource: {}", name, e);
            return false;
        }
    }

    /**
     * 手动注册数据源（用于测试）。
     *
     * @param name 数据源名称
     * @param dataSource 数据源实例
     */
    public void registerDataSource(String name, HikariDataSource dataSource) {
        dataSources.put(name, dataSource);
    }

    /**
     * 创建 HikariCP 数据源。
     *
     * @param name 数据源名称
     * @param config 数据源配置
     * @return HikariDataSource 实例
     */
    private HikariDataSource createHikariDataSource(String name, DataSourceConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("aegis-" + name);
        hikariConfig.setJdbcUrl(config.getUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setMaximumPoolSize(config.getPoolSize());
        hikariConfig.setMinimumIdle(config.getMinIdle());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        hikariConfig.setIdleTimeout(config.getIdleTimeout());
        hikariConfig.setMaxLifetime(config.getMaxLifetime());

        // MySQL specific settings
        if ("mysql".equalsIgnoreCase(config.getType())) {
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        }

        return new HikariDataSource(hikariConfig);
    }
}

