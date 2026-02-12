package com.mia.aegis.skill.exception;

import com.mia.aegis.skill.i18n.MessageUtil;

/**
 * Skill 异常基类。
 *
 * <p>所有 Skill 相关异常的基类，包含错误代码支持。</p>
 */
public class SkillException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 创建异常实例。
     *
     * @param message 错误信息
     */
    public SkillException(String message) {
        super(message);
        this.errorCode = ErrorCode.UNKNOWN_ERROR;
    }

    /**
     * 创建异常实例（带原因）。
     *
     * @param message 错误信息
     * @param cause 原始异常
     */
    public SkillException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.UNKNOWN_ERROR;
    }

    /**
     * 创建异常实例（带错误代码）。
     *
     * @param message 错误信息
     * @param errorCode 错误代码
     */
    public SkillException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : ErrorCode.UNKNOWN_ERROR;
    }

    /**
     * 创建异常实例（带错误代码和原因）。
     *
     * @param message 错误信息
     * @param errorCode 错误代码
     * @param cause 原始异常
     */
    public SkillException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : ErrorCode.UNKNOWN_ERROR;
    }

    /**
     * 创建异常实例（带错误代码，使用国际化消息）。
     *
     * @param errorCode 错误代码
     * @param args 消息参数
     */
    public SkillException(ErrorCode errorCode, Object... args) {
        super(MessageUtil.getMessage(errorCode.getMessageKey(), args));
        this.errorCode = errorCode != null ? errorCode : ErrorCode.UNKNOWN_ERROR;
    }

    /**
     * 创建异常实例（带错误代码和原因，使用国际化消息）。
     *
     * @param errorCode 错误代码
     * @param cause 原始异常
     * @param args 消息参数
     */
    public SkillException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(MessageUtil.getMessage(errorCode.getMessageKey(), args), cause);
        this.errorCode = errorCode != null ? errorCode : ErrorCode.UNKNOWN_ERROR;
    }

    /**
     * 获取错误代码。
     *
     * @return 错误代码
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 获取错误代码字符串。
     *
     * @return 错误代码字符串（例如 "SKILL_001"）
     */
    public String getErrorCodeString() {
        return errorCode.getCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "errorCode='" + errorCode.getCode() + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
