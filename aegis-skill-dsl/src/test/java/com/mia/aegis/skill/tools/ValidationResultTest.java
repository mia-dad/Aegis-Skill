package com.mia.aegis.skill.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ValidationResult 的单元测试。
 *
 * 测试覆盖：
 * - 成功和失败的创建
 * - 错误列表获取
 * - 错误信息格式化
 * - 边界情况
 */
@DisplayName("ValidationResult 测试")
class ValidationResultTest {

    @Test
    @DisplayName("应该创建成功的验证结果")
    void shouldCreateSuccessValidationResult() {
        ValidationResult result = ValidationResult.success();

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getFirstError()).isNull();
        assertThat(result.getErrorMessage()).isEmpty();
    }

    @Test
    @DisplayName("应该创建包含单个错误的失败结果")
    void shouldCreateFailureResultWithSingleError() {
        ValidationResult result = ValidationResult.failure("Error 1");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors()).containsExactly("Error 1");
        assertThat(result.getFirstError()).isEqualTo("Error 1");
        assertThat(result.getErrorMessage()).isEqualTo("Error 1");
    }

    @Test
    @DisplayName("应该创建包含多个错误的失败结果")
    void shouldCreateFailureResultWithMultipleErrors() {
        List<String> errors = Arrays.asList("Error 1", "Error 2", "Error 3");
        ValidationResult result = ValidationResult.failure(errors);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(3);
        assertThat(result.getErrors()).containsExactly("Error 1", "Error 2", "Error 3");
        assertThat(result.getFirstError()).isEqualTo("Error 1");
        assertThat(result.getErrorMessage()).isEqualTo("Error 1; Error 2; Error 3");
    }

    @Test
    @DisplayName("应该返回不可修改的错误列表")
    void shouldReturnUnmodifiableErrorList() {
        ValidationResult result = ValidationResult.failure("Error 1");

        assertThatThrownBy(() -> result.getErrors().add("Error 2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("创建失败结果时传入null应该返回空列表")
    void shouldReturnEmptyListWhenNullPassedToFailure() {
        ValidationResult result = ValidationResult.failure((List<String>) null);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("toString应该正确表示成功的结果")
    void toStringShouldRepresentSuccessResultCorrectly() {
        ValidationResult result = ValidationResult.success();

        assertThat(result.toString()).isEqualTo("ValidationResult{valid=true}");
    }

    @Test
    @DisplayName("toString应该正确表示失败的结果")
    void toStringShouldRepresentFailureResultCorrectly() {
        ValidationResult result = ValidationResult.failure("Error message");

        assertThat(result.toString()).isEqualTo("ValidationResult{valid=false, errors=[Error message]}");
    }

    @Test
    @DisplayName("应该正确格式化多个错误信息")
    void shouldFormatMultipleErrorsCorrectly() {
        List<String> errors = Arrays.asList("First error", "Second error", "Third error");
        ValidationResult result = ValidationResult.failure(errors);

        assertThat(result.getErrorMessage()).isEqualTo("First error; Second error; Third error");
    }

    @Test
    @DisplayName("空错误列表应该返回空错误信息")
    void emptyErrorListShouldReturnEmptyErrorMessage() {
        ValidationResult result = ValidationResult.failure(Arrays.<String>asList());

        assertThat(result.getErrorMessage()).isEmpty();
    }

    @Test
    @DisplayName("单个错误应该返回该错误作为错误信息")
    void singleErrorShouldReturnThatErrorAsErrorMessage() {
        ValidationResult result = ValidationResult.failure("Single error");

        assertThat(result.getErrorMessage()).isEqualTo("Single error");
    }

    @Test
    @DisplayName("getFirstError在空错误列表应该返回null")
    void getFirstErrorShouldReturnNullForEmptyErrorList() {
        ValidationResult result = ValidationResult.failure(Arrays.<String>asList());

        assertThat(result.getFirstError()).isNull();
    }

    @Test
    @DisplayName("getFirstError应该返回第一个错误")
    void getFirstErrorShouldReturnFirstError() {
        List<String> errors = Arrays.asList("First", "Second", "Third");
        ValidationResult result = ValidationResult.failure(errors);

        assertThat(result.getFirstError()).isEqualTo("First");
    }

    @Test
    @DisplayName("应该处理包含特殊字符的错误信息")
    void shouldHandleErrorsWithSpecialCharacters() {
        String error = "Error: 特殊字符 !@#$%^&*()";
        ValidationResult result = ValidationResult.failure(error);

        assertThat(result.getErrorMessage()).isEqualTo(error);
    }

    @Test
    @DisplayName("应该处理空字符串错误")
    void shouldHandleEmptyStringError() {
        ValidationResult result = ValidationResult.failure("");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).containsExactly("");
        assertThat(result.getErrorMessage()).isEmpty();
    }

    @Test
    @DisplayName("应该处理多个空字符串错误")
    void shouldHandleMultipleEmptyStringErrors() {
        List<String> errors = Arrays.asList("", "", "");
        ValidationResult result = ValidationResult.failure(errors);

        // 三个空字符串产生两个分隔符 "; ; "
        assertThat(result.getErrorMessage()).isEqualTo("; ; ");
    }

    @Test
    @DisplayName("应该正确处理Unicode错误信息")
    void shouldHandleUnicodeErrorMessage() {
        List<String> errors = Arrays.asList("错误一", "错误二", "错误三");
        ValidationResult result = ValidationResult.failure(errors);

        assertThat(result.getErrorMessage()).isEqualTo("错误一; 错误二; 错误三");
    }

    @Test
    @DisplayName("错误列表应该被防御性复制")
    void errorListShouldBeDefensivelyCopied() {
        List<String> originalErrors = Arrays.asList("Error 1", "Error 2");
        ValidationResult result = ValidationResult.failure(originalErrors);

        // 修改原始列表不应该影响 ValidationResult
        originalErrors.set(0, "Modified error");

        assertThat(result.getErrors()).containsExactly("Error 1", "Error 2");
    }

    @Test
    @DisplayName("多个连续错误应该正确分隔")
    void multipleConsecutiveErrorsShouldBeCorrectlySeparated() {
        List<String> errors = Arrays.asList("A", "B", "C", "D", "E");
        ValidationResult result = ValidationResult.failure(errors);

        assertThat(result.getErrorMessage()).isEqualTo("A; B; C; D; E");
    }

    @Test
    @DisplayName("应该处理长错误信息")
    void shouldHandleLongErrorMessages() {
        StringBuilder longError = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longError.append("word");
            if (i < 999) longError.append(" ");
        }

        ValidationResult result = ValidationResult.failure(longError.toString());

        // 1000 * 4 (word) + 999 (spaces) = 4999
        assertThat(result.getErrorMessage()).hasSize(4999);
        assertThat(result.getErrorMessage()).startsWith("word word");
        assertThat(result.getErrorMessage()).endsWith("word");
    }

    @Test
    @DisplayName("null错误应该转换为空列表")
    void nullErrorsShouldConvertToEmptyList() {
        ValidationResult result = ValidationResult.failure((List<String>) null);

        assertThat(result.getErrors()).isNotNull();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("成功结果的isValid应该总是返回true")
    void successResultIsValidShouldAlwaysReturnTrue() {
        ValidationResult result = ValidationResult.success();

        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("失败结果的isValid应该总是返回false")
    void failureResultIsValidShouldAlwaysReturnFalse() {
        ValidationResult result = ValidationResult.failure("Some error");

        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("单个错误工厂方法应该创建包含一个错误的列表")
    void singleErrorFactoryMethodShouldCreateListWithOneError() {
        ValidationResult result = ValidationResult.failure("Single error");

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).isEqualTo("Single error");
    }
}
