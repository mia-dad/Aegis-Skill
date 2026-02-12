package com.mia.aegis.skill.dsl.validator;

import com.mia.aegis.skill.dsl.model.*;
import com.mia.aegis.skill.dsl.model.io.FieldSpec;
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
 * - Await 步骤不需要 varName
 * - Tool 步骤需要 output_schema
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

        assertThatThrownBy(() -> new Skill("", "1.0.0", null, null, null, steps, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("技能ID不能为空");

        // 验证器会检测不符合格式的ID（如包含大写字母）
        List<Step> steps2 = Arrays.asList(createMockToolStep("step1"));
        Skill skill2 = new Skill("InvalidID", "1.0.0", null, null, null, steps2, OutputContract.text(), null);
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
            Skill skill = new Skill(invalidId, "1.0.0", null, null, null, steps, OutputContract.text(), null);
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
            Skill skill = new Skill(validId, "1.0.0", null, null, null, steps, OutputContract.text(), null);
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
        Skill skill = new Skill("test_skill", "1.0.0", null, null, null, steps, OutputContract.text(), null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("重复的步骤名称") &&
                error.contains("duplicate"));
    }

    @Test
    @DisplayName("应该拒绝无效格式的Step名称")
    void shouldRejectInvalidStepNameFormat() {
        Map<String, Object> inputTemplate = new HashMap<String, Object>();
        inputTemplate.put("param", "value");

        ToolStepConfig config = new ToolStepConfig("tool", inputTemplate, Arrays.asList("result"));
        Step step = new Step("Invalid-Name", StepType.TOOL, config);

        List<Step> steps = Arrays.asList(step);
        Skill skill = new Skill("test_skill", "1.0.0", null, null, null, steps, OutputContract.text(), null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("无效的步骤名称格式") ||
                error.contains("Invalid-Name"));
    }

    @Test
    @DisplayName("应该拒绝Tool步骤的空工具名称")
    void shouldRejectEmptyToolName() {
        // ToolStepConfig构造函数会拒绝空工具名称
        Map<String, Object> inputTemplate = new HashMap<String, Object>();

        assertThatThrownBy(() -> new ToolStepConfig("", inputTemplate))
                .isInstanceOf(IllegalArgumentException.class);
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
    }

    @Test
    @DisplayName("应该拒绝引用未知Step的变量")
    void shouldRejectVariableReferencingUnknownStep() {
        Map<String, Object> inputTemplate = new HashMap<String, Object>();
        inputTemplate.put("param", "{{unknown_step.output}}");

        ToolStepConfig config = new ToolStepConfig("tool", inputTemplate, Arrays.asList("result"));
        Step step = new Step("step1", StepType.TOOL, config);

        List<Step> steps = Arrays.asList(step);
        Skill skill = new Skill("test_skill", "1.0.0", null, null, null, steps, OutputContract.text(), null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("unknown_step") &&
                error.contains("引用了未知的步骤"));
    }

    @Test
    @DisplayName("应该拒绝自引用变量")
    void shouldRejectSelfReferencingVariable() {
        Map<String, Object> inputTemplate = new HashMap<String, Object>();
        inputTemplate.put("param", "{{step1.output}}");

        ToolStepConfig config = new ToolStepConfig("tool", inputTemplate, Arrays.asList("result"));
        Step step = new Step("step1", StepType.TOOL, config);

        List<Step> steps = Arrays.asList(step);
        OutputContract outputContract = OutputContract.text();
        Skill skill = new Skill("test_skill", "1.0.0", null, null, null, steps, outputContract, null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("step1") &&
                error.contains("引用了当前步骤"));
    }

    @Test
    @DisplayName("应该允许引用输入字段的变量")
    void shouldAllowVariableReferencingInputField() {
        Map<String, Object> inputTemplate = new HashMap<String, Object>();
        inputTemplate.put("param", "{{input_field}}");

        ToolStepConfig config = new ToolStepConfig("tool", inputTemplate, Arrays.asList("result"));
        Step step = new Step("step1", StepType.TOOL, config);

        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("input_field", FieldSpec.of("string"));
        InputSchema inputSchema = new InputSchema(fields);

        List<Step> steps = Arrays.asList(step);
        Skill skill = new Skill("test_skill", "1.0.0", null, null, inputSchema, steps, OutputContract.text(), null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).noneMatch(error -> error.contains("input_field") &&
                error.contains("Unknown variable"));
    }

    @Test
    @DisplayName("应该允许引用context变量的变量")
    void shouldAllowVariableReferencingContext() {
        Map<String, Object> inputTemplate = new HashMap<String, Object>();
        inputTemplate.put("param", "{{context.startTime}}");

        ToolStepConfig config = new ToolStepConfig("tool", inputTemplate, Arrays.asList("result"));
        Step step = new Step("step1", StepType.TOOL, config);

        List<Step> steps = Arrays.asList(step);
        Skill skill = new Skill("test_skill", "1.0.0", null, null, null, steps, OutputContract.text(), null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).noneMatch(error -> error.contains("context"));
    }

    @Test
    @DisplayName("应该检测循环依赖")
    void shouldDetectCircularDependencies() {
        // step1 引用 step2
        Map<String, Object> inputTemplate1 = new HashMap<String, Object>();
        inputTemplate1.put("param", "{{step2.output}}");
        Step step1 = new Step("step1", StepType.TOOL,
                new ToolStepConfig("tool1", inputTemplate1, Arrays.asList("output")));

        // step2 引用 step1
        Map<String, Object> inputTemplate2 = new HashMap<String, Object>();
        inputTemplate2.put("param", "{{step1.output}}");
        Step step2 = new Step("step2", StepType.TOOL,
                new ToolStepConfig("tool2", inputTemplate2, Arrays.asList("output")));

        List<Step> steps = Arrays.asList(step1, step2);
        Skill skill = new Skill("test_skill", "1.0.0", null, null, null, steps, OutputContract.text(), null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("循环依赖") ||
                error.contains("循环"));
    }

    @Test
    @DisplayName("应该检测复杂的循环依赖（3个步骤）")
    void shouldDetectComplexCircularDependencies() {
        // step1 -> step2 -> step3 -> step1
        Map<String, Object> inputTemplate1 = new HashMap<String, Object>();
        inputTemplate1.put("param", "{{step2.output}}");
        Step step1 = new Step("step1", StepType.TOOL,
                new ToolStepConfig("tool1", inputTemplate1, Arrays.asList("output")));

        Map<String, Object> inputTemplate2 = new HashMap<String, Object>();
        inputTemplate2.put("param", "{{step3.output}}");
        Step step2 = new Step("step2", StepType.TOOL,
                new ToolStepConfig("tool2", inputTemplate2, Arrays.asList("output")));

        Map<String, Object> inputTemplate3 = new HashMap<String, Object>();
        inputTemplate3.put("param", "{{step1.output}}");
        Step step3 = new Step("step3", StepType.TOOL,
                new ToolStepConfig("tool3", inputTemplate3, Arrays.asList("output")));

        List<Step> steps = Arrays.asList(step1, step2, step3);
        Skill skill = new Skill("test_skill", "1.0.0", null, null, null, steps, OutputContract.text(), null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("循环依赖") ||
                error.contains("循环"));
    }

    @Test
    @DisplayName("应该允许无循环的有效依赖链")
    void shouldAllowValidDependencyChainWithoutCycles() {
        // step1 -> step2 -> step3
        Map<String, Object> inputTemplate2 = new HashMap<String, Object>();
        inputTemplate2.put("param", "{{step1.output}}");
        Step step2 = new Step("step2", StepType.TOOL,
                new ToolStepConfig("tool2", inputTemplate2, Arrays.asList("output")));

        Map<String, Object> inputTemplate3 = new HashMap<String, Object>();
        inputTemplate3.put("param", "{{step2.output}}");
        Step step3 = new Step("step3", StepType.TOOL,
                new ToolStepConfig("tool3", inputTemplate3, Arrays.asList("output")));

        Step step1 = new Step("step1", StepType.TOOL,
                new ToolStepConfig("tool1", new HashMap<String, Object>(), Arrays.asList("output")));

        List<Step> steps = Arrays.asList(step1, step2, step3);
        Skill skill = new Skill("test_skill", "1.0.0", null, null, null, steps, OutputContract.text(), null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).noneMatch(error -> error.contains("循环依赖") ||
                error.contains("循环"));
    }

    @Test
    @DisplayName("validate应该抛出异常包含所有错误")
    void validateShouldThrowExceptionWithAllErrors() {
        Step step1 = new Step("Invalid-Name", StepType.TOOL,
                new ToolStepConfig("tool", new HashMap<String, Object>(), Arrays.asList("result")));
        Step step2 = new Step("Invalid-Name", StepType.TOOL,
                new ToolStepConfig("tool", new HashMap<String, Object>(), Arrays.asList("result")));

        List<Step> steps = Arrays.asList(step1, step2);
        Skill skill = new Skill("123invalid", "1.0.0", null, null, null, steps, OutputContract.text(), null);

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
                new ToolStepConfig("tool", new HashMap<String, Object>(), Arrays.asList("result")));
        Step step2 = new Step("Invalid-Name", StepType.TOOL,
                new ToolStepConfig("tool", new HashMap<String, Object>(), Arrays.asList("result")));

        List<Step> steps = Arrays.asList(step1, step2);
        Skill skill = new Skill("123invalid", "1.0.0", null, null, null, steps, OutputContract.text(), null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).isNotEmpty();
        assertThat(errors.size()).isGreaterThan(1);
    }

    @Test
    @DisplayName("应该拒绝未知变量")
    void shouldRejectUnknownVariables() {
        Map<String, Object> inputTemplate = new HashMap<String, Object>();
        inputTemplate.put("param", "{{unknown_variable}}");

        ToolStepConfig config = new ToolStepConfig("tool", inputTemplate, Arrays.asList("result"));
        Step step = new Step("step1", StepType.TOOL, config);

        List<Step> steps = Arrays.asList(step);
        Skill skill = new Skill("test_skill", "1.0.0", null, null, null, steps, OutputContract.text(), null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("未知变量") &&
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
                new ToolStepConfig("tool", new HashMap<String, Object>(), Arrays.asList("result")));

        List<Step> steps = Arrays.asList(step);
        Skill skill = new Skill("test_skill", "1.0.0", null, null, null, steps, OutputContract.text(), null);

        assertThat(validator.isValid(skill)).isFalse();
    }

    // ---- Await 步骤不需要 varName ----

    @Test
    @DisplayName("Await 步骤不需要 varName - 不应报 varName 缺失错误")
    void awaitStepWithoutVarNameShouldNotReportVarNameError() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("confirm", FieldSpec.of("boolean"));
        InputSchema awaitInput = new InputSchema(fields);
        AwaitStepConfig awaitConfig = new AwaitStepConfig("请确认", awaitInput);

        Step awaitStep = Step.await("confirm_step", awaitConfig);

        List<Step> steps = Arrays.asList(
                createMockToolStep("fetch_data"),
                awaitStep
        );
        Skill skill = new Skill("test_skill", "1.0.0", null, null, null, steps, OutputContract.text(), null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).noneMatch(error ->
                error.contains("confirm_step") && error.contains("varName"));
    }

    // ---- Tool 步骤需要 output_schema ----

    @Test
    @DisplayName("Tool 步骤缺少 output_schema 应报错")
    void toolStepWithoutOutputSchemaShouldReportError() {
        Map<String, Object> inputTemplate = new HashMap<String, Object>();
        inputTemplate.put("param", "value");

        // 不提供 outputFields
        ToolStepConfig config = new ToolStepConfig("tool", inputTemplate);
        Step step = new Step("step1", StepType.TOOL, config, null, "result");

        List<Step> steps = Arrays.asList(step);
        Skill skill = new Skill("test_skill", "1.0.0", null, null, null, steps, OutputContract.text(), null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error ->
                error.contains("output_schema") && error.contains("step1"));
    }

    @Test
    @DisplayName("Tool 步骤有 output_schema 不应报此错误")
    void toolStepWithOutputSchemaShouldNotReportError() {
        Skill skill = createValidSkill();

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).noneMatch(error -> error.contains("output_schema"));
    }

    @Test
    @DisplayName("缺少 version 应校验失败")
    void shouldRejectMissingVersion() {
        Step step1 = createMockToolStep("step1");
        List<Step> steps = Arrays.asList(step1);
        Skill skill = new Skill("test_skill", null, null, null, null, steps, OutputContract.text(), null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("version"));
    }

    @Test
    @DisplayName("无效 version 格式应校验失败")
    void shouldRejectInvalidVersionFormat() {
        Step step1 = createMockToolStep("step1");
        List<Step> steps = Arrays.asList(step1);
        Skill skill = new Skill("test_skill", "abc", null, null, null, steps, OutputContract.text(), null);

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).anyMatch(error -> error.contains("version") && error.contains("格式无效"));
    }

    @Test
    @DisplayName("有效 version 格式应校验通过")
    void shouldAcceptValidVersionFormat() {
        Skill skill = createValidSkill();

        List<String> errors = validator.validateAndCollectErrors(skill);

        assertThat(errors).noneMatch(error -> error.contains("version"));
    }

    // Helper methods

    private Skill createValidSkill() {
        Map<String, Object> inputTemplate = new HashMap<String, Object>();
        inputTemplate.put("param", "value");

        Step step1 = new Step("valid_step", StepType.TOOL,
                new ToolStepConfig("tool", inputTemplate, Arrays.asList("result")), null, "result");

        List<Step> steps = Arrays.asList(step1);
        OutputContract outputContract = OutputContract.text();
        return new Skill("test_skill", "1.0.0", "description", null, null, steps, outputContract, null);
    }

    private Step createMockToolStep(String name) {
        Map<String, Object> inputTemplate = new HashMap<String, Object>();
        inputTemplate.put("param", "value");
        return Step.tool(name, new ToolStepConfig("tool", inputTemplate, Arrays.asList("result")), name + "_result");
    }

    private Step createMockPromptStep(String name) {
        return Step.prompt(name, new PromptStepConfig("test template"), name + "_result");
    }
}
