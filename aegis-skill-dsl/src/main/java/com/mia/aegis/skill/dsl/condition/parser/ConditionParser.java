package com.mia.aegis.skill.dsl.condition.parser;

import com.mia.aegis.skill.exception.ConditionParseException;

/**
 * 将条件表达式字符串解析为抽象语法树（AST）。
 *
 * <p>此接口定义了一项约定，用于将技能定义中的{@code when.expr}字符串解析为结构化的抽象语法树（AST），
 * 该语法树可在运行时完成求值操作。
 *
 * <h3>支持的语法</h3>
 * <pre>
 * 表达式        = 或表达式 ;
 * 或表达式      = 与表达式 ( "||" 与表达式 )* ;
 * 与表达式      = 比较表达式 ( "&&" 比较表达式 )* ;
 * 比较表达式    = 操作数 ( ( "==" | "!=" ) 操作数 )? ;
 * 操作数        = 变量 | 字面量 ;
 * 变量          = "{{" 标识符 ( "." 标识符 )* "}}" ;
 * 字面量        = "null" | "true" | "false" | 字符串字面量 ;
 * 字符串字面量  = "'" [^']* "'" | '"' [^"]* '"' ;
 * 标识符        = [a-zA-Z_][a-zA-Z0-9_]* ;
 * </pre>
 *
 * <h3>运算符优先级（从高到低）</h3>
 * <ol>
 *   <li>{@code ==}, {@code !=} - 相等性比较</li>
 *   <li>{@code &&} - 逻辑与</li>
 *   <li>{@code ||} - 逻辑或</li>
 * </ol>
 *
 * @since 0.2.0
 */
public interface ConditionParser {

    /**
     * Parses a condition expression string into an AST.
     *
     * @param expression The condition expression string to parse.
     *                   Must not be null or empty.
     * @return The root node of the parsed AST.
     * @throws ConditionParseException if the expression contains syntax errors
     * @throws IllegalArgumentException if expression is null or empty
     */
    ConditionExpression parse(String expression) throws ConditionParseException;

    /**
     * Validates a condition expression without returning the AST.
     *
     * @param expression The condition expression string to validate.
     * @return {@code true} if the expression is syntactically valid.
     * @throws ConditionParseException if the expression contains syntax errors
     * @throws IllegalArgumentException if expression is null or empty
     */
    default boolean validate(String expression) throws ConditionParseException {
        parse(expression);
        return true;
    }
}
