package com.mia.aegis.skill.executor.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 输出校验结果。
 *
 * <p>包含校验是否通过以及所有错误信息的聚合。</p>
 */
public class OutputValidationResult {

    private final boolean valid;
    private final List<ValidationError> errors;

    private OutputValidationResult(boolean valid, List<ValidationError> errors) {
        this.valid = valid;
        this.errors = errors != null
                ? Collections.unmodifiableList(new ArrayList<ValidationError>(errors))
                : Collections.<ValidationError>emptyList();
    }

    /**
     * 创建成功的校验结果。
     *
     * @return 成功结果
     */
    public static OutputValidationResult success() {
        return new OutputValidationResult(true, null);
    }

    /**
     * 创建失败的校验结果。
     *
     * @param errors 错误列表
     * @return 失败结果
     */
    public static OutputValidationResult failure(List<ValidationError> errors) {
        return new OutputValidationResult(false, errors);
    }

    /**
     * 创建单错误的失败结果。
     *
     * @param error 单个错误
     * @return 失败结果
     */
    public static OutputValidationResult failure(ValidationError error) {
        List<ValidationError> errors = new ArrayList<ValidationError>();
        errors.add(error);
        return new OutputValidationResult(false, errors);
    }

    /**
     * 是否校验通过。
     *
     * @return 是否通过
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * 是否校验失败。
     *
     * @return 是否失败
     */
    public boolean isFailed() {
        return !valid;
    }

    /**
     * 获取所有错误。
     *
     * @return 错误列表（不可变）
     */
    public List<ValidationError> getErrors() {
        return errors;
    }

    /**
     * 获取错误数量。
     *
     * @return 错误数量
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * 获取格式化的错误消息。
     *
     * @return 错误消息字符串
     */
    public String getErrorMessage() {
        if (valid) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Output validation failed with ").append(errors.size()).append(" error(s):");
        for (ValidationError error : errors) {
            sb.append("\n  - ").append(error.toString());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        if (valid) {
            return "OutputValidationResult{valid=true}";
        }
        return "OutputValidationResult{valid=false, errors=" + errors + "}";
    }

    /**
     * 校验错误详情。
     */
    public static class ValidationError {

        private final String field;
        private final String code;
        private final String message;

        /**
         * 创建校验错误。
         *
         * @param field 字段路径
         * @param code 错误代码
         * @param message 错误消息
         */
        public ValidationError(String field, String code, String message) {
            this.field = field;
            this.code = code;
            this.message = message;
        }

        /**
         * 创建缺失必填字段错误。
         *
         * @param field 字段名
         * @return 错误实例
         */
        public static ValidationError missingRequired(String field) {
            return new ValidationError(field, "MISSING_REQUIRED",
                    "Required field '" + field + "' is missing");
        }

        /**
         * 创建类型不匹配错误。
         *
         * @param field 字段名
         * @param expectedType 期望类型
         * @param actualType 实际类型
         * @return 错误实例
         */
        public static ValidationError typeMismatch(String field, String expectedType, String actualType) {
            return new ValidationError(field, "TYPE_MISMATCH",
                    "Field '" + field + "' expected type '" + expectedType + "' but got '" + actualType + "'");
        }

        /**
         * 创建输出为空错误。
         *
         * @return 错误实例
         */
        public static ValidationError nullOutput() {
            return new ValidationError(null, "NULL_OUTPUT", "Output is null");
        }

        /**
         * 创建输出类型错误。
         *
         * @param expectedType 期望类型
         * @param actualType 实际类型
         * @return 错误实例
         */
        public static ValidationError invalidOutputType(String expectedType, String actualType) {
            return new ValidationError(null, "INVALID_OUTPUT_TYPE",
                    "Output expected type '" + expectedType + "' but got '" + actualType + "'");
        }

        public String getField() {
            return field;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            if (field != null) {
                return "[" + code + "] " + field + ": " + message;
            }
            return "[" + code + "] " + message;
        }
    }
}
