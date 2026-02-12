package com.mia.aegis.skill.document.validation;

import java.util.Objects;

/**
 * 验证结果。
 *
 * <p>包含错误级别、位置路径和描述信息。</p>
 *
 * @since 0.3.0
 */
public final class ValidationResult {

    private final ValidationLevel level;
    private final String path;
    private final String message;

    /**
     * 创建验证结果。
     *
     * @param level   错误级别
     * @param path    错误位置路径
     * @param message 错误描述
     * @throws NullPointerException 如果任何参数为 null
     */
    public ValidationResult(ValidationLevel level, String path, String message) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(message, "message must not be null");
        this.level = level;
        this.path = path;
        this.message = message;
    }

    /**
     * 获取错误级别。
     *
     * @return 错误级别
     */
    public ValidationLevel getLevel() {
        return level;
    }

    /**
     * 获取错误位置路径。
     *
     * @return 错误位置路径（如 "blocks[0].chart"）
     */
    public String getPath() {
        return path;
    }

    /**
     * 获取错误描述。
     *
     * @return 错误描述信息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 创建 ERROR 级别的验证结果。
     *
     * @param path    错误位置路径
     * @param message 错误描述
     * @return 新的 ValidationResult 实例
     */
    public static ValidationResult error(String path, String message) {
        return new ValidationResult(ValidationLevel.ERROR, path, message);
    }

    /**
     * 创建 WARNING 级别的验证结果。
     *
     * @param path    错误位置路径
     * @param message 警告描述
     * @return 新的 ValidationResult 实例
     */
    public static ValidationResult warning(String path, String message) {
        return new ValidationResult(ValidationLevel.WARNING, path, message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationResult that = (ValidationResult) o;
        return level == that.level &&
               Objects.equals(path, that.path) &&
               Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, path, message);
    }

    @Override
    public String toString() {
        return level + " at '" + path + "': " + message;
    }
}
