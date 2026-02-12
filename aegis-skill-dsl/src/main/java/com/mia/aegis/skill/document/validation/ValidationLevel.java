package com.mia.aegis.skill.document.validation;

/**
 * 验证错误级别枚举。
 *
 * @since 0.3.0
 */
public enum ValidationLevel {

    /**
     * 致命错误，Document 无效。
     */
    ERROR,

    /**
     * 警告，Document 仍可处理。
     */
    WARNING
}
