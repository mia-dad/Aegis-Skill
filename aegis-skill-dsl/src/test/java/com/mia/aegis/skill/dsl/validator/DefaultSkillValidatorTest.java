package com.mia.aegis.skill.dsl.validator;

import com.mia.aegis.skill.dsl.model.*;
import com.mia.aegis.skill.dsl.model.io.InputSchema;
import com.mia.aegis.skill.dsl.model.io.OutputContract;
import com.mia.aegis.skill.exception.SkillValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DefaultSkillValidator 的单元测试。
 *
 * 测试覆盖：
 * - 正常验证
 * - Skill ID格式验证
 * - Step名称唯一性验证
 * - 变量引用验证
 * - 循环依赖检测
 * - 边界情况
 */
@DisplayName("DefaultSkillValidator 测试")
class DefaultSkillValidatorTest {

    private final DefaultSkillValidator validator = new DefaultSkillValidator();

    @Test
    @DisplayName("应该验证有效的技能")
    void shouldValidateValidSkill() {
        Skill skill = createValidSkill();

        assertThat(validator.isValid(skill)).isTrue();
    }

    @Test
    @DisplayName("验证有效的技能不应该抛出异常")
    void validateValidSkillShouldNotThrowException() {
        Skill skill = createValidSkill();

        validator.validate(skill);
    }

    @Test
    @DisplayName("应该拒绝null技能")
    void shouldRejectNullSkill() {
        List<String> errors = validator.validateAndCollectErrors(null);

        assertThat(errors).isNotEmpty();
        assertThat(errors).contains("检验的技能为空");
    }

    @Test
    @DisplayName("应该拒绝空ID")
    void shouldRejectEmptyId() {
        // Skill构造函数会拒绝空ID，所以测试构造函数的异常处理
        List<Step> steps = Arrays.asList(createMockToolStep("step1"));

        assertThatThrownBy(() -> new Skill("", null, null, null, steps, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("技能ID不能为空");

        // 验证器会检测不符合格式的ID（如包含大写字母）
        List<Step> steps2 = Arrays.asList(createMockToolStep("step1"));
        Skill skill2 = new Skill("InvalidID", null, null, null, steps2, null, null);
        List<String> errors = validator.validateAndCollectErrors(skill2);

        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(error -> error.contains("ID") || error.contains("format"));
    }

    @Test
    @DisplayName("应该拒绝无效格式的Skill ID")
    void shouldRejectInvalidSkillIdFormat() {
        List<Step> steps = Arrays.asList(createMockToolStep("step1"));

        // 测试各种无效格式
        String[] invalidIds = {
                "123invalid",  // 数字开头
                "Invalid-Id",  // 包含大写字母和连字符
                "invalid.id",  // 包含点号
                "invalid id",  // 包含空格
                "invalid@id",  // 包含特殊字符
                "1",           // 纯数字
                "_invalid"     // 下划线开头
        };

        for (String invalidId : invalidIds) {
            Skill skill = new Skill(invalidId, null, null, null, steps, null, null);
            List<String> errors = validator.validateAndCollectErrors(skill);

            assertThat(errors).anyMatch(error -> error.contains("Invalid Skill ID format") ||
                    error.contains(invalidId));
        }
    }

    @Test
    @DisplayName("应该接受有效格式的Skill ID")
    void shouldAcceptValidSkillIdFormat() {
        List<Step> steps = Arrays.asList(createMockToolStep("step1"));

        String[] validIds = {
                "valid",
                "valid_skill",
                "valid_skill_123",
                "a",
                "skill_1_2_3"
        };

        for (String validId : validIds) {
            Skill skill = new Skill(validId, null, null, null, steps, null, null);
            List<String> errors = validator.validateAndCollectErrors(skill);

            assertThat(errors).noneMatch(error -> error.contains("ID") || error.contains("格式"));
        }
    }

    @Test
    @DisplayName("应该拒绝重复的Step名称")
    void shouldRejectDuplicateStepNames() {
        Step step1 = createMockToolStep("duplicate");
        Step step2 = createMockPromptStep("duplicate");

        List<Step> steps = Arrays.asList(step1, step2);
        Skill skill = new Skill("test_skill", null, null, null, steps, null, null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("Duplicate step name") &&
                error.contains("duplicate"));
    }

    @Test
    @DisplayName("应该拒绝无效格式的Step名称")
    void shouldRejectInvalidStepNameFormat() {
        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("param", "value");

        ToolStepConfig config = new ToolStepConfig("tool", inputTemplate);
        Step step = new Step("Invalid-Name", StepType.TOOL, config);

        List<Step> steps = Arrays.asList(step);
        Skill skill = new Skill("test_skill", null, null, null, steps, null, null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("Invalid step name format") ||
                error.contains("Invalid-Name"));
    }

    @Test
    @DisplayName("应该拒绝Tool步骤的空工具名称")
    void shouldRejectEmptyToolName() {
        // ToolStepConfig构造函数会拒绝空工具名称
        Map<String, String> inputTemplate = new HashMap<String, String>();

        assertThatThrownBy(() -> new ToolStepConfig("", inputTemplate))
                .isInstanceOf(IllegalArgumentException.class);

        // 验证器会拒绝仅包含空格的工具名称（如果构造函数允许的话）
        // 但由于构造函数已经处理，我们主要测试验证器能识别出这种情况
        // 这个测试保留以确保未来的代码更改不会遗漏这个检查
    }

    @Test
    @DisplayName("应该拒绝Prompt步骤的空模板")
    void shouldRejectEmptyPromptTemplate() {
        // PromptStepConfig构造函数会拒绝空模板
        assertThatThrownBy(() -> new PromptStepConfig("   "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new PromptStepConfig(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new PromptStepConfig(null))
                .isInstanceOf(IllegalArgumentException.class);
        // 构造函数已经处理了所有空值情况，验证器不需要重复检查
    }

    @Test
    @DisplayName("应该拒绝引用未知Step的变量")
    void shouldRejectVariableReferencingUnknownStep() {
        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("param", "{{unknown_step.output}}");

        ToolStepConfig config = new ToolStepConfig("tool", inputTemplate);
        Step step = new Step("step1", StepType.TOOL, config);

        List<Step> steps = Arrays.asList(step);
        Skill skill = new Skill("test_skill", null, null, null, steps, null, null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("unknown_step") &&
                error.contains("references unknown step"));
    }

    @Test
    @DisplayName("应该拒绝自引用变量")
    void shouldRejectSelfReferencingVariable() {
        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("param", "{{step1.output}}");

        ToolStepConfig config = new ToolStepConfig("tool", inputTemplate);
        Step step = new Step("step1", StepType.TOOL, config);

        List<Step> steps = Arrays.asList(step);
        Skill skill = new Skill("test_skill", null, null, null, steps, null, null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("step1") &&
                error.contains("self-reference"));
    }

    @Test
    @DisplayName("应该允许引用输入字段的变量")
    void shouldAllowVariableReferencingInputField() {
        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("param", "{{input_field}}");

        ToolStepConfig config = new ToolStepConfig("tool", inputTemplate);
        Step step = new Step("step1", StepType.TOOL, config);

        Map<String, com.mia.aegis.skill.dsl.model.io.FieldSpec> fields =
                new HashMap<String, com.mia.aegis.skill.dsl.model.io.FieldSpec>();
        fields.put("input_field", com.mia.aegis.skill.dsl.model.io.FieldSpec.of("string"));
        InputSchema inputSchema = new InputSchema(fields);

        List<Step> steps = Arrays.asList(step);
        Skill skill = new Skill("test_skill", null, null, inputSchema, steps, null, null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).noneMatch(error -> error.contains("input_field") &&
                error.contains("Unknown variable"));
    }

    @Test
    @DisplayName("应该允许引用context变量的变量")
    void shouldAllowVariableReferencingContext() {
        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("param", "{{context.startTime}}");

        ToolStepConfig config = new ToolStepConfig("tool", inputTemplate);
        Step step = new Step("step1", StepType.TOOL, config);

        List<Step> steps = Arrays.asList(step);
        Skill skill = new Skill("test_skill", null, null, null, steps, null, null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).noneMatch(error -> error.contains("context"));
    }

    @Test
    @DisplayName("应该检测循环依赖")
    void shouldDetectCircularDependencies() {
        // step1 引用 step2
        Map<String, String> inputTemplate1 = new HashMap<String, String>();
        inputTemplate1.put("param", "{{step2.output}}");
        Step step1 = new Step("step1", StepType.TOOL,
                new ToolStepConfig("tool1", inputTemplate1));

        // step2 引用 step1
        Map<String, String> inputTemplate2 = new HashMap<String, String>();
        inputTemplate2.put("param", "{{step1.output}}");
        Step step2 = new Step("step2", StepType.TOOL,
                new ToolStepConfig("tool2", inputTemplate2));

        List<Step> steps = Arrays.asList(step1, step2);
        Skill skill = new Skill("test_skill", null, null, null, steps, null, null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("Circular dependency") ||
                error.contains("循环"));
    }

    @Test
    @DisplayName("应该检测复杂的循环依赖（3个步骤）")
    void shouldDetectComplexCircularDependencies() {
        // step1 -> step2 -> step3 -> step1
        Map<String, String> inputTemplate1 = new HashMap<String, String>();
        inputTemplate1.put("param", "{{step2.output}}");
        Step step1 = new Step("step1", StepType.TOOL,
                new ToolStepConfig("tool1", inputTemplate1));

        Map<String, String> inputTemplate2 = new HashMap<String, String>();
        inputTemplate2.put("param", "{{step3.output}}");
        Step step2 = new Step("step2", StepType.TOOL,
                new ToolStepConfig("tool2", inputTemplate2));

        Map<String, String> inputTemplate3 = new HashMap<String, String>();
        inputTemplate3.put("param", "{{step1.output}}");
        Step step3 = new Step("step3", StepType.TOOL,
                new ToolStepConfig("tool3", inputTemplate3));

        List<Step> steps = Arrays.asList(step1, step2, step3);
        Skill skill = new Skill("test_skill", null, null, null, steps, null, null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("Circular") ||
                error.contains("循环"));
    }

    @Test
    @DisplayName("应该允许无循环的有效依赖链")
    void shouldAllowValidDependencyChainWithoutCycles() {
        // step1 -> step2 -> step3
        Map<String, String> inputTemplate2 = new HashMap<String, String>();
        inputTemplate2.put("param", "{{step1.output}}");
        Step step2 = new Step("step2", StepType.TOOL,
                new ToolStepConfig("tool2", inputTemplate2));

        Map<String, String> inputTemplate3 = new HashMap<String, String>();
        inputTemplate3.put("param", "{{step2.output}}");
        Step step3 = new Step("step3", StepType.TOOL,
                new ToolStepConfig("tool3", inputTemplate3));

        Step step1 = new Step("step1", StepType.TOOL,
                new ToolStepConfig("tool1", new HashMap<String, String>()));

        List<Step> steps = Arrays.asList(step1, step2, step3);
        Skill skill = new Skill("test_skill", null, null, null, steps, null, null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).noneMatch(error -> error.contains("Circular") ||
                error.contains("循环"));
    }

    @Test
    @DisplayName("应该拒绝Compose步骤引用自身")
    void shouldRejectComposeStepReferencingItself() {
        ComposeStepConfig config = new ComposeStepConfig(
                Arrays.asList("step1.output", "step1.data")
        );
        Step step = new Step("step1", StepType.COMPOSE, config);

        List<Step> steps = Arrays.asList(step);
        Skill skill = new Skill("test_skill", null, null, null, steps, null, null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("Cannot reference itself"));
    }

    @Test
    @DisplayName("应该拒绝Compose步骤引用未知步骤")
    void shouldRejectComposeStepReferencingUnknownStep() {
        ComposeStepConfig config = new ComposeStepConfig(
                Arrays.asList("unknown_step.output")
        );
        Step step = new Step("step1", StepType.COMPOSE, config);

        List<Step> steps = Arrays.asList(step);
        Skill skill = new Skill("test_skill", null, null, null, steps, null, null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("unknown_step") &&
                error.contains("Source references unknown step"));
    }

    @Test
    @DisplayName("validate应该抛出异常包含所有错误")
    void validateShouldThrowExceptionWithAllErrors() {
        Step step1 = new Step("Invalid-Name", StepType.TOOL,
                new ToolStepConfig("tool", new HashMap<String, String>()));
        Step step2 = new Step("Invalid-Name", StepType.TOOL,
                new ToolStepConfig("tool", new HashMap<String, String>()));

        List<Step> steps = Arrays.asList(step1, step2);
        Skill skill = new Skill("123invalid", null, null, null, steps, null, null);

        assertThatThrownBy(() -> validator.validate(skill))
                .isInstanceOf(SkillValidationException.class)
                .satisfies(exception -> {
                    SkillValidationException ex = (SkillValidationException) exception;
                    assertThat(ex.getErrors()).isNotEmpty();
                    // 应该包含多个错误
                    assertThat(ex.getErrors().size()).isGreaterThan(1);
                });
    }

    @Test
    @DisplayName("validateAndCollectErrors应该返回所有错误")
    void validateAndCollectErrorsShouldReturnAllErrors() {
        Step step1 = new Step("Invalid-Name", StepType.TOOL,
                new ToolStepConfig("tool", new HashMap<String, String>()));
        Step step2 = new Step("Invalid-Name", StepType.TOOL,
                new ToolStepConfig("tool", new HashMap<String, String>()));

        List<Step> steps = Arrays.asList(step1, step2);
        Skill skill = new Skill("123invalid", null, null, null, steps, null, null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).isNotEmpty();
        assertThat(errors.size()).isGreaterThan(1);
    }

    @Test
    @DisplayName("应该拒绝未知变量")
    void shouldRejectUnknownVariables() {
        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("param", "{{unknown_variable}}");

        ToolStepConfig config = new ToolStepConfig("tool", inputTemplate);
        Step step = new Step("step1", StepType.TOOL, config);

        List<Step> steps = Arrays.asList(step);
        Skill skill = new Skill("test_skill", null, null, null, steps, null, null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("Unknown variable") &&
                error.contains("unknown_variable"));
    }

    @Test
    @DisplayName("isValid应该返回true对于有效技能")
    void isValidShouldReturnTrueForValidSkill() {
        Skill skill = createValidSkill();

        assertThat(validator.isValid(skill)).isTrue();
    }

    @Test
    @DisplayName("isValid应该返回false对于无效技能")
    void isValidShouldReturnFalseForInvalidSkill() {
        Step step = new Step("Invalid-Name", StepType.TOOL,
                new ToolStepConfig("tool", new HashMap<String, String>()));

        List<Step> steps = Arrays.asList(step);
        Skill skill = new Skill("test_skill", null, null, null, steps, null, null);

        assertThat(validator.isValid(skill)).isFalse();
    }

    // Helper methods

    private Skill createValidSkill() {
        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("param", "value");

        Step step1 = new Step("valid_step", StepType.TOOL,
                new ToolStepConfig("tool", inputTemplate));

        List<Step> steps = Arrays.asList(step1);
        return new Skill("test_skill", "description", null, null, steps, null, null);
    }

    private Step createMockToolStep(String name) {
        Map<String, String> inputTemplate = new HashMap<String, String>();
        inputTemplate.put("param", "value");
        return Step.tool(name, new ToolStepConfig("tool", inputTemplate));
    }

    private Step createMockPromptStep(String name) {
        return Step.prompt(name, new PromptStepConfig("test template"));
    }
}
