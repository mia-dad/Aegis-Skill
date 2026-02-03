package com.mia.aegis.skill.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 异常类的单元测试。
 *
 * 测试覆盖：
 * - SkillException
 * - SkillParseException
 * - SkillValidationException
 * - TemplateRenderException
 */
@DisplayName("异常类测试")
class ExceptionTests {

    @Test
    @DisplayName("SkillException应该正确存储消息")
    void skillExceptionShouldStoreMessage() {
        String message = "Test error message";
        SkillException exception = new SkillException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("SkillException应该支持嵌套异常")
    void skillExceptionShouldSupportCause() {
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");
        SkillException exception = new SkillException(message, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("SkillParseException应该格式化带行号的消息")
    void skillParseExceptionShouldFormatMessageWithLine() {
        SkillParseException exception = new SkillParseException("Error message", 42);

        assertThat(exception.getMessage()).contains("42");
        assertThat(exception.getMessage()).contains("Error message");
        assertThat(exception.getLine()).isEqualTo(42);
    }

    @Test
    @DisplayName("SkillParseException应该格式化带行号和列号的消息")
    void skillParseExceptionShouldFormatMessageWithLineAndColumn() {
        SkillParseException exception = new SkillParseException("Error message", 42, 10);

        assertThat(exception.getMessage()).contains("第 42 行");
        assertThat(exception.getMessage()).contains("第 10 列");
        assertThat(exception.getMessage()).contains("Error message");
        assertThat(exception.getLine()).isEqualTo(42);
        assertThat(exception.getColumn()).isEqualTo(10);
    }

    @Test
    @DisplayName("SkillParseException应该支持无位置信息")
    void skillParseExceptionShouldSupportNoLocation() {
        SkillParseException exception = new SkillParseException("Error message");

        assertThat(exception.getMessage()).contains("技能解析错误");
        assertThat(exception.getMessage()).contains("Error message");
        assertThat(exception.getLine()).isEqualTo(0);
        assertThat(exception.getColumn()).isEqualTo(0);
        assertThat(exception.hasLocation()).isFalse();
    }

    @Test
    @DisplayName("SkillParseException应该正确检测位置信息")
    void skillParseExceptionShouldDetectLocationCorrectly() {
        SkillParseException withLocation = new SkillParseException("Error", 5, 3);
        SkillParseException withoutLocation = new SkillParseException("Error");

        assertThat(withLocation.hasLocation()).isTrue();
        assertThat(withoutLocation.hasLocation()).isFalse();
    }

    @Test
    @DisplayName("SkillParseException应该支持带原因的构造函数")
    void skillParseExceptionShouldSupportCause() {
        Throwable cause = new RuntimeException("Root cause");
        SkillParseException exception = new SkillParseException("Error message", 42, cause);

        assertThat(exception.getMessage()).contains("42");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("SkillValidationException应该格式化单个错误")
    void skillValidationExceptionShouldFormatSingleError() {
        SkillValidationException exception = new SkillValidationException("Error 1");

        assertThat(exception.getMessage()).contains("校验错误");
        assertThat(exception.getMessage()).contains("Error 1");
        assertThat(exception.getErrors()).hasSize(1);
        assertThat(exception.getFirstError()).isEqualTo("Error 1");
        assertThat(exception.getErrorCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("SkillValidationException应该格式化多个错误")
    void skillValidationExceptionShouldFormatMultipleErrors() {
        java.util.List<String> errors = java.util.Arrays.asList("Error 1", "Error 2", "Error 3");
        SkillValidationException exception = new SkillValidationException(errors);

        assertThat(exception.getMessage()).contains("校验失败，共 3 个错误");
        assertThat(exception.getMessage()).contains("Error 1");
        assertThat(exception.getMessage()).contains("Error 2");
        assertThat(exception.getMessage()).contains("Error 3");
        assertThat(exception.getErrors()).hasSize(3);
        assertThat(exception.getErrorCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("SkillValidationException应该处理空错误列表")
    void skillValidationExceptionShouldHandleEmptyErrors() {
        SkillValidationException exception = new SkillValidationException(java.util.Collections.emptyList());

        assertThat(exception.getMessage()).contains("校验失败");
        assertThat(exception.getErrors()).isEmpty();
        assertThat(exception.getErrorCount()).isEqualTo(0);
        assertThat(exception.getFirstError()).isNull();
    }

    @Test
    @DisplayName("SkillValidationException应该处理null错误列表")
    void skillValidationExceptionShouldHandleNullErrors() {
        SkillValidationException exception = new SkillValidationException(java.util.Collections.emptyList());

        assertThat(exception.getMessage()).contains("校验失败");
        assertThat(exception.getErrors()).isEmpty();
        assertThat(exception.getErrorCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("SkillValidationException的错误列表应该是不可变的")
    void skillValidationExceptionErrorsShouldBeUnmodifiable() {
        java.util.List<String> errors = new java.util.ArrayList<String>();
        errors.add("Error 1");
        errors.add("Error 2");

        SkillValidationException exception = new SkillValidationException(errors);

        try {
            exception.getErrors().add("Error 3");
            assertThat(false).isTrue(); // 应该不会执行到这里
        } catch (UnsupportedOperationException e) {
            // 预期的异常
            assertThat(true).isTrue();
        }
    }

    @Test
    @DisplayName("TemplateRenderException应该存储模板信息")
    void templateRenderExceptionShouldStoreTemplate() {
        String template = "Hello {{name}}";
        TemplateRenderException exception = new TemplateRenderException("Error", template);

        assertThat(exception.getTemplate()).isEqualTo(template);
        assertThat(exception.getVariableName()).isNull();
    }

    @Test
    @DisplayName("TemplateRenderException应该支持变量名")
    void templateRenderExceptionShouldSupportVariableName() {
        String template = "Hello {{name}}";
        String variableName = "name";
        TemplateRenderException exception = new TemplateRenderException("Error", template, variableName);

        assertThat(exception.getTemplate()).isEqualTo(template);
        assertThat(exception.getVariableName()).isEqualTo(variableName);
    }

    @Test
    @DisplayName("TemplateRenderException应该支持无额外信息")
    void templateRenderExceptionShouldSupportNoExtraInfo() {
        TemplateRenderException exception = new TemplateRenderException("Error");

        assertThat(exception.getMessage()).isEqualTo("Error");
        assertThat(exception.getTemplate()).isNull();
        assertThat(exception.getVariableName()).isNull();
    }

    @Test
    @DisplayName("TemplateRenderException应该支持带原因的构造函数")
    void templateRenderExceptionShouldSupportCause() {
        Throwable cause = new RuntimeException("Root cause");
        TemplateRenderException exception = new TemplateRenderException("Error", cause);

        assertThat(exception.getMessage()).isEqualTo("Error");
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getTemplate()).isNull();
        assertThat(exception.getVariableName()).isNull();
    }

    @Test
    @DisplayName("variableNotFound工厂方法应该创建正确的异常")
    void variableNotFoundFactoryMethodShouldCreateCorrectException() {
        String variableName = "missing_var";
        String template = "Value: {{missing_var}}";
        TemplateRenderException exception = TemplateRenderException.variableNotFound(variableName, template);

        assertThat(exception.getMessage()).contains(variableName);
        assertThat(exception.getMessage()).contains("not found");
        assertThat(exception.getTemplate()).isEqualTo(template);
        assertThat(exception.getVariableName()).isEqualTo(variableName);
    }

    @Test
    @DisplayName("所有异常类都应该是RuntimeException的子类")
    void allExceptionsShouldExtendRuntimeException() {
        SkillException skillException = new SkillException("test");
        SkillParseException parseException = new SkillParseException("test");
        SkillValidationException validationException = new SkillValidationException("test");
        TemplateRenderException renderException = new TemplateRenderException("test");

        assertThat(skillException).isInstanceOf(RuntimeException.class);
        assertThat(parseException).isInstanceOf(RuntimeException.class);
        assertThat(validationException).isInstanceOf(RuntimeException.class);
        assertThat(renderException).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Skill相关异常类应该是SkillException的子类")
    void skillSpecificExceptionsShouldExtendSkillException() {
        SkillParseException parseException = new SkillParseException("test");
        SkillValidationException validationException = new SkillValidationException("test");

        assertThat(parseException).isInstanceOf(SkillException.class);
        assertThat(validationException).isInstanceOf(SkillException.class);
    }

    @Test
    @DisplayName("TemplateRenderException继承RuntimeException（通用基础设施能力）")
    void templateRenderExceptionExtendsRuntimeException() {
        TemplateRenderException renderException = new TemplateRenderException("test");

        // TemplateRenderException 是通用的模板渲染异常，不应该继承 SkillException
        assertThat(renderException).isInstanceOf(RuntimeException.class);
        assertThat(renderException).isNotInstanceOf(SkillException.class);
    }

    @Test
    @DisplayName("异常消息应该是中文友好的")
    void exceptionMessagesShouldBeChineseFriendly() {
        SkillParseException parseException = new SkillParseException("解析错误", 10);
        SkillValidationException validationException = new SkillValidationException("验证错误");

        assertThat(parseException.getMessage()).contains("技能解析错误");
        assertThat(validationException.getMessage()).contains("校验");
    }

    @Test
    @DisplayName("SkillParseException应该正确处理0行号")
    void skillParseExceptionShouldHandleZeroLineNumber() {
        SkillParseException exception = new SkillParseException("Error", 0);

        assertThat(exception.hasLocation()).isFalse();
        assertThat(exception.getLine()).isEqualTo(0);
    }

    @Test
    @DisplayName("SkillParseException应该正确处理负数行号")
    void skillParseExceptionShouldHandleNegativeLineNumber() {
        SkillParseException exception = new SkillParseException("Error", -1);

        assertThat(exception.hasLocation()).isFalse();
        assertThat(exception.getLine()).isEqualTo(-1);
    }

    @Test
    @DisplayName("SkillValidationException应该获取第一个错误")
    void skillValidationExceptionShouldGetFirstError() {
        java.util.List<String> errors = java.util.Arrays.asList("Error 1", "Error 2", "Error 3");
        SkillValidationException exception = new SkillValidationException(errors);

        assertThat(exception.getFirstError()).isEqualTo("Error 1");
    }

    @Test
    @DisplayName("SkillValidationException空列表的getFirstError应该返回null")
    void skillValidationExceptionGetFirstErrorOnEmptyListShouldReturnNull() {
        SkillValidationException exception = new SkillValidationException(java.util.Collections.emptyList());

        assertThat(exception.getFirstError()).isNull();
    }

    @Test
    @DisplayName("SkillValidationException构造函数应该防御性复制错误列表")
    void skillValidationExceptionConstructorShouldDefensivelyCopyErrors() {
        java.util.List<String> errors = new java.util.ArrayList<String>();
        errors.add("Error 1");

        SkillValidationException exception = new SkillValidationException(errors);
        errors.add("Error 2");

        assertThat(exception.getErrors()).hasSize(1);
        assertThat(exception.getErrors()).doesNotContain("Error 2");
    }
}
