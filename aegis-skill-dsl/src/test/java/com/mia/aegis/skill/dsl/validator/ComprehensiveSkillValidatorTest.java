package com.mia.aegis.skill.dsl.validator;

import com.mia.aegis.skill.dsl.condition.ast.VariableReference;
import com.mia.aegis.skill.dsl.condition.parser.DefaultConditionParser;
import com.mia.aegis.skill.dsl.model.*;
import com.mia.aegis.skill.dsl.model.io.FieldSpec;
import com.mia.aegis.skill.dsl.model.io.InputSchema;
import com.mia.aegis.skill.dsl.model.io.OutputContract;
import com.mia.aegis.skill.dsl.model.io.OutputFormat;
import com.mia.aegis.skill.dsl.validator.report.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ComprehensiveSkillValidator 的单元测试。
 */
@DisplayName("ComprehensiveSkillValidator 测试")
class ComprehensiveSkillValidatorTest {

    private final ComprehensiveSkillValidator validator = new ComprehensiveSkillValidator();

    // ===== 正常校验 =====

    @Test
    @DisplayName("有效技能应校验通过")
    void validSkillShouldPass() {
        Skill skill = createValidSkill();
        SkillValidationReport report = validator.validate(skill);

        assertThat(report.isValid()).isTrue();
        assertThat(report.getErrorCount()).isZero();
        assertThat(report.getSummary()).isNotNull();
        assertThat(report.getSummary().getSkillId()).isEqualTo("test_skill");
        assertThat(report.getSummary().getVersion()).isEqualTo("1.0.0");
        assertThat(report.getSummary().getStepCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("校验报告应包含摘要信息")
    void reportShouldContainSummary() {
        Skill skill = createFullSkill();
        SkillValidationReport report = validator.validate(skill);

        SkillSummary summary = report.getSummary();
        assertThat(summary).isNotNull();
        assertThat(summary.getSkillId()).isEqualTo("full_skill");
        assertThat(summary.getDescription()).isEqualTo("完整技能");
        assertThat(summary.getStepCount()).isEqualTo(2);
        assertThat(summary.getStepTypes()).containsExactly("TOOL", "PROMPT");
        assertThat(summary.getInputFieldCount()).isEqualTo(1);
        assertThat(summary.getOutputFieldCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("toFormattedString 应生成可读报告")
    void toFormattedStringShouldBeReadable() {
        Skill skill = createValidSkill();
        SkillValidationReport report = validator.validate(skill);

        String formatted = report.toFormattedString();
        assertThat(formatted).contains("Skill Validation Report");
        assertThat(formatted).contains("VALID");
        assertThat(formatted).contains("test_skill");
    }

    // ===== SYNTAX 类校验 =====

    @Nested
    @DisplayName("SYNTAX 校验")
    class SyntaxTests {

        @Test
        @DisplayName("S01: null Skill 应报错")
        void s01_nullSkillShouldFail() {
            SkillValidationReport report = validator.validate(null);

            assertThat(report.isValid()).isFalse();
            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("S01"));
        }

        @Test
        @DisplayName("S02: 无效 skill ID 格式应报错")
        void s02_invalidSkillIdShouldFail() {
            Skill skill = new Skill("InvalidID", "1.0.0", null, null, null,
                    Arrays.asList(createToolStep("step1")), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("S02"));
        }

        @Test
        @DisplayName("S03: version 缺失应报错")
        void s03_missingVersionShouldFail() {
            Skill skill = new Skill("test_skill", null, null, null, null,
                    Arrays.asList(createToolStep("step1")), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("S03"));
        }

        @Test
        @DisplayName("S04: 无效 version 格式应报错")
        void s04_invalidVersionShouldFail() {
            Skill skill = new Skill("test_skill", "abc", null, null, null,
                    Arrays.asList(createToolStep("step1")), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("S04"));
        }

        @Test
        @DisplayName("S06: 无效 step 名称格式应报错")
        void s06_invalidStepNameShouldFail() {
            Map<String, Object> input = new HashMap<String, Object>();
            input.put("p", "v");
            Step step = new Step("Invalid-Name", StepType.TOOL,
                    new ToolStepConfig("tool", input, Arrays.asList("r")));

            Skill skill = new Skill("test_skill", "1.0.0", null, null, null,
                    Arrays.asList(step), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("S06"));
        }

        @Test
        @DisplayName("S07: 重复 step 名称应报错")
        void s07_duplicateStepNameShouldFail() {
            Step step1 = createToolStep("dup");
            Step step2 = createToolStepWithPrompt("dup");

            Skill skill = new Skill("test_skill", "1.0.0", null, null, null,
                    Arrays.asList(step1, step2), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("S07"));
        }

        @Test
        @DisplayName("S09: output_schema 缺失应报错")
        void s09_missingOutputSchemaShouldFail() {
            Skill skill = new Skill("test_skill", "1.0.0", null, null, null,
                    Arrays.asList(createToolStep("step1")),
                    new OutputContract(null, OutputFormat.TEXT), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("S09"));
        }

        @Test
        @DisplayName("S10: description 缺失应产生 WARNING")
        void s10_missingDescriptionShouldWarn() {
            Skill skill = new Skill("test_skill", "1.0.0", null, null, null,
                    Arrays.asList(createToolStep("step1")), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getWarnings()).anyMatch(i -> i.getCode().equals("S10"));
        }

        @Test
        @DisplayName("S11: intents 缺失应产生 SUGGESTION")
        void s11_missingIntentsShouldSuggest() {
            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(createToolStep("step1")), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getSuggestions()).anyMatch(i -> i.getCode().equals("S11"));
        }

        @Test
        @DisplayName("S17: PROMPT 步骤缺少 varName 应报错")
        void s17_promptWithoutVarNameShouldFail() {
            Step step = Step.prompt("analyze", new PromptStepConfig("请分析 {{query}}"));

            Map<String, FieldSpec> inputFields = new HashMap<String, FieldSpec>();
            inputFields.put("query", FieldSpec.of("string"));

            Skill skill = new Skill("test_skill", "1.0.0", null, null,
                    new InputSchema(inputFields),
                    Arrays.asList(step), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("S17"));
        }

        @Test
        @DisplayName("S18: TOOL 步骤使用 varName 应报错")
        void s18_toolStepWithVarNameShouldFail() {
            Step step = Step.tool("write_report",
                    new ToolStepConfig("write_file",
                            Collections.<String, Object>singletonMap("path", "test.txt"),
                            Arrays.asList("result")),
                    "write_result");

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(step), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i ->
                    i.getCode().equals("S18") && i.getMessage().contains("write_report"));
        }

        @Test
        @DisplayName("S14: varName 与 stepName 冲突应报错")
        void s14_varNameConflictsWithStepNameShouldFail() {
            // step1 的 varName 是 "step2"，与另一个步骤名称冲突
            Step step1 = Step.prompt("step1", new PromptStepConfig("模板"), "step2");
            Step step2 = Step.prompt("step2", new PromptStepConfig("模板2"), "other");

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(step1, step2), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("S14"));
        }

        @Test
        @DisplayName("S15: varName 与 inputField 冲突应报错")
        void s15_varNameConflictsWithInputFieldShouldFail() {
            Step step1 = Step.prompt("step1", new PromptStepConfig("模板"), "query");

            Map<String, FieldSpec> inputFields = new HashMap<String, FieldSpec>();
            inputFields.put("query", FieldSpec.of("string"));

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null,
                    new InputSchema(inputFields),
                    Arrays.asList(step1), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("S15"));
        }

        @Test
        @DisplayName("S16: 无条件重复 varName 应报错")
        void s16_duplicateVarNameShouldFail() {
            Step step1 = Step.prompt("step1", new PromptStepConfig("模板1"), "result");
            Step step2 = Step.prompt("step2", new PromptStepConfig("模板2"), "result");

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(step1, step2), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("S16"));
        }

        @Test
        @DisplayName("S16: 条件分支共享 varName 不应报错")
        void s16_conditionalBranchesWithSameVarNameShouldPass() {
            DefaultConditionParser parser = new DefaultConditionParser();

            WhenCondition whenTrue = new WhenCondition(
                    "{{confirm}} == true",
                    parser.parse("{{confirm}} == true"));
            WhenCondition whenFalse = new WhenCondition(
                    "{{confirm}} == false",
                    parser.parse("{{confirm}} == false"));

            // 两个步骤共享 varName "order_result"，但各有 when 条件（互斥分支）
            Step step1 = new Step("process_order", StepType.TEMPLATE,
                    new TemplateStepConfig("处理订单"), whenTrue, "order_result");
            Step step2 = new Step("cancel_order", StepType.TEMPLATE,
                    new TemplateStepConfig("取消订单"), whenFalse, "order_result");

            Map<String, FieldSpec> inputFields = new HashMap<String, FieldSpec>();
            inputFields.put("confirm", new FieldSpec("boolean", true, "确认"));

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null,
                    new InputSchema(inputFields),
                    Arrays.asList(step1, step2), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).noneMatch(i -> i.getCode().equals("S16"));
        }

        @Test
        @DisplayName("S16: 部分有条件部分无条件的重复 varName 应报错")
        void s16_mixedConditionalVarNameShouldFail() {
            DefaultConditionParser parser = new DefaultConditionParser();

            WhenCondition whenTrue = new WhenCondition(
                    "{{flag}} == true",
                    parser.parse("{{flag}} == true"));

            // step1 有 when 条件，step2 没有 → 冲突
            Step step1 = new Step("step1", StepType.TEMPLATE,
                    new TemplateStepConfig("模板1"), whenTrue, "result");
            Step step2 = Step.prompt("step2", new PromptStepConfig("模板2"), "result");

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(step1, step2), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("S16"));
        }
    }

    // ===== SCHEMA 类校验 =====

    @Nested
    @DisplayName("SCHEMA 校验")
    class SchemaTests {

        @Test
        @DisplayName("SC01: input field 类型缺失应报错")
        void sc01_inputFieldMissingTypeShouldFail() {
            Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
            fields.put("query", new FieldSpec(null, true, "描述"));

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null,
                    new InputSchema(fields),
                    Arrays.asList(createToolStep("step1")), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("SC01"));
        }

        @Test
        @DisplayName("SC02: 简写格式 input field 无描述应产生 SUGGESTION")
        void sc02_simpleFormatFieldMissingDescriptionShouldSuggest() {
            Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
            fields.put("query", FieldSpec.of("string"));

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null,
                    new InputSchema(fields),
                    Arrays.asList(createToolStep("step1")), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getSuggestions()).anyMatch(i -> i.getCode().equals("SC02"));
            assertThat(report.getWarnings()).noneMatch(i -> i.getCode().equals("SC02"));
        }

        @Test
        @DisplayName("SC02: 扩展格式 input field 无描述应产生 WARNING")
        void sc02_extendedFormatFieldMissingDescriptionShouldWarn() {
            Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
            // 扩展格式：有 label 但缺 description
            fields.put("query", new FieldSpec("string", true, null,
                    null, null, null, "text", "查询", null));

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null,
                    new InputSchema(fields),
                    Arrays.asList(createToolStep("step1")), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getWarnings()).anyMatch(i -> i.getCode().equals("SC02"));
            assertThat(report.getSuggestions()).noneMatch(i -> i.getCode().equals("SC02"));
        }

        @Test
        @DisplayName("SC05: tool step 缺少 output_schema 应报错")
        void sc05_toolStepMissingOutputSchemaShouldFail() {
            Map<String, Object> input = new HashMap<String, Object>();
            input.put("p", "v");
            ToolStepConfig config = new ToolStepConfig("tool", input); // 无 outputFields
            Step step = new Step("step1", StepType.TOOL, config, null, "result");

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(step), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("SC05"));
        }

        @Test
        @DisplayName("SC06: input field 类型不合法应产生 WARNING")
        void sc06_invalidFieldTypeShouldWarn() {
            Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
            fields.put("query", new FieldSpec("invalid_type", true, "desc"));

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null,
                    new InputSchema(fields),
                    Arrays.asList(createToolStep("step1")), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getWarnings()).anyMatch(i -> i.getCode().equals("SC06"));
        }
    }

    // ===== LOGIC 类校验 =====

    @Nested
    @DisplayName("LOGIC 校验")
    class LogicTests {

        @Test
        @DisplayName("L01: 循环依赖应报错")
        void l01_circularDependencyShouldFail() {
            Map<String, Object> input1 = new HashMap<String, Object>();
            input1.put("p", "{{step2.output}}");
            Step step1 = new Step("step1", StepType.TOOL,
                    new ToolStepConfig("tool1", input1, Arrays.asList("output")));

            Map<String, Object> input2 = new HashMap<String, Object>();
            input2.put("p", "{{step1.output}}");
            Step step2 = new Step("step2", StepType.TOOL,
                    new ToolStepConfig("tool2", input2, Arrays.asList("output")));

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(step1, step2), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("L01"));
        }

        @Test
        @DisplayName("L04: 未被使用的中间步骤应产生 SUGGESTION")
        void l04_unusedIntermediateStepShouldSuggest() {
            // step1 的输出未被 step2 引用
            Step step1 = createToolStep("step1");
            Step step2 = Step.prompt("step2", new PromptStepConfig("直接输出"), "output");

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(step1, step2), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getSuggestions()).anyMatch(i -> i.getCode().equals("L04"));
        }

        @Test
        @DisplayName("L03: when 条件引用 tool output_schema 变量不应产生 WARNING")
        void l03_whenConditionRefToolOutputVarShouldNotWarn() {
            DefaultConditionParser parser = new DefaultConditionParser();

            // tool 步骤声明了 output_schema 字段 target_achieved
            Map<String, Object> toolInput = new HashMap<String, Object>();
            toolInput.put("region", "华东");
            Step toolStep = Step.tool("fetch_data",
                    new ToolStepConfig("mock_tool", toolInput, Arrays.asList("target_achieved")));

            // 后续步骤的 when 条件引用 target_achieved
            WhenCondition when = new WhenCondition(
                    "{{target_achieved}} == true",
                    parser.parse("{{target_achieved}} == true"));
            Map<String, Object> logInput = new HashMap<String, Object>();
            logInput.put("msg", "success");
            Step condStep = new Step("show_message", StepType.TOOL,
                    new ToolStepConfig("log", logInput, Arrays.asList("logged")),
                    when);

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(toolStep, condStep), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getWarnings()).noneMatch(i ->
                    i.getCode().equals("L03") && i.getMessage().contains("target_achieved"));
        }

        @Test
        @DisplayName("L03: when 条件引用不存在的变量应产生 WARNING")
        void l03_whenConditionRefUndefinedVarShouldWarn() {
            DefaultConditionParser parser = new DefaultConditionParser();
            WhenCondition when = new WhenCondition(
                    "{{nonexistent}} != null",
                    parser.parse("{{nonexistent}} != null"));

            Map<String, Object> input = new HashMap<String, Object>();
            input.put("p", "v");
            Step step = new Step("step1", StepType.TOOL,
                    new ToolStepConfig("tool", input, Arrays.asList("r")),
                    when);

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(step), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getWarnings()).anyMatch(i -> i.getCode().equals("L03"));
        }
    }

    // ===== TOOL 类校验 =====

    @Nested
    @DisplayName("TOOL 校验")
    class ToolTests {

        @Test
        @DisplayName("T01: tool 步骤缺少 tool 名称会在构造时抛异常")
        void t01_toolStepWithoutToolNameThrowsAtConstruction() {
            // ToolStepConfig 构造函数会拒绝空 toolName
            // 所以 T01 只在极端情况下触发
            // 这里验证现有构造行为
            try {
                new ToolStepConfig("", new HashMap<String, Object>());
                assertThat(false).isTrue(); // 应该抛异常
            } catch (IllegalArgumentException e) {
                assertThat(e).isNotNull();
            }
        }
    }

    // ===== DATA_FLOW 类校验 =====

    @Nested
    @DisplayName("DATA_FLOW 校验")
    class DataFlowTests {

        @Test
        @DisplayName("D01: 引用不存在的变量应报错")
        void d01_unreachableVariableShouldFail() {
            Map<String, Object> input = new HashMap<String, Object>();
            input.put("p", "{{nonexistent.value}}");

            Step step = new Step("step1", StepType.TOOL,
                    new ToolStepConfig("tool", input, Arrays.asList("r")));

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(step), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("D01"));
        }

        @Test
        @DisplayName("D02: 自引用应报错")
        void d02_selfReferenceShouldFail() {
            Map<String, Object> input = new HashMap<String, Object>();
            input.put("p", "{{step1.output}}");

            Step step = new Step("step1", StepType.TOOL,
                    new ToolStepConfig("tool", input, Arrays.asList("r")));

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(step), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("D02"));
        }

        @Test
        @DisplayName("引用 input 字段应合法")
        void referenceToInputFieldShouldBeValid() {
            Map<String, FieldSpec> inputFields = new HashMap<String, FieldSpec>();
            inputFields.put("query", new FieldSpec("string", true, "查询内容"));

            Map<String, Object> input = new HashMap<String, Object>();
            input.put("p", "{{query}}");

            Step step = new Step("step1", StepType.TOOL,
                    new ToolStepConfig("tool", input, Arrays.asList("r")));

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null,
                    new InputSchema(inputFields),
                    Arrays.asList(step), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).noneMatch(i ->
                    i.getCode().equals("D01") && i.getMessage().contains("query"));
        }

        @Test
        @DisplayName("引用 context 变量应合法")
        void referenceToContextShouldBeValid() {
            Map<String, Object> input = new HashMap<String, Object>();
            input.put("p", "{{context.startTime}}");

            Step step = new Step("step1", StepType.TOOL,
                    new ToolStepConfig("tool", input, Arrays.asList("r")));

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(step), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).noneMatch(i -> i.getMessage().contains("context"));
        }

        @Test
        @DisplayName("引用前序步骤应合法")
        void referenceToPreviousStepShouldBeValid() {
            Step step1 = createToolStep("fetch_data");

            Map<String, Object> input2 = new HashMap<String, Object>();
            input2.put("p", "{{fetch_data.value}}");
            Step step2 = new Step("process", StepType.TOOL,
                    new ToolStepConfig("tool2", input2, Arrays.asList("r")));

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(step1, step2), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).noneMatch(i ->
                    i.getCode().equals("D01") && i.getMessage().contains("fetch_data"));
        }

        @Test
        @DisplayName("引用 await 步骤 input_schema 字段应合法")
        void referenceToAwaitInputFieldShouldBeValid() {
            // await 步骤定义了 confirm 和 notes 字段
            Map<String, FieldSpec> awaitFields = new LinkedHashMap<String, FieldSpec>();
            awaitFields.put("confirm", new FieldSpec("boolean", true, "确认"));
            awaitFields.put("notes", new FieldSpec("string", false, "备注"));
            InputSchema awaitSchema = new InputSchema(awaitFields);

            Step awaitStep = Step.await("user_confirmation",
                    new AwaitStepConfig("请确认", awaitSchema), "confirmation");

            // 后续步骤引用 await 的 input_schema 字段 notes
            Step processStep = Step.prompt("process_order",
                    new PromptStepConfig("备注: {{notes}}"), "order_result");

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(awaitStep, processStep), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).noneMatch(i ->
                    i.getCode().equals("D01") && i.getMessage().contains("notes"));
        }

        @Test
        @DisplayName("引用 tool 步骤 output_schema 字段应合法")
        void referenceToToolOutputFieldShouldBeValid() {
            // tool 步骤 output_schema 声明了 affectedRows
            Map<String, Object> toolInput = new HashMap<String, Object>();
            toolInput.put("table", "customers");
            Step toolStep = Step.tool("update_customer",
                    new ToolStepConfig("db_update", toolInput, Arrays.asList("affectedRows")),
                    null);

            // 后续 template 步骤引用 {{affectedRows}}
            Step templateStep = Step.template("build_result",
                    new TemplateStepConfig("影响 {{affectedRows}} 行"), "message");

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(toolStep, templateStep), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).noneMatch(i ->
                    i.getCode().equals("D01") && i.getMessage().contains("affectedRows"));
        }

        @Test
        @DisplayName("tool 步骤之前不应能引用其 output_schema 字段")
        void referenceToToolOutputFieldBeforeToolShouldFail() {
            // template 步骤在 tool 之前，引用 affectedRows
            Step templateStep = Step.template("show_result",
                    new TemplateStepConfig("影响 {{affectedRows}} 行"), "message");

            Map<String, Object> toolInput = new HashMap<String, Object>();
            toolInput.put("table", "customers");
            Step toolStep = Step.tool("update_customer",
                    new ToolStepConfig("db_update", toolInput, Arrays.asList("affectedRows")),
                    null);

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(templateStep, toolStep), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i ->
                    i.getCode().equals("D01") && i.getMessage().contains("affectedRows"));
        }

        @Test
        @DisplayName("await 步骤之前的步骤不应能引用 await input_schema 字段")
        void referenceToAwaitFieldBeforeAwaitShouldFail() {
            // process 步骤在 await 之前，引用 notes
            Step processStep = Step.prompt("process",
                    new PromptStepConfig("备注: {{notes}}"), "result");

            Map<String, FieldSpec> awaitFields = new LinkedHashMap<String, FieldSpec>();
            awaitFields.put("notes", new FieldSpec("string", false, "备注"));
            InputSchema awaitSchema = new InputSchema(awaitFields);

            Step awaitStep = Step.await("user_input",
                    new AwaitStepConfig("请输入", awaitSchema), "user_data");

            Skill skill = new Skill("test_skill", "1.0.0", "desc", null, null,
                    Arrays.asList(processStep, awaitStep), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            assertThat(report.getErrors()).anyMatch(i ->
                    i.getCode().equals("D01") && i.getMessage().contains("notes"));
        }
    }

    // ===== Markdown 校验 =====

    @Nested
    @DisplayName("Markdown 校验")
    class MarkdownTests {

        @Test
        @DisplayName("S12: null markdown 应报错")
        void s12_nullMarkdownShouldFail() {
            SkillValidationReport report = validator.validateMarkdown(null);

            assertThat(report.isValid()).isFalse();
            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("S12"));
            assertThat(report.getSummary()).isNull();
        }

        @Test
        @DisplayName("S12: 空 markdown 应报错")
        void s12_emptyMarkdownShouldFail() {
            SkillValidationReport report = validator.validateMarkdown("");

            assertThat(report.isValid()).isFalse();
            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("S12"));
        }

        @Test
        @DisplayName("S12: 无效 markdown 应报解析错误")
        void s12_invalidMarkdownShouldFail() {
            SkillValidationReport report = validator.validateMarkdown("这不是有效的 skill markdown");

            assertThat(report.isValid()).isFalse();
            assertThat(report.getErrors()).anyMatch(i -> i.getCode().equals("S12"));
        }
    }

    // ===== 便捷方法 =====

    @Nested
    @DisplayName("报告便捷方法")
    class ReportTests {

        @Test
        @DisplayName("getIssuesByCategory 应正确过滤")
        void getIssuesByCategoryShouldFilter() {
            Skill skill = new Skill("InvalidID", "abc", null, null, null,
                    Arrays.asList(createToolStep("step1")), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            List<ValidationIssue> syntaxIssues = report.getIssuesByCategory(ValidationCategory.SYNTAX);
            assertThat(syntaxIssues).isNotEmpty();
            assertThat(syntaxIssues).allMatch(i -> i.getCategory() == ValidationCategory.SYNTAX);
        }

        @Test
        @DisplayName("所有 issue 应包含 suggestion")
        void allIssuesShouldHaveSuggestion() {
            Skill skill = new Skill("InvalidID", null, null, null, null,
                    Arrays.asList(createToolStep("step1")), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            for (ValidationIssue issue : report.getIssues()) {
                assertThat(issue.getSuggestion())
                        .as("Issue %s should have a suggestion", issue.getCode())
                        .isNotNull()
                        .isNotEmpty();
            }
        }

        @Test
        @DisplayName("所有 issue 应包含 location")
        void allIssuesShouldHaveLocation() {
            Skill skill = new Skill("InvalidID", null, null, null, null,
                    Arrays.asList(createToolStep("step1")), OutputContract.text(), null);

            SkillValidationReport report = validator.validate(skill);

            for (ValidationIssue issue : report.getIssues()) {
                assertThat(issue.getLocation())
                        .as("Issue %s should have a location", issue.getCode())
                        .isNotNull()
                        .isNotEmpty();
            }
        }

        @Test
        @DisplayName("validationTimeMs 应大于等于 0")
        void validationTimeShouldBeNonNegative() {
            SkillValidationReport report = validator.validate(createValidSkill());

            assertThat(report.getValidationTimeMs()).isGreaterThanOrEqualTo(0);
        }
    }

    // ===== 辅助方法 =====

    private Skill createValidSkill() {
        Map<String, Object> inputTemplate = new HashMap<String, Object>();
        inputTemplate.put("param", "value");

        Step step = Step.tool("valid_step",
                new ToolStepConfig("tool", inputTemplate, Arrays.asList("result")));

        Map<String, FieldSpec> outputFields = new HashMap<String, FieldSpec>();
        outputFields.put("result", new FieldSpec("string", true, "结果"));
        OutputContract outputContract = OutputContract.json(outputFields);

        return new Skill("test_skill", "1.0.0", "测试技能", Arrays.asList("test"),
                null, Arrays.asList(step), outputContract, null);
    }

    private Skill createFullSkill() {
        // input schema
        Map<String, FieldSpec> inputFields = new HashMap<String, FieldSpec>();
        inputFields.put("query", new FieldSpec("string", true, "查询内容"));
        InputSchema inputSchema = new InputSchema(inputFields);

        // step 1: tool（工具输出通过 output_schema 的 data 字段暴露为顶级变量）
        Map<String, Object> toolInput = new HashMap<String, Object>();
        toolInput.put("keyword", "{{query}}");
        Step step1 = Step.tool("fetch_data",
                new ToolStepConfig("search_tool", toolInput, Arrays.asList("data")));

        // step 2: prompt（引用 tool 的 output_schema 字段 data）
        Step step2 = Step.prompt("analyze",
                new PromptStepConfig("请分析: {{data}}"),
                "analysis");

        // output
        Map<String, FieldSpec> outputFields = new HashMap<String, FieldSpec>();
        outputFields.put("answer", new FieldSpec("string", true, "分析结果"));
        OutputContract outputContract = OutputContract.json(outputFields);

        return new Skill("full_skill", "1.0.0", "完整技能",
                Arrays.asList("search", "analyze"),
                inputSchema, Arrays.asList(step1, step2), outputContract, null);
    }

    private Step createToolStep(String name) {
        Map<String, Object> inputTemplate = new HashMap<String, Object>();
        inputTemplate.put("param", "value");
        return Step.tool(name, new ToolStepConfig("tool", inputTemplate, Arrays.asList("result")));
    }

    private Step createToolStepWithPrompt(String name) {
        return Step.prompt(name, new PromptStepConfig("test template"), name + "_result");
    }
}
