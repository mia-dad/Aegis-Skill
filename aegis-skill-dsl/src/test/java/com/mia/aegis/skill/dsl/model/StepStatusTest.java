package com.mia.aegis.skill.dsl.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StepStatus 枚举的单元测试。
 */
@DisplayName("StepStatus 枚举测试")
class StepStatusTest {

    @Test
    @DisplayName("应该包含所有预期的状态值")
    void shouldContainAllExpectedValues() {
        assertThat(StepStatus.values())
                .containsExactly(
                        StepStatus.PENDING,
                        StepStatus.RUNNING,
                        StepStatus.SUCCESS,
                        StepStatus.FAILED,
                        StepStatus.SKIPPED
                );
    }

    @Test
    @DisplayName("状态值数量应该正确")
    void shouldHaveCorrectNumberOfValues() {
        assertThat(StepStatus.values()).hasSize(5);
    }

    @Test
    @DisplayName("枚举值应该是唯一的")
    void valuesShouldBeUnique() {
        assertThat(StepStatus.values())
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("toString应该返回枚举名称")
    void toStringShouldReturnEnumName() {
        assertThat(StepStatus.PENDING.toString()).isEqualTo("PENDING");
        assertThat(StepStatus.RUNNING.toString()).isEqualTo("RUNNING");
        assertThat(StepStatus.SUCCESS.toString()).isEqualTo("SUCCESS");
        assertThat(StepStatus.FAILED.toString()).isEqualTo("FAILED");
        assertThat(StepStatus.SKIPPED.toString()).isEqualTo("SKIPPED");
    }

    @Test
    @DisplayName("valueOf应该正确解析枚举值")
    void valueOfShouldCorrectlyParseEnumValues() {
        assertThat(StepStatus.valueOf("PENDING")).isEqualTo(StepStatus.PENDING);
        assertThat(StepStatus.valueOf("SUCCESS")).isEqualTo(StepStatus.SUCCESS);
        assertThat(StepStatus.valueOf("FAILED")).isEqualTo(StepStatus.FAILED);
    }

    @Test
    @DisplayName("枚举应该按预期顺序定义")
    void enumsShouldBeInExpectedOrder() {
        StepStatus[] values = StepStatus.values();
        assertThat(values[0]).isEqualTo(StepStatus.PENDING);
        assertThat(values[1]).isEqualTo(StepStatus.RUNNING);
        assertThat(values[2]).isEqualTo(StepStatus.SUCCESS);
        assertThat(values[3]).isEqualTo(StepStatus.FAILED);
        assertThat(values[4]).isEqualTo(StepStatus.SKIPPED);
    }
}
