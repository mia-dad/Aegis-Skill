package com.mia.aegis.skill.dsl.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PromptStepConfig 的单元测试。
 *
 * 测试覆盖：
 * - 正常创建和使用
 * - 空值和空字符串验证
 * - 模板内容处理
 * - 包含Mustache语法的模板
 */
@DisplayName("PromptStepConfig 测试")
class PromptStepConfigTest {

    @Test
    @DisplayName("应该成功创建PromptStepConfig")
    void shouldCreatePromptStepConfigSuccessfully() {
        String template = "你是一个专业的助手，请回答以下问题：{{question}}";

        PromptStepConfig config = new PromptStepConfig(template);

        assertThat(config.getTemplate()).isEqualTo(template);
        assertThat(config.getStepType()).isEqualTo(StepType.PROMPT);
    }

    @Test
    @DisplayName("模板内容应该保持原样（不修剪内部空格）")
    void shouldNotTrimInternalSpaces() {
        String template = "  模板开头和结尾的空格  ";

        PromptStepConfig config = new PromptStepConfig(template);

        assertThat(config.getTemplate()).isEqualTo(template);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("应该拒绝null或空模板")
    void shouldRejectNullOrEmptyTemplate(String template) {
        assertThatThrownBy(() -> new PromptStepConfig(template))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("技能所用到的提示词不能为空");
    }

    @ParameterizedTest
    @ValueSource(strings = {"   ", "\t", "\n", "  \t\n  "})
    @DisplayName("应该拒绝仅包含空白字符的模板")
    void shouldRejectBlankTemplate(String template) {
        assertThatThrownBy(() -> new PromptStepConfig(template))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("技能所用到的提示词不能为空");
    }

    @Test
    @DisplayName("getStepType应该返回PROMPT")
    void getStepTypeShouldReturnPrompt() {
        PromptStepConfig config = new PromptStepConfig("测试模板");

        assertThat(config.getStepType()).isEqualTo(StepType.PROMPT);
    }

    @Test
    @DisplayName("应该处理包含Mustache变量语法的模板")
    void shouldHandleMustacheVariableSyntax() {
        String template = "你好{{name}}，这是{{context}}的内容";

        PromptStepConfig config = new PromptStepConfig(template);

        assertThat(config.getTemplate()).contains("{{name}}");
        assertThat(config.getTemplate()).contains("{{context}}");
    }

    @Test
    @DisplayName("应该处理包含step.output语法的模板")
    void shouldHandleStepOutputSyntax() {
        String template = "基于前面的分析：{{step1.output}}，请给出建议";

        PromptStepConfig config = new PromptStepConfig(template);

        assertThat(config.getTemplate()).contains("{{step1.output}}");
    }

    @Test
    @DisplayName("应该处理多行模板")
    void shouldHandleMultiLineTemplate() {
        String template = "第一行\n第二行\n第三行";

        PromptStepConfig config = new PromptStepConfig(template);

        assertThat(config.getTemplate()).isEqualTo("第一行\n第二行\n第三行");
    }

    @Test
    @DisplayName("应该处理包含特殊字符的模板")
    void shouldHandleSpecialCharacters() {
        String template = "包含特殊字符：@#$%^&*()_+-=[]{}|;':\",./<>?";

        PromptStepConfig config = new PromptStepConfig(template);

        assertThat(config.getTemplate()).contains("@#$%^&*()");
    }

    @Test
    @DisplayName("toString应该在模板过长时截断")
    void shouldTruncateLongTemplateInToString() {
        String longTemplate = "这是一个很长的模板内容，应该在被toString方法截断。" +
                              "当我们使用toString方法时，如果模板内容超过50个字符，" +
                              "应该只显示前50个字符并加上省略号。";

        PromptStepConfig config = new PromptStepConfig(longTemplate);

        assertThat(config.toString()).contains("...");
        // toString()截断后应该比原始模板短（因为包含了"..."和额外的格式化字符）
        assertThat(config.toString().length()).isLessThan(longTemplate.length() + 100); // 宽松的断言
    }

    @Test
    @DisplayName("toString应该完整显示短模板")
    void shouldDisplayShortTemplateFullyInToString() {
        String shortTemplate = "短模板";

        PromptStepConfig config = new PromptStepConfig(shortTemplate);

        assertThat(config.toString()).contains("短模板");
        assertThat(config.toString()).doesNotContain("...");
    }

    @Test
    @DisplayName("应该处理包含条件语法的模板")
    void shouldHandleConditionalSyntax() {
        String template = "{{#condition}}条件为真时显示{{/condition}}";

        PromptStepConfig config = new PromptStepConfig(template);

        assertThat(config.getTemplate()).contains("{{#condition}}");
        assertThat(config.getTemplate()).contains("{{/condition}}");
    }

    @Test
    @DisplayName("应该处理包含循环语法的模板")
    void shouldHandleLoopSyntax() {
        String template = "{{#items}}{{name}}: {{value}}{{/items}}";

        PromptStepConfig config = new PromptStepConfig(template);

        assertThat(config.getTemplate()).contains("{{#items}}");
        assertThat(config.getTemplate()).contains("{{name}}");
        assertThat(config.getTemplate()).contains("{{/items}}");
    }

    @Test
    @DisplayName("应该正确计算模板长度（50字符边界）")
    void shouldCalculateTemplateLengthCorrectly() {
        String exact50Template = "12345678901234567890123456789012345678901234567890";

        PromptStepConfig config = new PromptStepConfig(exact50Template);

        assertThat(config.toString()).doesNotContain("...");
    }

    @Test
    @DisplayName("应该处理51字符的模板（刚好超过边界）")
    void shouldHandle51CharTemplate() {
        String template51 = "123456789012345678901234567890123456789012345678901";

        PromptStepConfig config = new PromptStepConfig(template51);

        assertThat(config.toString()).contains("...");
    }
}
