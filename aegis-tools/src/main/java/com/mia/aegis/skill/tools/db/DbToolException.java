package com.aegis.skill.api.tools.builtin.db;

import com.aegis.skill.spi.ToolExecutionException;

/**
 * 数据库工具异常。
 *
 * <p>用于封装数据库操作过程中的各类错误，提供结构化的错误信息。</p>
 */
public class DbToolException extends ToolExecutionException {

    /** 数据源不存在 */
    public static final String ERR_DATASOURCE_NOT_FOUND = "DATASOURCE_NOT_FOUND";

    /** 连接失败 */
    public static final String ERR_CONNECTION_FAILED = "CONNECTION_FAILED";

    /** 非法标识符 */
    public static final String ERR_INVALID_IDENTIFIER = "INVALID_IDENTIFIER";

    /** WHERE 子句为空 */
    public static final String ERR_EMPTY_WHERE = "EMPTY_WHERE_CLAUSE";

    /** SQL 执行错误 */
    public static final String ERR_SQL_EXECUTION = "SQL_EXECUTION_ERROR";

    /** 模板渲染错误 */
    public static final String ERR_TEMPLATE_RENDER = "TEMPLATE_RENDER_ERROR";

    /** 结果集过大 */
    public static final String ERR_RESULT_SET_TOO_LARGE = "RESULT_SET_TOO_LARGE";

    /** 参数错误 */
    public static final String ERR_INVALID_PARAMETER = "INVALID_PARAMETER";

    private final String errorCode;
    private final String datasource;

    /**
     * 构造函数。
     *
     * @param toolName 工具名称
     * @param errorCode 错误代码
     * @param message 错误消息
     */
    public DbToolException(String toolName, String errorCode, String message) {
        super(toolName, message);
        this.errorCode = errorCode;
        this.datasource = null;
    }

    /**
     * 构造函数（带数据源）。
     *
     * @param toolName 工具名称
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param datasource 数据源名称
     */
    public DbToolException(String toolName, String errorCode, String message, String datasource) {
        super(toolName, message);
        this.errorCode = errorCode;
        this.datasource = datasource;
    }

    /**
     * 构造函数（带原因）。
     *
     * @param toolName 工具名称
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param cause 原因
     */
    public DbToolException(String toolName, String errorCode, String message, Throwable cause) {
        super(toolName, message, cause);
        this.errorCode = errorCode;
        this.datasource = null;
    }

    /**
     * 构造函数（完整信息）。
     *
     * @param toolName 工具名称
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param datasource 数据源名称
     * @param cause 原因
     */
    public DbToolException(String toolName, String errorCode, String message, String datasource, Throwable cause) {
        super(toolName, message, cause);
        this.errorCode = errorCode;
        this.datasource = datasource;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDatasource() {
        return datasource;
    }

    /**
     * 创建数据源不存在异常。
     */
    public static DbToolException datasourceNotFound(String datasource, String operation) {
        return new DbToolException(
                operation,
                ERR_DATASOURCE_NOT_FOUND,
                "Datasource not found: " + datasource,
                datasource
        );
    }

    /**
     * 创建连接失败异常。
     */
    public static DbToolException connectionFailed(String datasource, String operation, Throwable cause) {
        return new DbToolException(
                operation,
                ERR_CONNECTION_FAILED,
                "Failed to connect to datasource: " + datasource,
                datasource,
                cause
        );
    }

    /**
     * 创建非法标识符异常。
     */
    public static DbToolException invalidIdentifier(String identifier, String operation) {
        return new DbToolException(
                operation,
                ERR_INVALID_IDENTIFIER,
                "Invalid identifier: " + identifier
        );
    }

    /**
     * 创建 WHERE 子句为空异常。
     */
    public static DbToolException emptyWhereClause(String operation) {
        return new DbToolException(
                operation,
                ERR_EMPTY_WHERE,
                "WHERE clause cannot be empty for " + operation + " (safety restriction)"
        );
    }

    /**
     * 创建 SQL 执行错误异常。
     */
    public static DbToolException sqlExecutionError(String datasource, String operation, Throwable cause) {
        // 避免暴露敏感信息
        String safeMessage = "SQL execution failed";
        if (cause != null && cause.getMessage() != null) {
            // 只保留基本错误类型，不暴露具体 SQL
            String msg = cause.getMessage();
            if (msg.contains("Duplicate entry")) {
                safeMessage = "Duplicate entry error";
            } else if (msg.contains("foreign key")) {
                safeMessage = "Foreign key constraint error";
            } else if (msg.contains("Data truncation")) {
                safeMessage = "Data truncation error";
            }
        }
        return new DbToolException(
                operation,
                ERR_SQL_EXECUTION,
                safeMessage,
                datasource,
                cause
        );
    }

    /**
     * 创建参数错误异常。
     */
    public static DbToolException invalidParameter(String paramName, String reason, String operation) {
        return new DbToolException(
                operation,
                ERR_INVALID_PARAMETER,
                "Invalid parameter '" + paramName + "': " + reason
        );
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DbToolException{");
        sb.append("errorCode='").append(errorCode).append('\'');
        if (datasource != null) {
            sb.append(", datasource='").append(datasource).append('\'');
        }
        sb.append(", message='").append(getMessage()).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
