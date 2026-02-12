package com.mia.aegis.skill.exception;

import java.util.Collections;
import java.util.List;

/**
 * 输入验证异常。
 *
 * <p>当恢复执行时用户提供的输入不符合 InputSchema 定义时抛出。</p>
 *
 * <p>验证失败场景包括：</p>
 * <ul>
 *   <li>缺少必填字段</li>
 *   <li>字段类型不匹配</li>
 *   <li>字段值超出范围</li>
 * </ul>
 *
 * <p>HTTP 映射：400 Bad Request</p>
 */
public class InputValidationException extends AwaitResumeException {

    private final List<String> validationErrors;

    /**
     * 创建输入验证异常。
     *
     * @param executionId 执行标识符
     * @param validationErrors 验证错误列表
     */
    public InputValidationException(String executionId, List<String> validationErrors) {
        super(executionId, buildMessage(validationErrors));
        this.validationErrors = validationErrors != null
            ? Collections.unmodifiableList(validationErrors)
            : Collections.<String>emptyList();
    }

    /**
     * 创建输入验证异常（单个错误）。
     *
     * @param executionId 执行标识符
     * @param validationError 验证错误
     */
    public InputValidationException(String executionId, String validationError) {
        super(executionId, "Input validation failed: " + validationError);
        this.validationErrors = Collections.singletonList(validationError);
    }

    /**
     * 创建输入验证异常（带自定义消息）。
     *
     * @param message 错误信息
     * @param executionId 执行标识符
     * @param validationErrors 验证错误列表
     */
    public InputValidationException(String message, String executionId, List<String> validationErrors) {
        super(executionId, message);
        this.validationErrors = validationErrors != null
            ? Collections.unmodifiableList(validationErrors)
            : Collections.<String>emptyList();
    }

    /**
     * 获取验证错误列表。
     *
     * @return 不可变的验证错误列表
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }

    /**
     * 获取验证错误数量。
     *
     * @return 错误数量
     */
    public int getErrorCount() {
        return validationErrors.size();
    }

    private static String buildMessage(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return "Input validation failed";
        }
        if (errors.size() == 1) {
            return "Input validation failed: " + errors.get(0);
        }
        StringBuilder sb = new StringBuilder("Input validation failed with ");
        sb.append(errors.size()).append(" errors: ");
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(errors.get(i));
        }
        return sb.toString();
    }
}
