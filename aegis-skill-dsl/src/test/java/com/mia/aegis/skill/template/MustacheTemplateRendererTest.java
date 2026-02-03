package com.mia.aegis.skill.template;

import com.mia.aegis.skill.exception.TemplateRenderException;
import com.mia.aegis.skill.executor.context.ExecutionContext;
import com.mia.aegis.skill.executor.context.StepResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MustacheTemplateRenderer 的单元测试。
 *
 * 测试覆盖：
 * - 基本变量替换
 * - 嵌套属性访问
 * - 条件和循环
 * - 边界情况（空值、null等）
 * - 变量提取
 * - 模板验证
 */
@DisplayName("MustacheTemplateRenderer 测试")
class MustacheTemplateRendererTest {

    @Test
    @DisplayName("应该渲染简单的变量替换")
    void shouldRenderSimpleVariableSubstitution() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "Hello, {{name}}!";
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("name", "World");

        String result = renderer.render(template, context);

        assertThat(result).isEqualTo("Hello, World!");
    }

    @Test
    @DisplayName("应该渲染多个变量")
    void shouldRenderMultipleVariables() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "{{greeting}}, {{name}}! Your age is {{age}}.";
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("greeting", "Hello");
        context.put("name", "Alice");
        context.put("age", 30);

        String result = renderer.render(template, context);

        assertThat(result).isEqualTo("Hello, Alice! Your age is 30.");
    }

    @Test
    @DisplayName("应该处理嵌套属性访问")
    void shouldHandleNestedPropertyAccess() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "Result: {{data.output}}";

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("output", "success");

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("data", data);

        String result = renderer.render(template, context);

        assertThat(result).isEqualTo("Result: success");
    }

    @Test
    @DisplayName("应该处理null上下文")
    void shouldHandleNullContext() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "Hello, {{name}}!";

        String result = renderer.render(template, (java.util.Map<String, Object>) null);

        assertThat(result).isEqualTo("Hello, !");
    }

    @Test
    @DisplayName("应该处理null模板")
    void shouldHandleNullTemplate() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        Map<String, Object> context = new HashMap<String, Object>();

        String result = renderer.render(null, context);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("应该处理空模板")
    void shouldHandleEmptyTemplate() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        Map<String, Object> context = new HashMap<String, Object>();

        String result = renderer.render("", context);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("应该将缺失的变量替换为空字符串")
    void shouldReplaceMissingVariablesWithEmptyString() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "Hello, {{missing}}!";
        Map<String, Object> context = new HashMap<String, Object>();

        String result = renderer.render(template, context);

        assertThat(result).isEqualTo("Hello, !");
    }

    @Test
    @DisplayName("应该使用ExecutionContext渲染模板")
    void shouldRenderTemplateWithExecutionContext() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "Input: {{query}}, Step output: {{step1.output}}";

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("query", "test query");

        ExecutionContext execContext = new ExecutionContext(input);
        execContext.addStepResult(StepResult.success("step1", "step output", 100));

        String result = renderer.render(template, (java.util.Map<String, Object>) execContext.buildVariableContext());

        assertThat(result).isEqualTo("Input: test query, Step output: step output");
    }

    @Test
    @DisplayName("应该处理条件块（存在时显示）")
    void shouldHandleConditionalBlockWhenPresent() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "{{#show}}This is shown{{/show}}";

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("show", true);

        String result = renderer.render(template, context);

        assertThat(result).isEqualTo("This is shown");
    }

    @Test
    @DisplayName("应该处理条件块（不存在时隐藏）")
    void shouldHandleConditionalBlockWhenAbsent() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "{{#show}}This is shown{{/show}}";

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("show", false);

        String result = renderer.render(template, context);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("应该处理循环")
    void shouldHandleLoop() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "{{#items}}{{name}}, {{/items}}";

        Map<String, Object> item1 = new HashMap<String, Object>();
        item1.put("name", "Item1");
        Map<String, Object> item2 = new HashMap<String, Object>();
        item2.put("name", "Item2");
        Map<String, Object> item3 = new HashMap<String, Object>();
        item3.put("name", "Item3");

        List<Map<String, Object>> items = java.util.Arrays.asList(item1, item2, item3);

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("items", items);

        String result = renderer.render(template, context);

        assertThat(result).isEqualTo("Item1, Item2, Item3, ");
    }

    @Test
    @DisplayName("应该提取模板中的变量")
    void shouldExtractVariablesFromTemplate() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "Hello {{name}}, your age is {{age}}";

        List<String> variables = renderer.extractVariables(template);

        assertThat(variables).containsExactlyInAnyOrder("name", "age");
    }

    @Test
    @DisplayName("提取变量应该去除重复")
    void extractVariablesShouldRemoveDuplicates() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "{{name}} is {{name}}, age is {{age}}";

        List<String> variables = renderer.extractVariables(template);

        assertThat(variables).containsExactlyInAnyOrder("name", "age");
        assertThat(variables).hasSize(2);
    }

    @Test
    @DisplayName("提取变量应该修剪空格")
    void extractVariablesShouldTrimWhitespace() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "{{ name }} is {{ age }}";

        List<String> variables = renderer.extractVariables(template);

        assertThat(variables).containsExactlyInAnyOrder("name", "age");
    }

    @Test
    @DisplayName("应该提取嵌套变量引用")
    void shouldExtractNestedVariableReferences() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "{{step1.output}} and {{step2.data.result}}";

        List<String> variables = renderer.extractVariables(template);

        assertThat(variables).containsExactlyInAnyOrder("step1.output", "step2.data.result");
    }

    @Test
    @DisplayName("extractVariables应该处理null模板")
    void extractVariablesShouldHandleNullTemplate() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();

        List<String> variables = renderer.extractVariables(null);

        assertThat(variables).isEmpty();
    }

    @Test
    @DisplayName("extractVariables应该处理空模板")
    void extractVariablesShouldHandleEmptyTemplate() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();

        List<String> variables = renderer.extractVariables("");

        assertThat(variables).isEmpty();
    }

    @Test
    @DisplayName("isValid应该验证正确的模板")
    void isValidShouldValidateCorrectTemplate() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();

        assertThat(renderer.isValid("Hello {{name}}")).isTrue();
    }

    @Test
    @DisplayName("isValid应该接受null或空模板")
    void isValidShouldAcceptNullOrEmptyTemplate() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();

        assertThat(renderer.isValid(null)).isTrue();
        assertThat(renderer.isValid("")).isTrue();
    }

    @Test
    @DisplayName("应该渲染输入模板Map")
    void shouldRenderInputTemplateMap() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();

        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("param1", "{{value1}}");
        inputTemplate.put("param2", "{{value2}}");

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("value1", "data1");
        context.put("value2", "data2");

        Map<String, Object> result = renderer.renderInputTemplate(inputTemplate, context);

        assertThat(result).hasSize(2);
        assertThat(result.get("param1")).isEqualTo("data1");
        assertThat(result.get("param2")).isEqualTo("data2");
    }

    @Test
    @DisplayName("渲染输入模板应该使用ExecutionContext")
    void renderInputTemplateShouldUseExecutionContext() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();

        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("query", "{{input}}");

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("input", "test query");

        ExecutionContext context = new ExecutionContext(input);

        Map<String, Object> result = renderer.renderInputTemplate(inputTemplate, context);

        assertThat(result.get("query")).isEqualTo("test query");
    }

    @Test
    @DisplayName("应该处理带空格的变量")
    void shouldHandleVariablesWithSpaces() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "Value: {{ value }}";
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("value", "test");

        String result = renderer.render(template, context);

        assertThat(result).isEqualTo("Value: test");
    }

    @Test
    @DisplayName("应该处理特殊字符（不转义）")
    void shouldHandleSpecialCharacters() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "Special: {{value}}";
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("value", "@#$%^&*()");

        String result = renderer.render(template, context);

        // 默认不转义，保持原始字符
        assertThat(result).isEqualTo("Special: @#$%^&*()");
    }

    @Test
    @DisplayName("应该支持HTML转义模式")
    void shouldSupportHtmlEscapingMode() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer(true);
        String template = "Special: {{value}}";
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("value", "@#$%^&*()");

        String result = renderer.render(template, context);

        // 启用HTML转义时，&会被转义
        assertThat(result).isEqualTo("Special: @#$%^&amp;*()");
    }

    @Test
    @DisplayName("应该处理多行模板")
    void shouldHandleMultiLineTemplate() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "Line 1: {{line1}}\nLine 2: {{line2}}";
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("line1", "first");
        context.put("line2", "second");

        String result = renderer.render(template, context);

        assertThat(result).isEqualTo("Line 1: first\nLine 2: second");
    }

    @Test
    @DisplayName("严格模式构造函数应该工作")
    void strictModeConstructorShouldWork() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer(true);
        String template = "Hello, {{name}}!";
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("name", "World");

        String result = renderer.render(template, context);

        assertThat(result).isEqualTo("Hello, World!");
    }

    @Test
    @DisplayName("应该处理数字类型的变量")
    void shouldHandleNumericVariables() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "Count: {{count}}, Price: {{price}}";
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("count", 42);
        context.put("price", 19.99);

        String result = renderer.render(template, context);

        assertThat(result).isEqualTo("Count: 42, Price: 19.99");
    }

    @Test
    @DisplayName("应该处理布尔类型的变量")
    void shouldHandleBooleanVariables() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "Flag1: {{flag1}}, Flag2: {{flag2}}";
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("flag1", true);
        context.put("flag2", false);

        String result = renderer.render(template, context);

        assertThat(result).isEqualTo("Flag1: true, Flag2: false");
    }

    @Test
    @DisplayName("应该处理列表类型的变量")
    void shouldHandleListVariables() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "{{#items}}{{.}}, {{/items}}";
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("items", java.util.Arrays.asList("a", "b", "c"));

        String result = renderer.render(template, context);

        assertThat(result).isEqualTo("a, b, c, ");
    }

    @Test
    @DisplayName("应该不提取注释变量")
    void shouldNotExtractCommentVariables() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "{{! comment }}Variable: {{name}}";

        List<String> variables = renderer.extractVariables(template);

        assertThat(variables).containsExactly("name");
    }

    @Test
    @DisplayName("应该处理带点的嵌套变量")
    void shouldHandleDottedNestedVariables() {
        MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();
        String template = "{{user.name}} - {{user.age}}";

        Map<String, Object> user = new HashMap<String, Object>();
        user.put("name", "Alice");
        user.put("age", 30);

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("user", user);

        String result = renderer.render(template, context);

        assertThat(result).isEqualTo("Alice - 30");
    }
}
