package com.mia.aegis.skill.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 校验结果。
 *
 * <p>用于表示 Tool 输入校验或 Output Contract 校验的结果。</p>
 */
public class ValidationResult {

    private final boolean valid;
    private final List<String> errors;

    private ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors != null
                ? Collections.unmodifiableList(new ArrayList<String>(errors))
                : Collections.<String>emptyList();
    }

    /**
     * 创建成功结果。
     *
     * @return 成功的 ValidationResult
     */
    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }

    /**
     * 创建失败结果。
     *
     * @param errors 错误列表
     * @return 失败的 ValidationResult
     */
    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors);
    }

    /**
     * 创建单个错误的失败结果。
     *
     * @param error 错误信息
     * @return 失败的 ValidationResult
     */
    public static ValidationResult failure(String error) {
        List<String> errors = new ArrayList<String>();
        errors.add(error);
        return new ValidationResult(false, errors);
    }

    /**
     * 判断是否有效。
     *
     * @return 是否有效
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * 获取错误列表。
     *
     * @return 错误列表
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * 获取第一个错误。
     *
     * @return 第一个错误信息，无错误返回 null
     */
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }

    /**
     * 获取所有错误的合并字符串。
     *
     * @return 错误信息
     */
    public String getErrorMessage() {
        if (errors.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(errors.get(i));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return valid ? "ValidationResult{valid=true}"
                : "ValidationResult{valid=false, errors=" + errors + '}';
    }
}

