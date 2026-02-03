package com.mia.aegis.skill.dsl.model;

import com.mia.aegis.skill.dsl.model.io.InputSchema;
import com.mia.aegis.skill.dsl.model.io.OutputContract;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Skill 的单元测试。
 *
 * 测试覆盖：
 * - 正常创建和使用
 * - 空值验证
 * - Step查询和管理
 * - 扩展字段处理
 * - 不可变性验证
 */
@DisplayName("Skill 测试")
class SkillTest {

    @Test
    @DisplayName("应该成功创建Skill")
    void shouldCreateSkillSuccessfully() {
        List<Step> steps = createTestSteps();
        InputSchema inputSchema = createTestInputSchema();
        OutputContract outputContract = createTestOutputContract();
        List<String> intents = Arrays.asList("test", "example");

        Skill skill = new Skill(
                "test_skill",
                "测试技能描述",
                intents,
                inputSchema,
                steps,
                outputContract,
                new HashMap<String, Object>()
        );

        assertThat(skill.getId()).isEqualTo("test_skill");
        assertThat(skill.getDescription()).isEqualTo("测试技能描述");
        assertThat(skill.getIntents()).containsExactly("test", "example");
        assertThat(skill.getSteps()).hasSize(2);
        assertThat(skill.getStepCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Skill ID前后空格应该被修剪")
    void shouldTrimSkillId() {
        List<Step> steps = createTestSteps();

        Skill skill = new Skill(
                "  test_skill  ",
                null,
                null,
                null,
                steps,
                null,
                null
        );

        assertThat(skill.getId()).isEqualTo("test_skill");
    }

    @Test
    @DisplayName("应该拒绝null或空的Skill ID")
    void shouldRejectNullOrEmptySkillId() {
        List<Step> steps = createTestSteps();

        assertThatThrownBy(() -> new Skill(null, null, null, null, steps, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("技能ID不能为空");

        assertThatThrownBy(() -> new Skill("", null, null, null, steps, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("技能ID不能为空");

        assertThatThrownBy(() -> new Skill("   ", null, null, null, steps, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("技能ID不能为空");
    }

    @Test
    @DisplayName("应该拒绝null或空的Step列表")
    void shouldRejectNullOrEmptyStepList() {
        assertThatThrownBy(() -> new Skill("test_skill", null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("技能必须包含至少一个步骤");

        assertThatThrownBy(() -> new Skill("test_skill", null, null, null, new ArrayList<Step>(), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("技能必须包含至少一个步骤");
    }

    @Test
    @DisplayName("null intents应该被转换为空列表")
    void shouldConvertNullIntentsToEmptyList() {
        List<Step> steps = createTestSteps();

        Skill skill = new Skill(
                "test_skill",
                null,
                null,
                null,
                steps,
                null,
                null
        );

        assertThat(skill.getIntents()).isNotNull();
        assertThat(skill.getIntents()).isEmpty();
    }

    @Test
    @DisplayName("null InputSchema应该被转换为空Schema")
    void shouldConvertNullInputSchemaToEmpty() {
        List<Step> steps = createTestSteps();

        Skill skill = new Skill(
                "test_skill",
                null,
                null,
                null,
                steps,
                null,
                null
        );

        assertThat(skill.getInputSchema()).isNotNull();
        assertThat(skill.getInputSchema().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("null OutputContract应该被转换为空契约")
    void shouldConvertNullOutputContractToEmpty() {
        List<Step> steps = createTestSteps();

        Skill skill = new Skill(
                "test_skill",
                null,
                null,
                null,
                steps,
                null,
                null
        );

        assertThat(skill.getOutputContract()).isNotNull();
        assertThat(skill.getOutputContract().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("null extensions应该被转换为空Map")
    void shouldConvertNullExtensionsToEmptyMap() {
        List<Step> steps = createTestSteps();

        Skill skill = new Skill(
                "test_skill",
                null,
                null,
                null,
                steps,
                null,
                null
        );

        assertThat(skill.getExtensions()).isNotNull();
        assertThat(skill.getExtensions()).isEmpty();
    }

    @Test
    @DisplayName("intents列表应该是不可变的")
    void intentsShouldBeUnmodifiable() {
        List<String> intents = new ArrayList<String>();
        intents.add("test");
        List<Step> steps = createTestSteps();

        Skill skill = new Skill(
                "test_skill",
                null,
                intents,
                null,
                steps,
                null,
                null
        );

        assertThatThrownBy(() -> skill.getIntents().add("another"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("steps列表应该是不可变的")
    void stepsShouldBeUnmodifiable() {
        List<Step> steps = createTestSteps();

        Skill skill = new Skill(
                "test_skill",
                null,
                null,
                null,
                steps,
                null,
                null
        );

        assertThatThrownBy(() -> skill.getSteps().add(createMockStep("extra")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("extensions Map应该是不可变的")
    void extensionsShouldBeUnmodifiable() {
        Map<String, Object> extensions = new HashMap<String, Object>();
        extensions.put("key1", "value1");
        List<Step> steps = createTestSteps();

        Skill skill = new Skill(
                "test_skill",
                null,
                null,
                null,
                steps,
                null,
                extensions
        );

        assertThatThrownBy(() -> skill.getExtensions().put("key2", "value2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("构造函数应该防御性复制集合")
    void constructorShouldDefensivelyCopyCollections() {
        List<Step> steps = createTestSteps();
        List<String> intents = new ArrayList<String>();
        intents.add("test");
        Map<String, Object> extensions = new HashMap<String, Object>();
        extensions.put("key", "value");

        Skill skill = new Skill(
                "test_skill",
                "description",
                intents,
                null,
                steps,
                null,
                extensions
        );

        // 修改原始集合
        intents.add("another");
        steps.add(createMockStep("extra"));
        extensions.put("key2", "value2");

        // 验证Skill对象不受影响
        assertThat(skill.getIntents()).hasSize(1);
        assertThat(skill.getSteps()).hasSize(2);
        assertThat(skill.getExtensions()).hasSize(1);
    }

    @Test
    @DisplayName("getStep应该根据名称查找Step")
    void getStepShouldFindStepByName() {
        List<Step> steps = createTestSteps();

        Skill skill = new Skill(
                "test_skill",
                null,
                null,
                null,
                steps,
                null,
                null
        );

        assertThat(skill.getStep("step1")).isNotNull();
        assertThat(skill.getStep("step1").getName()).isEqualTo("step1");
        assertThat(skill.getStep("step2")).isNotNull();
        assertThat(skill.getStep("step2").getName()).isEqualTo("step2");
    }

    @Test
    @DisplayName("getStep对于不存在的Step应该返回null")
    void getStepShouldReturnNullForUnknownStep() {
        List<Step> steps = createTestSteps();

        Skill skill = new Skill(
                "test_skill",
                null,
                null,
                null,
                steps,
                null,
                null
        );

        assertThat(skill.getStep("unknown")).isNull();
    }

    @Test
    @DisplayName("getExtension应该返回扩展字段值")
    void getExtensionShouldReturnValue() {
        Map<String, Object> extensions = new HashMap<String, Object>();
        extensions.put("key1", "value1");
        extensions.put("key2", 123);
        List<Step> steps = createTestSteps();

        Skill skill = new Skill(
                "test_skill",
                null,
                null,
                null,
                steps,
                null,
                extensions
        );

        assertThat(skill.getExtension("key1")).isEqualTo("value1");
        assertThat(skill.getExtension("key2")).isEqualTo(123);
        assertThat(skill.getExtension("unknown")).isNull();
    }

    @Test
    @DisplayName("hasExtension应该检查扩展字段是否存在")
    void hasExtensionShouldCheckExtensionExistence() {
        Map<String, Object> extensions = new HashMap<String, Object>();
        extensions.put("key1", "value1");
        List<Step> steps = createTestSteps();

        Skill skill = new Skill(
                "test_skill",
                null,
                null,
                null,
                steps,
                null,
                extensions
        );

        assertThat(skill.hasExtension("key1")).isTrue();
        assertThat(skill.hasExtension("unknown")).isFalse();
    }

    @Test
    @DisplayName("toString应该包含ID、步骤数量和意图")
    void toStringShouldContainIdStepCountAndIntents() {
        List<String> intents = Arrays.asList("test", "example");
        List<Step> steps = createTestSteps();

        Skill skill = new Skill(
                "test_skill",
                "description",
                intents,
                null,
                steps,
                null,
                null
        );

        assertThat(skill.toString()).contains("test_skill");
        assertThat(skill.toString()).contains("2");
        assertThat(skill.toString()).contains("test");
        assertThat(skill.toString()).contains("example");
    }

    @Test
    @DisplayName("应该保持intents的插入顺序")
    void shouldMaintainIntentsOrder() {
        List<String> intents = Arrays.asList("intent2", "intent1", "intent3");
        List<Step> steps = createTestSteps();

        Skill skill = new Skill(
                "test_skill",
                null,
                intents,
                null,
                steps,
                null,
                null
        );

        assertThat(skill.getIntents()).containsExactly("intent2", "intent1", "intent3");
    }

    @Test
    @DisplayName("应该保持extensions的插入顺序")
    void shouldMaintainExtensionsOrder() {
        Map<String, Object> extensions = new LinkedHashMap<String, Object>();
        extensions.put("key3", "value3");
        extensions.put("key1", "value1");
        extensions.put("key2", "value2");
        List<Step> steps = createTestSteps();

        Skill skill = new Skill(
                "test_skill",
                null,
                null,
                null,
                steps,
                null,
                extensions
        );

        assertThat(skill.getExtensions().keySet())
                .containsExactly("key3", "key1", "key2");
    }

    @Test
    @DisplayName("应该正确处理多行描述")
    void shouldHandleMultiLineDescription() {
        List<Step> steps = createTestSteps();
        String multiLineDescription = "第一行描述\n第二行描述\n第三行描述";

        Skill skill = new Skill(
                "test_skill",
                multiLineDescription,
                null,
                null,
                steps,
                null,
                null
        );

        assertThat(skill.getDescription()).isEqualTo(multiLineDescription);
    }

    @Test
    @DisplayName("getStepCount应该返回正确的步骤数量")
    void getStepCountShouldReturnCorrectCount() {
        List<Step> singleStep = Arrays.asList(createMockStep("step1"));
        List<Step> threeSteps = Arrays.asList(
                createMockStep("step1"),
                createMockStep("step2"),
                createMockStep("step3")
        );

        Skill skill1 = new Skill("skill1", null, null, null, singleStep, null, null);
        Skill skill2 = new Skill("skill2", null, null, null, threeSteps, null, null);

        assertThat(skill1.getStepCount()).isEqualTo(1);
        assertThat(skill2.getStepCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("应该处理包含特殊字符的ID")
    void shouldHandleSpecialCharactersInId() {
        List<Step> steps = createTestSteps();

        Skill skill = new Skill(
                "test_skill_123",
                null,
                null,
                null,
                steps,
                null,
                null
        );

        assertThat(skill.getId()).isEqualTo("test_skill_123");
    }

    // Helper methods

    private List<Step> createTestSteps() {
        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("param", "value");

        Step step1 = Step.tool("step1", new ToolStepConfig("tool1", inputTemplate));
        Step step2 = Step.prompt("step2", new PromptStepConfig("template"));

        List<Step> steps = new ArrayList<Step>();
        steps.add(step1);
        steps.add(step2);
        return steps;  // 返回可修改的ArrayList以支持防御性拷贝测试
    }

    private Step createMockStep(String name) {
        return Step.tool(name, new ToolStepConfig("tool", new HashMap<String, String>()));
    }

    private InputSchema createTestInputSchema() {
        Map<String, com.mia.aegis.skill.dsl.model.io.FieldSpec> fields =
                new HashMap<String, com.mia.aegis.skill.dsl.model.io.FieldSpec>();
        fields.put("param1", com.mia.aegis.skill.dsl.model.io.FieldSpec.of("string"));
        return new InputSchema(fields);
    }

    private OutputContract createTestOutputContract() {
        Map<String, com.mia.aegis.skill.dsl.model.io.FieldSpec> fields =
                new HashMap<String, com.mia.aegis.skill.dsl.model.io.FieldSpec>();
        fields.put("result", com.mia.aegis.skill.dsl.model.io.FieldSpec.of("string"));
        return new OutputContract(fields, com.mia.aegis.skill.dsl.model.io.OutputFormat.JSON);
    }
}
