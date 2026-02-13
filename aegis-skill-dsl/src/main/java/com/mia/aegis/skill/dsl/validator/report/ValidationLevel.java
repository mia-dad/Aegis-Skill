package com.mia.aegis.skill.dsl.validator.report;

/**
 * 校验问题严重级别。
 */
public enum ValidationLevel {
    /** 严重错误，技能无法执行 */
    ERROR,
    /** 潜在问题，可能影响执行 */
    WARNING,
    /** 最佳实践建议，不影响执行 */
    SUGGESTION
}
