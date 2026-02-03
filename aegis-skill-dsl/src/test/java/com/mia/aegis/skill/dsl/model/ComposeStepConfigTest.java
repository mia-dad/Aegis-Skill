package com.mia.aegis.skill.dsl.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ComposeStepConfig 的单元测试。
 *
 * 测试覆盖：
 * - 正常创建和使用
 * - 空值验证
 * - 数据源列表处理
 * - 不可变性验证
 */
@DisplayName("ComposeStepConfig 测试")
class ComposeStepConfigTest {

    @Test
    @DisplayName("应该成功创建ComposeStepConfig")
    void shouldCreateComposeStepConfigSuccessfully() {
        List<String> sources = Arrays.asList("step1.output", "step2.output", "step3");

        ComposeStepConfig config = new ComposeStepConfig(sources);

        assertThat(config.getSources()).hasSize(3);
        assertThat(config.getSources()).containsExactly("step1.output", "step2.output", "step3");
        assertThat(config.getStepType()).isEqualTo(StepType.COMPOSE);
    }

    @Test
    @DisplayName("应该拒绝null数据源列表")
    void shouldRejectNullSources() {
        assertThatThrownBy(() -> new ComposeStepConfig(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("技能进行合并其源数据引用不能为空");
    }

    @Test
    @DisplayName("应该拒绝空数据源列表")
    void shouldRejectEmptySources() {
        assertThatThrownBy(() -> new ComposeStepConfig(new ArrayList<String>()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("技能进行合并其源数据引用不能为空");
    }

    @Test
    @DisplayName("数据源列表应该是不可变的")
    void sourcesListShouldBeUnmodifiable() {
        List<String> sources = Arrays.asList("step1.output");

        ComposeStepConfig config = new ComposeStepConfig(sources);

        assertThatThrownBy(() -> config.getSources().add("step2.output"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("构造函数应该防御性复制数据源列表")
    void constructorShouldDefensivelyCopySources() {
        List<String> sources = new ArrayList<String>();
        sources.add("step1.output");

        ComposeStepConfig config = new ComposeStepConfig(sources);
        sources.add("step2.output");

        assertThat(config.getSources()).hasSize(1);
        assertThat(config.getSources()).doesNotContain("step2.output");
    }

    @Test
    @DisplayName("getStepType应该返回COMPOSE")
    void getStepTypeShouldReturnCompose() {
        List<String> sources = Arrays.asList("step1.output");

        ComposeStepConfig config = new ComposeStepConfig(sources);

        assertThat(config.getStepType()).isEqualTo(StepType.COMPOSE);
    }

    @Test
    @DisplayName("toString应该包含数据源列表信息")
    void toStringShouldContainSourcesInfo() {
        List<String> sources = Arrays.asList("step1.output", "step2.output");

        ComposeStepConfig config = new ComposeStepConfig(sources);

        assertThat(config.toString()).contains("step1.output");
        assertThat(config.toString()).contains("step2.output");
    }

    @Test
    @DisplayName("应该处理单个数据源")
    void shouldHandleSingleSource() {
        List<String> sources = Arrays.asList("step1.output");

        ComposeStepConfig config = new ComposeStepConfig(sources);

        assertThat(config.getSources()).hasSize(1);
        assertThat(config.getSources().get(0)).isEqualTo("step1.output");
    }

    @Test
    @DisplayName("应该处理多个数据源")
    void shouldHandleMultipleSources() {
        List<String> sources = Arrays.asList(
                "step1.output",
                "step2.output",
                "step3.output",
                "step4",
                "step5.data"
        );

        ComposeStepConfig config = new ComposeStepConfig(sources);

        assertThat(config.getSources()).hasSize(5);
    }

    @Test
    @DisplayName("应该处理带.output后缀的数据源引用")
    void shouldHandleOutputSuffixSources() {
        List<String> sources = Arrays.asList("step1.output", "step2.output");

        ComposeStepConfig config = new ComposeStepConfig(sources);

        assertThat(config.getSources()).allMatch(source -> source.endsWith(".output"));
    }

    @Test
    @DisplayName("应该处理不带.output后缀的数据源引用")
    void shouldHandleSourcesWithoutOutputSuffix() {
        List<String> sources = Arrays.asList("step1", "step2", "step3");

        ComposeStepConfig config = new ComposeStepConfig(sources);

        assertThat(config.getSources()).doesNotContainAnyElementsOf(Arrays.asList("step1.output", "step2.output"));
    }

    @Test
    @DisplayName("应该保持数据源的插入顺序")
    void shouldMaintainSourcesOrder() {
        List<String> sources = Arrays.asList("step3", "step1", "step2");

        ComposeStepConfig config = new ComposeStepConfig(sources);

        assertThat(config.getSources()).containsExactly("step3", "step1", "step2");
    }

    @Test
    @DisplayName("应该处理包含嵌套引用的数据源")
    void shouldHandleNestedReferences() {
        List<String> sources = Arrays.asList("step1.output.data", "step2.result.items");

        ComposeStepConfig config = new ComposeStepConfig(sources);

        assertThat(config.getSources()).contains("step1.output.data");
        assertThat(config.getSources()).contains("step2.result.items");
    }

    @Test
    @DisplayName("应该处理重复的数据源引用")
    void shouldHandleDuplicateSources() {
        List<String> sources = Arrays.asList("step1.output", "step1.output", "step2.output");

        ComposeStepConfig config = new ComposeStepConfig(sources);

        assertThat(config.getSources()).hasSize(3);
        assertThat(config.getSources()).containsExactly("step1.output", "step1.output", "step2.output");
    }

    @Test
    @DisplayName("应该处理包含特殊字符的数据源名称")
    void shouldHandleSpecialCharactersInSourceNames() {
        List<String> sources = Arrays.asList("step_1.output", "step-2.data", "step.3.result");

        ComposeStepConfig config = new ComposeStepConfig(sources);

        assertThat(config.getSources()).contains("step_1.output", "step-2.data", "step.3.result");
    }
}
