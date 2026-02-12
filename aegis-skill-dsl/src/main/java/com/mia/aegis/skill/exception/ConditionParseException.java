package com.mia.aegis.skill.exception;

/**
 * 条件表达式解析异常。
 *
 * <p>当条件表达式语法错误时抛出。包含详细的错误位置和描述信息，
 * 帮助 Skill 作者快速定位和修复问题。</p>
 *
 * <h3>错误消息格式</h3>
 * <pre>
 * ConditionParseException: Unexpected token at position 15
 *   Expression: {{focusCode} != null
 *                           ^
 *   Expected: '}}'
 *   Found: '!'
 * </pre>
 *
 * @since 0.2.0
 */
public class ConditionParseException extends SkillParseException {

    private final String expression;
    private final int position;
    private final String expected;
    private final String found;

    /**
     * 创建解析异常。
     *
     * @param message 错误消息
     * @param expression 原始表达式
     * @param position 错误位置（从 0 开始）
     */
    public ConditionParseException(String message, String expression, int position) {
        this(message, expression, position, null, null);
    }

    /**
     * 创建解析异常（包含期望和实际 token）。
     *
     * @param message 错误消息
     * @param expression 原始表达式
     * @param position 错误位置（从 0 开始）
     * @param expected 期望的 token
     * @param found 实际找到的 token
     */
    public ConditionParseException(String message, String expression, int position,
                                   String expected, String found) {
        super(formatMessage(message, expression, position, expected, found));
        this.expression = expression;
        this.position = position;
        this.expected = expected;
        this.found = found;
    }

    /**
     * 获取原始表达式。
     *
     * @return 原始表达式字符串
     */
    public String getExpression() {
        return expression;
    }

    /**
     * 获取错误位置。
     *
     * @return 错误位置（从 0 开始）
     */
    public int getPosition() {
        return position;
    }

    /**
     * 获取期望的 token。
     *
     * @return 期望的 token，如果未指定则返回 null
     */
    public String getExpected() {
        return expected;
    }

    /**
     * 获取实际找到的 token。
     *
     * @return 实际找到的 token，如果未指定则返回 null
     */
    public String getFound() {
        return found;
    }

    private static String formatMessage(String message, String expression, int position,
                                        String expected, String found) {
        StringBuilder sb = new StringBuilder();
        sb.append(message);

        if (expression != null) {
            sb.append("\n  Expression: ").append(expression);

            if (position >= 0 && position <= expression.length()) {
                sb.append("\n              ");
                for (int i = 0; i < position; i++) {
                    sb.append(' ');
                }
                sb.append('^');
            }
        }

        if (expected != null) {
            sb.append("\n  Expected: ").append(expected);
        }

        if (found != null) {
            sb.append("\n  Found: ").append(found);
        }

        return sb.toString();
    }
}
