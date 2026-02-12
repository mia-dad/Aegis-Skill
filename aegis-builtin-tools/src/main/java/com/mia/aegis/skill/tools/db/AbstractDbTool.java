package com.mia.aegis.skill.tools.db;


import com.mia.aegis.skill.tools.ToolOutputContext;
import com.mia.aegis.skill.tools.ToolProvider;
import com.mia.aegis.skill.tools.ToolSchema;
import com.mia.aegis.skill.tools.ValidationResult;
import com.mia.aegis.skill.tools.config.DataSourceRegistry;
import com.mia.aegis.skill.tools.config.DatabaseToolsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * 数据库工具抽象基类。
 *
 * <p>提供所有数据库工具共享的通用功能：</p>
 * <ul>
 *   <li>数据源获取和连接管理</li>
 *   <li>输入参数校验</li>
 *   <li>标识符安全校验</li>
 *   <li>错误处理</li>
 * </ul>
 */
public abstract class AbstractDbTool implements ToolProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final DatabaseToolsConfig config;
    protected final DataSourceRegistry dataSourceRegistry;

    /**
     * 构造函数。
     *
     * @param config 数据库工具配置
     * @param dataSourceRegistry 数据源注册表
     */
    protected AbstractDbTool(DatabaseToolsConfig config, DataSourceRegistry dataSourceRegistry) {
        this.config = config;
        this.dataSourceRegistry = dataSourceRegistry;
    }

    @Override
    public ValidationResult validateInput(Map<String, Object> input) {
        try {
            validateRequiredParams(input);
            return ValidationResult.success();
        } catch (IllegalArgumentException e) {
            return ValidationResult.failure(e.getMessage());
        }
    }

    @Override
    public ToolSchema getOutputSchema() {
        // 子类可覆盖
        return null;
    }

    /**
     * 校验必需参数。
     *
     * @param input 输入参数
     * @param requiredKeys 必需的参数名列表
     * @throws IllegalArgumentException 如果缺少必需参数
     */
    protected void validateRequiredParams(Map<String, Object> input, String... requiredKeys) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }

        for (String key : requiredKeys) {
            if (!input.containsKey(key)) {
                throw new IllegalArgumentException("Missing required parameter: " + key);
            }
            Object value = input.get(key);
            if (value == null) {
                throw new IllegalArgumentException("Parameter cannot be null: " + key);
            }
        }
    }

    /**
     * 校验必需参数（子类需实现）。
     */
    protected abstract void validateRequiredParams(Map<String, Object> input);

    /**
     * 获取字符串参数。
     *
     * @param input 输入参数
     * @param key 参数名
     * @return 参数值
     * @throws DbToolException 如果参数不存在或类型错误
     */
    protected String getStringParam(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) {
            throw DbToolException.invalidParameter(key, "cannot be null", getName());
        }
        if (!(value instanceof String)) {
            throw DbToolException.invalidParameter(key, "must be a string", getName());
        }
        return (String) value;
    }

    /**
     * 获取 Map 参数。
     *
     * @param input 输入参数
     * @param key 参数名
     * @return 参数值
     * @throws DbToolException 如果参数类型错误
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> getMapParam(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map)) {
            throw DbToolException.invalidParameter(key, "must be a map", getName());
        }
        return (Map<String, Object>) value;
    }

    /**
     * 获取 Map 参数（必需）。
     *
     * @param input 输入参数
     * @param key 参数名
     * @return 参数值
     * @throws DbToolException 如果参数不存在或类型错误
     */
    protected Map<String, Object> getRequiredMapParam(Map<String, Object> input, String key) {
        Map<String, Object> value = getMapParam(input, key);
        if (value == null || value.isEmpty()) {
            throw DbToolException.invalidParameter(key, "cannot be null or empty", getName());
        }
        return value;
    }

    /**
     * 获取数据库连接。
     *
     * @param datasourceName 数据源名称
     * @return 数据库连接
     * @throws DbToolException 如果数据源不存在或连接失败
     */
    protected Connection getConnection(String datasourceName) {
        if (!dataSourceRegistry.hasDataSource(datasourceName)) {
            throw DbToolException.datasourceNotFound(datasourceName, getName());
        }

        try {
            return dataSourceRegistry.getConnection(datasourceName);
        } catch (SQLException e) {
            throw DbToolException.connectionFailed(datasourceName, getName(), e);
        }
    }

    /**
     * 安全关闭连接。
     *
     * @param connection 数据库连接
     */
    protected void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.warn("Failed to close connection", e);
            }
        }
    }

    /**
     * 校验标识符（表名/列名）。
     *
     * @param identifier 标识符
     * @throws DbToolException 如果标识符不合法
     */
    protected void validateIdentifier(String identifier) {
        try {
            SqlBuilder.validateIdentifier(identifier);
        } catch (IllegalArgumentException e) {
            throw DbToolException.invalidIdentifier(identifier, getName());
        }
    }

    /**
     * 校验 Map 中所有 key 的标识符合法性。
     *
     * @param map Map 对象
     * @throws DbToolException 如果任何 key 不合法
     */
    protected void validateMapKeys(Map<String, Object> map) {
        if (map == null) {
            return;
        }
        for (String key : map.keySet()) {
            validateIdentifier(key);
        }
    }
}
