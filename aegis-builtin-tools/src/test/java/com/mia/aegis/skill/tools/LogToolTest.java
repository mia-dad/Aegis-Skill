package com.mia.aegis.skill.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogTool 单元测试。
 */
public class LogToolTest {

    private LogTool logTool;

    @BeforeEach
    void setUp() {
        // 使用缓存模式以便测试
        logTool = new LogTool(true);
    }

    /** 简单的 ToolOutputContext 实现用于测试 */
    private static class MapOutputContext implements ToolOutputContext {
        private final Map<String, Object> data = new HashMap<String, Object>();

        @Override
        public void put(String key, Object value) {
            data.put(key, value);
        }

        public Object get(String key) {
            return data.get(key);
        }
    }

    @Test
    void testExecuteWithInfoLevel() {
        // Given
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("level", "info");
        input.put("message", "Test log message");
        MapOutputContext output = new MapOutputContext();

        // When
        logTool.execute(input, output);

        // Then
        assertThat(output.get("logged")).isEqualTo(true);
        assertThat(output.get("level")).isEqualTo("info");
        assertThat(output.get("message")).isEqualTo("Test log message");
        assertThat(output.get("timestamp")).isNotNull();

        // 验证日志缓存
        assertThat(logTool.getLogBuffer()).hasSize(1);
        LogTool.LogEntry entry = logTool.getLogBuffer().get(0);
        assertThat(entry.level).isEqualTo("info");
        assertThat(entry.message).isEqualTo("Test log message");
    }

    @Test
    void testExecuteWithData() {
        // Given
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("level", "debug");
        input.put("message", "Debug message");
        input.put("data", 12345);
        MapOutputContext output = new MapOutputContext();

        // When
        logTool.execute(input, output);

        // Then
        assertThat(output.get("data")).isEqualTo("12345");
    }

    @Test
    void testValidateInput_Success() {
        // Given
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("level", "info");
        input.put("message", "Test");

        // When
        ValidationResult result = logTool.validateInput(input);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testValidateInput_MissingLevel_DefaultsToInfo() {
        // Given - level 未提供，应默认为 info
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("message", "Test");

        // When
        ValidationResult result = logTool.validateInput(input);

        // Then - 验证通过（level 默认 info）
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testExecute_MissingLevel_DefaultsToInfo() {
        // Given - level 未提供
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("message", "Default level test");
        MapOutputContext output = new MapOutputContext();

        // When
        logTool.execute(input, output);

        // Then - 默认使用 info 级别
        assertThat(output.get("level")).isEqualTo("info");
        assertThat(output.get("message")).isEqualTo("Default level test");
    }

    @Test
    void testValidateInput_InvalidLevel() {
        // Given
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("level", "invalid");
        input.put("message", "Test");

        // When
        ValidationResult result = logTool.validateInput(input);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("level must be one of");
    }

    @Test
    void testClearLogBuffer() {
        // Given
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("level", "info");
        input.put("message", "Test");
        MapOutputContext output = new MapOutputContext();

        // When
        logTool.execute(input, output);
        assertThat(logTool.getLogBuffer()).hasSize(1);

        logTool.clearLogBuffer();

        // Then
        assertThat(logTool.getLogBuffer()).isEmpty();
    }
}
