package com.mia.aegis.skill.executor.context;

import com.mia.aegis.skill.dsl.model.StepStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ExecutionContext 的单元测试。
 *
 * 测试覆盖：
 * - 正常创建和使用
 * - 输入参数管理
 * - Step结果管理
 * - 元数据管理
 * - 变量上下文构建
 */
@DisplayName("ExecutionContext 测试")
class ExecutionContextTest {

    @Test
    @DisplayName("应该成功创建ExecutionContext")
    void shouldCreateExecutionContextSuccessfully() {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("param1", "value1");
        input.put("param2", 123);

        ExecutionContext context = new ExecutionContext(input);

        assertThat(context.getInput()).hasSize(2);
        assertThat(context.getInputValue("param1")).isEqualTo("value1");
        assertThat(context.getInputValue("param2")).isEqualTo(123);
    }

    @Test
    @DisplayName("应该成功创建空的ExecutionContext")
    void shouldCreateEmptyExecutionContext() {
        ExecutionContext context = ExecutionContext.empty();

        assertThat(context.getInput()).isNotNull();
        assertThat(context.getInput()).isEmpty();
        assertThat(context.getStepResults()).isEmpty();
        assertThat(context.getRuntime()).isNotNull();
    }

    @Test
    @DisplayName("null输入应该被转换为空Map")
    void shouldConvertNullInputToEmptyMap() {
        ExecutionContext context = new ExecutionContext(null);

        assertThat(context.getInput()).isNotNull();
        assertThat(context.getInput()).isEmpty();
    }

    @Test
    @DisplayName("输入Map应该是不可变的")
    void inputMapShouldBeUnmodifiable() {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("param1", "value1");

        ExecutionContext context = new ExecutionContext(input);

        assertThatThrownBy(() -> context.getInput().put("param2", "value2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("构造函数应该防御性复制输入Map")
    void constructorShouldDefensivelyCopyInputMap() {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("param1", "value1");

        ExecutionContext context = new ExecutionContext(input);
        input.put("param2", "value2");

        assertThat(context.getInput()).hasSize(1);
        assertThat(context.getInput()).doesNotContainKey("param2");
    }

    @Test
    @DisplayName("getInputValue应该返回参数值")
    void getInputValueShouldReturnValue() {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("param1", "value1");

        ExecutionContext context = new ExecutionContext(input);

        assertThat(context.getInputValue("param1")).isEqualTo("value1");
    }

    @Test
    @DisplayName("getInputValue带默认值应该返回默认值当参数不存在时")
    void getInputValueWithDefaultShouldReturnDefaultValueWhenParamNotExists() {
        Map<String, Object> input = new HashMap<String, Object>();

        ExecutionContext context = new ExecutionContext(input);

        assertThat(context.getInputValue("param1", "default")).isEqualTo("default");
    }

    @Test
    @DisplayName("getInputValue带默认值应该返回实际值当参数存在时")
    void getInputValueWithDefaultShouldReturnValueWhenParamExists() {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("param1", "value1");

        ExecutionContext context = new ExecutionContext(input);

        assertThat(context.getInputValue("param1", "default")).isEqualTo("value1");
    }

    @Test
    @DisplayName("应该能够添加和获取Step结果")
    void shouldAddAndGetStepResults() {
        ExecutionContext context = new ExecutionContext(new HashMap<String, Object>());
        StepResult result = StepResult.success("step1", "output1", 100);

        context.addStepResult(result);

        assertThat(context.getStepResults()).hasSize(1);
        assertThat(context.getStepResult("step1")).isSameAs(result);
        assertThat(context.hasStepResult("step1")).isTrue();
    }

    @Test
    @DisplayName("Step结果Map应该是不可变的")
    void stepResultsMapShouldBeUnmodifiable() {
        ExecutionContext context = new ExecutionContext(new HashMap<String, Object>());
        context.addStepResult(StepResult.success("step1", "output1", 100));

        assertThatThrownBy(() -> context.getStepResults().put("step2", null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("getStepResult对于不存在的Step应该返回null")
    void getStepResultShouldReturnNullForUnknownStep() {
        ExecutionContext context = new ExecutionContext(new HashMap<String, Object>());

        assertThat(context.getStepResult("unknown")).isNull();
    }

    @Test
    @DisplayName("getStepOutput应该返回成功Step的输出")
    void getStepOutputShouldReturnOutputOfSuccessfulStep() {
        ExecutionContext context = new ExecutionContext(new HashMap<String, Object>());
        StepResult result = StepResult.success("step1", "output1", 100);

        context.addStepResult(result);

        assertThat(context.getStepOutput("step1")).isEqualTo("output1");
    }

    @Test
    @DisplayName("getStepOutput对于失败的Step应该返回null")
    void getStepOutputShouldReturnNullForFailedStep() {
        ExecutionContext context = new ExecutionContext(new HashMap<String, Object>());
        StepResult result = StepResult.failed("step1", "error", 100);

        context.addStepResult(result);

        assertThat(context.getStepOutput("step1")).isNull();
    }

    @Test
    @DisplayName("getStepOutput对于不存在的Step应该返回null")
    void getStepOutputShouldReturnNullForUnknownStep() {
        ExecutionContext context = new ExecutionContext(new HashMap<String, Object>());

        assertThat(context.getStepOutput("unknown")).isNull();
    }

    @Test
    @DisplayName("应该能够设置和获取元数据")
    void shouldSetAndGetMetadata() {
        ExecutionContext context = new ExecutionContext(new HashMap<String, Object>());

        context.setMetadata("key1", "value1");
        context.setMetadata("key2", 123);

        assertThat(context.getMetadata().get("key1")).isEqualTo("value1");
        assertThat(context.getMetadata().get("key2")).isEqualTo(123);
    }

    @Test
    @DisplayName("元数据Map应该是不可变的")
    void metadataMapShouldBeUnmodifiable() {
        ExecutionContext context = new ExecutionContext(new HashMap<String, Object>());

        assertThatThrownBy(() -> context.getMetadata().put("key", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("getRuntime应该返回RuntimeInfo")
    void getRuntimeShouldReturnRuntimeInfo() {
        ExecutionContext context = new ExecutionContext(new HashMap<String, Object>());

        assertThat(context.getRuntime()).isNotNull();
        assertThat(context.getRuntime().getStartTime()).isGreaterThan(0);
    }

    @Test
    @DisplayName("buildVariableContext应该包含输入参数")
    void buildVariableContextShouldContainInputParameters() {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("param1", "value1");
        input.put("param2", 123);

        ExecutionContext context = new ExecutionContext(input);
        Map<String, Object> variableContext = context.buildVariableContext();

        assertThat(variableContext).containsEntry("param1", "value1");
        assertThat(variableContext).containsEntry("param2", 123);
    }

    @Test
    @DisplayName("buildVariableContext应该包含成功Step的输出")
    void buildVariableContextShouldContainSuccessfulStepOutputs() {
        ExecutionContext context = new ExecutionContext(new HashMap<String, Object>());
        context.addStepResult(StepResult.success("step1", "output1", 100));

        Map<String, Object> variableContext = context.buildVariableContext();

        assertThat(variableContext).containsKey("step1");
        assertThat(variableContext.get("step1")).isInstanceOf(ExecutionContext.StepOutputWrapper.class);
    }

    @Test
    @DisplayName("buildVariableContext应该不包含失败Step的输出")
    void buildVariableContextShouldNotContainFailedStepOutputs() {
        ExecutionContext context = new ExecutionContext(new HashMap<String, Object>());
        context.addStepResult(StepResult.failed("step1", "error", 100));

        Map<String, Object> variableContext = context.buildVariableContext();

        assertThat(variableContext).doesNotContainKey("step1");
    }

    @Test
    @DisplayName("buildVariableContext应该包含context命名空间")
    void buildVariableContextShouldContainContextNamespace() {
        ExecutionContext context = new ExecutionContext(new HashMap<String, Object>());
        context.setMetadata("customKey", "customValue");

        Map<String, Object> variableContext = context.buildVariableContext();

        assertThat(variableContext).containsKey("context");
        Object contextObj = variableContext.get("context");
        assertThat(contextObj).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> contextMap = (Map<String, Object>) contextObj;
        assertThat(contextMap).containsKey("startTime");
        assertThat(contextMap).containsKey("elapsed");
        assertThat(contextMap).containsEntry("customKey", "customValue");
    }

    @Test
    @DisplayName("toString应该包含输入键和完成步骤数")
    void toStringShouldContainInputKeysAndCompletedSteps() {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("param1", "value1");
        input.put("param2", "value2");

        ExecutionContext context = new ExecutionContext(input);
        context.addStepResult(StepResult.success("step1", "output", 100));

        assertThat(context.toString()).contains("param1");
        assertThat(context.toString()).contains("param2");
        assertThat(context.toString()).contains("1");
    }

    @Test
    @DisplayName("应该能够处理复杂的输出对象")
    void shouldHandleComplexOutputObjects() {
        ExecutionContext context = new ExecutionContext(new HashMap<String, Object>());

        Map<String, Object> complexOutput = new HashMap<String, Object>();
        complexOutput.put("result", "success");
        complexOutput.put("data", java.util.Arrays.asList(1, 2, 3));

        context.addStepResult(StepResult.success("step1", complexOutput, 100));

        assertThat(context.getStepOutput("step1")).isSameAs(complexOutput);
    }

    @Test
    @DisplayName("应该能够处理多个Step结果")
    void shouldHandleMultipleStepResults() {
        ExecutionContext context = new ExecutionContext(new HashMap<String, Object>());

        context.addStepResult(StepResult.success("step1", "output1", 100));
        context.addStepResult(StepResult.success("step2", "output2", 200));
        context.addStepResult(StepResult.failed("step3", "error", 50));

        assertThat(context.getStepResults()).hasSize(3);
        assertThat(context.getStepResult("step1").isSuccess()).isTrue();
        assertThat(context.getStepResult("step2").isSuccess()).isTrue();
        assertThat(context.getStepResult("step3").isFailed()).isTrue();
    }

    @Test
    @DisplayName("StepOutputWrapper应该正确包装输出")
    void stepOutputWrapperShouldWrapOutputCorrectly() {
        ExecutionContext context = new ExecutionContext(new HashMap<String, Object>());
        context.addStepResult(StepResult.success("step1", "test output", 100));

        Map<String, Object> variableContext = context.buildVariableContext();
        ExecutionContext.StepOutputWrapper wrapper =
                (ExecutionContext.StepOutputWrapper) variableContext.get("step1");

        assertThat(wrapper.getOutput()).isEqualTo("test output");
        assertThat(wrapper.toString()).isEqualTo("test output");
    }

    @Test
    @DisplayName("StepOutputWrapper应该处理null输出")
    void stepOutputWrapperShouldHandleNullOutput() {
        ExecutionContext.StepOutputWrapper wrapper =
                new ExecutionContext.StepOutputWrapper(null);

        assertThat(wrapper.getOutput()).isNull();
        assertThat(wrapper.toString()).isEmpty();
    }

    @Test
    @DisplayName("hasStepResult应该正确检查Step结果是否存在")
    void hasStepResultShouldCheckStepResultExistence() {
        ExecutionContext context = new ExecutionContext(new HashMap<String, Object>());

        assertThat(context.hasStepResult("step1")).isFalse();

        context.addStepResult(StepResult.success("step1", "output", 100));

        assertThat(context.hasStepResult("step1")).isTrue();
    }

    @Test
    @DisplayName("应该能够处理null输入值")
    void shouldHandleNullInputValues() {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("param1", null);

        ExecutionContext context = new ExecutionContext(input);

        assertThat(context.getInputValue("param1")).isNull();
        assertThat(context.getInputValue("param2", "default")).isEqualTo("default");
    }
}
