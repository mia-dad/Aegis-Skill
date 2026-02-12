package com.mia.aegis.skill.dsl.condition;

import com.mia.aegis.skill.dsl.condition.parser.ConditionExpression;
import com.mia.aegis.skill.executor.context.ExecutionContext;



/**
 * 基于执行上下文对条件表达式进行求值。
 *
 * <p>此接口定义了步骤定义中{@code when}条件的求值约定。求值器会从执行上下文
 * 中解析变量引用，并通过布尔/比较运算符判定步骤是否需要执行。
 *
 * <h3>求值语义</h3>
 * <ul>
 *   <li>对{@code &&}和{@code ||}采用短路求值规则</li>
 *   <li>空安全比较（null == null 判定为true）</li>
 *   <li>无隐式类型转换（字符串"true" 不等于 布尔值true）</li>
 *   <li>未定义的变量均解析为null（不会抛出异常）</li>
 * </ul>
 *
 * <h3>变量解析顺序</h3>
 * <ol>
 *   <li>步骤输出（按步骤唯一标识匹配）</li>
 *   <li>技能入参</li>
 *   <li>全局上下文</li>
 * </ol>
 *
 * @since 0.2.0
 */

public interface ConditionEvaluator {

    /**
     * Evaluates a condition expression against the given execution context.
     *
     * @param expression The parsed condition expression (AST root node).
     *                   Must not be null.
     * @param context    The execution context containing variable values.
     *                   Must not be null.
     * @return {@code true} if the condition is satisfied and the step should
     *         execute; {@code false} if the step should be skipped.
     * @throws IllegalArgumentException if expression or context is null
     */
    boolean evaluate(ConditionExpression expression, ExecutionContext context);

    /**
     * Evaluates a condition expression and returns a detailed result
     * including the evaluation trace for debugging purposes.
     *
     * @param expression The parsed condition expression (AST root node).
     * @param context    The execution context containing variable values.
     * @return An evaluation result containing the boolean outcome and trace.
     * @throws IllegalArgumentException if expression or context is null
     */
    EvaluationResult evaluateWithTrace(ConditionExpression expression, ExecutionContext context);

    /**
     * Result of condition evaluation with optional trace information.
     */
    interface EvaluationResult {
        /**
         * @return The boolean result of the evaluation.
         */
        boolean getResult();

        /**
         * @return Human-readable trace of the evaluation process,
         *         useful for debugging. May be empty if tracing is disabled.
         */
        String getTrace();
    }
}
