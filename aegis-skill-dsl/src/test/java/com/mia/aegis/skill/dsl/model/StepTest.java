package com.mia.aegis.skill.dsl.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Step 的单元测试。
 *
 * 测试覆盖：
 * - 正常创建和使用
 * - 三种类型的Step创建
 * - 状态管理
 * - 输出管理
 * - 类型检查方法
 * - 配置获取的边界情况
 */
@DisplayName("Step 测试")
class StepTest {

    @Test
    @DisplayName("应该成功创建Tool类型的Step")
    void shouldCreateToolStepSuccessfully() {
        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("param1", "value1");
        ToolStepConfig config = new ToolStepConfig("test_tool", inputTemplate);

        Step step = Step.tool("test_step", config);

        assertThat(step.getName()).isEqualTo("test_step");
        assertThat(step.getType()).isEqualTo(StepType.TOOL);
        assertThat(step.isTool()).isTrue();
        assertThat(step.isPrompt()).isFalse();
        assertThat(step.isCompose()).isFalse();
    }

    @Test
    @DisplayName("应该成功创建Prompt类型的Step")
    void shouldCreatePromptStepSuccessfully() {
        PromptStepConfig config = new PromptStepConfig("测试提示词模板");

        Step step = Step.prompt("test_step", config);

        assertThat(step.getName()).isEqualTo("test_step");
        assertThat(step.getType()).isEqualTo(StepType.PROMPT);
        assertThat(step.isPrompt()).isTrue();
        assertThat(step.isTool()).isFalse();
        assertThat(step.isCompose()).isFalse();
    }

    @Test
    @DisplayName("应该成功创建Compose类型的Step")
    void shouldCreateComposeStepSuccessfully() {
        ComposeStepConfig config = new ComposeStepConfig(java.util.Arrays.asList("step1.output"));

        Step step = Step.compose("test_step", config);

        assertThat(step.getName()).isEqualTo("test_step");
        assertThat(step.getType()).isEqualTo(StepType.COMPOSE);
        assertThat(step.isCompose()).isTrue();
        assertThat(step.isTool()).isFalse();
        assertThat(step.isPrompt()).isFalse();
    }

    @Test
    @DisplayName("Step名称前后空格应该被修剪")
    void shouldTrimStepName() {
        ToolStepConfig config = new ToolStepConfig("tool", new HashMap<String, String>());

        Step step = new Step("  test_step  ", StepType.TOOL, config);

        assertThat(step.getName()).isEqualTo("test_step");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("应该拒绝null或空的Step名称")
    void shouldRejectNullOrEmptyStepName(String name) {
        ToolStepConfig config = new ToolStepConfig("tool", new HashMap<String, String>());

        assertThatThrownBy(() -> new Step(name, StepType.TOOL, config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("步骤名称不能为空");
    }

    @Test
    @DisplayName("应该拒绝null的StepType")
    void shouldRejectNullStepType() {
        ToolStepConfig config = new ToolStepConfig("tool", new HashMap<String, String>());

        assertThatThrownBy(() -> new Step("test_step", null, config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("步骤类型不能为空");
    }

    @Test
    @DisplayName("应该拒绝null的StepConfig")
    void shouldRejectNullStepConfig() {
        assertThatThrownBy(() -> new Step("test_step", StepType.TOOL, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("步骤配置对象不能为空");
    }

    @Test
    @DisplayName("应该拒绝类型不匹配的配置")
    void shouldRejectMismatchedConfigType() {
        ToolStepConfig toolConfig = new ToolStepConfig("tool", new HashMap<String, String>());

        assertThatThrownBy(() -> new Step("test_step", StepType.PROMPT, toolConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("配置类型不匹配")
                .hasMessageContaining("PROMPT")
                .hasMessageContaining("TOOL");
    }

    @Test
    @DisplayName("初始状态应该是PENDING")
    void initialStatusShouldBePending() {
        ToolStepConfig config = new ToolStepConfig("tool", new HashMap<String, String>());
        Step step = new Step("test_step", StepType.TOOL, config);

        assertThat(step.getStatus()).isEqualTo(StepStatus.PENDING);
    }

    @Test
    @DisplayName("应该能够设置和获取状态")
    void shouldSetAndGetStatus() {
        ToolStepConfig config = new ToolStepConfig("tool", new HashMap<String, String>());
        Step step = new Step("test_step", StepType.TOOL, config);

        step.setStatus(StepStatus.RUNNING);
        assertThat(step.getStatus()).isEqualTo(StepStatus.RUNNING);

        step.setStatus(StepStatus.SUCCESS);
        assertThat(step.getStatus()).isEqualTo(StepStatus.SUCCESS);

        step.setStatus(StepStatus.FAILED);
        assertThat(step.getStatus()).isEqualTo(StepStatus.FAILED);
    }

    @Test
    @DisplayName("初始输出应该是null")
    void initialOutputShouldBeNull() {
        ToolStepConfig config = new ToolStepConfig("tool", new HashMap<String, String>());
        Step step = new Step("test_step", StepType.TOOL, config);

        assertThat(step.getOutput()).isNull();
    }

    @Test
    @DisplayName("应该能够设置和获取输出")
    void shouldSetAndGetOutput() {
        ToolStepConfig config = new ToolStepConfig("tool", new HashMap<String, String>());
        Step step = new Step("test_step", StepType.TOOL, config);

        Object output = "test output";
        step.setOutput(output);

        assertThat(step.getOutput()).isEqualTo(output);
    }

    @Test
    @DisplayName("应该能够设置null输出")
    void shouldAllowSettingNullOutput() {
        ToolStepConfig config = new ToolStepConfig("tool", new HashMap<String, String>());
        Step step = new Step("test_step", StepType.TOOL, config);

        step.setOutput("test output");
        step.setOutput(null);

        assertThat(step.getOutput()).isNull();
    }

    @Test
    @DisplayName("getToolConfig应该返回Tool配置")
    void getToolConfigShouldReturnToolConfig() {
        ToolStepConfig config = new ToolStepConfig("test_tool", new HashMap<String, String>());
        Step step = new Step("test_step", StepType.TOOL, config);

        assertThat(step.getToolConfig()).isSameAs(config);
    }

    @Test
    @DisplayName("非Tool类型Step调用getToolConfig应该抛出异常")
    void getToolConfigOnNonToolStepShouldThrowException() {
        PromptStepConfig config = new PromptStepConfig("test");
        Step step = new Step("test_step", StepType.PROMPT, config);

        assertThatThrownBy(() -> step.getToolConfig())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Step is not a TOOL type")
                .hasMessageContaining(StepType.PROMPT.toString());
    }

    @Test
    @DisplayName("getPromptConfig应该返回Prompt配置")
    void getPromptConfigShouldReturnPromptConfig() {
        PromptStepConfig config = new PromptStepConfig("test template");
        Step step = new Step("test_step", StepType.PROMPT, config);

        assertThat(step.getPromptConfig()).isSameAs(config);
    }

    @Test
    @DisplayName("非Prompt类型Step调用getPromptConfig应该抛出异常")
    void getPromptConfigOnNonPromptStepShouldThrowException() {
        ToolStepConfig config = new ToolStepConfig("tool", new HashMap<String, String>());
        Step step = new Step("test_step", StepType.TOOL, config);

        assertThatThrownBy(() -> step.getPromptConfig())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Step is not a PROMPT type")
                .hasMessageContaining(StepType.TOOL.toString());
    }

    @Test
    @DisplayName("getComposeConfig应该返回Compose配置")
    void getComposeConfigShouldReturnComposeConfig() {
        ComposeStepConfig config = new ComposeStepConfig(java.util.Arrays.asList("step1.output"));
        Step step = new Step("test_step", StepType.COMPOSE, config);

        assertThat(step.getComposeConfig()).isSameAs(config);
    }

    @Test
    @DisplayName("非Compose类型Step调用getComposeConfig应该抛出异常")
    void getComposeConfigOnNonComposeStepShouldThrowException() {
        ToolStepConfig config = new ToolStepConfig("tool", new HashMap<String, String>());
        Step step = new Step("test_step", StepType.TOOL, config);

        assertThatThrownBy(() -> step.getComposeConfig())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Step is not a COMPOSE type")
                .hasMessageContaining(StepType.TOOL.toString());
    }

    @Test
    @DisplayName("getConfig应该返回配置")
    void getConfigShouldReturnConfig() {
        ToolStepConfig config = new ToolStepConfig("tool", new HashMap<String, String>());
        Step step = new Step("test_step", StepType.TOOL, config);

        assertThat(step.getConfig()).isSameAs(config);
    }

    @Test
    @DisplayName("toString应该包含名称、类型和状态信息")
    void toStringShouldContainNameTypeAndStatus() {
        ToolStepConfig config = new ToolStepConfig("tool", new HashMap<String, String>());
        Step step = new Step("test_step", StepType.TOOL, config);

        assertThat(step.toString()).contains("test_step");
        assertThat(step.toString()).contains("TOOL");
        assertThat(step.toString()).contains("PENDING");
    }

    @Test
    @DisplayName("toString应该反映当前状态")
    void toStringShouldReflectCurrentStatus() {
        ToolStepConfig config = new ToolStepConfig("tool", new HashMap<String, String>());
        Step step = new Step("test_step", StepType.TOOL, config);

        step.setStatus(StepStatus.SUCCESS);

        assertThat(step.toString()).contains("SUCCESS");
    }

    @Test
    @DisplayName("应该支持所有类型的StepStatus")
    void shouldSupportAllStepStatusTypes() {
        ToolStepConfig config = new ToolStepConfig("tool", new HashMap<String, String>());
        Step step = new Step("test_step", StepType.TOOL, config);

        for (StepStatus status : StepStatus.values()) {
            step.setStatus(status);
            assertThat(step.getStatus()).isEqualTo(status);
        }
    }

    @Test
    @DisplayName("工厂方法tool应该正确创建Step")
    void factoryMethodToolShouldCreateStepCorrectly() {
        ToolStepConfig config = new ToolStepConfig("tool", new HashMap<String, String>());
        Step step = Step.tool("my_step", config);

        assertThat(step.getName()).isEqualTo("my_step");
        assertThat(step.getType()).isEqualTo(StepType.TOOL);
        assertThat(step.getConfig()).isSameAs(config);
    }

    @Test
    @DisplayName("工厂方法prompt应该正确创建Step")
    void factoryMethodPromptShouldCreateStepCorrectly() {
        PromptStepConfig config = new PromptStepConfig("template");
        Step step = Step.prompt("my_step", config);

        assertThat(step.getName()).isEqualTo("my_step");
        assertThat(step.getType()).isEqualTo(StepType.PROMPT);
        assertThat(step.getConfig()).isSameAs(config);
    }

    @Test
    @DisplayName("工厂方法compose应该正确创建Step")
    void factoryMethodComposeShouldCreateStepCorrectly() {
        ComposeStepConfig config = new ComposeStepConfig(java.util.Arrays.asList("step1"));
        Step step = Step.compose("my_step", config);

        assertThat(step.getName()).isEqualTo("my_step");
        assertThat(step.getType()).isEqualTo(StepType.COMPOSE);
        assertThat(step.getConfig()).isSameAs(config);
    }

    @Test
    @DisplayName("应该能够设置复杂的输出对象")
    void shouldHandleComplexOutputObject() {
        ToolStepConfig config = new ToolStepConfig("tool", new HashMap<String, String>());
        Step step = new Step("test_step", StepType.TOOL, config);

        Map<String, Object> complexOutput = new HashMap<String, Object>();
        complexOutput.put("result", "success");
        complexOutput.put("data", java.util.Arrays.asList(1, 2, 3));

        step.setOutput(complexOutput);

        assertThat(step.getOutput()).isSameAs(complexOutput);
    }
}
