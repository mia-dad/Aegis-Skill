package com.mia.aegis.skill.exception;

/**
 * Skill 异常基类。
 *
 * <p>所有 Skill 相关异常的父类。</p>
 */
public class SkillException extends RuntimeException {

    /**
     * 创建 Skill 异常。
     *
     * @param message 错误信息
     */
    public SkillException(String message) {
        super(message);
    }

    /**
     * 创建 Skill 异常（带原因）。
     *
     * @param message 错误信息
     * @param cause 原始异常
     */
    public SkillException(String message, Throwable cause) {
        super(message, cause);
    }
}
