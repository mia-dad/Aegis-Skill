package com.mia.aegis.skill.dsl.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * StepType 枚举的单元测试。
 *
 * 测试覆盖：
 * - 枚举值的存在性和正确性
 * - fromString 方法的正常情况
 * - fromString 方法的边界情况（空值、空字符串、大小写）
 * - fromString 方法的异常情况（无效值）
 */
@DisplayName("StepType 枚举测试")
class StepTypeTest {

    @Test
    @DisplayName("应该包含所有预期的枚举值")
    void shouldContainAllExpectedValues() {
        assertThat(StepType.values())
                .hasSize(4)
                .containsExactly(StepType.TOOL, StepType.PROMPT, StepType.AWAIT, StepType.TEMPLATE);
    }

    @Test
    @DisplayName("fromString 应该正确解析 TOOL（不区分大小写）")
    void fromString_shouldParseToolCaseInsensitive() {
        assertThat(StepType.fromString("tool")).isEqualTo(StepType.TOOL);
        assertThat(StepType.fromString("TOOL")).isEqualTo(StepType.TOOL);
        assertThat(StepType.fromString("Tool")).isEqualTo(StepType.TOOL);
        assertThat(StepType.fromString("tOoL")).isEqualTo(StepType.TOOL);
        assertThat(StepType.fromString("  tool  ")).isEqualTo(StepType.TOOL);
    }

    @Test
    @DisplayName("fromString 应该正确解析 PROMPT（不区分大小写）")
    void fromString_shouldParsePromptCaseInsensitive() {
        assertThat(StepType.fromString("prompt")).isEqualTo(StepType.PROMPT);
        assertThat(StepType.fromString("PROMPT")).isEqualTo(StepType.PROMPT);
        assertThat(StepType.fromString("Prompt")).isEqualTo(StepType.PROMPT);
        assertThat(StepType.fromString("  prompt  ")).isEqualTo(StepType.PROMPT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("fromString 应该拒绝空值或空白字符串")
    void fromString_shouldRejectBlankValues(String value) {
        assertThatThrownBy(() -> StepType.fromString(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("技能步骤类型值不能为空");
    }

    @Test
    @DisplayName("fromString 应该拒绝 null 值")
    void fromString_shouldRejectNull() {
        assertThatThrownBy(() -> StepType.fromString(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("技能步骤类型值不能为空");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "toolx", "TOOLZ", "PROMP", "COMPOS", "xyz", "123"})
    @DisplayName("fromString 应该拒绝无效的类型字符串")
    void fromString_shouldRejectInvalidValues(String value) {
        assertThatThrownBy(() -> StepType.fromString(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未知的技能步骤类型值")
                .hasMessageContaining(value)
                .hasMessageContaining("TOOL, PROMPT, TEMPLATE, AWAIT");
    }

    @Test
    @DisplayName("fromString 错误消息应该包含所有支持的类型")
    void fromString_errorMessageShouldListAllSupportedTypes() {
        assertThatThrownBy(() -> StepType.fromString("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TOOL")
                .hasMessageContaining("PROMPT")
                .hasMessageContaining("AWAIT");
    }
}
