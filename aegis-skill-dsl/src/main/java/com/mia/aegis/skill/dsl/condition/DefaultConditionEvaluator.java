package com.mia.aegis.skill.dsl.condition;


import com.mia.aegis.skill.dsl.condition.ast.*;
import com.mia.aegis.skill.dsl.condition.parser.ConditionExpression;
import com.mia.aegis.skill.dsl.condition.parser.ConditionExpressionVisitor;
import com.mia.aegis.skill.dsl.condition.ast.NumberLiteral;
import com.mia.aegis.skill.executor.context.ExecutionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of ConditionEvaluator using the Visitor pattern.
 *
 * <p>This evaluator is thread-safe and can be shared across concurrent evaluations.
 * It implements the following evaluation semantics:
 * <ul>
 *   <li>Short-circuit evaluation for && and ||</li>
 *   <li>Null-safe comparisons (null == null is true)</li>
 *   <li>No implicit type coercion ("true" != true)</li>
 *   <li>Missing variables resolve to null (no exception)</li>
 * </ul>
 *
 * @since 0.2.0
 */
public class DefaultConditionEvaluator implements ConditionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(DefaultConditionEvaluator.class);

    @Override
    public boolean evaluate(ConditionExpression expression, ExecutionContext context) {
        if (expression == null) {
            throw new IllegalArgumentException("Expression cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        logger.debug("评估条件表达式 - executionId: {}, expression: {}",
            context.getExecutionId(), expression);

        EvaluationVisitor visitor = new EvaluationVisitor(context, false);
        Object result = expression.accept(visitor);
        boolean boolResult = isTruthy(result);

        logger.debug("条件表达式评估结果 - executionId: {}, result: {}",
            context.getExecutionId(), boolResult);

        return boolResult;
    }

    @Override
    public EvaluationResult evaluateWithTrace(ConditionExpression expression, ExecutionContext context) {
        if (expression == null) {
            throw new IllegalArgumentException("Expression cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        logger.debug("评估条件表达式（带跟踪） - executionId: {}, expression: {}",
            context.getExecutionId(), expression);

        EvaluationVisitor visitor = new EvaluationVisitor(context, true);
        Object result = expression.accept(visitor);
        final boolean boolResult = isTruthy(result);
        final String trace = visitor.getTrace();

        logger.debug("条件表达式评估结果（带跟踪） - executionId: {}, result: {}, traceLength: {}",
            context.getExecutionId(), boolResult, trace.length());

        return new EvaluationResult() {
            @Override
            public boolean getResult() {
                return boolResult;
            }

            @Override
            public String getTrace() {
                return trace;
            }
        };
    }

    /**
     * Determines if a value is "truthy" for condition evaluation.
     *
     * <p>Truthiness rules:
     * <ul>
     *   <li>null → false</li>
     *   <li>Boolean → its value</li>
     *   <li>String → false if empty, true otherwise</li>
     *   <li>Number → false if 0, true otherwise</li>
     *   <li>Everything else → true</li>
     * </ul>
     */
    private static boolean isTruthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return !((String) value).isEmpty();
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        }
        return true;
    }

    /**
     * Visitor implementation for evaluating condition expressions.
     */
    private static class EvaluationVisitor implements ConditionExpressionVisitor<Object> {

        private static final Logger logger = LoggerFactory.getLogger(EvaluationVisitor.class);

        private final ExecutionContext context;
        private final boolean traceEnabled;
        private final StringBuilder traceBuilder;

        EvaluationVisitor(ExecutionContext context, boolean traceEnabled) {
            this.context = context;
            this.traceEnabled = traceEnabled;
            this.traceBuilder = traceEnabled ? new StringBuilder() : null;
        }

        String getTrace() {
            return traceEnabled ? traceBuilder.toString() : "";
        }

        private void trace(String message) {
            if (traceEnabled) {
                traceBuilder.append(message).append("\n");
            }
        }

        @Override
        public Object visitBinary(BinaryExpression expr) {
            BinaryOperator op = expr.getOperator();

            switch (op) {
                case AND:
                    return evaluateAnd(expr);
                case OR:
                    return evaluateOr(expr);
                case EQ:
                    return evaluateEquality(expr, true);
                case NEQ:
                    return evaluateEquality(expr, false);
                case GT:
                    return evaluateComparison(expr, ">");
                case LT:
                    return evaluateComparison(expr, "<");
                case GTE:
                    return evaluateComparison(expr, ">=");
                case LTE:
                    return evaluateComparison(expr, "<=");
                default:
                    throw new IllegalStateException("Unknown operator: " + op);
            }
        }

        private boolean evaluateAnd(BinaryExpression expr) {
            Object leftValue = expr.getLeft().accept(this);
            boolean leftTruthy = isTruthy(leftValue);

            trace("AND left: " + leftValue + " -> " + leftTruthy);

            if (!leftTruthy) {
                trace("AND short-circuit: false");
                return false; // Short-circuit
            }

            Object rightValue = expr.getRight().accept(this);
            boolean rightTruthy = isTruthy(rightValue);

            trace("AND right: " + rightValue + " -> " + rightTruthy);
            trace("AND result: " + (leftTruthy && rightTruthy));

            return rightTruthy;
        }

        private boolean evaluateOr(BinaryExpression expr) {
            Object leftValue = expr.getLeft().accept(this);
            boolean leftTruthy = isTruthy(leftValue);

            trace("OR left: " + leftValue + " -> " + leftTruthy);

            if (leftTruthy) {
                trace("OR short-circuit: true");
                return true; // Short-circuit
            }

            Object rightValue = expr.getRight().accept(this);
            boolean rightTruthy = isTruthy(rightValue);

            trace("OR right: " + rightValue + " -> " + rightTruthy);
            trace("OR result: " + (leftTruthy || rightTruthy));

            return rightTruthy;
        }

        private boolean evaluateComparison(BinaryExpression expr, String opSymbol) {
            Object leftValue = expr.getLeft().accept(this);
            Object rightValue = expr.getRight().accept(this);

            trace(opSymbol + ": " + leftValue + " " + opSymbol + " " + rightValue);

            if (leftValue instanceof Number && rightValue instanceof Number) {
                double l = ((Number) leftValue).doubleValue();
                double r = ((Number) rightValue).doubleValue();
                switch (opSymbol) {
                    case ">": return l > r;
                    case "<": return l < r;
                    case ">=": return l >= r;
                    case "<=": return l <= r;
                    default: return false;
                }
            }

            if (leftValue instanceof String && rightValue instanceof String) {
                int cmp = ((String) leftValue).compareTo((String) rightValue);
                switch (opSymbol) {
                    case ">": return cmp > 0;
                    case "<": return cmp < 0;
                    case ">=": return cmp >= 0;
                    case "<=": return cmp <= 0;
                    default: return false;
                }
            }

            // Type mismatch -> false
            trace(opSymbol + " type mismatch -> false");
            return false;
        }

        private boolean evaluateEquality(BinaryExpression expr, boolean isEquals) {
            Object leftValue = expr.getLeft().accept(this);
            Object rightValue = expr.getRight().accept(this);

            boolean equal = Objects.equals(leftValue, rightValue);
            boolean result = isEquals ? equal : !equal;

            trace((isEquals ? "EQ" : "NEQ") + ": " + leftValue + " " +
                    (isEquals ? "==" : "!=") + " " + rightValue + " -> " + result);

            return result;
        }

        @Override
        public Object visitVariable(VariableReference expr) {
            String path = expr.getPath();
            Object value = resolveVariable(path);

            logger.debug("解析条件变量 - executionId: {}, path: {}, value: {}",
                context.getExecutionId(), path, value);

            trace("Variable {{" + path + "}} -> " + value);

            return value;
        }

        private Object resolveVariable(String path) {
            // Try to resolve from step outputs first (for nested paths like step1.value)
            if (path.contains(".")) {
                String[] parts = path.split("\\.", 2);
                String rootName = parts[0];
                String subPath = parts[1];

                logger.trace("解析嵌套路径 - executionId: {}, root: {}, subPath: {}",
                    context.getExecutionId(), rootName, subPath);

                // Check step outputs (supports varName alias lookup)
                Object stepOutput = context.getOutputByVarName(rootName);
                if (stepOutput != null) {
                    logger.trace("从 step 输出解析 - executionId: {}, name: {}, output: {}",
                        context.getExecutionId(), rootName, stepOutput);
                    return resolveNestedPath(stepOutput, subPath);
                }

                // Check inputs for nested objects
                Object inputValue = context.getInputValue(rootName);
                if (inputValue != null) {
                    logger.trace("从输入参数解析 - executionId: {}, param: {}, value: {}",
                        context.getExecutionId(), rootName, inputValue);
                    return resolveNestedPath(inputValue, subPath);
                }

                // Check await inputs (flattened fields may contain nested objects)
                Map<String, Object> awaitInputs = context.getAwaitInputs();
                for (Object awaitValue : awaitInputs.values()) {
                    if (awaitValue instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> awaitMap = (Map<String, Object>) awaitValue;
                        if (awaitMap.containsKey(rootName)) {
                            Object nested = awaitMap.get(rootName);
                            if (nested != null) {
                                logger.trace("从 await 输入解析 - executionId: {}, key: {}, value: {}",
                                    context.getExecutionId(), rootName, nested);
                                return resolveNestedPath(nested, subPath);
                            }
                        }
                    }
                }

                logger.trace("嵌套路径未找到 - executionId: {}, path: {}",
                    context.getExecutionId(), path);
                return null;
            }

            // Simple path - check inputs first, then step outputs, then await inputs
            Object inputValue = context.getInputValue(path);
            if (inputValue != null) {
                logger.trace("简单路径 - 从输入参数解析 - executionId: {}, path: {}, value: {}",
                    context.getExecutionId(), path, inputValue);
                return inputValue;
            }

            // Check if it's a step output (supports varName alias lookup)
            Object stepOutput = context.getOutputByVarName(path);
            if (stepOutput != null) {
                logger.trace("简单路径 - 从 step 输出解析 - executionId: {}, name: {}, output: {}",
                    context.getExecutionId(), path, stepOutput);
                return stepOutput;
            }

            // Check await inputs (flattened fields like confirm, notes)
            Map<String, Object> awaitInputs = context.getAwaitInputs();
            for (Object awaitValue : awaitInputs.values()) {
                if (awaitValue instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> awaitMap = (Map<String, Object>) awaitValue;
                    if (awaitMap.containsKey(path)) {
                        Object val = awaitMap.get(path);
                        logger.trace("简单路径 - 从 await 输入解析 - executionId: {}, path: {}, value: {}",
                            context.getExecutionId(), path, val);
                        return val;
                    }
                }
            }

            // Check if input contains null explicitly (key exists but value is null)
            if (context.getInput().containsKey(path)) {
                logger.trace("简单路径 - 输入参数显式为 null - executionId: {}, path: {}",
                    context.getExecutionId(), path);
                return null;
            }

            logger.trace("变量未找到 - executionId: {}, path: {}",
                context.getExecutionId(), path);
            return null; // Variable not found
        }

        private Object resolveNestedPath(Object root, String subPath) {
            if (root == null) {
                return null;
            }

            String[] parts = subPath.split("\\.", 2);
            String currentKey = parts[0];

            Object value = null;

            // Try to get value from Map
            if (root instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) root;
                value = map.get(currentKey);
            }

            // If there are more path segments, recurse
            if (parts.length > 1 && value != null) {
                return resolveNestedPath(value, parts[1]);
            }

            return value;
        }

        @Override
        public Object visitNull(NullLiteral expr) {
            trace("Literal: null");
            return null;
        }

        @Override
        public Object visitBoolean(BooleanLiteral expr) {
            boolean value = expr.getValue();
            trace("Literal: " + value);
            return value;
        }

        @Override
        public Object visitString(StringLiteral expr) {
            String value = expr.getValue();
            trace("Literal: '" + value + "'");
            return value;
        }

        @Override
        public Object visitNumber(NumberLiteral expr) {
            double value = expr.getValue();
            trace("Literal: " + value);
            return value;
        }
    }
}