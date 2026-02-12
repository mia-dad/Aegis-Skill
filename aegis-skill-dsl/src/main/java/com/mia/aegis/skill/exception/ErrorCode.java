package com.mia.aegis.skill.exception;

/**
 * 错误代码枚举。
 *
 * <p>定义系统中所有的错误代码，用于统一错误处理和国际化支持。</p>
 *
 * <p>错误代码格式：</p>
 * <ul>
 *   <li>SKILL_* - Skill 定义和解析相关错误</li>
 *   <li>EXEC_* - Skill 执行相关错误</li>
 *   <li>COND_* - 条件表达式相关错误</li>
 *   <li>TEMPLATE_* - 模板渲染相关错误</li>
 *   <li>LLM_* - LLM 调用相关错误</li>
 *   <li>STORE_* - 执行存储相关错误</li>
 * </ul>
 */
public enum ErrorCode {

    // ==================== Skill 定义和解析错误 ====================

    /**
     * Skill 定义无效。
     */
    SKILL_INVALID("SKILL_001", "skill.invalid"),

    /**
     * Skill 名称为空。
     */
    SKILL_NAME_EMPTY("SKILL_002", "skill.name.empty"),

    /**
     * Skill 解析失败。
     */
    SKILL_PARSE_ERROR("SKILL_003", "skill.parse.error"),

    /**
     * Skill 校验失败。
     */
    SKILL_VALIDATION_ERROR("SKILL_004", "skill.validation.error"),

    /**
     * Skill 未找到。
     */
    SKILL_NOT_FOUND("SKILL_005", "skill.notFound"),

    /**
     * Step 名称重复。
     */
    SKILL_DUPLICATE_STEP("SKILL_006", "skill.duplicate.step"),

    /**
     * Step 引用不存在。
     */
    SKILL_STEP_NOT_FOUND("SKILL_007", "skill.step.notFound"),

    /**
     * 参数名称重复。
     */
    SKILL_DUPLICATE_PARAM("SKILL_008", "skill.duplicate.param"),

    // ==================== Skill 执行错误 ====================

    /**
     * Skill 执行失败。
     */
    EXEC_FAILED("EXEC_001", "skill.execution.failed"),

    /**
     * Step 执行失败。
     */
    EXEC_STEP_FAILED("EXEC_002", "skill.execution.step.failed"),

    /**
     * 参数缺失。
     */
    EXEC_MISSING_PARAM("EXEC_003", "skill.execution.missing.param"),

    /**
     * 参数类型不匹配。
     */
    EXEC_PARAM_TYPE_MISMATCH("EXEC_004", "skill.execution.param.typeMismatch"),

    /**
     * 条件评估失败。
     */
    EXEC_CONDITION_FAILED("EXEC_005", "skill.execution.condition.failed"),

    /**
     * 模板渲染失败。
     */
    EXEC_TEMPLATE_FAILED("EXEC_006", "skill.execution.template.failed"),

    /**
     * LLM 调用失败。
     */
    EXEC_LLM_FAILED("EXEC_007", "skill.execution.llm.failed"),

    /**
     * 工具调用失败。
     */
    EXEC_TOOL_FAILED("EXEC_008", "skill.execution.tool.failed"),

    /**
     * 执行超时。
     */
    EXEC_TIMEOUT("EXEC_009", "skill.execution.timeout"),

    /**
     * 执行被中断。
     */
    EXEC_INTERRUPTED("EXEC_010", "skill.execution.interrupted"),

    // ==================== 条件表达式错误 ====================

    /**
     * 条件表达式解析失败。
     */
    COND_PARSE_ERROR("COND_001", "condition.parse.error"),

    /**
     * 条件表达式语法错误。
     */
    COND_SYNTAX_ERROR("COND_002", "condition.syntax.error"),

    /**
     * 条件表达式求值失败。
     */
    COND_EVALUATION_ERROR("COND_003", "condition.evaluation.error"),

    /**
     * 变量未定义。
     */
    COND_UNDEFINED_VARIABLE("COND_004", "condition.undefined.variable"),

    // ==================== 模板渲染错误 ====================

    /**
     * 模板渲染失败。
     */
    TEMPLATE_RENDER_ERROR("TEMPLATE_001", "template.render.error"),

    /**
     * 模板变量未定义。
     */
    TEMPLATE_UNDEFINED_VARIABLE("TEMPLATE_002", "template.undefined.variable"),

    // ==================== LLM 调用错误 ====================

    /**
     * LLM 调用失败。
     */
    LLM_INVOCATION_ERROR("LLM_001", "llm.invocation.error"),

    /**
     * LLM 返回无效响应。
     */
    LLM_INVALID_RESPONSE("LLM_002", "llm.invalid.response"),

    /**
     * LLM 超时。
     */
    LLM_TIMEOUT("LLM_003", "llm.timeout"),

    // ==================== 执行存储错误 ====================

    /**
     * 执行未找到。
     */
    STORE_EXECUTION_NOT_FOUND("STORE_001", "store.execution.notFound"),

    /**
     * 执行已完成。
     */
    STORE_EXECUTION_COMPLETED("STORE_002", "store.execution.completed"),

    /**
     * 执行已过期。
     */
    STORE_EXECUTION_EXPIRED("STORE_003", "store.execution.expired"),

    /**
     * 执行恢复失败。
     */
    STORE_RESUME_FAILED("STORE_004", "store.resume.failed"),

    /**
     * 快照保存失败。
     */
    STORE_SAVE_FAILED("STORE_005", "store.save.failed"),

    // ==================== Document 错误 ====================

    /**
     * Document 校验失败。
     */
    DOC_VALIDATION_ERROR("DOC_001", "document.validation.error"),

    /**
     * Document 构建失败。
     */
    DOC_BUILD_ERROR("DOC_002", "document.build.error"),

    /**
     * Document Block 错误。
     */
    DOC_BLOCK_ERROR("DOC_003", "document.block.error"),

    /**
     * Chart 规格错误。
     */
    DOC_CHART_ERROR("DOC_004", "document.chart.error"),

    /**
     * Series 数据错误。
     */
    DOC_SERIES_ERROR("DOC_005", "document.series.error"),

    // ==================== 通用错误 ====================

    /**
     * 未知错误。
     */
    UNKNOWN_ERROR("UNKNOWN", "error.unknown");

    private final String code;
    private final String messageKey;

    ErrorCode(String code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

    /**
     * 获取错误代码。
     *
     * @return 错误代码字符串
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取国际化消息的 key。
     *
     * @return 消息 key
     */
    public String getMessageKey() {
        return messageKey;
    }

    /**
     * 根据错误代码查找枚举值。
     *
     * @param code 错误代码字符串
     * @return ErrorCode 枚举值，未找到返回 UNKNOWN_ERROR
     */
    public static ErrorCode fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return UNKNOWN_ERROR;
        }
        for (ErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return UNKNOWN_ERROR;
    }
}
