package com.mia.aegis.skill.dsl.condition.ast;

import com.mia.aegis.skill.dsl.condition.parser.ConditionExpression;
import com.mia.aegis.skill.dsl.condition.parser.ConditionExpressionVisitor;

/**
 * 数字字面量节点。
 *
 * <p>表示条件表达式中的数字值（整数或小数）。</p>
 *
 * @since 0.2.0
 */
public final class NumberLiteral implements ConditionExpression {

    private final double value;

    /**
     * 创建一个数字字面量节点。
     *
     * @param value 数字值
     */
    public NumberLiteral(double value) {
        this.value = value;
    }

    /**
     * 获取数字值。
     *
     * @return 数字值
     */
    public double getValue() {
        return value;
    }

    @Override
    public <T> T accept(ConditionExpressionVisitor<T> visitor) {
        return visitor.visitNumber(this);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NumberLiteral)) return false;
        NumberLiteral that = (NumberLiteral) obj;
        return Double.compare(that.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }
}
