package com.mia.aegis.skill.exception;

/**
 * Skill 执行异常。
 *
 * <p>当 Skill 执行过程中发生错误时抛出，包含失败的 Step 名称。</p>
 */
public class SkillExecutionException extends SkillException {

    private final String stepName;

    /**
     * 创建执行异常。
     *
     * @param stepName 失败的 Step 名称
     * @param message 错误信息
     */
    public SkillExecutionException(String stepName, String message) {
        super(formatMessage(stepName, message));
        this.stepName = stepName;
    }

    /**
     * 创建执行异常（带原因）。
     *
     * @param stepName 失败的 Step 名称
     * @param message 错误信息
     * @param cause 原始异常
     */
    public SkillExecutionException(String stepName, String message, Throwable cause) {
        super(formatMessage(stepName, message), cause);
        this.stepName = stepName;
    }

    /**
     * 创建执行异常（无 Step 名称）。
     *
     * @param message 错误信息
     */
    public SkillExecutionException(String message) {
        super("Skill execution error: " + message);
        this.stepName = null;
    }

    /**
     * 创建执行异常（无 Step 名称，带原因）。
     *
     * @param message 错误信息
     * @param cause 原始异常
     */
    public SkillExecutionException(String message, Throwable cause) {
        super("Skill execution error: " + message, cause);
        this.stepName = null;
    }

    private static String formatMessage(String stepName, String message) {
        if (stepName != null && !stepName.isEmpty()) {
            return "Skill execution error at step '" + stepName + "': " + message;
        }
        return "Skill execution error: " + message;
    }

    /**
     * 获取失败的 Step 名称。
     *
     * @return Step 名称，如果无关联 Step 返回 null
     */
    public String getStepName() {
        return stepName;
    }

    /**
     * 检查是否有关联的 Step。
     *
     * @return 是否有关联 Step
     */
    public boolean hasStepName() {
        return stepName != null && !stepName.isEmpty();
    }

    /**
     * 获取格式化的完整错误消息（包含原因链）。
     *
     * @return 完整错误消息
     */
    public String getFullMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage());

        Throwable cause = getCause();
        int depth = 0;
        while (cause != null && depth < 5) {
            sb.append("\n  Caused by: ").append(cause.getMessage());
            cause = cause.getCause();
            depth++;
        }

        return sb.toString();
    }
}
