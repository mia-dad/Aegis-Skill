package com.mia.aegis.skill.dsl.condition.parser;

/**
 * 条件表达式 AST 节点的基础接口。
 *
 * <p>使用访问者模式支持对 AST 的多种操作（评估、格式化、优化），
 * 无需修改节点类本身。</p>
 *
 * <h3>AST 节点层次结构</h3>
 * <pre>
 * ConditionExpression
 * ├── BinaryExpression     (&&, ||, ==, !=, >, <, >=, <=)
 * ├── VariableReference    ({{varName}})
 * ├── NullLiteral          (null)
 * ├── BooleanLiteral       (true, false)
 * ├── NumberLiteral        (integer or decimal numbers)
 * └── StringLiteral        ('string' or "string")
 * </pre>
 *
 * <h3>不可变性</h3>
 * <p>所有实现类必须是不可变的。AST 节点在解析时创建，之后不再修改。</p>
 *
 * @since 0.2.0
 */
public interface ConditionExpression {

    /**
     * 接受访问者处理此表达式节点。
     *
     * @param visitor 要接受的访问者
     * @param <T>     访问者的返回类型
     * @return 访问此节点的结果
     */
    <T> T accept(ConditionExpressionVisitor<T> visitor);
}
