package com.mia.aegis.skill.dsl.condition.ast;

import com.mia.aegis.skill.dsl.condition.parser.ConditionExpression;
import com.mia.aegis.skill.dsl.condition.parser.ConditionExpressionVisitor;

/**
 * 二元表达式节点。
 *
 * <p>表示由操作符连接的左右两个表达式，如：</p>
 * <ul>
 *   <li>{{focusCode}} != null</li>
 *   <li>{{a}} == {{b}}</li>
 *   <li>{{x}} && {{y}}</li>
 *   <li>{{a}} || {{b}}</li>
 * </ul>
 *
 * @since 0.2.0
 */
public final class BinaryExpression implements ConditionExpression {

    private final BinaryOperator operator;
    private final ConditionExpression left;
    private final ConditionExpression right;

    /**
     * 创建二元表达式。
     *
     * @param operator 操作符
     * @param left 左操作数
     * @param right 右操作数
     */
    public BinaryExpression(BinaryOperator operator, ConditionExpression left, ConditionExpression right) {
        if (operator == null) {
            throw new IllegalArgumentException("Operator cannot be null");
        }
        if (left == null) {
            throw new IllegalArgumentException("Left operand cannot be null");
        }
        if (right == null) {
            throw new IllegalArgumentException("Right operand cannot be null");
        }
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    /**
     * 获取操作符。
     *
     * @return 二元操作符
     */
    public BinaryOperator getOperator() {
        return operator;
    }

    /**
     * 获取左操作数。
     *
     * @return 左操作数表达式
     */
    public ConditionExpression getLeft() {
        return left;
    }

    /**
     * 获取右操作数。
     *
     * @return 右操作数表达式
     */
    public ConditionExpression getRight() {
        return right;
    }

    /**
     * 判断是否为逻辑表达式。
     *
     * @return 如果操作符是 AND 或 OR 返回 true
     */
    public boolean isLogical() {
        return operator.isLogical();
    }

    /**
     * 判断是否为比较表达式。
     *
     * @return 如果操作符是 EQ 或 NEQ 返回 true
     */
    public boolean isComparison() {
        return operator.isComparison();
    }

    @Override
    public <T> T accept(ConditionExpressionVisitor<T> visitor) {
        return visitor.visitBinary(this);
    }

    @Override
    public String toString() {
        return "(" + left + " " + operator.getSymbol() + " " + right + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BinaryExpression)) return false;
        BinaryExpression that = (BinaryExpression) obj;
        return operator == that.operator &&
                left.equals(that.left) &&
                right.equals(that.right);
    }

    @Override
    public int hashCode() {
        int result = operator.hashCode();
        result = 31 * result + left.hashCode();
        result = 31 * result + right.hashCode();
        return result;
    }
}
