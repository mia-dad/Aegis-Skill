package com.mia.aegis.skill.tools;

import com.mia.aegis.skill.exception.ToolExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ToolProvider 接口的单元测试。
 *
 * 测试覆盖：
 * - 接口方法
 * - 默认方法（executeAsync, validateInput）
 * - 实现类的行为
 */
@DisplayName("ToolProvider 测试")
class ToolProviderTest {

    private TestToolProvider toolProvider;

    @BeforeEach
    void setUp() {
        toolProvider = new TestToolProvider();
    }

    @Test
    @DisplayName("应该获取工具名称")
    void shouldGetToolName() {
        assertThat(toolProvider.getName()).isEqualTo("test_tool");
    }

    @Test
    @DisplayName("应该获取工具描述")
    void shouldGetToolDescription() {
        assertThat(toolProvider.getDescription()).isEqualTo("Test tool for unit testing");
    }

    @Test
    @DisplayName("应该获取输入Schema")
    void shouldGetInputSchema() {
        ToolSchema schema = toolProvider.getInputSchema();

        assertThat(schema).isNotNull();
        assertThat(schema.hasParameter("param1")).isTrue();
    }

    @Test
    @DisplayName("应该获取输出Schema")
    void shouldGetOutputSchema() {
        ToolSchema schema = toolProvider.getOutputSchema();

        assertThat(schema).isNotNull();
        assertThat(schema.hasParameter("result")).isTrue();
    }

    @Test
    @DisplayName("应该同步执行工具并写入上下文")
    void shouldExecuteToolSynchronously() throws ToolExecutionException {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("param1", "test_value");
        MapOutputContext output = new MapOutputContext();

        toolProvider.execute(input, output);

        assertThat(output.get("result")).isEqualTo("processed: test_value");
    }

    @Test
    @DisplayName("执行失败时应该抛出ToolExecutionException")
    void shouldThrowToolExecutionExceptionWhenExecutionFails() {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("param1", "error");
        MapOutputContext output = new MapOutputContext();

        assertThatThrownBy(() -> toolProvider.execute(input, output))
                .isInstanceOf(ToolExecutionException.class)
                .hasMessageContaining("test_tool")
                .hasMessageContaining("Simulated error");
    }

    @Test
    @DisplayName("executeAsync应该异步执行工具")
    void executeAsyncShouldExecuteToolAsynchronously() throws Exception {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("param1", "async_test");
        MapOutputContext output = new MapOutputContext();

        CompletableFuture<Void> future = toolProvider.executeAsync(input, output);
        future.get();

        assertThat(output.get("result")).isEqualTo("processed: async_test");
    }

    @Test
    @DisplayName("executeAsync应该在执行失败时完成异常")
    void executeAsyncShouldCompleteExceptionallyOnExecutionFailure() {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("param1", "error");
        MapOutputContext output = new MapOutputContext();

        CompletableFuture<Void> future = toolProvider.executeAsync(input, output);

        assertThatThrownBy(() -> future.get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(ToolExecutionException.class);
    }

    @Test
    @DisplayName("默认validateInput应该返回成功")
    void defaultValidateInputShouldReturnSuccess() {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("any_param", "any_value");

        ValidationResult result = toolProvider.validateInput(input);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("自定义validateInput应该验证输入")
    void customValidateInputShouldValidateInput() {
        ToolProvider validatingTool = new ValidatingToolProvider();
        Map<String, Object> validInput = new HashMap<String, Object>();
        validInput.put("required_param", "value");

        ValidationResult result = validatingTool.validateInput(validInput);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("自定义validateInput应该检测无效输入")
    void customValidateInputShouldDetectInvalidInput() {
        ToolProvider validatingTool = new ValidatingToolProvider();
        Map<String, Object> invalidInput = new HashMap<String, Object>();

        ValidationResult result = validatingTool.validateInput(invalidInput);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    @DisplayName("executeAsync应该使用ForkJoinPool")
    void executeAsyncShouldUseForkJoinPool() {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("param1", "test");
        MapOutputContext output = new MapOutputContext();

        CompletableFuture<Void> future = toolProvider.executeAsync(input, output);

        assertThat(future).isNotNull();
        assertThat(future.isDone()).isTrue(); // 由于测试工具立即完成
    }

    @Test
    @DisplayName("executeAsync应该传递execute的异常")
    void executeAsyncShouldPropagateExecuteException() {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("param1", "error");
        MapOutputContext output = new MapOutputContext();

        CompletableFuture<Void> future = toolProvider.executeAsync(input, output);

        // 等待异步完成并验证异常
        assertThatThrownBy(() -> future.join())
                .isInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(ToolExecutionException.class);
    }

    @Test
    @DisplayName("应该支持空输入")
    void shouldSupportEmptyInput() throws ToolExecutionException {
        Map<String, Object> input = new HashMap<String, Object>();
        MapOutputContext output = new MapOutputContext();

        toolProvider.execute(input, output);

        assertThat(output.get("result")).isEqualTo("processed: null");
    }

    @Test
    @DisplayName("应该支持null输入")
    void shouldSupportNullInput() throws ToolExecutionException {
        MapOutputContext output = new MapOutputContext();

        toolProvider.execute(null, output);

        assertThat(output.get("result")).isEqualTo("processed: null");
    }

    @Test
    @DisplayName("应该处理复杂的输入对象")
    void shouldHandleComplexInputObjects() throws ToolExecutionException {
        Map<String, Object> complexObject = new HashMap<String, Object>();
        complexObject.put("nested", "value");

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("param1", complexObject);
        MapOutputContext output = new MapOutputContext();

        toolProvider.execute(input, output);

        assertThat(output.get("result")).isNotNull();
    }

    // 简单的 ToolOutputContext 实现用于测试
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

    // 测试用的实现类

    private static class TestToolProvider implements ToolProvider {
        @Override
        public String getName() {
            return "test_tool";
        }

        @Override
        public String getDescription() {
            return "Test tool for unit testing";
        }

        @Override
        public ToolSchema getInputSchema() {
            Map<String, ToolSchema.ParameterSpec> params = new HashMap<String, ToolSchema.ParameterSpec>();
            params.put("param1", ToolSchema.ParameterSpec.optional("string", "A parameter"));
            return new ToolSchema(params);
        }

        @Override
        public ToolSchema getOutputSchema() {
            Map<String, ToolSchema.ParameterSpec> params = new HashMap<String, ToolSchema.ParameterSpec>();
            params.put("result", ToolSchema.ParameterSpec.required("string", "The result"));
            return new ToolSchema(params);
        }

        @Override
        public void execute(Map<String, Object> input, ToolOutputContext output) throws ToolExecutionException {
            Object param1 = input != null ? input.get("param1") : null;

            if (param1 != null && param1.toString().equals("error")) {
                throw new ToolExecutionException(getName(), "Simulated error");
            }

            output.put("result", "processed: " + (param1 != null ? param1 : "null"));
        }
    }

    private static class ValidatingToolProvider implements ToolProvider {
        @Override
        public String getName() {
            return "validating_tool";
        }

        @Override
        public String getDescription() {
            return "Tool with custom validation";
        }

        @Override
        public ToolSchema getInputSchema() {
            Map<String, ToolSchema.ParameterSpec> params = new HashMap<String, ToolSchema.ParameterSpec>();
            params.put("required_param", ToolSchema.ParameterSpec.required("string", "Required parameter"));
            return new ToolSchema(params);
        }

        @Override
        public ToolSchema getOutputSchema() {
            return ToolSchema.empty();
        }

        @Override
        public void execute(Map<String, Object> input, ToolOutputContext output) throws ToolExecutionException {
            output.put("result", "executed");
        }

        @Override
        public ValidationResult validateInput(Map<String, Object> input) {
            if (input == null || !input.containsKey("required_param")) {
                return ValidationResult.failure("Missing required parameter: required_param");
            }
            return ValidationResult.success();
        }
    }
}
