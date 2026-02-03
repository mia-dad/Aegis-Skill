package com.mia.aegis.skill.exception;

import com.mia.aegis.skill.i18n.MessageUtil;

/**
 * Skill 解析异常。
 *
 * <p>当 DSL 解析过程中发生错误时抛出，包含错误位置信息。</p>
 */
public class SkillParseException extends SkillException {

    private final int line;
    private final int column;

    /**
     * 创建解析异常。
     *
     * @param message 错误信息
     * @param line 错误行号（1-based）
     * @param column 错误列号（1-based）
     */
    public SkillParseException(String message, int line, int column) {
        super(formatMessage(message, line, column));
        this.line = line;
        this.column = column;
    }

    /**
     * 创建解析异常（仅行号）。
     *
     * @param message 错误信息
     * @param line 错误行号
     */
    public SkillParseException(String message, int line) {
        this(message, line, 0);
    }

    /**
     * 创建解析异常（无位置信息）。
     *
     * @param message 错误信息
     */
    public SkillParseException(String message) {
        super(MessageUtil.getMessage("skill.parse.error") + ": " + message);
        this.line = 0;
        this.column = 0;
    }

    /**
     * 创建解析异常（带原因）。
     *
     * @param message 错误信息
     * @param line 错误行号
     * @param cause 原始异常
     */
    public SkillParseException(String message, int line, Throwable cause) {
        super(formatMessage(message, line, 0), cause);
        this.line = line;
        this.column = 0;
    }

    private static String formatMessage(String message, int line, int column) {
        if (line > 0 && column > 0) {
            return MessageUtil.getMessage("skill.parse.error.atlinecol", line, column, message);
        } else if (line > 0) {
            return MessageUtil.getMessage("skill.parse.error.atline", line, message);
        }
        return MessageUtil.getMessage("skill.parse.error") + ": " + message;
    }

    /**
     * 获取错误行号。
     *
     * @return 错误行号（1-based），未知返回 0
     */
    public int getLine() {
        return line;
    }

    /**
     * 获取错误列号。
     *
     * @return 错误列号（1-based），未知返回 0
     */
    public int getColumn() {
        return column;
    }

    /**
     * 检查是否有位置信息。
     *
     * @return 是否有位置信息
     */
    public boolean hasLocation() {
        return line > 0;
    }
}
