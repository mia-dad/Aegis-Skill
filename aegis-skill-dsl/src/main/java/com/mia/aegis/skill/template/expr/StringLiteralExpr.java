package com.mia.aegis.skill.template.expr;

/**
 * 字符串字面量表达式。
 *
 * @since 0.3.0
 */
public final class StringLiteralExpr implements TemplateExpression {

    private final String value;

    public StringLiteralExpr(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "String(\"" + value + "\")";
    }
}
