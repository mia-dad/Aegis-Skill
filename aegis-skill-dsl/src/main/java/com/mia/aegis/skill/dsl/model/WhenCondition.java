package com.mia.aegis.skill.dsl.model;


import com.mia.aegis.skill.dsl.condition.parser.ConditionExpression;

/**
 * Step 条件执行块。
 *
 * <p>表示附加到 Step 的条件块，封装原始表达式字符串和解析后的 AST。
 * 当条件评估为 true 时步骤执行，评估为 false 时步骤被跳过。</p>
 *
 * <h3>示例</h3>
 * <pre>
 * when:
 *   expr: "{{focusCode}} != null"
 * </pre>
 *
 * <h3>生命周期</h3>
 * <ol>
 *   <li><b>创建</b>: 在 MarkdownSkillParser 解析 Step 时创建</li>
 *   <li><b>不可变</b>: 创建后不可修改</li>
 *   <li><b>使用</b>: 在 DefaultSkillExecutor 执行步骤前评估</li>
 * </ol>
 *
 * @since 0.2.0
 */
public final class WhenCondition {

    private final String rawExpression;
    private final ConditionExpression parsedExpression;

    /**
     * 创建 WhenCondition。
     *
     * @param rawExpression 原始表达式字符串
     * @param parsedExpression 解析后的 AST
     */
    public WhenCondition(String rawExpression, ConditionExpression parsedExpression) {
        if (rawExpression == null || rawExpression.trim().isEmpty()) {
            throw new IllegalArgumentException("Raw expression cannot be null or empty");
        }
        if (parsedExpression == null) {
            throw new IllegalArgumentException("Parsed expression cannot be null");
        }
        this.rawExpression = rawExpression.trim();
        this.parsedExpression = parsedExpression;
    }

    /**
     * 获取原始表达式字符串。
     *
     * @return 原始表达式
     */
    public String getRawExpression() {
        return rawExpression;
    }

    /**
     * 获取解析后的 AST。
     *
     * @return 解析后的条件表达式
     */
    public ConditionExpression getParsedExpression() {
        return parsedExpression;
    }

    @Override
    public String toString() {
        return "WhenCondition{" +
                "expr='" + rawExpression + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WhenCondition)) return false;
        WhenCondition that = (WhenCondition) obj;
        return rawExpression.equals(that.rawExpression);
    }

    @Override
    public int hashCode() {
        return rawExpression.hashCode();
    }
}
