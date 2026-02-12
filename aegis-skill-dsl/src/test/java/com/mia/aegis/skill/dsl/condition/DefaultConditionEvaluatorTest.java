package com.mia.aegis.skill.dsl.condition;

import com.mia.aegis.skill.dsl.condition.parser.ConditionExpression;
import com.mia.aegis.skill.dsl.condition.parser.DefaultConditionParser;
import com.mia.aegis.skill.executor.context.ExecutionContext;
import com.mia.aegis.skill.executor.context.StepResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultConditionEvaluator 的单元测试。
 *
 * 覆盖 GT/LT/GTE/LTE 操作符和 NumberLiteral。
 */
@DisplayName("DefaultConditionEvaluator 测试")
class DefaultConditionEvaluatorTest {

    private final DefaultConditionParser parser = new DefaultConditionParser();
    private final DefaultConditionEvaluator evaluator = new DefaultConditionEvaluator();

    private ExecutionContext contextWith(String key, Object value) {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put(key, value);
        return new ExecutionContext(input);
    }

    private ExecutionContext contextWith(Map<String, Object> input) {
        return new ExecutionContext(input);
    }

    // ---- 大于 > ----

    @Nested
    @DisplayName("大于 (>)")
    class GreaterThan {

        @Test
        @DisplayName("数字大于比较 - true")
        void shouldReturnTrueWhenGreater() {
            ConditionExpression expr = parser.parse("{{count}} > 5");
            boolean result = evaluator.evaluate(expr, contextWith("count", 10));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("数字大于比较 - false（等于）")
        void shouldReturnFalseWhenEqual() {
            ConditionExpression expr = parser.parse("{{count}} > 10");
            boolean result = evaluator.evaluate(expr, contextWith("count", 10));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("数字大于比较 - false（小于）")
        void shouldReturnFalseWhenLess() {
            ConditionExpression expr = parser.parse("{{count}} > 20");
            boolean result = evaluator.evaluate(expr, contextWith("count", 10));
            assertThat(result).isFalse();
        }
    }

    // ---- 小于 < ----

    @Nested
    @DisplayName("小于 (<)")
    class LessThan {

        @Test
        @DisplayName("数字小于比较 - true")
        void shouldReturnTrueWhenLess() {
            ConditionExpression expr = parser.parse("{{count}} < 20");
            boolean result = evaluator.evaluate(expr, contextWith("count", 10));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("数字小于比较 - false（等于）")
        void shouldReturnFalseWhenEqual() {
            ConditionExpression expr = parser.parse("{{count}} < 10");
            boolean result = evaluator.evaluate(expr, contextWith("count", 10));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("数字小于比较 - false（大于）")
        void shouldReturnFalseWhenGreater() {
            ConditionExpression expr = parser.parse("{{count}} < 5");
            boolean result = evaluator.evaluate(expr, contextWith("count", 10));
            assertThat(result).isFalse();
        }
    }

    // ---- 大于等于 >= ----

    @Nested
    @DisplayName("大于等于 (>=)")
    class GreaterThanOrEqual {

        @Test
        @DisplayName("大于等于比较 - true（大于）")
        void shouldReturnTrueWhenGreater() {
            ConditionExpression expr = parser.parse("{{count}} >= 5");
            boolean result = evaluator.evaluate(expr, contextWith("count", 10));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("大于等于比较 - true（等于）")
        void shouldReturnTrueWhenEqual() {
            ConditionExpression expr = parser.parse("{{count}} >= 10");
            boolean result = evaluator.evaluate(expr, contextWith("count", 10));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("大于等于比较 - false（小于）")
        void shouldReturnFalseWhenLess() {
            ConditionExpression expr = parser.parse("{{count}} >= 20");
            boolean result = evaluator.evaluate(expr, contextWith("count", 10));
            assertThat(result).isFalse();
        }
    }

    // ---- 小于等于 <= ----

    @Nested
    @DisplayName("小于等于 (<=)")
    class LessThanOrEqual {

        @Test
        @DisplayName("小于等于比较 - true（小于）")
        void shouldReturnTrueWhenLess() {
            ConditionExpression expr = parser.parse("{{count}} <= 20");
            boolean result = evaluator.evaluate(expr, contextWith("count", 10));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("小于等于比较 - true（等于）")
        void shouldReturnTrueWhenEqual() {
            ConditionExpression expr = parser.parse("{{count}} <= 10");
            boolean result = evaluator.evaluate(expr, contextWith("count", 10));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("小于等于比较 - false（大于）")
        void shouldReturnFalseWhenGreater() {
            ConditionExpression expr = parser.parse("{{count}} <= 5");
            boolean result = evaluator.evaluate(expr, contextWith("count", 10));
            assertThat(result).isFalse();
        }
    }

    // ---- 数字字面量 ----

    @Nested
    @DisplayName("数字字面量")
    class NumberLiterals {

        @Test
        @DisplayName("整数字面量比较")
        void shouldCompareWithIntegerLiteral() {
            ConditionExpression expr = parser.parse("{{count}} > 5");
            boolean result = evaluator.evaluate(expr, contextWith("count", 10));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("浮点数字面量比较")
        void shouldCompareWithFloatingPointLiteral() {
            ConditionExpression expr = parser.parse("{{price}} > 9.99");
            boolean result = evaluator.evaluate(expr, contextWith("price", 10.0));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("浮点数字面量比较 - false")
        void shouldReturnFalseForFloatComparison() {
            ConditionExpression expr = parser.parse("{{price}} > 9.99");
            boolean result = evaluator.evaluate(expr, contextWith("price", 9.5));
            assertThat(result).isFalse();
        }
    }

    // ---- 字符串比较 ----

    @Nested
    @DisplayName("字符串比较")
    class StringComparison {

        @Test
        @DisplayName("字符串大于 - 字典序")
        void shouldCompareStringsLexicographically() {
            ConditionExpression expr = parser.parse("{{name}} > \"abc\"");
            boolean result = evaluator.evaluate(expr, contextWith("name", "xyz"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("字符串小于 - 字典序")
        void shouldCompareLessThanStrings() {
            ConditionExpression expr = parser.parse("{{name}} < \"xyz\"");
            boolean result = evaluator.evaluate(expr, contextWith("name", "abc"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("字符串大于等于 - 等于情况")
        void shouldCompareGteStringsWhenEqual() {
            ConditionExpression expr = parser.parse("{{name}} >= \"abc\"");
            boolean result = evaluator.evaluate(expr, contextWith("name", "abc"));
            assertThat(result).isTrue();
        }
    }

    // ---- 类型不匹配 ----

    @Nested
    @DisplayName("类型不匹配")
    class TypeMismatch {

        @Test
        @DisplayName("数字与字符串比较应返回 false")
        void shouldReturnFalseForNumberVsString() {
            ConditionExpression expr = parser.parse("{{count}} > \"abc\"");
            boolean result = evaluator.evaluate(expr, contextWith("count", 10));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("字符串与数字比较应返回 false")
        void shouldReturnFalseForStringVsNumber() {
            ConditionExpression expr = parser.parse("{{name}} > 5");
            boolean result = evaluator.evaluate(expr, contextWith("name", "abc"));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("null 与数字比较应返回 false")
        void shouldReturnFalseForNullVsNumber() {
            ConditionExpression expr = parser.parse("{{missing}} > 5");
            boolean result = evaluator.evaluate(expr, contextWith("other", 10));
            assertThat(result).isFalse();
        }
    }

    // ---- 组合场景 ----

    @Nested
    @DisplayName("组合场景")
    class CombinedScenarios {

        @Test
        @DisplayName("比较操作符与 AND 组合")
        void shouldCombineComparisonWithAnd() {
            ConditionExpression expr = parser.parse("{{count}} > 5 && {{count}} < 20");
            boolean result = evaluator.evaluate(expr, contextWith("count", 10));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("比较操作符与 AND 组合 - false")
        void shouldReturnFalseForAndWithFailingCondition() {
            ConditionExpression expr = parser.parse("{{count}} > 5 && {{count}} < 8");
            boolean result = evaluator.evaluate(expr, contextWith("count", 10));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("比较操作符与 OR 组合")
        void shouldCombineComparisonWithOr() {
            ConditionExpression expr = parser.parse("{{count}} < 5 || {{count}} > 8");
            boolean result = evaluator.evaluate(expr, contextWith("count", 10));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("输入值的比较")
        void shouldCompareInputValue() {
            Map<String, Object> input = new HashMap<String, Object>();
            input.put("score", 42);
            ExecutionContext ctx = new ExecutionContext(input);

            ConditionExpression expr = parser.parse("{{score}} > 30");
            boolean result = evaluator.evaluate(expr, ctx);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("evaluateWithTrace 应返回跟踪信息")
        void shouldReturnTraceInfo() {
            ConditionExpression expr = parser.parse("{{count}} > 5");
            ConditionEvaluator.EvaluationResult result =
                    evaluator.evaluateWithTrace(expr, contextWith("count", 10));
            assertThat(result.getResult()).isTrue();
            assertThat(result.getTrace()).isNotEmpty();
        }
    }
}
