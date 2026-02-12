package com.mia.aegis.skill.document;

import com.mia.aegis.skill.exception.ErrorCode;

/**
 * Document 异常类。
 *
 * <p>用于表示 Document 构建和验证过程中的错误。</p>
 *
 * @since 0.3.0
 */
public class DocumentException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 创建 Document 异常。
     *
     * @param message 错误消息
     * @param errorCode 错误代码
     */
    public DocumentException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 创建 Document 异常（带原因）。
     *
     * @param message 错误消息
     * @param errorCode 错误代码
     * @param cause 原始异常
     */
    public DocumentException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误代码。
     *
     * @return ErrorCode
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
