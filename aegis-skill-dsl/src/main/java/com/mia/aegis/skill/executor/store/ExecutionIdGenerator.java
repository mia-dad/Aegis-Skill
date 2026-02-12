package com.mia.aegis.skill.executor.store;

import java.util.UUID;

/**
 * 执行 ID 生成器。
 *
 * <p>生成格式为 {@code exec-{UUID}} 的执行标识符。</p>
 *
 * <p>示例：{@code exec-550e8400-e29b-41d4-a716-446655440000}</p>
 */
public final class ExecutionIdGenerator {

    private static final String PREFIX = "exec-";

    private ExecutionIdGenerator() {
        // 工具类，禁止实例化
    }

    /**
     * 生成新的执行 ID。
     *
     * @return 格式为 exec-{UUID} 的执行标识符
     */
    public static String generate() {
        return PREFIX + UUID.randomUUID().toString();
    }

    /**
     * 验证执行 ID 格式是否有效。
     *
     * @param executionId 待验证的执行 ID
     * @return 如果格式有效返回 true
     */
    public static boolean isValid(String executionId) {
        if (executionId == null || executionId.isEmpty()) {
            return false;
        }
        if (!executionId.startsWith(PREFIX)) {
            return false;
        }
        String uuidPart = executionId.substring(PREFIX.length());
        return isValidUUID(uuidPart);
    }

    /**
     * 从执行 ID 中提取 UUID 部分。
     *
     * @param executionId 执行 ID
     * @return UUID 字符串，如果格式无效返回 null
     */
    public static String extractUUID(String executionId) {
        if (!isValid(executionId)) {
            return null;
        }
        return executionId.substring(PREFIX.length());
    }

    private static boolean isValidUUID(String uuidString) {
        if (uuidString == null || uuidString.isEmpty()) {
            return false;
        }
        try {
            UUID.fromString(uuidString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
