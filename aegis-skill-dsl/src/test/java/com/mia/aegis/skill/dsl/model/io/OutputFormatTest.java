package com.mia.aegis.skill.dsl.model.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OutputFormat 枚举的单元测试。
 *
 * 测试覆盖：
 * - 枚举值的存在性和正确性
 * - fromString 方法的正常情况
 * - fromString 方法的边界情况（空值、空字符串、大小写、无效值）
 * - 默认行为
 */
@DisplayName("OutputFormat 枚举测试")
class OutputFormatTest {

    @Test
    @DisplayName("应该包含所有预期的枚举值")
    void shouldContainAllExpectedValues() {
        assertThat(OutputFormat.values())
                .hasSize(2)
                .containsExactly(OutputFormat.JSON, OutputFormat.TEXT);
    }

    @Test
    @DisplayName("fromString 应该正确解析 JSON（不区分大小写）")
    void fromString_shouldParseJsonCaseInsensitive() {
        assertThat(OutputFormat.fromString("json")).isEqualTo(OutputFormat.JSON);
        assertThat(OutputFormat.fromString("JSON")).isEqualTo(OutputFormat.JSON);
        assertThat(OutputFormat.fromString("Json")).isEqualTo(OutputFormat.JSON);
        assertThat(OutputFormat.fromString("  json  ")).isEqualTo(OutputFormat.JSON);
    }

    @Test
    @DisplayName("fromString 应该正确解析 TEXT（不区分大小写）")
    void fromString_shouldParseTextCaseInsensitive() {
        assertThat(OutputFormat.fromString("text")).isEqualTo(OutputFormat.TEXT);
        assertThat(OutputFormat.fromString("TEXT")).isEqualTo(OutputFormat.TEXT);
        assertThat(OutputFormat.fromString("Text")).isEqualTo(OutputFormat.TEXT);
        assertThat(OutputFormat.fromString("  text  ")).isEqualTo(OutputFormat.TEXT);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("fromString 应该默认返回 JSON（空值或空白字符串）")
    void fromString_shouldDefaultToJsonForBlankValues(String value) {
        assertThat(OutputFormat.fromString(value)).isEqualTo(OutputFormat.JSON);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "xml", "yaml", "csv", "html", "xyz", "123"})
    @DisplayName("fromString 应该默认返回 JSON（无效字符串）")
    void fromString_shouldDefaultToJsonForInvalidValues(String value) {
        assertThat(OutputFormat.fromString(value)).isEqualTo(OutputFormat.JSON);
    }

    @Test
    @DisplayName("fromString 应该对无效值不抛出异常")
    void fromString_shouldNotThrowExceptionForInvalidValues() {
        assertThat(OutputFormat.fromString("invalid")).isEqualTo(OutputFormat.JSON);
    }

    @Test
    @DisplayName("fromString 默认行为应该是返回 JSON")
    void fromString_defaultBehaviorShouldReturnJson() {
        assertThat(OutputFormat.fromString(null)).isEqualTo(OutputFormat.JSON);
        assertThat(OutputFormat.fromString("")).isEqualTo(OutputFormat.JSON);
        assertThat(OutputFormat.fromString("invalid")).isEqualTo(OutputFormat.JSON);
    }

    @Test
    @DisplayName("fromString 应该处理混合大小写")
    void fromString_shouldHandleMixedCase() {
        assertThat(OutputFormat.fromString("JsOn")).isEqualTo(OutputFormat.JSON);
        assertThat(OutputFormat.fromString("TeXt")).isEqualTo(OutputFormat.TEXT);
        assertThat(OutputFormat.fromString("jSoN")).isEqualTo(OutputFormat.JSON);
        assertThat(OutputFormat.fromString("tExT")).isEqualTo(OutputFormat.TEXT);
    }

    @Test
    @DisplayName("fromString 应该处理前后空格")
    void fromString_shouldHandleLeadingAndTrailingSpaces() {
        assertThat(OutputFormat.fromString("  json  ")).isEqualTo(OutputFormat.JSON);
        assertThat(OutputFormat.fromString("\ttext\t")).isEqualTo(OutputFormat.TEXT);
        assertThat(OutputFormat.fromString("\n JSON \n")).isEqualTo(OutputFormat.JSON);
    }
}
