package com.aegis.skill.api.tools.builtin.db;

import com.aegis.skill.api.config.DataSourceRegistry;
import com.aegis.skill.api.config.DatabaseToolsConfig;
import com.aegis.skill.spi.ToolSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库更新工具。
 *
 * <p>基于 WHERE 条件更新数据库表中的记录。</p>
 *
 * <h3>输入参数</h3>
 * <ul>
 *   <li><b>datasource</b> (string, required): 数据源名称</li>
 *   <li><b>table</b> (string, required): 表名</li>
 *   <li><b>set</b> (object, required): 更新的字段值映射</li>
 *   <li><b>where</b> (object, required): WHERE 条件映射（不能为空）</li>
 * </ul>
 *
 * <h3>输出格式</h3>
 * <pre>
 * {
 *   "affectedRows": N
 * }
 * </pre>
 *
 * <h3>安全特性</h3>
 * <ul>
 *   <li>表名和字段名仅允许字母、数字、下划线</li>
 *   <li>所有值通过 PreparedStatement 参数化</li>
 *   <li>强制 WHERE 条件非空，防止全表更新</li>
 * </ul>
 */
public class DbUpdateTool extends AbstractDbTool {

    private static final Logger logger = LoggerFactory.getLogger(DbUpdateTool.class);

    /**
     * 构造函数。
     *
     * @param config 数据库工具配置
     * @param dataSourceRegistry 数据源注册表
     */
    public DbUpdateTool(DatabaseToolsConfig config, DataSourceRegistry dataSourceRegistry) {
        super(config, dataSourceRegistry);
    }

    @Override
    public String getName() {
        return "db_update";
    }

    @Override
    public String getDescription() {
        return "基于 WHERE 条件更新数据库表记录，返回影响行数";
    }

    @Override
    public ToolSchema getInputSchema() {
        Map<String, ToolSchema.ParameterSpec> parameters = new HashMap<String, ToolSchema.ParameterSpec>();

        // datasource parameter
        parameters.put("datasource", ToolSchema.ParameterSpec.required(
                "string", "数据源名称（在系统配置中定义）"));

        // table parameter
        parameters.put("table", ToolSchema.ParameterSpec.required(
                "string", "表名（仅允许字母、数字、下划线）"));

        // set parameter
        parameters.put("set", ToolSchema.ParameterSpec.required(
                "object", "更新的字段值映射，key 为列名，value 为值"));

        // where parameter
        parameters.put("where", ToolSchema.ParameterSpec.required(
                "object", "WHERE 条件映射（不能为空，防止全表更新）"));

        return new ToolSchema(parameters);
    }

    @Override
    protected void validateRequiredParams(Map<String, Object> input) {
        validateRequiredParams(input, "datasource", "table", "set", "where");

        // Validate set is a non-empty Map
        Object setObj = input.get("set");
        if (!(setObj instanceof Map)) {
            throw new IllegalArgumentException("Parameter 'set' must be a map");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> set = (Map<String, Object>) setObj;
        if (set.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'set' cannot be empty");
        }

        // Validate where is a non-empty Map
        Object whereObj = input.get("where");
        if (!(whereObj instanceof Map)) {
            throw new IllegalArgumentException("Parameter 'where' must be a map");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> where = (Map<String, Object>) whereObj;
        if (where.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'where' cannot be empty (safety restriction)");
        }
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input) {
        String datasource = getStringParam(input, "datasource");
        String table = getStringParam(input, "table");
        Map<String, Object> set = getRequiredMapParam(input, "set");
        Map<String, Object> where = getMapParam(input, "where");

        logger.debug("Executing db_update: datasource={}, table={}, set={}, where={}",
                datasource, table, set.keySet(), where != null ? where.keySet() : "null");

        // Validate WHERE clause is not empty (safety restriction)
        if (where == null || where.isEmpty()) {
            throw DbToolException.emptyWhereClause(getName());
        }

        // Validate table identifier
        validateIdentifier(table);

        // Validate set field identifiers
        validateMapKeys(set);

        // Validate where field identifiers
        validateMapKeys(where);

        // Build SQL
        SqlBuilder.SqlBuildResult sqlResult;
        try {
            sqlResult = SqlBuilder.buildUpdate(table, set, where);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("WHERE")) {
                throw DbToolException.emptyWhereClause(getName());
            }
            throw DbToolException.invalidIdentifier(e.getMessage(), getName());
        }

        logger.debug("Generated SQL: {}", sqlResult.getSql());

        // Execute UPDATE
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getConnection(datasource);
            ps = conn.prepareStatement(sqlResult.getSql());
            sqlResult.bindParameters(ps);

            int affectedRows = ps.executeUpdate();

            logger.debug("Update successful: affectedRows={}", affectedRows);

            // Build result
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("affectedRows", affectedRows);

            return result;

        } catch (SQLException e) {
            logger.error("SQL execution failed for db_update on table {}: {}", table, e.getMessage());
            throw DbToolException.sqlExecutionError(datasource, getName(), e);
        } finally {
            closeQuietly(ps);
            closeConnection(conn);
        }
    }

    /**
     * 安全关闭 PreparedStatement。
     */
    private void closeQuietly(PreparedStatement ps) {
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                logger.warn("Failed to close PreparedStatement", e);
            }
        }
    }
}
