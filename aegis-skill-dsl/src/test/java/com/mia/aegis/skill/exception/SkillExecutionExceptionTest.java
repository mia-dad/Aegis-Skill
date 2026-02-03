package com.mia.aegis.skill.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SkillExecutionException 的单元测试。
 *
 * 测试覆盖：
 * - 异常创建（带/不带Step名称）
 * - Step名称获取
 * - 错误消息格式
 * - 带原因的异常
 * - 完整错误消息
 */
@DisplayName("SkillExecutionException 测试")
class SkillExecutionExceptionTest {

    @Test
    @DisplayName("应该创建包含Step名称和消息的异常")
    void shouldCreateExceptionWithStepNameAndMessage() {
        SkillExecutionException exception = new SkillExecutionException("step1", "Execution failed");

        assertThat(exception.getStepName()).isEqualTo("step1");
        assertThat(exception.hasStepName()).isTrue();
        assertThat(exception.getMessage()).contains("step1");
        assertThat(exception.getMessage()).contains("Execution failed");
    }

    @Test
    @DisplayName("应该创建包含Step名称、消息和原因的异常")
    void shouldCreateExceptionWithStepNameMessageAndCause() {
        Throwable cause = new RuntimeException("Root cause");
        SkillExecutionException exception = new SkillExecutionException("step1", "Execution failed", cause);

        assertThat(exception.getStepName()).isEqualTo("step1");
        assertThat(exception.getMessage()).contains("step1");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("应该创建不带Step名称的异常")
    void shouldCreateExceptionWithoutStepName() {
        SkillExecutionException exception = new SkillExecutionException("Execution failed");

        assertThat(exception.getStepName()).isNull();
        assertThat(exception.hasStepName()).isFalse();
        assertThat(exception.getMessage()).contains("Execution failed");
        assertThat(exception.getMessage()).contains("Skill execution error");
    }

    @Test
    @DisplayName("应该创建不带Step名称但带原因的异常")
    void shouldCreateExceptionWithoutStepNameButWithCause() {
        Throwable cause = new RuntimeException("Root cause");
        SkillExecutionException exception = new SkillExecutionException("Execution failed", cause);

        assertThat(exception.getStepName()).isNull();
        assertThat(exception.hasStepName()).isFalse();
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("带Step名称的异常消息格式应该正确")
    void exceptionMessageWithStepNameFormatShouldBeCorrect() {
        SkillExecutionException exception = new SkillExecutionException("my_step", "failed to execute");

        assertThat(exception.getMessage()).isEqualTo("Skill execution error at step 'my_step': failed to execute");
    }

    @Test
    @DisplayName("不带Step名称的异常消息格式应该正确")
    void exceptionMessageWithoutStepNameFormatShouldBeCorrect() {
        SkillExecutionException exception = new SkillExecutionException("failed to execute");

        // 英文消息格式（aegis-skill-tools 基础包使用英文）
        assertThat(exception.getMessage()).isEqualTo("Skill execution error: failed to execute");
    }

    @Test
    @DisplayName("空Step名称应该返回false给hasStepName")
    void emptyStepNameShouldReturnFalseForHasStepName() {
        SkillExecutionException exception = new SkillExecutionException("", "Error");

        // 空字符串被视为有效，但hasStepName应该检查非空
        assertThat(exception.getStepName()).isEmpty();
        assertThat(exception.hasStepName()).isFalse();
    }

    @Test
    @DisplayName("getFullMessage应该包含完整的原因链")
    void getFullMessageShouldIncludeCompleteCauseChain() {
        Throwable cause1 = new IllegalStateException("Cause 1");
        Throwable cause2 = new RuntimeException("Cause 2", cause1);
        Throwable cause3 = new Exception("Cause 3", cause2);

        SkillExecutionException exception = new SkillExecutionException("step1", "Error", cause3);

        String fullMessage = exception.getFullMessage();

        assertThat(fullMessage).contains("Error");
        assertThat(fullMessage).contains("Cause 3");
        assertThat(fullMessage).contains("Cause 2");
        assertThat(fullMessage).contains("Cause 1");
        assertThat(fullMessage).contains("Caused by");
    }

    @Test
    @DisplayName("getFullMessage应该限制原因链深度")
    void getFullMessageShouldLimitCauseChainDepth() {
        // 创建超过5层的原因链
        Throwable cause = new RuntimeException("Cause 5");
        for (int i = 4; i >= 1; i--) {
            cause = new RuntimeException("Cause " + i, cause);
        }

        SkillExecutionException exception = new SkillExecutionException("step1", "Error", cause);

        String fullMessage = exception.getFullMessage();

        // 应该包含原始错误和前5个原因
        assertThat(fullMessage).contains("Error");
        assertThat(fullMessage).contains("Cause 1");
        assertThat(fullMessage).contains("Cause 5");

        // 验证有多个 "Caused by"
        int causedByCount = 0;
        int index = 0;
        while ((index = fullMessage.indexOf("Caused by", index)) != -1) {
            causedByCount++;
            index += "Caused by".length();
        }
        assertThat(causedByCount).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("没有原因时getFullMessage应该只返回主消息")
    void getFullMessageWithoutCauseShouldReturnOnlyMainMessage() {
        SkillExecutionException exception = new SkillExecutionException("step1", "Error");

        String fullMessage = exception.getFullMessage();

        assertThat(fullMessage).isEqualTo(exception.getMessage());
        assertThat(fullMessage).doesNotContain("Caused by");
    }

    @Test
    @DisplayName("应该处理中文Step名称")
    void shouldHandleChineseStepName() {
        SkillExecutionException exception = new SkillExecutionException("步骤一", "执行失败");

        assertThat(exception.getStepName()).isEqualTo("步骤一");
        assertThat(exception.getMessage()).contains("步骤一");
    }

    @Test
    @DisplayName("应该处理特殊字符的Step名称")
    void shouldHandleSpecialCharactersInStepName() {
        SkillExecutionException exception = new SkillExecutionException("step-with_special.chars", "Error");

        assertThat(exception.getStepName()).isEqualTo("step-with_special.chars");
        assertThat(exception.getMessage()).contains("step-with_special.chars");
    }

    @Test
    @DisplayName("异常应该是SkillException的子类")
    void exceptionShouldBeSubclassOfSkillException() {
        SkillExecutionException exception = new SkillExecutionException("step1", "Error");

        assertThat(exception).isInstanceOf(SkillException.class);
    }

    @Test
    @DisplayName("异常应该是RuntimeException")
    void exceptionShouldBeRuntimeException() {
        SkillExecutionException exception = new SkillExecutionException("step1", "Error");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("异常应该可以正确捕获")
    void exceptionShouldBeCatchable() {
        try {
            throw new SkillExecutionException("step1", "Error");
        } catch (SkillExecutionException e) {
            assertThat(e.getStepName()).isEqualTo("step1");
        }
    }

    @Test
    @DisplayName("异常应该可以作为SkillException捕获")
    void exceptionShouldBeCatchableAsSkillException() {
        try {
            throw new SkillExecutionException("step1", "Error");
        } catch (SkillException e) {
            assertThat(e).isInstanceOf(SkillExecutionException.class);
        }
    }

    @Test
    @DisplayName("异常应该可以作为RuntimeException捕获")
    void exceptionShouldBeCatchableAsRuntimeException() {
        try {
            throw new SkillExecutionException("step1", "Error");
        } catch (RuntimeException e) {
            assertThat(e).isInstanceOf(SkillExecutionException.class);
        }
    }

    @Test
    @DisplayName("应该处理多行错误消息")
    void shouldHandleMultiLineErrorMessage() {
        String multiLineMessage = "Line 1\nLine 2\nLine 3";
        SkillExecutionException exception = new SkillExecutionException("step1", multiLineMessage);

        assertThat(exception.getMessage()).contains(multiLineMessage);
    }

    @Test
    @DisplayName("应该处理null原因")
    void shouldHandleNullCause() {
        SkillExecutionException exception = new SkillExecutionException("step1", "Error", null);

        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("应该处理带有Unicode字符的错误消息")
    void shouldHandleUnicodeErrorMessage() {
        String message = "错误: 🚨 发生问题 🔥";
        SkillExecutionException exception = new SkillExecutionException("step1", message);

        assertThat(exception.getMessage()).contains(message);
    }

    @Test
    @DisplayName("null Step名称应该使用默认格式")
    void nullStepNameShouldUseDefaultFormat() {
        SkillExecutionException exception = new SkillExecutionException(null, "Error");

        assertThat(exception.getStepName()).isNull();
        // 英文消息格式（aegis-skill-tools 基础包使用英文）
        assertThat(exception.getMessage()).isEqualTo("Skill execution error: Error");
    }

    @Test
    @DisplayName("异常应该支持链式异常")
    void exceptionShouldSupportChainedCauses() {
        Throwable rootCause = new IllegalStateException("Root");
        Throwable intermediateCause = new RuntimeException("Intermediate", rootCause);
        SkillExecutionException exception = new SkillExecutionException("step1", "Error", intermediateCause);

        assertThat(exception.getCause()).isSameAs(intermediateCause);
        assertThat(exception.getCause().getCause()).isSameAs(rootCause);
    }

    @Test
    @DisplayName("getFullMessage应该正确格式化多行原因")
    void getFullMessageShouldCorrectlyFormatMultipleCauses() {
        Throwable cause = new RuntimeException("Cause");
        SkillExecutionException exception = new SkillExecutionException("step1", "Main error", cause);

        String fullMessage = exception.getFullMessage();

        assertThat(fullMessage).contains("Main error");
        assertThat(fullMessage).contains("\n  Caused by: Cause");
    }
}
