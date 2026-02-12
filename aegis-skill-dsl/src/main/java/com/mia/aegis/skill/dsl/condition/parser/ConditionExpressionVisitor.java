package com.mia.aegis.skill.dsl.condition.parser;

import com.mia.aegis.skill.dsl.condition.ast.*;

/**
 * 条件表达式 AST 访问者接口。
 *
 * <p>实现此接口以遍历和处理条件表达式 AST 树。</p>
 *
 * @param <T> 访问方法的返回类型
 * @since 0.2.0
 */
public interface ConditionExpressionVisitor<T> {

    /**
     * 访问二元表达式 (&&, ||, ==, !=, >, <, >=, <=)。
     *
     * @param expr 二元表达式节点
     * @return 访问结果
     */
    T visitBinary(BinaryExpression expr);

    /**
     * 访问变量引用 ({{varName}})。
     *
     * @param expr 变量引用节点
     * @return 访问结果
     */
    T visitVariable(VariableReference expr);

    /**
     * 访问 null 字面量。
     *
     * @param expr null 字面量节点
     * @return 访问结果
     */
    T visitNull(NullLiteral expr);

    /**
     * 访问布尔字面量 (true/false)。
     *
     * @param expr 布尔字面量节点
     * @return 访问结果
     */
    T visitBoolean(BooleanLiteral expr);

    /**
     * 访问字符串字面量。
     *
     * @param expr 字符串字面量节点
     * @return 访问结果
     */
    T visitString(StringLiteral expr);

    /**
     * 访问数字字面量。
     *
     * @param expr 数字字面量节点
     * @return 访问结果
     */
    T visitNumber(NumberLiteral expr);
}
