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
 * 数据库插入工具。
 *
 * <p>向指定数据库表插入单行数据，返回影响行数和自增主键。</p>
 *
 * <h3>输入参数</h3>
 * <ul>
 *   <li><b>datasource</b> (string, required): 数据源名称</li>
 *   <li><b>table</b> (string, required): 表名</li>
 *   <li><b>fields</b> (object, required): 字段值映射</li>
 * </ul>
 *
 * <h3>输出格式</h3>
 * <pre>
 * {
 *   "affectedRows": 1,
 *   "generatedKey": 12345
 * }
 * </pre>
 *
 * <h3>安全特性</h3>
 * <ul>
 *   <li>表名和字段名仅允许字母、数字、下划线</li>
 *   <li>所有值通过 PreparedStatement 参数化</li>
 * </ul>
 */
public class DbInsertTool extends AbstractDbTool {

    private static final Logger logger = LoggerFactory.getLogger(DbInsertTool.class);

    /**
     * 构造函数。
     *
     * @param config 数据库工具配置
     * @param dataSourceRegistry 数据源注册表
     */
    public DbInsertTool(DatabaseToolsConfig config, DataSourceRegistry dataSourceRegistry) {
        super(config, dataSourceRegistry);
    }

    @Override
    public String getName() {
        return "db_insert";
    }

    @Override
    public String getDescription() {
        return "插入单行数据到数据库表，返回影响行数和自增主键";
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

        // fields parameter
        parameters.put("fields", ToolSchema.ParameterSpec.required(
                "object", "字段值映射，key 为列名，value 为值"));

        return new ToolSchema(parameters);
    }

    @Override
    protected void validateRequiredParams(Map<String, Object> input) {
        validateRequiredParams(input, "datasource", "table", "fields");

        // Validate fields is a non-empty Map
        Object fieldsObj = input.get("fields");
        if (!(fieldsObj instanceof Map)) {
            throw new IllegalArgumentException("Parameter 'fields' must be a map");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) fieldsObj;
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'fields' cannot be empty");
        }
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input) {
        String datasource = getStringParam(input, "datasource");
        String table = getStringParam(input, "table");
        Map<String, Object> fields = getRequiredMapParam(input, "fields");

        logger.debug("Executing db_insert: datasource={}, table={}, fields={}",
                datasource, table, fields.keySet());

        // Validate table identifier
        validateIdentifier(table);

        // Validate field identifiers
        validateMapKeys(fields);

        // Build SQL
        SqlBuilder.SqlBuildResult sqlResult;
        try {
            sqlResult = SqlBuilder.buildInsert(table, fields);
        } catch (IllegalArgumentException e) {
            throw DbToolException.invalidIdentifier(e.getMessage(), getName());
        }

        logger.debug("Generated SQL: {}", sqlResult.getSql());

        // Execute INSERT
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet generatedKeys = null;

        try {
            conn = getConnection(datasource);
            ps = conn.prepareStatement(sqlResult.getSql(), Statement.RETURN_GENERATED_KEYS);
            sqlResult.bindParameters(ps);

            int affectedRows = ps.executeUpdate();

            // Get generated key if available
            Long generatedKey = null;
            generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                generatedKey = generatedKeys.getLong(1);
            }

            logger.debug("Insert successful: affectedRows={}, generatedKey={}", affectedRows, generatedKey);

            // Build result
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("affectedRows", affectedRows);
            result.put("generatedKey", generatedKey);

            return result;

        } catch (SQLException e) {
            logger.error("SQL execution failed for db_insert on table {}: {}", table, e.getMessage());
            throw DbToolException.sqlExecutionError(datasource, getName(), e);
        } finally {
            closeQuietly(generatedKeys);
            closeQuietly(ps);
            closeConnection(conn);
        }
    }

    /**
     * 安全关闭 ResultSet。
     */
    private void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.warn("Failed to close ResultSet", e);
            }
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
