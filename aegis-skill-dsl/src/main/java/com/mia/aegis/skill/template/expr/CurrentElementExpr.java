package com.mia.aegis.skill.template.expr;

/**
 * 当前循环元素表达式 {@code {{_}}}。
 *
 * <p>在 {@code {{#for}}} 循环中引用当前迭代元素。</p>
 *
 * @since 0.3.0
 */
public final class CurrentElementExpr implements TemplateExpression {

    /** 共享单例。 */
    public static final CurrentElementExpr INSTANCE = new CurrentElementExpr();

    private CurrentElementExpr() {}

    @Override
    public String toString() {
        return "CurrentElement(_)";
    }
}
