package com.mia.aegis.skill.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SkillException 的单元测试。
 *
 * 测试覆盖：
 * - 异常创建
 * - 消息设置
 * - 带原因的异常
 * - 继承关系
 */
@DisplayName("SkillException 测试")
class SkillExceptionTest {

    @Test
    @DisplayName("应该创建包含消息的异常")
    void shouldCreateExceptionWithMessage() {
        SkillException exception = new SkillException("Test error message");

        assertThat(exception.getMessage()).isEqualTo("Test error message");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("应该创建包含消息和原因的异常")
    void shouldCreateExceptionWithMessageAndCause() {
        Throwable cause = new RuntimeException("Root cause");
        SkillException exception = new SkillException("Test error message", cause);

        assertThat(exception.getMessage()).isEqualTo("Test error message");
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getCause().getMessage()).isEqualTo("Root cause");
    }

    @Test
    @DisplayName("异常应该是RuntimeException的子类")
    void exceptionShouldBeSubclassOfRuntimeException() {
        SkillException exception = new SkillException("Error");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("异常应该是未检查的异常")
    void exceptionShouldBeUnchecked() {
        // RuntimeException是未检查的异常
        assertThat(RuntimeException.class.isAssignableFrom(SkillException.class)).isTrue();
    }

    @Test
    @DisplayName("应该处理空消息")
    void shouldHandleEmptyMessage() {
        SkillException exception = new SkillException("");

        assertThat(exception.getMessage()).isEmpty();
    }

    @Test
    @DisplayName("应该处理null原因")
    void shouldHandleNullCause() {
        SkillException exception = new SkillException("Error", null);

        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("应该可以正确捕获")
    void exceptionShouldBeCatchable() {
        try {
            throw new SkillException("Test error");
        } catch (SkillException e) {
            assertThat(e.getMessage()).isEqualTo("Test error");
        }
    }

    @Test
    @DisplayName("应该可以作为RuntimeException捕获")
    void exceptionShouldBeCatchableAsRuntimeException() {
        try {
            throw new SkillException("Test error");
        } catch (RuntimeException e) {
            assertThat(e).isInstanceOf(SkillException.class);
            assertThat(e.getMessage()).isEqualTo("Test error");
        }
    }

    @Test
    @DisplayName("应该处理中文错误消息")
    void shouldHandleChineseErrorMessage() {
        String message = "这是一个错误消息";
        SkillException exception = new SkillException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("应该处理包含特殊字符的消息")
    void shouldHandleMessageWithSpecialCharacters() {
        String message = "Error: @#$%^&*()";
        SkillException exception = new SkillException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("应该处理包含Unicode字符的消息")
    void shouldHandleMessageWithUnicodeCharacters() {
        String message = "Error: 🚨 发生问题 🔥";
        SkillException exception = new SkillException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("应该处理多行错误消息")
    void shouldHandleMultiLineErrorMessage() {
        String multiLineMessage = "Line 1\nLine 2\nLine 3";
        SkillException exception = new SkillException(multiLineMessage);

        assertThat(exception.getMessage()).isEqualTo(multiLineMessage);
    }

    @Test
    @DisplayName("应该保留原因异常的堆栈跟踪")
    void shouldPreserveCauseStackTrace() {
        Throwable cause = new NullPointerException("Null value");
        SkillException exception = new SkillException("Error", cause);

        assertThat(exception.getCause()).isNotNull();
        assertThat(exception.getCause().getMessage()).isEqualTo("Null value");
    }

    @Test
    @DisplayName("应该支持链式异常")
    void exceptionShouldSupportChainedCauses() {
        Throwable rootCause = new IllegalStateException("Root");
        Throwable intermediateCause = new RuntimeException("Intermediate", rootCause);
        SkillException exception = new SkillException("Error", intermediateCause);

        assertThat(exception.getCause()).isSameAs(intermediateCause);
        assertThat(exception.getCause().getCause()).isSameAs(rootCause);
    }

    @Test
    @DisplayName("应该处理很长的错误消息")
    void shouldHandleLongErrorMessage() {
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longMessage.append("error");
        }

        String message = longMessage.toString();
        SkillException exception = new SkillException(message);

        assertThat(exception.getMessage()).hasSize(5000); // 1000 * 5
    }

    @Test
    @DisplayName("子类应该继承SkillException")
    void subclassShouldInheritSkillException() {
        SkillException exception = new SkillExecutionException("step1", "Error");

        assertThat(exception).isInstanceOf(SkillException.class);
    }

    @Test
    @DisplayName("异常堆栈跟踪应该包含正确的调用信息")
    void exceptionStackTraceShouldContainCorrectCallInfo() {
        SkillException exception = new SkillException("Test error");

        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertThat(stackTrace).isNotNull();
        assertThat(stackTrace).isNotEmpty();
    }

    @Test
    @DisplayName("应该可以设置和获取原因")
    void shouldSetAndGetCause() {
        Throwable cause = new RuntimeException("Cause");
        SkillException exception = new SkillException("Error", cause);

        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("没有原因时getCause应该返回null")
    void getCauseShouldReturnNullWhenNoCause() {
        SkillException exception = new SkillException("Error");

        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("异常消息应该保持不变性")
    void exceptionMessageShouldBeImmutable() {
        SkillException exception = new SkillException("Original message");

        String message = exception.getMessage();
        assertThat(message).isEqualTo("Original message");

        // 验证多次调用getMessage返回相同的结果
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("应该处理包含换行符和制表符的消息")
    void shouldHandleMessageWithNewlinesAndTabs() {
        String message = "Line 1\n\tIndented\nLine 3";
        SkillException exception = new SkillException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
    }
}
