package com.mia.aegis.skill.dsl.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ToolStepConfig 的单元测试。
 *
 * 测试覆盖：
 * - 正常创建和使用
 * - 空值和空字符串验证
 * - 输入模板的处理
 * - 不可变性验证
 */
@DisplayName("ToolStepConfig 测试")
class ToolStepConfigTest {

    @Test
    @DisplayName("应该成功创建ToolStepConfig")
    void shouldCreateToolStepConfigSuccessfully() {
        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("param1", "value1");
        inputTemplate.put("param2", "{{variable}}");

        ToolStepConfig config = new ToolStepConfig("test_tool", inputTemplate);

        assertThat(config.getToolName()).isEqualTo("test_tool");
        assertThat(config.getInputTemplate()).hasSize(2);
        assertThat(config.getStepType()).isEqualTo(StepType.TOOL);
    }

    @Test
    @DisplayName("工具名称前后空格应该被修剪")
    void shouldTrimToolName() {
        ToolStepConfig config = new ToolStepConfig("  test_tool  ", null);

        assertThat(config.getToolName()).isEqualTo("test_tool");
    }

    @Test
    @DisplayName("应该拒绝null工具名称")
    void shouldRejectNullToolName() {
        assertThatThrownBy(() -> new ToolStepConfig(null, new HashMap<String, String>()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("技能所用到的工具名称不能为空");
    }

    @Test
    @DisplayName("应该拒绝空字符串工具名称")
    void shouldRejectEmptyToolName() {
        assertThatThrownBy(() -> new ToolStepConfig("", new HashMap<String, String>()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("技能所用到的工具名称不能为空");
    }

    @Test
    @DisplayName("应该拒绝仅包含空格的工具名称")
    void shouldRejectBlankToolName() {
        assertThatThrownBy(() -> new ToolStepConfig("   ", new HashMap<String, String>()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("技能所用到的工具名称不能为空");
    }

    @Test
    @DisplayName("null输入模板应该被视为空模板")
    void shouldTreatNullInputTemplateAsEmpty() {
        ToolStepConfig config = new ToolStepConfig("test_tool", null);

        assertThat(config.getInputTemplate()).isNotNull();
        assertThat(config.getInputTemplate()).isEmpty();
    }

    @Test
    @DisplayName("输入模板应该是不可变的")
    void inputTemplateShouldBeUnmodifiable() {
        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("param1", "value1");

        ToolStepConfig config = new ToolStepConfig("test_tool", inputTemplate);

        assertThatThrownBy(() -> config.getInputTemplate().put("param2", "value2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("构造函数应该防御性复制输入模板")
    void constructorShouldDefensivelyCopyInputTemplate() {
        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("param1", "value1");

        ToolStepConfig config = new ToolStepConfig("test_tool", inputTemplate);
        inputTemplate.put("param2", "value2");

        assertThat(config.getInputTemplate()).hasSize(1);
        assertThat(config.getInputTemplate()).doesNotContainKey("param2");
    }

    @Test
    @DisplayName("getStepType应该返回TOOL")
    void getStepTypeShouldReturnTool() {
        ToolStepConfig config = new ToolStepConfig("test_tool", new HashMap<String, String>());

        assertThat(config.getStepType()).isEqualTo(StepType.TOOL);
    }

    @Test
    @DisplayName("toString应该包含工具名称和输入模板信息")
    void toStringShouldContainToolNameAndInputTemplate() {
        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("param1", "value1");

        ToolStepConfig config = new ToolStepConfig("test_tool", inputTemplate);

        assertThat(config.toString()).contains("test_tool");
        assertThat(config.toString()).contains("param1");
        assertThat(config.toString()).contains("value1");
    }

    @Test
    @DisplayName("应该处理包含Mustache语法的输入模板")
    void shouldHandleMustacheSyntaxInInputTemplate() {
        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("query", "{{user_input}}");
        inputTemplate.put("context", "{{step1.output}}");

        ToolStepConfig config = new ToolStepConfig("search_tool", inputTemplate);

        assertThat(config.getInputTemplate().get("query")).isEqualTo("{{user_input}}");
        assertThat(config.getInputTemplate().get("context")).isEqualTo("{{step1.output}}");
    }

    @Test
    @DisplayName("应该处理空输入模板")
    void shouldHandleEmptyInputTemplate() {
        Map<String, String> emptyTemplate = new HashMap<String, String>();

        ToolStepConfig config = new ToolStepConfig("test_tool", emptyTemplate);

        assertThat(config.getInputTemplate()).isEmpty();
    }

    @Test
    @DisplayName("应该处理多参数输入模板")
    void shouldHandleMultiParameterInputTemplate() {
        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("param1", "value1");
        inputTemplate.put("param2", "value2");
        inputTemplate.put("param3", "value3");

        ToolStepConfig config = new ToolStepConfig("test_tool", inputTemplate);

        assertThat(config.getInputTemplate()).hasSize(3);
        assertThat(config.getInputTemplate()).containsEntry("param1", "value1");
        assertThat(config.getInputTemplate()).containsEntry("param2", "value2");
        assertThat(config.getInputTemplate()).containsEntry("param3", "value3");
    }

    @Test
    @DisplayName("应该保持输入模板的插入顺序")
    void shouldMaintainInputTemplateOrder() {
        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("param3", "value3");
        inputTemplate.put("param1", "value1");
        inputTemplate.put("param2", "value2");

        ToolStepConfig config = new ToolStepConfig("test_tool", inputTemplate);

        assertThat(config.getInputTemplate().keySet()).containsExactly("param3", "param1", "param2");
    }
}
