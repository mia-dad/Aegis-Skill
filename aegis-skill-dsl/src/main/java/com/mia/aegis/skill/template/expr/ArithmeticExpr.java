package com.mia.aegis.skill.template.expr;

/**
 * 二元算术表达式。
 *
 * <p>支持 {@code +}、{@code -}、{@code *}、{@code /} 四种运算符。</p>
 * <p>{@code +} 在任一侧为 String 时执行字符串拼接。</p>
 *
 * @since 0.3.0
 */
public final class ArithmeticExpr implements TemplateExpression {

    /** 运算符类型。 */
    public enum Op {
        ADD("+"), SUB("-"), MUL("*"), DIV("/");

        private final String symbol;
        Op(String symbol) { this.symbol = symbol; }
        public String getSymbol() { return symbol; }
    }

    private final Op op;
    private final TemplateExpression left;
    private final TemplateExpression right;

    public ArithmeticExpr(Op op, TemplateExpression left, TemplateExpression right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    public Op getOp() { return op; }
    public TemplateExpression getLeft() { return left; }
    public TemplateExpression getRight() { return right; }

    @Override
    public String toString() {
        return "Arithmetic(" + left + " " + op.getSymbol() + " " + right + ")";
    }
}
