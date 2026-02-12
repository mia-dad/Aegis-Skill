package com.mia.aegis.skill.dsl.condition.ast;

import com.mia.aegis.skill.dsl.condition.parser.ConditionExpression;
import com.mia.aegis.skill.dsl.condition.parser.ConditionExpressionVisitor;

/**
 * null 字面量节点。
 *
 * <p>表示条件表达式中的 null 值。使用单例模式，
 * 因为所有 null 字面量都是等价的。</p>
 *
 * @since 0.2.0
 */
public final class NullLiteral implements ConditionExpression {

    /**
     * NullLiteral 单例实例。
     */
    public static final NullLiteral INSTANCE = new NullLiteral();

    private NullLiteral() {
        // 私有构造函数，强制使用单例
    }

    @Override
    public <T> T accept(ConditionExpressionVisitor<T> visitor) {
        return visitor.visitNull(this);
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NullLiteral;
    }

    @Override
    public int hashCode() {
        return NullLiteral.class.hashCode();
    }
}
