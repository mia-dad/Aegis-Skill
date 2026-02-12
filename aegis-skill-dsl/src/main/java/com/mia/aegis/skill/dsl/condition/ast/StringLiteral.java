package com.mia.aegis.skill.dsl.condition.ast;

import com.mia.aegis.skill.dsl.condition.parser.ConditionExpression;
import com.mia.aegis.skill.dsl.condition.parser.ConditionExpressionVisitor;

/**
 * 字符串字面量节点。
 *
 * <p>表示条件表达式中的字符串值（单引号或双引号包围）。
 * 存储的值不包含包围的引号。</p>
 *
 * @since 0.2.0
 */
public final class StringLiteral implements ConditionExpression {

    private final String value;

    /**
     * 创建字符串字面量。
     *
     * @param value 字符串值（不包含引号）
     */
    public StringLiteral(String value) {
        if (value == null) {
            throw new IllegalArgumentException("String literal value cannot be null");
        }
        this.value = value;
    }

    /**
     * 获取字符串值。
     *
     * @return 不包含引号的字符串值
     */
    public String getValue() {
        return value;
    }

    @Override
    public <T> T accept(ConditionExpressionVisitor<T> visitor) {
        return visitor.visitString(this);
    }

    @Override
    public String toString() {
        return "'" + value + "'";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof StringLiteral)) return false;
        StringLiteral that = (StringLiteral) obj;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
