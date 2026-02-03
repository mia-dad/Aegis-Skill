package com.mia.aegis.skill.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ToolExecutionException 的单元测试。
 *
 * 测试覆盖：
 * - 异常创建
 * - Tool名称获取
 * - 错误消息格式
 * - 带原因的异常
 */
@DisplayName("ToolExecutionException 测试")
class ToolExecutionExceptionTest {

    @Test
    @DisplayName("应该创建包含工具名称和消息的异常")
    void shouldCreateExceptionWithToolNameAndMessage() {
        ToolExecutionException exception = new ToolExecutionException("test_tool", "Execution failed");

        assertThat(exception.getToolName()).isEqualTo("test_tool");
        assertThat(exception.getMessage()).contains("test_tool");
        assertThat(exception.getMessage()).contains("Execution failed");
    }

    @Test
    @DisplayName("应该创建包含工具名称、消息和原因的异常")
    void shouldCreateExceptionWithToolNameMessageAndCause() {
        Throwable cause = new RuntimeException("Root cause");
        ToolExecutionException exception = new ToolExecutionException("test_tool", "Execution failed", cause);

        assertThat(exception.getToolName()).isEqualTo("test_tool");
        assertThat(exception.getMessage()).contains("test_tool");
        assertThat(exception.getMessage()).contains("Execution failed");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("异常消息格式应该正确")
    void exceptionMessageFormatShouldBeCorrect() {
        ToolExecutionException exception = new ToolExecutionException("my_tool", "failed to execute");

        // 英文消息格式（aegis-skill-tools 基础包使用英文）
        assertThat(exception.getMessage()).isEqualTo("Tool 'my_tool' execution error: failed to execute");
    }

    @Test
    @DisplayName("应该处理特殊字符的工具名称")
    void shouldHandleSpecialCharactersInToolName() {
        ToolExecutionException exception = new ToolExecutionException("tool-with_special.chars", "Error");

        assertThat(exception.getToolName()).isEqualTo("tool-with_special.chars");
        assertThat(exception.getMessage()).contains("tool-with_special.chars");
    }

    @Test
    @DisplayName("应该处理中文工具名称")
    void shouldHandleChineseToolName() {
        ToolExecutionException exception = new ToolExecutionException("数据分析工具", "执行失败");

        assertThat(exception.getToolName()).isEqualTo("数据分析工具");
        assertThat(exception.getMessage()).contains("数据分析工具");
    }

    @Test
    @DisplayName("应该处理空错误消息")
    void shouldHandleEmptyErrorMessage() {
        ToolExecutionException exception = new ToolExecutionException("tool", "");

        assertThat(exception.getMessage()).contains("tool");
    }

    @Test
    @DisplayName("应该处理多行错误消息")
    void shouldHandleMultiLineErrorMessage() {
        String multiLineMessage = "Line 1\nLine 2\nLine 3";
        ToolExecutionException exception = new ToolExecutionException("tool", multiLineMessage);

        assertThat(exception.getMessage()).contains(multiLineMessage);
    }

    @Test
    @DisplayName("应该保留原因异常的堆栈")
    void shouldPreserveCauseStackTrace() {
        Throwable cause = new NullPointerException("Null value");
        ToolExecutionException exception = new ToolExecutionException("tool", "Error", cause);

        assertThat(exception.getCause()).isNotNull();
        assertThat(exception.getCause().getMessage()).isEqualTo("Null value");
    }

    @Test
    @DisplayName("应该处理null原因")
    void shouldHandleNullCause() {
        ToolExecutionException exception = new ToolExecutionException("tool", "Error", null);

        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("异常应该是RuntimeException")
    void exceptionShouldBeRuntimeException() {
        ToolExecutionException exception = new ToolExecutionException("tool", "Error");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("异常应该是未检查的异常")
    void exceptionShouldBeUnchecked() {
        // 不需要 try-catch 块来验证这是一个未检查的异常
        @SuppressWarnings("unused")
        Class<ToolExecutionException> exceptionClass = ToolExecutionException.class;
        assertThat(RuntimeException.class.isAssignableFrom(exceptionClass)).isTrue();
    }

    @Test
    @DisplayName("应该处理很长的错误消息")
    void shouldHandleLongErrorMessage() {
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longMessage.append("error");
        }

        String message = longMessage.toString();
        ToolExecutionException exception = new ToolExecutionException("tool", message);

        assertThat(exception.getMessage()).contains(message);
    }

    @Test
    @DisplayName("异常应该可以正确捕获")
    void exceptionShouldBeCatchable() {
        try {
            throw new ToolExecutionException("tool", "Error");
        } catch (ToolExecutionException e) {
            assertThat(e.getToolName()).isEqualTo("tool");
        }
    }

    @Test
    @DisplayName("异常应该可以作为RuntimeException捕获")
    void exceptionShouldBeCatchableAsRuntimeException() {
        try {
            throw new ToolExecutionException("tool", "Error");
        } catch (RuntimeException e) {
            assertThat(e).isInstanceOf(ToolExecutionException.class);
        }
    }

    @Test
    @DisplayName("异常应该保持工具名称的不可变性")
    void toolNameShouldBeImmutable() {
        ToolExecutionException exception = new ToolExecutionException("tool", "Error");

        String toolName = exception.getToolName();
        assertThat(toolName).isEqualTo("tool");

        // 验证获取的名称不会改变
        assertThat(exception.getToolName()).isSameAs(toolName);
    }

    @Test
    @DisplayName("应该处理带有Unicode字符的错误消息")
    void shouldHandleUnicodeErrorMessage() {
        String message = "错误: 🚨 发生问题 🔥";
        ToolExecutionException exception = new ToolExecutionException("tool", message);

        assertThat(exception.getMessage()).contains(message);
    }

    @Test
    @DisplayName("异常应该支持链式异常")
    void exceptionShouldSupportChainedCauses() {
        Throwable rootCause = new IllegalStateException("Root");
        Throwable intermediateCause = new RuntimeException("Intermediate", rootCause);
        ToolExecutionException exception = new ToolExecutionException("tool", "Error", intermediateCause);

        assertThat(exception.getCause()).isSameAs(intermediateCause);
        assertThat(exception.getCause().getCause()).isSameAs(rootCause);
    }
}
