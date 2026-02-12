package com.mia.aegis.skill.dsl.condition.ast;

import com.mia.aegis.skill.dsl.condition.parser.ConditionExpression;
import com.mia.aegis.skill.dsl.condition.parser.ConditionExpressionVisitor;

/**
 * 布尔字面量节点。
 *
 * <p>表示条件表达式中的 true 或 false 值。
 * 使用静态工厂方法和预定义常量以确保值的唯一性。</p>
 *
 * @since 0.2.0
 */
public final class BooleanLiteral implements ConditionExpression {

    /**
     * true 字面量的共享实例。
     */
    public static final BooleanLiteral TRUE = new BooleanLiteral(true);

    /**
     * false 字面量的共享实例。
     */
    public static final BooleanLiteral FALSE = new BooleanLiteral(false);

    private final boolean value;

    private BooleanLiteral(boolean value) {
        this.value = value;
    }

    /**
     * 获取指定布尔值的字面量实例。
     *
     * @param value 布尔值
     * @return 对应的 BooleanLiteral 实例
     */
    public static BooleanLiteral of(boolean value) {
        return value ? TRUE : FALSE;
    }

    /**
     * 获取布尔值。
     *
     * @return 布尔值
     */
    public boolean getValue() {
        return value;
    }

    @Override
    public <T> T accept(ConditionExpressionVisitor<T> visitor) {
        return visitor.visitBoolean(this);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BooleanLiteral)) return false;
        BooleanLiteral that = (BooleanLiteral) obj;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(value);
    }
}
