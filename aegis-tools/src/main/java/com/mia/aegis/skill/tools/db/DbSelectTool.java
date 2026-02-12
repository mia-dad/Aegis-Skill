package com.aegis.skill.api.tools.builtin.db;

import com.aegis.skill.api.config.DataSourceRegistry;
import com.aegis.skill.api.config.DatabaseToolsConfig;
import com.aegis.skill.spi.ToolSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库查询工具。
 *
 * <p>从数据库表中查询数据，支持列选择、条件过滤、排序和分页。</p>
 *
 * <h3>输入参数</h3>
 * <ul>
 *   <li><b>datasource</b> (string, required): 数据源名称</li>
 *   <li><b>table</b> (string, required): 表名</li>
 *   <li><b>columns</b> (array, optional): 查询的列名列表（默认查询所有列）</li>
 *   <li><b>where</b> (object, optional): WHERE 条件映射</li>
 *   <li><b>orderBy</b> (string, optional): 排序子句</li>
 *   <li><b>limit</b> (integer, optional): 返回行数限制</li>
 * </ul>
 *
 * <h3>输出格式</h3>
 * <pre>
 * {
 *   "rows": [
 *     {"column1": value1, "column2": value2},
 *     ...
 *   ]
 * }
 * </pre>
 *
 * <h3>安全特性</h3>
 * <ul>
 *   <li>表名和列名仅允许字母、数字、下划线</li>
 *   <li>所有值通过 PreparedStatement 参数化</li>
 *   <li>orderBy 仅允许列名 + ASC/DESC</li>
 *   <li>结果集受 max-rows 配置限制</li>
 * </ul>
 */
public class DbSelectTool extends AbstractDbTool {

    private static final Logger logger = LoggerFactory.getLogger(DbSelectTool.class);

    /**
     * 构造函数。
     *
     * @param config 数据库工具配置
     * @param dataSourceRegistry 数据源注册表
     */
    public DbSelectTool(DatabaseToolsConfig config, DataSourceRegistry dataSourceRegistry) {
        super(config, dataSourceRegistry);
    }

    @Override
    public String getName() {
        return "db_select";
    }

    @Override
    public String getDescription() {
        return "查询数据库表记录，支持列选择、条件过滤、排序和分页";
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

        // columns parameter (optional)
        parameters.put("columns", ToolSchema.ParameterSpec.optional(
                "array", "查询的列名列表（默认查询所有列）"));

        // where parameter (optional)
        parameters.put("where", ToolSchema.ParameterSpec.optional(
                "object", "WHERE 条件映射（可选，空则查询全表）"));

        // orderBy parameter (optional)
        parameters.put("orderBy", ToolSchema.ParameterSpec.optional(
                "string", "排序子句，如 \"created_at DESC\""));

        // limit parameter (optional)
        parameters.put("limit", ToolSchema.ParameterSpec.optional(
                "integer", "返回行数限制"));

        return new ToolSchema(parameters);
    }

    @Override
    protected void validateRequiredParams(Map<String, Object> input) {
        validateRequiredParams(input, "datasource", "table");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Map<String, Object> input) {
        String datasource = getStringParam(input, "datasource");
        String table = getStringParam(input, "table");

        // Get optional parameters
        List<String> columns = null;
        Object columnsObj = input.get("columns");
        if (columnsObj != null) {
            if (columnsObj instanceof List) {
                columns = (List<String>) columnsObj;
            } else {
                throw DbToolException.invalidParameter("columns", "must be a list", getName());
            }
        }

        Map<String, Object> where = getMapParam(input, "where");
        String orderBy = input.get("orderBy") != null ? input.get("orderBy").toString() : null;
        Integer limit = getIntegerParam(input, "limit");

        // Apply max-rows limit if not specified or exceeds max
        int maxRows = config.getMaxRows();
        if (limit == null || limit > maxRows) {
            limit = maxRows;
        }

        logger.debug("Executing db_select: datasource={}, table={}, columns={}, where={}, orderBy={}, limit={}",
                datasource, table, columns, where != null ? where.keySet() : "null", orderBy, limit);

        // Validate table identifier
        validateIdentifier(table);

        // Validate column identifiers
        if (columns != null && !columns.isEmpty()) {
            for (String column : columns) {
                validateIdentifier(column);
            }
        }

        // Validate where field identifiers
        if (where != null && !where.isEmpty()) {
            validateMapKeys(where);
        }

        // Build SQL
        SqlBuilder.SqlBuildResult sqlResult;
        try {
            sqlResult = SqlBuilder.buildSelect(table, columns, where, orderBy, limit);
        } catch (IllegalArgumentException e) {
            throw DbToolException.invalidIdentifier(e.getMessage(), getName());
        }

        logger.debug("Generated SQL: {}", sqlResult.getSql());

        // Execute SELECT
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection(datasource);
            ps = conn.prepareStatement(sqlResult.getSql());
            sqlResult.bindParameters(ps);

            rs = ps.executeQuery();

            // Convert ResultSet to List<Map>
            List<Map<String, Object>> rows = resultSetToList(rs);

            logger.debug("Select successful: rowCount={}", rows.size());

            // Build result
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("rows", rows);
            result.put("rowCount", rows.size());

            return result;

        } catch (SQLException e) {
            logger.error("SQL execution failed for db_select on table {}: {}", table, e.getMessage());
            throw DbToolException.sqlExecutionError(datasource, getName(), e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeConnection(conn);
        }
    }

    /**
     * 获取整数参数。
     *
     * @param input 输入参数
     * @param key 参数名
     * @return 参数值，如果不存在则返回 null
     */
    private Integer getIntegerParam(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                throw DbToolException.invalidParameter(key, "must be an integer", getName());
            }
        }
        throw DbToolException.invalidParameter(key, "must be an integer", getName());
    }

    /**
     * 将 ResultSet 转换为 List<Map>。
     *
     * @param rs ResultSet
     * @return 行数据列表
     * @throws SQLException 如果读取失败
     */
    private List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new HashMap<String, Object>();
            for (int i = 1; i <= columnCount; i++) {
                // Use lowercase column name for consistency across different databases
                String columnName = metaData.getColumnLabel(i).toLowerCase();
                Object value = getColumnValue(rs, i, metaData.getColumnType(i));
                row.put(columnName, value);
            }
            rows.add(row);
        }

        return rows;
    }

    /**
     * 获取列值并转换为适当的 Java 类型。
     *
     * @param rs ResultSet
     * @param index 列索引
     * @param sqlType SQL 类型
     * @return 列值
     * @throws SQLException 如果读取失败
     */
    private Object getColumnValue(ResultSet rs, int index, int sqlType) throws SQLException {
        Object value = rs.getObject(index);
        if (rs.wasNull()) {
            return null;
        }

        switch (sqlType) {
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                return rs.getInt(index);
            case Types.BIGINT:
                return rs.getLong(index);
            case Types.FLOAT:
            case Types.REAL:
                return rs.getFloat(index);
            case Types.DOUBLE:
            case Types.NUMERIC:
            case Types.DECIMAL:
                return rs.getDouble(index);
            case Types.BOOLEAN:
            case Types.BIT:
                return rs.getBoolean(index);
            case Types.DATE:
                Date date = rs.getDate(index);
                return date != null ? date.toString() : null;
            case Types.TIME:
                Time time = rs.getTime(index);
                return time != null ? time.toString() : null;
            case Types.TIMESTAMP:
                Timestamp ts = rs.getTimestamp(index);
                return ts != null ? ts.toString() : null;
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
            case Types.NVARCHAR:
            case Types.NCHAR:
            case Types.LONGNVARCHAR:
            case Types.CLOB:
            case Types.NCLOB:
                return rs.getString(index);
            default:
                return value;
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
