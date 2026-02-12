package com.mia.aegis.skill.dsl.condition.ast;

/**
 * 二元操作符枚举。
 *
 * <p>支持的操作符：</p>
 * <ul>
 *   <li>AND (&&) - 逻辑与</li>
 *   <li>OR (||) - 逻辑或</li>
 *   <li>EQ (==) - 相等比较</li>
 *   <li>NEQ (!=) - 不等比较</li>
 *   <li>GT (>) - 大于比较</li>
 *   <li>LT (<) - 小于比较</li>
 *   <li>GTE (>=) - 大于等于比较</li>
 *   <li>LTE (<=) - 小于等于比较</li>
 * </ul>
 *
 * @since 0.2.0
 */
public enum BinaryOperator {

    /**
     * 逻辑与操作符 (&&)。
     */
    AND("&&"),

    /**
     * 逻辑或操作符 (||)。
     */
    OR("||"),

    /**
     * 相等比较操作符 (==)。
     */
    EQ("=="),

    /**
     * 不等比较操作符 (!=)。
     */
    NEQ("!="),

    /**
     * 大于比较操作符 (>)。
     */
    GT(">"),

    /**
     * 小于比较操作符 (<)。
     */
    LT("<"),

    /**
     * 大于等于比较操作符 (>=)。
     */
    GTE(">="),

    /**
     * 小于等于比较操作符 (<=)。
     */
    LTE("<=");

    private final String symbol;

    BinaryOperator(String symbol) {
        this.symbol = symbol;
    }

    /**
     * 获取操作符符号。
     *
     * @return 操作符符号字符串
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * 判断是否为逻辑操作符。
     *
     * @return 如果是 AND 或 OR 返回 true
     */
    public boolean isLogical() {
        return this == AND || this == OR;
    }

    /**
     * 判断是否为比较操作符。
     *
     * @return 如果是 EQ、NEQ、GT、LT、GTE 或 LTE 返回 true
     */
    public boolean isComparison() {
        return this == EQ || this == NEQ || this == GT || this == LT || this == GTE || this == LTE;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
