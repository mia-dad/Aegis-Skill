package com.mia.aegis.skill.template.expr;

/**
 * 数字字面量表达式。
 *
 * @since 0.3.0
 */
public final class NumberLiteralExpr implements TemplateExpression {

    private final double value;

    public NumberLiteralExpr(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Number(" + value + ")";
    }
}
