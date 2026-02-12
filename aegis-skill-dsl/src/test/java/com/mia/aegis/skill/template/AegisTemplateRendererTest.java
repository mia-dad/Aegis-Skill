package com.mia.aegis.skill.template;

import com.mia.aegis.skill.executor.context.ExecutionContext;
import com.mia.aegis.skill.executor.context.StepResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AegisTemplateRenderer 的单元测试。
 *
 * 测试覆盖：
 * - 基本变量替换
 * - 嵌套属性访问
 * - 算术表达式（四则运算）
 * - 字符串拼接
 * - 数组字面量索引
 * - 数组变量索引
 * - For 循环（结构化数组 + 简单数组）
 * - {{_}} 当前循环元素
 * - 混合场景
 * - extractVariables
 * - 缺失变量 / null / 空模板
 * - renderInputTemplate
 */
@DisplayName("AegisTemplateRenderer 测试")
class AegisTemplateRendererTest {

    private final AegisTemplateRenderer renderer = new AegisTemplateRenderer();

    // ---- 基本变量替换 ----

    @Nested
    @DisplayName("基本变量替换")
    class VariableSubstitution {

        @Test
        @DisplayName("应该渲染简单变量")
        void shouldRenderSimpleVariable() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("name", "World");

            String result = renderer.render("Hello, {{name}}!", ctx);
            assertThat(result).isEqualTo("Hello, World!");
        }

        @Test
        @DisplayName("应该渲染多个变量")
        void shouldRenderMultipleVariables() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("greeting", "Hello");
            ctx.put("name", "Alice");
            ctx.put("age", 30);

            String result = renderer.render("{{greeting}}, {{name}}! Age: {{age}}.", ctx);
            assertThat(result).isEqualTo("Hello, Alice! Age: 30.");
        }

        @Test
        @DisplayName("应该处理带空格的变量")
        void shouldHandleVariablesWithSpaces() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("value", "test");

            String result = renderer.render("Value: {{ value }}", ctx);
            assertThat(result).isEqualTo("Value: test");
        }

        @Test
        @DisplayName("缺失变量应渲染为空字符串")
        void shouldRenderMissingVariablesAsEmpty() {
            Map<String, Object> ctx = new HashMap<String, Object>();

            String result = renderer.render("Hello, {{missing}}!", ctx);
            assertThat(result).isEqualTo("Hello, !");
        }

        @Test
        @DisplayName("应该处理数字类型变量")
        void shouldHandleNumericVariables() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("count", 42);
            ctx.put("price", 19.99);

            String result = renderer.render("Count: {{count}}, Price: {{price}}", ctx);
            assertThat(result).isEqualTo("Count: 42, Price: 19.99");
        }

        @Test
        @DisplayName("应该处理布尔类型变量")
        void shouldHandleBooleanVariables() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("flag", true);

            String result = renderer.render("Flag: {{flag}}", ctx);
            assertThat(result).isEqualTo("Flag: true");
        }
    }

    // ---- 嵌套属性访问 ----

    @Nested
    @DisplayName("嵌套属性访问")
    class NestedPropertyAccess {

        @Test
        @DisplayName("应该访问嵌套属性")
        void shouldAccessNestedProperty() {
            Map<String, Object> user = new HashMap<String, Object>();
            user.put("name", "Alice");
            user.put("age", 30);

            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("user", user);

            String result = renderer.render("{{user.name}} - {{user.age}}", ctx);
            assertThat(result).isEqualTo("Alice - 30");
        }

        @Test
        @DisplayName("应该访问深层嵌套属性")
        void shouldAccessDeeplyNestedProperty() {
            Map<String, Object> address = new HashMap<String, Object>();
            address.put("city", "Shanghai");

            Map<String, Object> user = new HashMap<String, Object>();
            user.put("address", address);

            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("user", user);

            String result = renderer.render("City: {{user.address.city}}", ctx);
            assertThat(result).isEqualTo("City: Shanghai");
        }

        @Test
        @DisplayName("嵌套属性不存在时应渲染为空")
        void shouldRenderEmptyForMissingNestedProperty() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("user", new HashMap<String, Object>());

            String result = renderer.render("Name: {{user.name}}", ctx);
            assertThat(result).isEqualTo("Name: ");
        }
    }

    // ---- 算术表达式 ----

    @Nested
    @DisplayName("算术表达式")
    class ArithmeticExpressions {

        @Test
        @DisplayName("应该计算加法")
        void shouldEvaluateAddition() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("a", 10);
            ctx.put("b", 20);

            String result = renderer.render("Sum: {{a + b}}", ctx);
            assertThat(result).isEqualTo("Sum: 30");
        }

        @Test
        @DisplayName("应该计算减法")
        void shouldEvaluateSubtraction() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("a", 50);
            ctx.put("b", 20);

            String result = renderer.render("Diff: {{a - b}}", ctx);
            assertThat(result).isEqualTo("Diff: 30");
        }

        @Test
        @DisplayName("应该计算乘法")
        void shouldEvaluateMultiplication() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("a", 5);
            ctx.put("b", 6);

            String result = renderer.render("Product: {{a * b}}", ctx);
            assertThat(result).isEqualTo("Product: 30");
        }

        @Test
        @DisplayName("应该计算除法")
        void shouldEvaluateDivision() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("a", 100);
            ctx.put("b", 4);

            String result = renderer.render("Quotient: {{a / b}}", ctx);
            assertThat(result).isEqualTo("Quotient: 25");
        }

        @Test
        @DisplayName("应该遵守运算符优先级")
        void shouldRespectOperatorPrecedence() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("a", 2);
            ctx.put("b", 3);
            ctx.put("c", 4);

            // a + b * c = 2 + 12 = 14
            String result = renderer.render("Result: {{a + b * c}}", ctx);
            assertThat(result).isEqualTo("Result: 14");
        }

        @Test
        @DisplayName("应该处理浮点数运算")
        void shouldHandleFloatingPointArithmetic() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("price", 10.5);
            ctx.put("qty", 3);

            String result = renderer.render("Total: {{price * qty}}", ctx);
            assertThat(result).isEqualTo("Total: 31.5");
        }

        @Test
        @DisplayName("除以零应返回0")
        void shouldReturnZeroForDivisionByZero() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("a", 10);
            ctx.put("b", 0);

            String result = renderer.render("Result: {{a / b}}", ctx);
            assertThat(result).isEqualTo("Result: 0");
        }
    }

    // ---- 字符串拼接 ----

    @Nested
    @DisplayName("字符串拼接")
    class StringConcatenation {

        @Test
        @DisplayName("变量与字符串字面量拼接")
        void shouldConcatenateVariableAndStringLiteral() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("name", "Alice");

            String result = renderer.render("{{name + \" suffix\"}}", ctx);
            assertThat(result).isEqualTo("Alice suffix");
        }

        @Test
        @DisplayName("两个字符串变量拼接")
        void shouldConcatenateTwoStringVariables() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("first", "Hello");
            ctx.put("second", " World");

            String result = renderer.render("{{first + second}}", ctx);
            assertThat(result).isEqualTo("Hello World");
        }
    }

    // ---- 数组索引 ----

    @Nested
    @DisplayName("数组索引")
    class ArrayIndexing {

        @Test
        @DisplayName("应该支持字面量索引")
        void shouldSupportLiteralIndex() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("arr", Arrays.asList("a", "b", "c"));

            String result = renderer.render("{{arr[0]}}, {{arr[1]}}, {{arr[2]}}", ctx);
            assertThat(result).isEqualTo("a, b, c");
        }

        @Test
        @DisplayName("应该支持数组元素的字段访问")
        void shouldSupportArrayElementFieldAccess() {
            Map<String, Object> item0 = new HashMap<String, Object>();
            item0.put("name", "Item A");
            Map<String, Object> item1 = new HashMap<String, Object>();
            item1.put("name", "Item B");

            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("items", Arrays.asList(item0, item1));

            String result = renderer.render("{{items[0].name}}, {{items[1].name}}", ctx);
            assertThat(result).isEqualTo("Item A, Item B");
        }

        @Test
        @DisplayName("应该支持变量索引 [#var]")
        void shouldSupportVariableIndex() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("arr", Arrays.asList("x", "y", "z"));
            ctx.put("idx", 1);

            String result = renderer.render("{{arr[#idx]}}", ctx);
            assertThat(result).isEqualTo("y");
        }

        @Test
        @DisplayName("应该支持变量索引加字段访问")
        void shouldSupportVariableIndexWithFieldAccess() {
            Map<String, Object> item0 = new HashMap<String, Object>();
            item0.put("name", "First");
            Map<String, Object> item1 = new HashMap<String, Object>();
            item1.put("name", "Second");

            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("items", Arrays.asList(item0, item1));
            ctx.put("idx", 1);

            String result = renderer.render("{{items[#idx].name}}", ctx);
            assertThat(result).isEqualTo("Second");
        }

        @Test
        @DisplayName("索引越界应渲染为空")
        void shouldRenderEmptyForOutOfBoundsIndex() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("arr", Arrays.asList("a", "b"));

            String result = renderer.render("{{arr[5]}}", ctx);
            assertThat(result).isEqualTo("");
        }
    }

    // ---- For 循环 ----

    @Nested
    @DisplayName("For 循环")
    class ForLoop {

        @Test
        @DisplayName("应该渲染结构化数组的 for 循环")
        void shouldRenderForLoopWithStructuredArray() {
            Map<String, Object> item1 = new HashMap<String, Object>();
            item1.put("name", "Alice");
            Map<String, Object> item2 = new HashMap<String, Object>();
            item2.put("name", "Bob");
            Map<String, Object> item3 = new HashMap<String, Object>();
            item3.put("name", "Charlie");

            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("users", Arrays.asList(item1, item2, item3));

            String result = renderer.render("{{#for users}}{{name}}, {{/for}}", ctx);
            assertThat(result).isEqualTo("Alice, Bob, Charlie, ");
        }

        @Test
        @DisplayName("应该渲染简单数组的 for 循环（使用 {{_}}）")
        void shouldRenderForLoopWithSimpleArray() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("tags", Arrays.asList("java", "python", "go"));

            String result = renderer.render("{{#for tags}}{{_}} {{/for}}", ctx);
            assertThat(result).isEqualTo("java python go ");
        }

        @Test
        @DisplayName("{{_}} 在结构化数组中应代表整个元素")
        void shouldRepresentWholeElementAsUnderscore() {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("name", "Alice");
            item.put("age", 30);

            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("items", Arrays.asList(item));

            // {{_}} 是整个 Map，toString 会输出 Map 的内容
            String result = renderer.render("{{#for items}}{{_}}{{/for}}", ctx);
            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("空数组应渲染为空")
        void shouldRenderEmptyForEmptyArray() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("items", Collections.emptyList());

            String result = renderer.render("Before{{#for items}}{{_}}{{/for}}After", ctx);
            assertThat(result).isEqualTo("BeforeAfter");
        }

        @Test
        @DisplayName("非 List 变量应跳过循环体")
        void shouldSkipForBodyForNonListVariable() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("items", "not a list");

            String result = renderer.render("Before{{#for items}}Content{{/for}}After", ctx);
            assertThat(result).isEqualTo("BeforeAfter");
        }

        @Test
        @DisplayName("应该支持嵌套 for 循环")
        void shouldSupportNestedForLoops() {
            Map<String, Object> group1 = new HashMap<String, Object>();
            group1.put("name", "G1");
            group1.put("members", Arrays.asList("A", "B"));

            Map<String, Object> group2 = new HashMap<String, Object>();
            group2.put("name", "G2");
            group2.put("members", Arrays.asList("C"));

            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("groups", Arrays.asList(group1, group2));

            String result = renderer.render(
                    "{{#for groups}}[{{name}}:{{#for members}}{{_}}{{/for}}]{{/for}}", ctx);
            assertThat(result).isEqualTo("[G1:AB][G2:C]");
        }
    }

    // ---- 混合场景 ----

    @Nested
    @DisplayName("混合场景")
    class MixedScenarios {

        @Test
        @DisplayName("for 循环 + 算术表达式")
        void shouldCombineForLoopAndArithmetic() {
            Map<String, Object> item1 = new HashMap<String, Object>();
            item1.put("price", 10);
            item1.put("qty", 2);
            Map<String, Object> item2 = new HashMap<String, Object>();
            item2.put("price", 20);
            item2.put("qty", 3);

            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("items", Arrays.asList(item1, item2));

            String result = renderer.render(
                    "{{#for items}}{{price * qty}},{{/for}}", ctx);
            assertThat(result).isEqualTo("20,60,");
        }

        @Test
        @DisplayName("多行模板")
        void shouldHandleMultiLineTemplate() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("line1", "first");
            ctx.put("line2", "second");

            String result = renderer.render("Line 1: {{line1}}\nLine 2: {{line2}}", ctx);
            assertThat(result).isEqualTo("Line 1: first\nLine 2: second");
        }

        @Test
        @DisplayName("无模板占位符的纯文本")
        void shouldReturnPlainTextWithoutPlaceholders() {
            String result = renderer.render("No placeholders here", new HashMap<String, Object>());
            assertThat(result).isEqualTo("No placeholders here");
        }
    }

    // ---- 边界情况 ----

    @Nested
    @DisplayName("边界情况")
    class EdgeCases {

        @Test
        @DisplayName("null 模板应返回 null")
        void shouldReturnNullForNullTemplate() {
            String result = renderer.render(null, new HashMap<String, Object>());
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("空模板应返回空字符串")
        void shouldReturnEmptyForEmptyTemplate() {
            String result = renderer.render("", new HashMap<String, Object>());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("null 上下文应视为空上下文")
        void shouldTreatNullContextAsEmpty() {
            String result = renderer.render("Hello, {{name}}!", (Map<String, Object>) null);
            assertThat(result).isEqualTo("Hello, !");
        }

        @Test
        @DisplayName("整数 double 应不带小数点")
        void shouldFormatIntegerDoublesWithoutDecimal() {
            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("a", 3.0);

            String result = renderer.render("{{a}}", ctx);
            assertThat(result).isEqualTo("3");
        }
    }

    // ---- extractVariables ----

    @Nested
    @DisplayName("extractVariables")
    class ExtractVariablesTests {

        @Test
        @DisplayName("应该提取简单变量")
        void shouldExtractSimpleVariables() {
            List<String> vars = renderer.extractVariables("{{name}} and {{age}}");
            assertThat(vars).containsExactlyInAnyOrder("name", "age");
        }

        @Test
        @DisplayName("应该提取嵌套变量引用")
        void shouldExtractNestedVariableReferences() {
            List<String> vars = renderer.extractVariables("{{step1.value}} and {{step2.data}}");
            assertThat(vars).containsExactlyInAnyOrder("step1.value", "step2.data");
        }

        @Test
        @DisplayName("应该跳过 for 循环标记")
        void shouldSkipForLoopMarkers() {
            List<String> vars = renderer.extractVariables(
                    "{{#for items}}{{name}}{{/for}}");
            assertThat(vars).contains("name");
            assertThat(vars).noneMatch(v -> v.contains("for") || v.contains("items"));
        }

        @Test
        @DisplayName("应该从算术表达式中提取变量")
        void shouldExtractVariablesFromArithmeticExpression() {
            List<String> vars = renderer.extractVariables("{{a + b * c}}");
            assertThat(vars).containsExactlyInAnyOrder("a", "b", "c");
        }

        @Test
        @DisplayName("null 模板应返回空列表")
        void shouldReturnEmptyForNullTemplate() {
            List<String> vars = renderer.extractVariables(null);
            assertThat(vars).isEmpty();
        }

        @Test
        @DisplayName("空模板应返回空列表")
        void shouldReturnEmptyForEmptyTemplate() {
            List<String> vars = renderer.extractVariables("");
            assertThat(vars).isEmpty();
        }

        @Test
        @DisplayName("不应提取 {{_}} 为变量")
        void shouldNotExtractUnderscoreAsVariable() {
            List<String> vars = renderer.extractVariables("{{#for items}}{{_}}{{/for}}");
            assertThat(vars).noneMatch(v -> v.equals("_"));
        }
    }

    // ---- isValid ----

    @Nested
    @DisplayName("isValid")
    class IsValidTests {

        @Test
        @DisplayName("有效模板应返回 true")
        void shouldReturnTrueForValidTemplate() {
            assertThat(renderer.isValid("Hello {{name}}")).isTrue();
        }

        @Test
        @DisplayName("null 模板应返回 true")
        void shouldReturnTrueForNullTemplate() {
            assertThat(renderer.isValid(null)).isTrue();
        }

        @Test
        @DisplayName("空模板应返回 true")
        void shouldReturnTrueForEmptyTemplate() {
            assertThat(renderer.isValid("")).isTrue();
        }
    }

    // ---- renderInputTemplate ----

    @Nested
    @DisplayName("renderInputTemplate")
    class RenderInputTemplateTests {

        @Test
        @DisplayName("应该渲染 Map 输入模板")
        void shouldRenderMapInputTemplate() {
            Map<String, Object> inputTemplate = new LinkedHashMap<String, Object>();
            inputTemplate.put("param1", "{{value1}}");
            inputTemplate.put("param2", "{{value2}}");

            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("value1", "data1");
            ctx.put("value2", "data2");

            Map<String, Object> result = renderer.renderInputTemplate(inputTemplate, ctx);
            assertThat(result).hasSize(2);
            assertThat(result.get("param1")).isEqualTo("data1");
            assertThat(result.get("param2")).isEqualTo("data2");
        }

        @Test
        @DisplayName("应该递归渲染嵌套 Map")
        void shouldRecursivelyRenderNestedMap() {
            Map<String, Object> nested = new LinkedHashMap<String, Object>();
            nested.put("field", "{{name}}");

            Map<String, Object> inputTemplate = new LinkedHashMap<String, Object>();
            inputTemplate.put("outer", nested);

            Map<String, Object> ctx = new HashMap<String, Object>();
            ctx.put("name", "Alice");

            Map<String, Object> result = renderer.renderInputTemplate(inputTemplate, ctx);
            @SuppressWarnings("unchecked")
            Map<String, Object> resultNested = (Map<String, Object>) result.get("outer");
            assertThat(resultNested.get("field")).isEqualTo("Alice");
        }

        @Test
        @DisplayName("应该保留非模板字符串")
        void shouldPreserveNonTemplateStrings() {
            Map<String, Object> inputTemplate = new LinkedHashMap<String, Object>();
            inputTemplate.put("static", "no template here");

            Map<String, Object> ctx = new HashMap<String, Object>();

            Map<String, Object> result = renderer.renderInputTemplate(inputTemplate, ctx);
            assertThat(result.get("static")).isEqualTo("no template here");
        }

        @Test
        @DisplayName("应该使用 ExecutionContext 渲染输入模板")
        void shouldRenderInputTemplateWithExecutionContext() {
            Map<String, Object> inputTemplate = new LinkedHashMap<String, Object>();
            inputTemplate.put("query", "{{input}}");

            Map<String, Object> input = new HashMap<String, Object>();
            input.put("input", "test query");
            ExecutionContext execCtx = new ExecutionContext(input);

            Map<String, Object> result = renderer.renderInputTemplate(inputTemplate, execCtx);
            assertThat(result.get("query")).isEqualTo("test query");
        }
    }

    // ---- ExecutionContext 渲染 ----

    @Nested
    @DisplayName("ExecutionContext 渲染")
    class ExecutionContextRender {

        @Test
        @DisplayName("应该使用 ExecutionContext 渲染模板")
        void shouldRenderWithExecutionContext() {
            Map<String, Object> input = new HashMap<String, Object>();
            input.put("query", "test query");

            ExecutionContext execCtx = new ExecutionContext(input);
            execCtx.addStepResult(StepResult.success("step1", "step output", 100));

            String result = renderer.render(
                    "Input: {{query}}, Step output: {{step1.value}}", execCtx);
            assertThat(result).isEqualTo("Input: test query, Step output: step output");
        }

        @Test
        @DisplayName("null ExecutionContext 应视为空上下文")
        void shouldHandleNullExecutionContext() {
            String result = renderer.render("Hello, {{name}}!", (ExecutionContext) null);
            assertThat(result).isEqualTo("Hello, !");
        }
    }
}
