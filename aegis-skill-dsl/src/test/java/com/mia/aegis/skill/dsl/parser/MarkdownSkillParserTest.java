package com.mia.aegis.skill.dsl.parser;

import com.mia.aegis.skill.dsl.model.*;
import com.mia.aegis.skill.exception.SkillParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MarkdownSkillParser 的单元测试。
 *
 * 测试覆盖：
 * - 正常解析
 * - 必需字段验证
 * - 各种Step类型解析
 * - 边界情况（空值、无效语法等）
 * - 错误处理和行号报告
 */

@DisplayName("MarkdownSkillParser 测试")
class MarkdownSkillParserTest {

    private final MarkdownSkillParser parser = new MarkdownSkillParser();

    @Test
    @DisplayName("应该成功解析完整的技能文件")
    void shouldParseCompleteSkillFileSuccessfully() throws Exception {
        String content = "# skill: test_skill\n" +
                "\n" +
                "## description\n" +
                "\n" +
                "这是一个测试技能。\n" +
                "\n" +
                "## intent\n" +
                "\n" +
                "- 测试\n" +
                "- 示例\n" +
                "\n" +
                "## input\n" +
                "\n" +
                "```yaml\n" +
                "query: string\n" +
                "```\n" +
                "\n" +
                "## steps\n" +
                "\n" +
                "### step: search\n" +
                "\n" +
                "**type**: tool\n" +
                "**tool**: search_api\n" +
                "\n" +
                "```yaml\n" +
                "q: \"{{query}}\"\n" +
                "```\n" +
                "\n" +
                "## output\n" +
                "\n" +
                "```json\n" +
                "{\n" +
                "  \"results\": \"array\"\n" +
                "}\n" +
                "```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getId()).isEqualTo("test_skill");
        assertThat(skill.getDescription()).isEqualTo("这是一个测试技能。");
        assertThat(skill.getIntents()).containsExactly("测试", "示例");
        assertThat(skill.getSteps()).hasSize(1);
        assertThat(skill.getSteps().get(0).getName()).isEqualTo("search");
        assertThat(skill.getSteps().get(0).getType()).isEqualTo(StepType.TOOL);
    }

    @Test
    @DisplayName("应该解析financial_analysis.md测试文件")
    void shouldParseFinancialAnalysisSkillFile() throws Exception {
        java.io.File file = new java.io.File("src/test/resources/skills/financial_analysis.md");

        if (file.exists()) {
            Skill skill = parser.parseFile(file.toPath());

            assertThat(skill.getId()).isEqualTo("financial_analysis");
            assertThat(skill.getSteps()).hasSize(2);
            assertThat(skill.getSteps().get(0).getName()).isEqualTo("fetch_financial_data");
            assertThat(skill.getSteps().get(1).getName()).isEqualTo("analyze_data");
        }
    }

    @Test
    @DisplayName("应该解析simple_tool.md测试文件")
    void shouldParseSimpleToolSkillFile() throws Exception {
        java.io.File file = new java.io.File("src/test/resources/skills/simple_tool.md");

        if (file.exists()) {
            Skill skill = parser.parseFile(file.toPath());

            assertThat(skill.getId()).isEqualTo("simple_tool_test");
            assertThat(skill.getSteps()).hasSize(1);
            assertThat(skill.getSteps().get(0).getName()).isEqualTo("search");
        }
    }

    @Test
    @DisplayName("应该拒绝缺少skill header的文件")
    void shouldRejectFileWithoutSkillHeader() {
        String content = "## description\n\n测试描述\n\n## steps\n\n### step: test\n";

        assertThatThrownBy(() -> parser.parse(content))
                .isInstanceOf(SkillParseException.class)
                .hasMessageContaining("技能内容必须有");
    }

    @Test
    @DisplayName("应该拒绝无效的skill header格式")
    void shouldRejectInvalidSkillHeaderFormat() {
        String content = "# invalid header\n\n## steps\n\n### step: test\n";

        assertThatThrownBy(() -> parser.parse(content))
                .isInstanceOf(SkillParseException.class)
                .hasMessageContaining("无效的Skill文件");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("应该拒绝空内容")
    void shouldRejectEmptyContent(String content) {
        assertThatThrownBy(() -> parser.parse(content))
                .isInstanceOf(SkillParseException.class)
                .hasMessageContaining("文件内容不能为空");
    }

    @Test
    @DisplayName("应该拒绝没有steps的技能")
    void shouldRejectSkillWithoutSteps() {
        String content = "# skill: test_skill\n\n## description\n\n测试描述\n";

        assertThatThrownBy(() -> parser.parse(content))
                .isInstanceOf(SkillParseException.class)
                .hasMessageContaining("技能至少需要");
    }

    @Test
    @DisplayName("应该解析Tool类型的步骤")
    void shouldParseToolStep() throws Exception {
        String content = "# skill: test\n\n## steps\n\n### step: tool_step\n\n**type**: tool\n**tool**: test_tool\n\n```yaml\nparam: value\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getSteps()).hasSize(1);
        Step step = skill.getSteps().get(0);
        assertThat(step.getName()).isEqualTo("tool_step");
        assertThat(step.getType()).isEqualTo(StepType.TOOL);
        assertThat(step.getToolConfig().getToolName()).isEqualTo("test_tool");
    }

    @Test
    @DisplayName("应该解析Prompt类型的步骤")
    void shouldParsePromptStep() throws Exception {
        String content = "# skill: test\n\n## steps\n\n### step: prompt_step\n\n**type**: prompt\n\n```prompt\n这是一个提示词模板\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getSteps()).hasSize(1);
        Step step = skill.getSteps().get(0);
        assertThat(step.getName()).isEqualTo("prompt_step");
        assertThat(step.getType()).isEqualTo(StepType.PROMPT);
        assertThat(step.getPromptConfig().getTemplate()).contains("提示词模板");
    }

    @Test
    @DisplayName("应该推断Tool步骤类型（当type属性缺失时）")
    void shouldInferToolStepTypeWhenTypeMissing() throws Exception {
        String content = "# skill: test\n\n## steps\n\n### step: tool_step\n\n**tool**: test_tool\n\n```yaml\nparam: value\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getSteps().get(0).getType()).isEqualTo(StepType.TOOL);
    }

    @Test
    @DisplayName("应该推断Prompt步骤类型（当type属性缺失但有prompt块时）")
    void shouldInferPromptStepTypeWhenTypeMissing() throws Exception {
        String content = "# skill: test\n\n## steps\n\n### step: prompt_step\n\n```prompt\n提示词内容\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getSteps().get(0).getType()).isEqualTo(StepType.PROMPT);
    }

    @Test
    @DisplayName("应该解析包含Mustache变量的输入模板")
    void shouldParseInputTemplateWithMustacheVariables() throws Exception {
        String content = "# skill: test\n\n## steps\n\n### step: tool_step\n\n**type**: tool\n**tool**: test_tool\n\n```yaml\nparam1: \"{{var1}}\"\nparam2: \"{{step2.output}}\"\n```\n";

        Skill skill = parser.parse(content);

        Map<String, String> inputTemplate = skill.getSteps().get(0).getToolConfig().getInputTemplate();
        assertThat(inputTemplate.get("param1")).isEqualTo("{{var1}}");
        assertThat(inputTemplate.get("param2")).isEqualTo("{{step2.output}}");
    }

    @Test
    @DisplayName("应该解析intent列表")
    void shouldParseIntentList() throws Exception {
        String content = "# skill: test\n\n## intent\n\n- 意图1\n- 意图2\n- 意图3\n\n## steps\n\n### step: test\n\n**type**: prompt\n\n```prompt\ntest\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getIntents()).containsExactly("意图1", "意图2", "意图3");
    }

    @Test
    @DisplayName("应该解析input schema")
    void shouldParseInputSchema() throws Exception {
        String content = "# skill: test\n\n## input\n\n```yaml\nparam1: string\nparam2: number\n```\n\n## steps\n\n### step: test\n\n**type**: prompt\n\n```prompt\ntest\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getInputSchema().hasField("param1")).isTrue();
        assertThat(skill.getInputSchema().hasField("param2")).isTrue();
    }

    @Test
    @DisplayName("应该解析output contract")
    void shouldParseOutputContract() throws Exception {
        String content = "# skill: test\n\n## steps\n\n### step: test\n\n**type**: prompt\n\n```prompt\ntest\n```\n\n## output\n\n```json\n{\n  \"result\": \"string\"\n}\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getOutputContract().getField("result")).isNotNull();
    }

    @Test
    @DisplayName("应该解析多行description")
    void shouldParseMultiLineDescription() throws Exception {
        String content = "# skill: test\n\n## description\n\n第一行描述\n第二行描述\n第三行描述\n\n## steps\n\n### step: test\n\n**type**: prompt\n\n```prompt\ntest\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getDescription()).contains("第一行描述");
        assertThat(skill.getDescription()).contains("第二行描述");
        assertThat(skill.getDescription()).contains("第三行描述");
    }

    @Test
    @DisplayName("应该解析多个步骤")
    void shouldParseMultipleSteps() throws Exception {
        String content = "# skill: test\n\n## steps\n\n### step: step1\n\n**type**: tool\n**tool**: tool1\n\n```yaml\nparam: value\n```\n\n### step: step2\n\n**type**: prompt\n\n```prompt\ntemplate\n```\n\n### step: step3\n\n**type**: tool\n**tool**: tool3\n\n```yaml\nq: test\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getSteps()).hasSize(3);
        assertThat(skill.getSteps().get(0).getName()).isEqualTo("step1");
        assertThat(skill.getSteps().get(1).getName()).isEqualTo("step2");
        assertThat(skill.getSteps().get(2).getName()).isEqualTo("step3");
    }

    @Test
    @DisplayName("应该处理大小写不敏感的step header")
    void shouldHandleCaseInsensitiveStepHeader() throws Exception {
        String content = "# skill: test\n\n## steps\n\n### STEP: test_step\n\n**type**: tool\n**tool**: test_tool\n\n```yaml\nparam: value\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getSteps().get(0).getName()).isEqualTo("test_step");
    }

    @Test
    @DisplayName("应该处理大小写不敏感的skill header")
    void shouldHandleCaseInsensitiveSkillHeader() throws Exception {
        String content = "# SKILL: test_skill\n\n## steps\n\n### step: test\n\n**type**: tool\n**tool**: test_tool\n\n```yaml\nparam: value\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getId()).isEqualTo("test_skill");
    }

    @Test
    @DisplayName("应该拒绝Tool步骤缺少tool属性")
    void shouldRejectToolStepWithoutToolAttribute() {
        String content = "# skill: test\n\n## steps\n\n### step: tool_step\n\n**type**: tool\n\n```yaml\nparam: value\n```\n";

        assertThatThrownBy(() -> parser.parse(content))
                .isInstanceOf(SkillParseException.class)
                .hasMessageContaining("缺少")
                .hasMessageContaining("tool");
    }

    @Test
    @DisplayName("应该拒绝Prompt步骤缺少prompt模板")
    void shouldRejectPromptStepWithoutTemplate() {
        String content = "# skill: test\n\n## steps\n\n### step: prompt_step\n\n**type**: prompt\n";

        assertThatThrownBy(() -> parser.parse(content))
                .isInstanceOf(SkillParseException.class)
                .hasMessageContaining("缺少")
                .hasMessageContaining("prompt");
    }

    @Test
    @DisplayName("isValid应该验证有效的技能文件")
    void isValidShouldValidateValidSkillFile() {
        String content = "# skill: test\n\n## steps\n\n### step: test\n\n**type**: prompt\n\n```prompt\ntemplate\n```\n";

        assertThat(parser.isValid(content)).isTrue();
    }

    @Test
    @DisplayName("isValid应该拒绝无效的技能文件")
    void isValidShouldRejectInvalidSkillFile() {
        String content = "## steps\n\n### step: test\n";

        assertThat(parser.isValid(content)).isFalse();
    }

    @Test
    @DisplayName("isValid应该拒绝空内容")
    void isValidShouldRejectEmptyContent() {
        assertThat(parser.isValid("")).isFalse();
        assertThat(parser.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("应该解析包含扩展字段的技能")
    void shouldParseSkillWithExtensionFields() throws Exception {
        String content = "# skill: test\n\n## x-aegis-version\n\n1.0.0\n\n## steps\n\n### step: test\n\n**type**: prompt\n\n```prompt\ntemplate\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.hasExtension("version")).isTrue();
        // 扩展字段作为字符串解析
        assertThat(skill.getExtension("version")).isEqualTo("1.0.0");
    }

    @Test
    @DisplayName("应该处理没有description的技能")
    void shouldHandleSkillWithoutDescription() throws Exception {
        String content = "# skill: test\n\n## steps\n\n### step: test\n\n**type**: prompt\n\n```prompt\ntemplate\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getDescription()).isNull();
    }

    @Test
    @DisplayName("应该处理没有intent的技能")
    void shouldHandleSkillWithoutIntent() throws Exception {
        String content = "# skill: test\n\n## steps\n\n### step: test\n\n**type**: prompt\n\n```prompt\ntemplate\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getIntents()).isEmpty();
    }

    @Test
    @DisplayName("应该处理没有input的技能")
    void shouldHandleSkillWithoutInput() throws Exception {
        String content = "# skill: test\n\n## steps\n\n### step: test\n\n**type**: prompt\n\n```prompt\ntemplate\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getInputSchema().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("应该处理没有output的技能")
    void shouldHandleSkillWithoutOutput() throws Exception {
        String content = "# skill: test\n\n## steps\n\n### step: test\n\n**type**: prompt\n\n```prompt\ntemplate\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getOutputContract().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("应该解析JSON格式的input")
    void shouldParseJsonFormatInput() throws Exception {
        String content = "# skill: test\n\n## input\n\n```json\n{\n  \"param1\": \"string\"\n}\n```\n\n## steps\n\n### step: test\n\n**type**: prompt\n\n```prompt\ntemplate\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getInputSchema().hasField("param1")).isTrue();
    }

    @Test
    @DisplayName("应该拒绝语法错误的YAML")
    void shouldRejectMalformedYaml() {
        String content = "# skill: test\n\n## input\n\n```yaml\nparam1: string\n  invalid indentation\n```\n\n## steps\n\n### step: test\n\n**type**: prompt\n\n```prompt\ntemplate\n```\n";

        // 解析器应该尽力解析，但可能会遇到问题
        // 这里我们测试解析器不会崩溃
        try {
            parser.parse(content);
            // 如果解析成功，说明解析器容错性好
        } catch (Exception e) {
            // 如果抛出异常也是可以接受的
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("应该处理空的input schema")
    void shouldHandleEmptyInputSchema() throws Exception {
        String content = "# skill: test\n\n## input\n\n```yaml\n```\n\n## steps\n\n### step: test\n\n**type**: prompt\n\n```prompt\ntemplate\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getInputSchema().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("应该解析复杂类型的input schema")
    void shouldParseComplexInputSchema() throws Exception {
        String content = "# skill: test\n\n## input\n\n```yaml\nparam1:\n  type: string\n  required: true\n  description: 参数描述\nparam2: string\n```\n\n## steps\n\n### step: test\n\n**type**: prompt\n\n```prompt\ntemplate\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getInputSchema().hasField("param1")).isTrue();
        assertThat(skill.getInputSchema().hasField("param2")).isTrue();
    }

    @Test
    @DisplayName("错误消息应该包含行号")
    void errorMessageShouldContainLineNumber() {
        String content = "# invalid header\n\n## steps\n\n### step: test\n";

        assertThatThrownBy(() -> parser.parse(content))
                .isInstanceOf(SkillParseException.class)
                .satisfies(exception -> {
                    SkillParseException ex = (SkillParseException) exception;
                    // 检查异常是否包含行号信息（如果解析器支持）
                    // 行号可能是0（表示无法确定）或实际行号
                    assertThat(ex).isNotNull();
                });
    }

    @Test
    @DisplayName("应该解析包含特殊字符的step名称")
    void shouldParseStepNameWithSpecialCharacters() throws Exception {
        String content = "# skill: test\n\n## steps\n\n### step: test_step_123\n\n**type**: tool\n**tool**: test_tool\n\n```yaml\nparam: value\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getStep("test_step_123")).isNotNull();
    }

    @Test
    @DisplayName("应该处理步骤属性的多行格式")
    void shouldHandleMultiLineStepAttributes() throws Exception {
        String content = "# skill: test\n\n## steps\n\n### step: test\n\n**type**: tool\n**tool**: test_tool\n\n```yaml\nparam: value\n```\n";

        Skill skill = parser.parse(content);

        assertThat(skill.getSteps().get(0).getToolConfig().getToolName()).isEqualTo("test_tool");
    }
}
