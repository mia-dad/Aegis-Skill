package com.aegis.skill.api.tools.builtin.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * SQL 构建器。
 *
 * <p>提供安全的 SQL 构建能力，包含标识符校验和参数化查询支持。</p>
 *
 * <p>安全特性：</p>
 * <ul>
 *   <li>标识符（表名、列名）仅允许字母、数字、下划线</li>
 *   <li>所有值使用 PreparedStatement 参数化</li>
 *   <li>表名和列名使用反引号转义</li>
 * </ul>
 */
public class SqlBuilder {

    /** 合法标识符正则：字母或下划线开头，后续为字母、数字、下划线 */
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    /** 合法的 ORDER BY 方向 */
    private static final Pattern ORDER_DIRECTION_PATTERN = Pattern.compile("^(ASC|DESC)$", Pattern.CASE_INSENSITIVE);

    /**
     * 校验标识符是否合法。
     *
     * @param identifier 标识符（表名或列名）
     * @throws IllegalArgumentException 如果标识符不合法
     */
    public static void validateIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        if (!IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid identifier: " + identifier +
                    ". Only letters, numbers, and underscores are allowed, must start with letter or underscore.");
        }
    }

    /**
     * 转义标识符（使用反引号）。
     *
     * @param identifier 标识符
     * @return 转义后的标识符
     */
    public static String escapeIdentifier(String identifier) {
        validateIdentifier(identifier);
        return "`" + identifier + "`";
    }

    /**
     * 构建 INSERT 语句。
     *
     * @param table 表名
     * @param fields 字段值映射
     * @return SQL 构建结果
     */
    public static SqlBuildResult buildInsert(String table, Map<String, Object> fields) {
        validateIdentifier(table);
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("Fields cannot be null or empty for INSERT");
        }

        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> parameters = new ArrayList<Object>();

        boolean first = true;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            validateIdentifier(entry.getKey());

            if (!first) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append(escapeIdentifier(entry.getKey()));
            placeholders.append("?");
            parameters.add(entry.getValue());
            first = false;
        }

        String sql = "INSERT INTO " + escapeIdentifier(table) +
                " (" + columns + ") VALUES (" + placeholders + ")";

        return new SqlBuildResult(sql, parameters);
    }

    /**
     * 构建 UPDATE 语句。
     *
     * @param table 表名
     * @param set 更新的字段值映射
     * @param where WHERE 条件映射（不能为空）
     * @return SQL 构建结果
     */
    public static SqlBuildResult buildUpdate(String table, Map<String, Object> set, Map<String, Object> where) {
        validateIdentifier(table);
        if (set == null || set.isEmpty()) {
            throw new IllegalArgumentException("SET clause cannot be null or empty for UPDATE");
        }
        if (where == null || where.isEmpty()) {
            throw new IllegalArgumentException("WHERE clause cannot be null or empty for UPDATE (safety restriction)");
        }

        StringBuilder setClause = new StringBuilder();
        List<Object> parameters = new ArrayList<Object>();

        // Build SET clause
        boolean first = true;
        for (Map.Entry<String, Object> entry : set.entrySet()) {
            validateIdentifier(entry.getKey());

            if (!first) {
                setClause.append(", ");
            }
            setClause.append(escapeIdentifier(entry.getKey())).append(" = ?");
            parameters.add(entry.getValue());
            first = false;
        }

        // Build WHERE clause
        StringBuilder whereClause = new StringBuilder();
        first = true;
        for (Map.Entry<String, Object> entry : where.entrySet()) {
            validateIdentifier(entry.getKey());

            if (!first) {
                whereClause.append(" AND ");
            }
            whereClause.append(escapeIdentifier(entry.getKey())).append(" = ?");
            parameters.add(entry.getValue());
            first = false;
        }

        String sql = "UPDATE " + escapeIdentifier(table) +
                " SET " + setClause +
                " WHERE " + whereClause;

        return new SqlBuildResult(sql, parameters);
    }

    /**
     * 构建 SELECT 语句。
     *
     * @param table 表名
     * @param columns 列名列表（null 或空表示 SELECT *）
     * @param where WHERE 条件映射（可为 null）
     * @param orderBy 排序子句（可为 null）
     * @param limit 返回行数限制（null 或 <= 0 表示不限制）
     * @return SQL 构建结果
     */
    public static SqlBuildResult buildSelect(String table, List<String> columns,
                                              Map<String, Object> where, String orderBy, Integer limit) {
        validateIdentifier(table);

        StringBuilder sql = new StringBuilder("SELECT ");
        List<Object> parameters = new ArrayList<Object>();

        // Columns
        if (columns == null || columns.isEmpty()) {
            sql.append("*");
        } else {
            boolean first = true;
            for (String column : columns) {
                validateIdentifier(column);
                if (!first) {
                    sql.append(", ");
                }
                sql.append(escapeIdentifier(column));
                first = false;
            }
        }

        sql.append(" FROM ").append(escapeIdentifier(table));

        // WHERE clause
        if (where != null && !where.isEmpty()) {
            sql.append(" WHERE ");
            boolean first = true;
            for (Map.Entry<String, Object> entry : where.entrySet()) {
                validateIdentifier(entry.getKey());

                if (!first) {
                    sql.append(" AND ");
                }
                sql.append(escapeIdentifier(entry.getKey())).append(" = ?");
                parameters.add(entry.getValue());
                first = false;
            }
        }

        // ORDER BY clause
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            sql.append(" ORDER BY ").append(parseOrderBy(orderBy));
        }

        // LIMIT clause
        if (limit != null && limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }

        return new SqlBuildResult(sql.toString(), parameters);
    }

    /**
     * 解析并校验 ORDER BY 子句。
     *
     * <p>支持格式：</p>
     * <ul>
     *   <li>column_name</li>
     *   <li>column_name ASC</li>
     *   <li>column_name DESC</li>
     *   <li>column1 DESC, column2 ASC</li>
     * </ul>
     *
     * @param orderBy ORDER BY 子句
     * @return 校验并转义后的 ORDER BY 子句
     */
    public static String parseOrderBy(String orderBy) {
        if (orderBy == null || orderBy.trim().isEmpty()) {
            return "";
        }

        String[] parts = orderBy.split(",");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) {
                continue;
            }

            String[] tokens = part.split("\\s+");
            if (tokens.length == 0 || tokens.length > 2) {
                throw new IllegalArgumentException("Invalid ORDER BY clause: " + part);
            }

            // Validate column name
            validateIdentifier(tokens[0]);
            if (i > 0) {
                result.append(", ");
            }
            result.append(escapeIdentifier(tokens[0]));

            // Validate direction if present
            if (tokens.length == 2) {
                if (!ORDER_DIRECTION_PATTERN.matcher(tokens[1]).matches()) {
                    throw new IllegalArgumentException("Invalid ORDER BY direction: " + tokens[1]);
                }
                result.append(" ").append(tokens[1].toUpperCase());
            }
        }

        return result.toString();
    }

    /**
     * SQL 构建结果。
     */
    public static class SqlBuildResult {
        private final String sql;
        private final List<Object> parameters;

        public SqlBuildResult(String sql, List<Object> parameters) {
            this.sql = sql;
            this.parameters = parameters;
        }

        public String getSql() {
            return sql;
        }

        public List<Object> getParameters() {
            return parameters;
        }

        /**
         * 绑定参数到 PreparedStatement。
         *
         * @param ps PreparedStatement
         * @throws SQLException 如果绑定失败
         */
        public void bindParameters(PreparedStatement ps) throws SQLException {
            for (int i = 0; i < parameters.size(); i++) {
                setParameter(ps, i + 1, parameters.get(i));
            }
        }

        /**
         * 设置单个参数。
         */
        private void setParameter(PreparedStatement ps, int index, Object value) throws SQLException {
            if (value == null) {
                ps.setNull(index, Types.NULL);
            } else if (value instanceof String) {
                ps.setString(index, (String) value);
            } else if (value instanceof Integer) {
                ps.setInt(index, (Integer) value);
            } else if (value instanceof Long) {
                ps.setLong(index, (Long) value);
            } else if (value instanceof Double) {
                ps.setDouble(index, (Double) value);
            } else if (value instanceof Float) {
                ps.setFloat(index, (Float) value);
            } else if (value instanceof Boolean) {
                ps.setBoolean(index, (Boolean) value);
            } else if (value instanceof Date) {
                ps.setTimestamp(index, new Timestamp(((Date) value).getTime()));
            } else if (value instanceof java.sql.Date) {
                ps.setDate(index, (java.sql.Date) value);
            } else if (value instanceof Timestamp) {
                ps.setTimestamp(index, (Timestamp) value);
            } else {
                ps.setObject(index, value);
            }
        }

        @Override
        public String toString() {
            return "SqlBuildResult{sql='" + sql + "', parameters=" + parameters + '}';
        }
    }
}
