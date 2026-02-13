package com.mia.aegis.skill.dsl.validator;

import com.mia.aegis.skill.dsl.condition.ast.BinaryExpression;
import com.mia.aegis.skill.dsl.condition.ast.BooleanLiteral;
import com.mia.aegis.skill.dsl.condition.ast.NullLiteral;
import com.mia.aegis.skill.dsl.condition.ast.NumberLiteral;
import com.mia.aegis.skill.dsl.condition.ast.StringLiteral;
import com.mia.aegis.skill.dsl.condition.ast.VariableReference;
import com.mia.aegis.skill.dsl.condition.parser.ConditionExpression;
import com.mia.aegis.skill.dsl.condition.parser.ConditionExpressionVisitor;
import com.mia.aegis.skill.dsl.condition.parser.ConditionParser;
import com.mia.aegis.skill.dsl.condition.parser.DefaultConditionParser;
import com.mia.aegis.skill.dsl.model.*;
import com.mia.aegis.skill.dsl.model.io.FieldSpec;
import com.mia.aegis.skill.dsl.model.io.InputSchema;
import com.mia.aegis.skill.dsl.model.io.OutputContract;
import com.mia.aegis.skill.dsl.parser.MarkdownSkillParser;
import com.mia.aegis.skill.dsl.parser.SkillParser;
import com.mia.aegis.skill.dsl.validator.report.*;
import com.mia.aegis.skill.exception.ConditionParseException;
import com.mia.aegis.skill.template.AegisTemplateRenderer;
import com.mia.aegis.skill.template.TemplateRenderer;
import com.mia.aegis.skill.tools.ToolProvider;
import com.mia.aegis.skill.tools.ToolRegistry;
import com.mia.aegis.skill.tools.ToolSchema;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 综合 Skill 校验器。
 *
 * <p>对 Skill Markdown 文件进行全面校验，输出结构化的 {@link SkillValidationReport}，
 * 包含错误、警告和建议，便于 LLM 迭代修正生成的 Skill 文件。</p>
 */
public class ComprehensiveSkillValidator {

    private static final Pattern SKILL_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*$");
    private static final Pattern STEP_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+(\\.\\d+)?$");
    private static final Set<String> VALID_FIELD_TYPES = new HashSet<String>(
            Arrays.asList("string", "number", "boolean", "object", "array"));

    private final SkillParser skillParser;
    private final TemplateRenderer templateRenderer;
    private final ToolRegistry toolRegistry;
    private final ConditionParser conditionParser;

    /**
     * 创建综合校验器（无 ToolRegistry）。
     */
    public ComprehensiveSkillValidator() {
        this(new MarkdownSkillParser(), new AegisTemplateRenderer(), null);
    }

    /**
     * 创建综合校验器。
     *
     * @param skillParser      Skill 解析器
     * @param templateRenderer 模板渲染器
     * @param toolRegistry     工具注册表（可选，为 null 时跳过 T02-T04 校验）
     */
    public ComprehensiveSkillValidator(SkillParser skillParser, TemplateRenderer templateRenderer,
                                       ToolRegistry toolRegistry) {
        this.skillParser = skillParser;
        this.templateRenderer = templateRenderer;
        this.toolRegistry = toolRegistry;
        this.conditionParser = new DefaultConditionParser();
    }

    /**
     * 从 Markdown 原文校验（包含解析阶段的错误捕获）。
     *
     * @param markdown Skill Markdown 原文
     * @return 校验报告
     */
    public SkillValidationReport validateMarkdown(String markdown) {
        long startTime = System.currentTimeMillis();

        if (markdown == null || markdown.trim().isEmpty()) {
            List<ValidationIssue> issues = new ArrayList<ValidationIssue>();
            issues.add(ValidationIssue.error("S12", ValidationCategory.SYNTAX,
                    "Markdown 内容为空",
                    "markdown",
                    "提供有效的 Skill Markdown 内容"));
            long elapsed = System.currentTimeMillis() - startTime;
            return SkillValidationReport.failure(null, issues, elapsed);
        }

        Skill skill;
        try {
            skill = skillParser.parse(markdown);
        } catch (Exception e) {
            // 解析失败时，执行结构预扫描以尽可能多地报告问题
            List<ValidationIssue> issues = new ArrayList<ValidationIssue>();
            issues.add(ValidationIssue.error("S12", ValidationCategory.SYNTAX,
                    "Markdown 解析失败: " + e.getMessage(),
                    "markdown",
                    "检查 Markdown 格式是否符合 Skill DSL 规范"));
            scanMarkdownStructure(markdown, issues);
            long elapsed = System.currentTimeMillis() - startTime;
            return SkillValidationReport.failure(null, issues, elapsed);
        }

        return doValidate(skill, startTime);
    }

    /**
     * 从已解析的 Skill 对象校验。
     *
     * @param skill Skill 对象
     * @return 校验报告
     */
    public SkillValidationReport validate(Skill skill) {
        long startTime = System.currentTimeMillis();

        if (skill == null) {
            List<ValidationIssue> issues = new ArrayList<ValidationIssue>();
            issues.add(ValidationIssue.error("S01", ValidationCategory.SYNTAX,
                    "Skill 对象为 null",
                    "skill",
                    "提供有效的 Skill 对象"));
            long elapsed = System.currentTimeMillis() - startTime;
            return SkillValidationReport.failure(null, issues, elapsed);
        }

        return doValidate(skill, startTime);
    }

    private SkillValidationReport doValidate(Skill skill, long startTime) {
        List<ValidationIssue> issues = new ArrayList<ValidationIssue>();

        // 收集上下文信息
        Set<String> stepNames = collectStepNames(skill.getSteps());
        Set<String> inputFields = collectInputFields(skill.getInputSchema());
        Set<String> varNames = collectVarNames(skill.getSteps());
        Set<String> awaitInputFields = collectAwaitInputFields(skill.getSteps());
        Set<String> toolOutputFields = collectToolOutputFields(skill.getSteps());

        // 按类别执行校验
        validateSyntax(skill, stepNames, inputFields, varNames, issues);
        validateSchema(skill, issues);
        validateLogic(skill, stepNames, inputFields, varNames, awaitInputFields, toolOutputFields, issues);
        validateTools(skill, issues);
        validateDataFlow(skill, stepNames, inputFields, varNames, issues);

        // 构建摘要
        SkillSummary summary = buildSummary(skill);

        long elapsed = System.currentTimeMillis() - startTime;

        boolean hasErrors = false;
        for (ValidationIssue issue : issues) {
            if (issue.getLevel() == ValidationLevel.ERROR) {
                hasErrors = true;
                break;
            }
        }

        if (hasErrors) {
            return SkillValidationReport.failure(summary, issues, elapsed);
        } else {
            return SkillValidationReport.success(summary, issues, elapsed);
        }
    }

    // ===== SYNTAX 校验 (S 系列) =====

    private void validateSyntax(Skill skill, Set<String> stepNames,
                                Set<String> inputFields, Set<String> varNames,
                                List<ValidationIssue> issues) {
        // S01 & S02: skill ID
        String id = skill.getId();
        if (id == null || id.isEmpty()) {
            issues.add(ValidationIssue.error("S01", ValidationCategory.SYNTAX,
                    "skill ID 缺失",
                    "skill.id",
                    "在 frontmatter 中添加 id 字段，或使用 H1 标题声明。示例：\n---\nid: my_skill\n---\n或：\n# skill: my_skill\n\nID 格式：小写字母开头，仅含小写字母、数字和下划线"));
        } else if (!SKILL_ID_PATTERN.matcher(id).matches()) {
            issues.add(ValidationIssue.error("S02", ValidationCategory.SYNTAX,
                    "skill ID 格式无效: '" + id + "'",
                    "skill.id",
                    "ID 应匹配 ^[a-z][a-z0-9_]*$，即小写字母开头，仅含小写字母、数字和下划线。示例：my_skill、order_query_v2"));
        }

        // S03 & S04: version
        String version = skill.getVersion();
        if (version == null || version.isEmpty()) {
            issues.add(ValidationIssue.error("S03", ValidationCategory.SYNTAX,
                    "version 缺失",
                    "skill.version",
                    "在 frontmatter 中添加 version，或使用 H2 章节声明。示例：\n---\nid: my_skill\nversion: 1.0.0\n---\n或：\n## version\n1.0.0"));
        } else if (!VERSION_PATTERN.matcher(version).matches()) {
            issues.add(ValidationIssue.error("S04", ValidationCategory.SYNTAX,
                    "version 格式无效: '" + version + "'",
                    "skill.version",
                    "version 应匹配 ^\\d+\\.\\d+(\\.\\d+)?$，如 '1.0' 或 '1.0.0'"));
        }

        // S05: steps 为空
        if (skill.getSteps() == null || skill.getSteps().isEmpty()) {
            issues.add(ValidationIssue.error("S05", ValidationCategory.SYNTAX,
                    "steps 为空，无执行步骤",
                    "skill.steps",
                    "在 ## steps 章节下添加至少一个步骤。示例：\n## steps\n\n### step: process\n**type**: prompt\n**varName**: result\n\n```prompt\n处理内容\n```"));
            return; // 没有步骤则跳过后续步骤级别校验
        }

        // S09: output_schema 缺失
        if (skill.getOutputContract() == null || skill.getOutputContract().isEmpty()) {
            issues.add(ValidationIssue.error("S09", ValidationCategory.SYNTAX,
                    "output_schema 缺失",
                    "skill.output_schema",
                    "添加 ## output 章节声明技能输出结构。示例：\n## output\n```yaml\nresult:\n  type: string\n  description: \"输出结果\"\n```"));
        }

        // S10: description 缺失
        if (skill.getDescription() == null || skill.getDescription().isEmpty()) {
            issues.add(ValidationIssue.warning("S10", ValidationCategory.SYNTAX,
                    "description 缺失",
                    "skill.description",
                    "添加 ## description 章节描述技能的功能。示例：\n## description\n该技能用于处理用户查询并返回结果"));
        }

        // S11: intents 缺失
        if (skill.getIntents() == null || skill.getIntents().isEmpty()) {
            issues.add(ValidationIssue.suggestion("S11", ValidationCategory.SYNTAX,
                    "intents 缺失",
                    "skill.intents",
                    "添加 ## intents 章节声明意图标签，用于技能发现和路由。示例：\n## intents\n- 查询订单\n- 订单状态\n- order inquiry"));
        }

        // 校验每个步骤的语法
        Set<String> seenNames = new HashSet<String>();
        Map<String, String> varNameToStep = new LinkedHashMap<String, String>();
        // 跟踪同一 varName 的所有步骤是否都有 when 条件（用于条件分支共享 varName 的场景）
        Map<String, Boolean> varNameAllConditional = new LinkedHashMap<String, Boolean>();

        for (Step step : skill.getSteps()) {
            String name = step.getName();

            // S06: step 名称格式无效
            if (!STEP_NAME_PATTERN.matcher(name).matches()) {
                issues.add(ValidationIssue.error("S06", ValidationCategory.SYNTAX,
                        "step 名称格式无效: '" + name + "'",
                        "step:" + name,
                        "step 名称应匹配 ^[a-z][a-z0-9_]*$，即小写字母开头，仅含小写字母、数字和下划线。示例：### step: fetch_data"));
            }

            // S07: step 名称重复
            if (seenNames.contains(name)) {
                issues.add(ValidationIssue.error("S07", ValidationCategory.SYNTAX,
                        "step 名称重复: '" + name + "'",
                        "step:" + name,
                        "为步骤使用唯一名称"));
            }
            seenNames.add(name);

            // S08: step 类型缺失
            if (step.getType() == null) {
                issues.add(ValidationIssue.error("S08", ValidationCategory.SYNTAX,
                        "step 类型缺失",
                        "step:" + name,
                        "在步骤下方添加 **type** 声明。可选值：tool、prompt、template、await。示例：\n### step: " + name + "\n**type**: prompt"));
            }

            // S17: PROMPT/TEMPLATE 步骤缺少 varName
            if (step.getType() == StepType.PROMPT || step.getType() == StepType.TEMPLATE) {
                if (!step.hasVarName()) {
                    issues.add(ValidationIssue.error("S17", ValidationCategory.SYNTAX,
                            "PROMPT/TEMPLATE 步骤 '" + name + "' 缺少 varName",
                            "step:" + name,
                            "为 PROMPT/TEMPLATE 步骤添加 **varName** 声明输出变量名，后续步骤通过 {{varName}} 引用。示例：\n### step: " + name + "\n**type**: " + (step.getType() != null ? step.getType().name().toLowerCase() : "prompt") + "\n**varName**: result"));
                }
            }

            // S18: TOOL 步骤不应使用 varName
            // 执行引擎显式跳过 Tool 步骤的 varName 注册，工具输出通过 ToolOutputContext 直接写入
            if (step.getType() == StepType.TOOL && step.hasVarName()) {
                issues.add(ValidationIssue.error("S18", ValidationCategory.SYNTAX,
                        "tool 步骤 '" + name + "' 不应使用 varName",
                        "step:" + name + ".varName",
                        "移除 varName，工具输出通过 output_schema 声明，执行引擎会忽略 tool 步骤的 varName"));
            }

            // varName 相关校验
            if (step.hasVarName()) {
                String varName = step.getVarName();

                // S13: varName 格式无效
                if (!STEP_NAME_PATTERN.matcher(varName).matches()) {
                    issues.add(ValidationIssue.error("S13", ValidationCategory.SYNTAX,
                            "varName 格式无效: '" + varName + "'",
                            "step:" + name + ".varName",
                            "varName 应匹配 ^[a-z][a-z0-9_]*$"));
                }

                // S14: varName 与 stepName 冲突
                if (stepNames.contains(varName)) {
                    issues.add(ValidationIssue.error("S14", ValidationCategory.SYNTAX,
                            "varName '" + varName + "' 与 stepName 冲突",
                            "step:" + name + ".varName",
                            "使用不同于任何 step 名称的 varName"));
                }

                // S15: varName 与 inputField 冲突
                if (inputFields.contains(varName)) {
                    issues.add(ValidationIssue.error("S15", ValidationCategory.SYNTAX,
                            "varName '" + varName + "' 与 input field 冲突",
                            "step:" + name + ".varName",
                            "使用不同于任何 input field 名称的 varName"));
                }

                // S16: varName 重复 — 允许条件分支共享 varName
                // 当所有共享同一 varName 的步骤都有 when 条件时，视为合法的条件分支
                if (varNameToStep.containsKey(varName)) {
                    boolean prevAllConditional = varNameAllConditional.get(varName);
                    boolean currentConditional = step.hasWhenCondition();

                    if (!prevAllConditional || !currentConditional) {
                        issues.add(ValidationIssue.error("S16", ValidationCategory.SYNTAX,
                                "varName '" + varName + "' 重复（已被步骤 '" + varNameToStep.get(varName) + "' 使用）",
                                "step:" + name + ".varName",
                                "为每个步骤使用唯一的 varName，或确保所有共享 varName 的步骤都有 when 条件"));
                    }
                    varNameAllConditional.put(varName, prevAllConditional && currentConditional);
                } else {
                    varNameAllConditional.put(varName, step.hasWhenCondition());
                }
                varNameToStep.put(varName, name);
            }
        }
    }

    // ===== SCHEMA 校验 (SC 系列) =====

    private void validateSchema(Skill skill, List<ValidationIssue> issues) {
        // 校验 input schema
        InputSchema inputSchema = skill.getInputSchema();
        if (inputSchema != null && !inputSchema.isEmpty()) {
            for (Map.Entry<String, FieldSpec> entry : inputSchema.getFields().entrySet()) {
                String fieldName = entry.getKey();
                FieldSpec spec = entry.getValue();

                // SC01: input field 类型缺失
                if (spec.getType() == null || spec.getType().isEmpty()) {
                    issues.add(ValidationIssue.error("SC01", ValidationCategory.SCHEMA,
                            "input field '" + fieldName + "' 类型缺失",
                            "input_schema:" + fieldName,
                            "为字段指定类型，可选值：string、number、boolean、object、array。简写格式：\n" + fieldName + ": string\n或扩展格式：\n" + fieldName + ":\n  type: string\n  description: \"字段说明\""));
                } else {
                    // SC06: input field 类型不合法
                    if (!VALID_FIELD_TYPES.contains(spec.getType())) {
                        issues.add(ValidationIssue.warning("SC06", ValidationCategory.SCHEMA,
                                "input field '" + fieldName + "' 类型不合法: '" + spec.getType() + "'",
                                "input_schema:" + fieldName,
                                "使用标准类型: string/number/boolean/object/array"));
                    }
                }

                // SC02: input field 无描述
                // 简写格式（如 field: string）降级为 SUGGESTION，扩展格式缺 description 保持 WARNING
                if (spec.getDescription() == null || spec.getDescription().isEmpty()) {
                    if (isSimpleFormatField(spec)) {
                        issues.add(ValidationIssue.suggestion("SC02", ValidationCategory.SCHEMA,
                                "input field '" + fieldName + "' 无描述",
                                "input_schema:" + fieldName,
                                "建议使用扩展格式并添加 description。示例：\n" + fieldName + ":\n  type: " + (spec.getType() != null ? spec.getType() : "string") + "\n  description: \"字段说明\""));
                    } else {
                        issues.add(ValidationIssue.warning("SC02", ValidationCategory.SCHEMA,
                                "input field '" + fieldName + "' 无描述",
                                "input_schema:" + fieldName,
                                "添加 description 说明字段用途。示例：\n" + fieldName + ":\n  type: " + (spec.getType() != null ? spec.getType() : "string") + "\n  description: \"字段说明\""));
                    }
                }
            }
        }

        // 校验 output schema
        OutputContract outputContract = skill.getOutputContract();
        if (outputContract != null && !outputContract.isEmpty()) {
            for (Map.Entry<String, FieldSpec> entry : outputContract.getFields().entrySet()) {
                String fieldName = entry.getKey();
                FieldSpec spec = entry.getValue();

                // SC03: output field 类型缺失
                if (spec.getType() == null || spec.getType().isEmpty()) {
                    issues.add(ValidationIssue.error("SC03", ValidationCategory.SCHEMA,
                            "output field '" + fieldName + "' 类型缺失",
                            "output_schema:" + fieldName,
                            "为字段指定类型: string/number/boolean/object/array"));
                }

                // SC04: output field 无描述
                if (spec.getDescription() == null || spec.getDescription().isEmpty()) {
                    issues.add(ValidationIssue.warning("SC04", ValidationCategory.SCHEMA,
                            "output field '" + fieldName + "' 无描述",
                            "output_schema:" + fieldName,
                            "添加 description 说明字段用途"));
                }
            }
        }

        // SC05: tool step 缺少 output_schema
        if (skill.getSteps() != null) {
            for (Step step : skill.getSteps()) {
                if (step.getType() == StepType.TOOL) {
                    ToolStepConfig config = step.getToolConfig();
                    if (config.getOutputFields() == null || config.getOutputFields().isEmpty()) {
                        issues.add(ValidationIssue.error("SC05", ValidationCategory.SCHEMA,
                                "tool 步骤 '" + step.getName() + "' 缺少 output_schema",
                                "step:" + step.getName() + ".output_schema",
                                "在 tool 步骤的 yaml 代码块中添加 output_schema。示例：\n```yaml\ninput:\n  param: \"{{value}}\"\noutput_schema:\n  result: string\n```"));
                    }
                }
            }
        }
    }

    // ===== LOGIC 校验 (L 系列) =====

    private void validateLogic(Skill skill, Set<String> stepNames,
                               Set<String> inputFields, Set<String> varNames,
                               Set<String> awaitInputFields, Set<String> toolOutputFields,
                               List<ValidationIssue> issues) {
        if (skill.getSteps() == null || skill.getSteps().isEmpty()) {
            return;
        }

        // L01: 循环依赖
        detectCircularDependencies(skill.getSteps(), issues);

        for (Step step : skill.getSteps()) {
            // L02 & L03: when 条件校验
            if (step.hasWhenCondition()) {
                validateWhenCondition(step, stepNames, inputFields, varNames, awaitInputFields, toolOutputFields, issues);
            }

            // L05: prompt 步骤缺少 template
            if (step.getType() == StepType.PROMPT) {
                PromptStepConfig config = step.getPromptConfig();
                if (config.getTemplate() == null || config.getTemplate().isEmpty()) {
                    issues.add(ValidationIssue.error("L05", ValidationCategory.LOGIC,
                            "prompt 步骤 '" + step.getName() + "' 缺少 template",
                            "step:" + step.getName() + ".template",
                            "在步骤下方添加 prompt 代码块作为 LLM 提示词模板。示例：\n```prompt\n请根据 {{query}} 生成回答\n```"));
                }
            }

            // L06: template 步骤缺少 template
            if (step.getType() == StepType.TEMPLATE) {
                TemplateStepConfig config = step.getTemplateConfig();
                if (config.getTemplate() == null || config.getTemplate().isEmpty()) {
                    issues.add(ValidationIssue.error("L06", ValidationCategory.LOGIC,
                            "template 步骤 '" + step.getName() + "' 缺少 template",
                            "step:" + step.getName() + ".template",
                            "在步骤下方添加 template 代码块作为文本渲染模板（不经过 LLM）。示例：\n```template\n查询结果：{{result}}\n```"));
                }
            }

            // L07: await 步骤缺少 message 或 inputSchema
            if (step.getType() == StepType.AWAIT) {
                AwaitStepConfig config = step.getAwaitConfig();
                if (config.getMessage() == null || config.getMessage().isEmpty()) {
                    issues.add(ValidationIssue.error("L07", ValidationCategory.LOGIC,
                            "await 步骤 '" + step.getName() + "' 缺少 message",
                            "step:" + step.getName() + ".message",
                            "在 await 步骤的 yaml 代码块中添加 message 字段。示例：\n```yaml\nmessage: \"请确认以下信息\"\ninput_schema:\n  confirm:\n    type: boolean\n```"));
                }
                if (config.getInputSchema() == null || config.getInputSchema().isEmpty()) {
                    issues.add(ValidationIssue.error("L07", ValidationCategory.LOGIC,
                            "await 步骤 '" + step.getName() + "' 缺少 inputSchema",
                            "step:" + step.getName() + ".inputSchema",
                            "在 await 步骤的 yaml 代码块中添加 input_schema 定义用户输入结构。示例：\n```yaml\nmessage: \"请输入信息\"\ninput_schema:\n  user_input:\n    type: string\n    description: \"用户输入\"\n```"));
                }
            }
        }

        // L04: 存在无依赖的中间步骤（输出未被后续步骤使用）
        detectUnusedIntermediateSteps(skill, stepNames, inputFields, varNames, issues);
    }

    private void validateWhenCondition(Step step, Set<String> stepNames,
                                       Set<String> inputFields, Set<String> varNames,
                                       Set<String> awaitInputFields, Set<String> toolOutputFields,
                                       List<ValidationIssue> issues) {
        WhenCondition when = step.getWhenCondition();
        String rawExpr = when.getRawExpression();

        // L02: 尝试再次解析表达式以校验语法
        // 注意：WhenCondition 已持有 parsedExpression，但这里再检查一次以防万一
        try {
            conditionParser.validate(rawExpr);
        } catch (ConditionParseException e) {
            issues.add(ValidationIssue.warning("L02", ValidationCategory.LOGIC,
                    "when 条件表达式解析失败: " + e.getMessage(),
                    "step:" + step.getName() + ".when",
                    "检查条件表达式语法，支持 {{var}} == / != / && / || 操作符"));
            return;
        }

        // L03: 检查 when 条件引用的变量是否存在
        List<String> conditionVars = extractVariablesFromCondition(when.getParsedExpression());
        for (String varPath : conditionVars) {
            String rootVar = varPath.contains(".") ? varPath.split("\\.")[0] : varPath;

            // 检查是否是 context.* 引用
            if (rootVar.equals("context")) {
                continue;
            }

            boolean found = inputFields.contains(rootVar)
                    || stepNames.contains(rootVar)
                    || varNames.contains(rootVar)
                    || awaitInputFields.contains(rootVar)
                    || toolOutputFields.contains(rootVar);

            if (!found) {
                issues.add(ValidationIssue.warning("L03", ValidationCategory.LOGIC,
                        "when 条件引用的变量 '" + varPath + "' 不存在",
                        "step:" + step.getName() + ".when",
                        "确保引用的变量已在 input_schema 或前序步骤中定义"));
            }
        }
    }

    private List<String> extractVariablesFromCondition(ConditionExpression expr) {
        final List<String> variables = new ArrayList<String>();
        expr.accept(new ConditionExpressionVisitor<Void>() {
            @Override
            public Void visitBinary(BinaryExpression expr) {
                expr.getLeft().accept(this);
                expr.getRight().accept(this);
                return null;
            }

            @Override
            public Void visitVariable(VariableReference expr) {
                variables.add(expr.getPath());
                return null;
            }

            @Override
            public Void visitNull(NullLiteral expr) {
                return null;
            }

            @Override
            public Void visitBoolean(BooleanLiteral expr) {
                return null;
            }

            @Override
            public Void visitString(StringLiteral expr) {
                return null;
            }

            @Override
            public Void visitNumber(NumberLiteral expr) {
                return null;
            }
        });
        return variables;
    }

    private void detectUnusedIntermediateSteps(Skill skill, Set<String> stepNames,
                                                Set<String> inputFields, Set<String> varNames,
                                                List<ValidationIssue> issues) {
        List<Step> steps = skill.getSteps();
        if (steps.size() <= 1) {
            return;
        }

        // 收集所有被引用的步骤名和变量名
        Set<String> referencedNames = new HashSet<String>();

        for (Step step : steps) {
            collectReferencedNames(step, referencedNames);
        }

        // 检查中间步骤（非最后一步）是否被引用
        for (int i = 0; i < steps.size() - 1; i++) {
            Step step = steps.get(i);
            String name = step.getName();
            String varName = step.getVarName();

            boolean isReferenced = referencedNames.contains(name);
            if (varName != null) {
                isReferenced = isReferenced || referencedNames.contains(varName);
            }

            if (!isReferenced) {
                issues.add(ValidationIssue.suggestion("L04", ValidationCategory.LOGIC,
                        "中间步骤 '" + name + "' 的输出未被后续步骤使用",
                        "step:" + name,
                        "考虑是否需要此步骤，或在后续步骤中引用其输出"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void collectReferencedNames(Step step, Set<String> referencedNames) {
        switch (step.getType()) {
            case TOOL:
                collectRefsFromObject(step.getToolConfig().getInputTemplate(), referencedNames);
                break;
            case PROMPT:
                collectRefsFromTemplate(step.getPromptConfig().getTemplate(), referencedNames);
                break;
            case TEMPLATE:
                collectRefsFromTemplate(step.getTemplateConfig().getTemplate(), referencedNames);
                break;
            default:
                break;
        }

        // 也检查 when 条件中的引用
        if (step.hasWhenCondition()) {
            List<String> condVars = extractVariablesFromCondition(step.getWhenCondition().getParsedExpression());
            for (String v : condVars) {
                String root = v.contains(".") ? v.split("\\.")[0] : v;
                referencedNames.add(root);
            }
        }
    }

    private void collectRefsFromTemplate(String template, Set<String> refs) {
        if (template == null || template.isEmpty()) {
            return;
        }
        List<String> vars = templateRenderer.extractVariables(template);
        for (String v : vars) {
            String root = v.contains(".") ? v.split("\\.")[0] : v;
            refs.add(root);
        }
    }

    @SuppressWarnings("unchecked")
    private void collectRefsFromObject(Object obj, Set<String> refs) {
        if (obj == null) return;
        if (obj instanceof String) {
            collectRefsFromTemplate((String) obj, refs);
        } else if (obj instanceof Map) {
            for (Object value : ((Map<String, Object>) obj).values()) {
                collectRefsFromObject(value, refs);
            }
        } else if (obj instanceof List) {
            for (Object item : (List<Object>) obj) {
                collectRefsFromObject(item, refs);
            }
        }
    }

    private void detectCircularDependencies(List<Step> steps, List<ValidationIssue> issues) {
        // 构建依赖图
        Map<String, Set<String>> dependencies = new LinkedHashMap<String, Set<String>>();
        Set<String> knownSteps = new HashSet<String>();
        for (Step step : steps) {
            knownSteps.add(step.getName());
        }

        for (Step step : steps) {
            Set<String> deps = new LinkedHashSet<String>();
            collectStepDependencies(step, deps);
            // 只保留已知步骤的依赖
            deps.retainAll(knownSteps);
            dependencies.put(step.getName(), deps);
        }

        // DFS 检测循环
        for (String stepName : dependencies.keySet()) {
            List<String> path = new ArrayList<String>();
            if (detectCycle(stepName, dependencies, new HashSet<String>(), path)) {
                StringBuilder cycle = new StringBuilder();
                boolean inCycle = false;
                for (String p : path) {
                    if (p.equals(stepName)) {
                        inCycle = true;
                    }
                    if (inCycle) {
                        if (cycle.length() > 0) {
                            cycle.append(" -> ");
                        }
                        cycle.append(p);
                    }
                }
                cycle.append(" -> ").append(stepName);
                issues.add(ValidationIssue.error("L01", ValidationCategory.LOGIC,
                        "循环依赖: " + cycle.toString(),
                        "steps",
                        "重新组织步骤间的引用关系以消除循环"));
                break; // 只报告一个循环
            }
        }
    }

    private void collectStepDependencies(Step step, Set<String> deps) {
        switch (step.getType()) {
            case TOOL:
                collectDepsFromObject(step.getToolConfig().getInputTemplate(), deps);
                break;
            case PROMPT:
                collectDepsFromTemplate(step.getPromptConfig().getTemplate(), deps);
                break;
            case TEMPLATE:
                collectDepsFromTemplate(step.getTemplateConfig().getTemplate(), deps);
                break;
            default:
                break;
        }
    }

    private void collectDepsFromTemplate(String template, Set<String> deps) {
        if (template == null) return;
        List<String> vars = templateRenderer.extractVariables(template);
        for (String v : vars) {
            String root = v.contains(".") ? v.split("\\.")[0] : v;
            deps.add(root);
        }
    }

    @SuppressWarnings("unchecked")
    private void collectDepsFromObject(Object obj, Set<String> deps) {
        if (obj == null) return;
        if (obj instanceof String) {
            collectDepsFromTemplate((String) obj, deps);
        } else if (obj instanceof Map) {
            for (Object value : ((Map<String, Object>) obj).values()) {
                collectDepsFromObject(value, deps);
            }
        } else if (obj instanceof List) {
            for (Object item : (List<Object>) obj) {
                collectDepsFromObject(item, deps);
            }
        }
    }

    private boolean detectCycle(String start, Map<String, Set<String>> graph,
                                Set<String> visiting, List<String> path) {
        if (visiting.contains(start)) {
            return true;
        }
        visiting.add(start);
        path.add(start);

        Set<String> deps = graph.get(start);
        if (deps != null) {
            for (String dep : deps) {
                if (graph.containsKey(dep)) {
                    if (detectCycle(dep, graph, visiting, path)) {
                        return true;
                    }
                }
            }
        }

        visiting.remove(start);
        path.remove(path.size() - 1);
        return false;
    }

    // ===== TOOL 校验 (T 系列) =====

    private void validateTools(Skill skill, List<ValidationIssue> issues) {
        if (skill.getSteps() == null) return;

        for (Step step : skill.getSteps()) {
            if (step.getType() != StepType.TOOL) continue;

            ToolStepConfig config = step.getToolConfig();

            // T01: tool 步骤缺少 tool 名称
            if (config.getToolName() == null || config.getToolName().isEmpty()) {
                issues.add(ValidationIssue.error("T01", ValidationCategory.TOOL,
                        "tool 步骤 '" + step.getName() + "' 缺少 tool 名称",
                        "step:" + step.getName() + ".tool",
                        "在步骤下方添加 **tool** 声明工具名称。示例：\n### step: " + step.getName() + "\n**type**: tool\n**tool**: tool_name"));
                continue;
            }

            // T02-T04 需要 ToolRegistry
            if (toolRegistry == null) continue;

            String toolName = config.getToolName();
            java.util.Optional<ToolProvider> providerOpt = toolRegistry.find(toolName);

            // T02: 引用的 tool 未注册
            if (!providerOpt.isPresent()) {
                issues.add(ValidationIssue.warning("T02", ValidationCategory.TOOL,
                        "引用的 tool '" + toolName + "' 未注册",
                        "step:" + step.getName() + ".tool",
                        "确认 tool 名称拼写正确或已在系统中注册"));
                continue;
            }

            ToolProvider provider = providerOpt.get();
            ToolSchema inputSchema = provider.getInputSchema();
            if (inputSchema == null) continue;

            Map<String, Object> inputTemplate = config.getInputTemplate();

            // T03: tool input 缺少 required 参数
            for (Map.Entry<String, ToolSchema.ParameterSpec> paramEntry : inputSchema.getParameters().entrySet()) {
                String paramName = paramEntry.getKey();
                ToolSchema.ParameterSpec paramSpec = paramEntry.getValue();

                if (paramSpec.isRequired() && !inputTemplate.containsKey(paramName)) {
                    issues.add(ValidationIssue.warning("T03", ValidationCategory.TOOL,
                            "tool 步骤 '" + step.getName() + "' 缺少 required 参数 '" + paramName + "'",
                            "step:" + step.getName() + ".input." + paramName,
                            "在 input 中添加 '" + paramName + "' 参数"));
                }
            }

            // T04: tool input 存在未知参数
            for (String inputKey : inputTemplate.keySet()) {
                if (!inputSchema.hasParameter(inputKey)) {
                    // 检查是否引用了可选的 skill input field
                    if (isOptionalInputReference(inputKey, inputTemplate.get(inputKey), skill.getInputSchema())) {
                        issues.add(ValidationIssue.warning("T04", ValidationCategory.TOOL,
                                "tool 步骤 '" + step.getName() + "' 输入参数 '" + inputKey + "' 存在为空的可能",
                                "step:" + step.getName() + ".input." + inputKey,
                                "该参数引用了可选输入字段，建议增加 await 步骤在参数为空时获取用户输入"));
                    } else {
                        issues.add(ValidationIssue.warning("T04", ValidationCategory.TOOL,
                                "tool 步骤 '" + step.getName() + "' 存在未知参数 '" + inputKey + "'",
                                "step:" + step.getName() + ".input." + inputKey,
                                "移除未知参数或确认 tool 的 schema 定义"));
                    }
                }
            }
        }
    }

    // ===== DATA_FLOW 校验 (D 系列) =====

    private void validateDataFlow(Skill skill, Set<String> stepNames,
                                  Set<String> inputFields, Set<String> varNames,
                                  List<ValidationIssue> issues) {
        if (skill.getSteps() == null) return;

        // 按步骤顺序构建"可见变量"集合（步骤只能引用前序步骤的输出）
        Set<String> visibleSteps = new LinkedHashSet<String>();
        Set<String> visibleVars = new LinkedHashSet<String>();

        for (Step step : skill.getSteps()) {
            // 校验当前步骤引用的变量
            validateStepDataFlow(step, step.getName(), inputFields, visibleSteps, visibleVars, issues);

            // 当前步骤加入可见范围
            visibleSteps.add(step.getName());
            if (step.hasVarName()) {
                visibleVars.add(step.getVarName());
            }

            // tool 步骤的 output_schema 字段通过 ToolOutputContext.put() 写入为顶级变量，对后续步骤可见
            if (step.getType() == StepType.TOOL) {
                ToolStepConfig toolConfig = step.getToolConfig();
                if (toolConfig.getOutputFields() != null) {
                    for (String fieldName : toolConfig.getOutputFields()) {
                        visibleVars.add(fieldName);
                    }
                }
            }

            // await 步骤的 input_schema 字段在执行后由用户提供，对后续步骤可见
            if (step.getType() == StepType.AWAIT) {
                AwaitStepConfig awaitConfig = step.getAwaitConfig();
                if (awaitConfig.getInputSchema() != null && !awaitConfig.getInputSchema().isEmpty()) {
                    for (String fieldName : awaitConfig.getInputSchema().getFieldNames()) {
                        visibleVars.add(fieldName);
                    }
                }
            }
        }
    }

    private void validateStepDataFlow(Step step, String stepName,
                                      Set<String> inputFields,
                                      Set<String> visibleSteps, Set<String> visibleVars,
                                      List<ValidationIssue> issues) {
        List<String> variables = new ArrayList<String>();

        switch (step.getType()) {
            case TOOL:
                collectVariablesFromObject(step.getToolConfig().getInputTemplate(), variables);
                break;
            case PROMPT:
                variables.addAll(templateRenderer.extractVariables(
                        step.getPromptConfig().getTemplate() != null ? step.getPromptConfig().getTemplate() : ""));
                break;
            case TEMPLATE:
                variables.addAll(templateRenderer.extractVariables(
                        step.getTemplateConfig().getTemplate() != null ? step.getTemplateConfig().getTemplate() : ""));
                break;
            default:
                break;
        }

        for (String variable : variables) {
            // 跳过 context.* 引用
            if (variable.startsWith("context.")) {
                continue;
            }

            String rootVar = variable.contains(".") ? variable.split("\\.")[0] : variable;

            // D02: 自引用
            if (rootVar.equals(stepName)) {
                issues.add(ValidationIssue.error("D02", ValidationCategory.DATA_FLOW,
                        "步骤 '" + stepName + "' 自引用变量 '" + variable + "'",
                        "step:" + stepName,
                        "步骤不能引用自身输出，请引用前序步骤或输入变量"));
                continue;
            }

            // D01: 变量引用不可达
            boolean isInput = inputFields.contains(rootVar);
            boolean isVisibleStep = visibleSteps.contains(rootVar);
            boolean isVisibleVar = visibleVars.contains(rootVar);

            if (!isInput && !isVisibleStep && !isVisibleVar) {
                issues.add(ValidationIssue.error("D01", ValidationCategory.DATA_FLOW,
                        "步骤 '" + stepName + "' 引用的变量 '" + variable + "' 不可达",
                        "step:" + stepName,
                        "确保引用的变量已在 input_schema 中定义或由前序步骤产生"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void collectVariablesFromObject(Object obj, List<String> variables) {
        if (obj == null) return;
        if (obj instanceof String) {
            variables.addAll(templateRenderer.extractVariables((String) obj));
        } else if (obj instanceof Map) {
            for (Object value : ((Map<String, Object>) obj).values()) {
                collectVariablesFromObject(value, variables);
            }
        } else if (obj instanceof List) {
            for (Object item : (List<Object>) obj) {
                collectVariablesFromObject(item, variables);
            }
        }
    }

    // ===== 辅助方法 =====

    private SkillSummary buildSummary(Skill skill) {
        List<String> stepTypes = new ArrayList<String>();
        boolean hasConditional = false;
        boolean hasAwait = false;

        if (skill.getSteps() != null) {
            for (Step step : skill.getSteps()) {
                stepTypes.add(step.getType() != null ? step.getType().name() : "UNKNOWN");
                if (step.hasWhenCondition()) {
                    hasConditional = true;
                }
                if (step.getType() == StepType.AWAIT) {
                    hasAwait = true;
                }
            }
        }

        int inputFieldCount = 0;
        if (skill.getInputSchema() != null) {
            inputFieldCount = skill.getInputSchema().getFields().size();
        }

        int outputFieldCount = 0;
        if (skill.getOutputContract() != null) {
            outputFieldCount = skill.getOutputContract().getFields().size();
        }

        return new SkillSummary(
                skill.getId(),
                skill.getVersion(),
                skill.getDescription(),
                skill.getSteps() != null ? skill.getSteps().size() : 0,
                stepTypes,
                inputFieldCount,
                outputFieldCount,
                hasConditional,
                hasAwait
        );
    }

    private Set<String> collectStepNames(List<Step> steps) {
        Set<String> names = new LinkedHashSet<String>();
        if (steps != null) {
            for (Step step : steps) {
                names.add(step.getName());
            }
        }
        return names;
    }

    private Set<String> collectInputFields(InputSchema schema) {
        if (schema == null || schema.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<String>(schema.getFieldNames());
    }

    private Set<String> collectVarNames(List<Step> steps) {
        Set<String> varNames = new LinkedHashSet<String>();
        if (steps != null) {
            for (Step step : steps) {
                if (step.hasVarName()) {
                    varNames.add(step.getVarName());
                }
            }
        }
        return varNames;
    }

    /**
     * 检查 tool 步骤的输入参数是否引用了 skill 的可选 input field。
     *
     * <p>当参数名或参数值中的模板变量匹配一个 required=false 的 input field 时返回 true。</p>
     */
    private boolean isOptionalInputReference(String paramName, Object paramValue, InputSchema skillInputSchema) {
        if (skillInputSchema == null || skillInputSchema.isEmpty()) {
            return false;
        }

        Map<String, FieldSpec> fields = skillInputSchema.getFields();

        // 检查参数名是否直接匹配可选 input field
        FieldSpec field = fields.get(paramName);
        if (field != null && !field.isRequired()) {
            return true;
        }

        // 检查参数值是否是对可选 input field 的模板引用（如 "{{region}}"）
        if (paramValue instanceof String) {
            String strValue = ((String) paramValue).trim();
            if (strValue.matches("^\\{\\{\\s*\\w+\\s*\\}\\}$")) {
                String varName = strValue.replaceAll("^\\{\\{\\s*", "").replaceAll("\\s*\\}\\}$", "");
                FieldSpec refField = fields.get(varName);
                if (refField != null && !refField.isRequired()) {
                    return true;
                }
            }
        }

        return false;
    }

    private Set<String> collectToolOutputFields(List<Step> steps) {
        Set<String> fields = new LinkedHashSet<String>();
        if (steps != null) {
            for (Step step : steps) {
                if (step.getType() == StepType.TOOL) {
                    ToolStepConfig config = step.getToolConfig();
                    if (config.getOutputFields() != null) {
                        fields.addAll(config.getOutputFields());
                    }
                }
            }
        }
        return fields;
    }

    /**
     * 判断 FieldSpec 是否为简写格式（如 field: string）。
     *
     * <p>简写格式仅包含 type 和 required，所有扩展属性（placeholder、options、uiHint、label、validation、defaultValue）均为 null。</p>
     */
    private boolean isSimpleFormatField(FieldSpec spec) {
        return spec.getPlaceholder() == null
                && spec.getDefaultValue() == null
                && spec.getOptions() == null
                && spec.getUiHint() == null
                && spec.getLabel() == null
                && spec.getValidation() == null;
    }

    private Set<String> collectAwaitInputFields(List<Step> steps) {
        Set<String> fields = new LinkedHashSet<String>();
        if (steps != null) {
            for (Step step : steps) {
                if (step.getType() == StepType.AWAIT) {
                    AwaitStepConfig config = step.getAwaitConfig();
                    if (config.getInputSchema() != null && !config.getInputSchema().isEmpty()) {
                        fields.addAll(config.getInputSchema().getFieldNames());
                    }
                }
            }
        }
        return fields;
    }

    // ===== Markdown 结构预扫描 =====

    // 匹配 frontmatter 中的 id 字段
    private static final Pattern FRONTMATTER_ID_PATTERN = Pattern.compile(
            "^id\\s*:\\s*.+$", Pattern.MULTILINE);
    // 匹配 frontmatter 中的 version 字段
    private static final Pattern FRONTMATTER_VERSION_PATTERN = Pattern.compile(
            "^version\\s*:\\s*.+$", Pattern.MULTILINE);
    // 匹配 H1 skill 标题
    private static final Pattern H1_SKILL_PATTERN = Pattern.compile(
            "^#\\s+skill\\s*:\\s*.+$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    // 匹配 H2 章节标题
    private static final Pattern H2_SECTION_PATTERN = Pattern.compile(
            "^##\\s+(.+)$", Pattern.MULTILINE);
    // 匹配 H3 step 标题
    private static final Pattern H3_STEP_PATTERN = Pattern.compile(
            "^###\\s+step\\s*:\\s*(.+)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    // 匹配 step 内的 **type** 属性
    private static final Pattern STEP_TYPE_ATTR_PATTERN = Pattern.compile(
            "\\*\\*type\\*\\*\\s*:\\s*(\\S+)");
    // 匹配 step 内的 **tool** 属性
    private static final Pattern STEP_TOOL_ATTR_PATTERN = Pattern.compile(
            "\\*\\*tool\\*\\*\\s*:\\s*(\\S+)");
    // 匹配 step 内的 **varName** 属性
    private static final Pattern STEP_VARNAME_ATTR_PATTERN = Pattern.compile(
            "\\*\\*varName\\*\\*\\s*:\\s*(\\S+)");
    // 匹配 prompt 代码块
    private static final Pattern PROMPT_BLOCK_PATTERN = Pattern.compile(
            "```prompt");
    // 匹配 template 代码块
    private static final Pattern TEMPLATE_BLOCK_PATTERN = Pattern.compile(
            "```template");

    /**
     * 对 Markdown 原文进行轻量级结构预扫描。
     *
     * <p>在 parser 解析失败时调用，通过文本匹配检查 Markdown 结构，
     * 尽可能多地发现问题，避免用户每次只看到一条解析错误。</p>
     *
     * <p>此方法与 parser 完全独立，不修改 parser 代码，
     * 因此 parser 内部变更不影响预扫描逻辑。</p>
     *
     * @param markdown Markdown 原文
     * @param issues   问题列表（追加写入）
     */
    private void scanMarkdownStructure(String markdown, List<ValidationIssue> issues) {
        // 提取 frontmatter 区域（如果存在）
        String frontmatter = null;
        String body = markdown;
        if (markdown.trim().startsWith("---")) {
            int secondDash = markdown.indexOf("---", markdown.indexOf("---") + 3);
            if (secondDash > 0) {
                frontmatter = markdown.substring(markdown.indexOf("---") + 3, secondDash);
                body = markdown.substring(secondDash + 3);
            }
        }

        // 检查 skill ID
        boolean hasId = false;
        if (frontmatter != null && FRONTMATTER_ID_PATTERN.matcher(frontmatter).find()) {
            hasId = true;
        }
        if (H1_SKILL_PATTERN.matcher(body).find()) {
            hasId = true;
        }
        if (!hasId) {
            issues.add(ValidationIssue.error("S01", ValidationCategory.SYNTAX,
                    "skill ID 缺失",
                    "skill.id",
                    "在 frontmatter 中添加 id 字段，或使用 H1 标题声明。示例：\n---\nid: my_skill\n---\n或：\n# skill: my_skill\n\nID 格式：小写字母开头，仅含小写字母、数字和下划线"));
        }

        // 检查 version
        boolean hasVersion = false;
        if (frontmatter != null && FRONTMATTER_VERSION_PATTERN.matcher(frontmatter).find()) {
            hasVersion = true;
        }
        // 收集所有 H2 章节
        Set<String> h2Sections = new LinkedHashSet<String>();
        Matcher h2Matcher = H2_SECTION_PATTERN.matcher(body);
        while (h2Matcher.find()) {
            h2Sections.add(h2Matcher.group(1).trim().toLowerCase());
        }
        if (h2Sections.contains("version")) {
            hasVersion = true;
        }
        if (!hasVersion) {
            issues.add(ValidationIssue.error("S03", ValidationCategory.SYNTAX,
                    "version 缺失",
                    "skill.version",
                    "在 frontmatter 中添加 version，或使用 H2 章节声明。示例：\n---\nid: my_skill\nversion: 1.0.0\n---\n或：\n## version\n1.0.0"));
        }

        // 检查 description
        if (!h2Sections.contains("description")) {
            boolean frontmatterHasDesc = frontmatter != null
                    && Pattern.compile("^description\\s*:", Pattern.MULTILINE).matcher(frontmatter).find();
            if (!frontmatterHasDesc) {
                issues.add(ValidationIssue.warning("S10", ValidationCategory.SYNTAX,
                        "description 缺失",
                        "skill.description",
                        "添加 ## description 章节描述技能的功能。示例：\n## description\n该技能用于处理用户查询并返回结果"));
            }
        }

        // 检查 intents
        if (!h2Sections.contains("intent") && !h2Sections.contains("intents")) {
            boolean frontmatterHasIntent = frontmatter != null
                    && Pattern.compile("^intents?\\s*:", Pattern.MULTILINE).matcher(frontmatter).find();
            if (!frontmatterHasIntent) {
                issues.add(ValidationIssue.suggestion("S11", ValidationCategory.SYNTAX,
                        "intents 缺失",
                        "skill.intents",
                        "添加 ## intents 章节声明意图标签，用于技能发现和路由。示例：\n## intents\n- 查询订单\n- 订单状态\n- order inquiry"));
            }
        }

        // 检查 output
        boolean hasOutput = h2Sections.contains("output") || h2Sections.contains("output_schema");
        if (!hasOutput) {
            issues.add(ValidationIssue.error("S09", ValidationCategory.SYNTAX,
                    "output_schema 缺失",
                    "skill.output_schema",
                    "添加 ## output 章节声明技能输出结构。示例：\n## output\n```yaml\nresult:\n  type: string\n  description: \"输出结果\"\n```"));
        }

        // 检查 steps
        boolean hasStepsSection = h2Sections.contains("steps");
        if (!hasStepsSection) {
            issues.add(ValidationIssue.error("S05", ValidationCategory.SYNTAX,
                    "steps 章节缺失",
                    "skill.steps",
                    "在 ## steps 章节下添加至少一个步骤。示例：\n## steps\n\n### step: process\n**type**: prompt\n**varName**: result\n\n```prompt\n处理内容\n```"));
            return; // 没有 steps 章节，后续 step 级别扫描无意义
        }

        // 扫描各个 step
        Matcher stepMatcher = H3_STEP_PATTERN.matcher(body);
        List<int[]> stepRanges = new ArrayList<int[]>(); // [nameStart, contentStart, contentEnd]
        List<String> stepNames = new ArrayList<String>();

        while (stepMatcher.find()) {
            stepNames.add(stepMatcher.group(1).trim());
            if (!stepRanges.isEmpty()) {
                // 前一个 step 的内容到这个 step 标题开始处
                stepRanges.get(stepRanges.size() - 1)[2] = stepMatcher.start();
            }
            stepRanges.add(new int[]{stepMatcher.start(), stepMatcher.end(), body.length()});
        }

        if (stepNames.isEmpty()) {
            issues.add(ValidationIssue.error("S05", ValidationCategory.SYNTAX,
                    "steps 章节下未定义任何步骤",
                    "skill.steps",
                    "在 ## steps 章节下添加至少一个步骤。示例：\n## steps\n\n### step: process\n**type**: prompt\n**varName**: result\n\n```prompt\n处理内容\n```"));
            return;
        }

        // 逐个 step 扫描
        for (int i = 0; i < stepNames.size(); i++) {
            String name = stepNames.get(i);
            String stepContent = body.substring(stepRanges.get(i)[1], stepRanges.get(i)[2]);

            // 检查 step 名称格式
            if (!STEP_NAME_PATTERN.matcher(name).matches()) {
                issues.add(ValidationIssue.error("S06", ValidationCategory.SYNTAX,
                        "step 名称格式无效: '" + name + "'",
                        "step:" + name,
                        "step 名称应匹配 ^[a-z][a-z0-9_]*$，即小写字母开头，仅含小写字母、数字和下划线。示例：### step: fetch_data"));
            }

            // 检查 **type**
            Matcher typeMatcher = STEP_TYPE_ATTR_PATTERN.matcher(stepContent);
            String stepType = typeMatcher.find() ? typeMatcher.group(1).toLowerCase() : null;

            if (stepType == null) {
                // 检查是否能通过内容推断类型
                boolean hasToolAttr = STEP_TOOL_ATTR_PATTERN.matcher(stepContent).find();
                boolean hasPromptBlock = PROMPT_BLOCK_PATTERN.matcher(stepContent).find();
                boolean hasTemplateBlock = TEMPLATE_BLOCK_PATTERN.matcher(stepContent).find();

                if (!hasToolAttr && !hasPromptBlock && !hasTemplateBlock) {
                    issues.add(ValidationIssue.error("S08", ValidationCategory.SYNTAX,
                            "step '" + name + "' 类型缺失",
                            "step:" + name,
                            "在步骤下方添加 **type** 声明。可选值：tool、prompt、template、await。示例：\n### step: " + name + "\n**type**: prompt"));
                }
                // 如果有 tool/prompt/template 内容，parser 可以推断，不报 S08
            } else {
                // 有明确 type，检查对应的必要内容
                if ("tool".equals(stepType)) {
                    if (!STEP_TOOL_ATTR_PATTERN.matcher(stepContent).find()) {
                        issues.add(ValidationIssue.error("T01", ValidationCategory.TOOL,
                                "tool 步骤 '" + name + "' 缺少 tool 名称",
                                "step:" + name + ".tool",
                                "在步骤下方添加 **tool** 声明工具名称。示例：\n### step: " + name + "\n**type**: tool\n**tool**: tool_name"));
                    }
                } else if ("prompt".equals(stepType)) {
                    if (!PROMPT_BLOCK_PATTERN.matcher(stepContent).find()) {
                        issues.add(ValidationIssue.error("L05", ValidationCategory.LOGIC,
                                "prompt 步骤 '" + name + "' 缺少 template",
                                "step:" + name + ".template",
                                "在步骤下方添加 prompt 代码块作为 LLM 提示词模板。示例：\n```prompt\n请根据 {{query}} 生成回答\n```"));
                    }
                    if (!STEP_VARNAME_ATTR_PATTERN.matcher(stepContent).find()) {
                        issues.add(ValidationIssue.error("S17", ValidationCategory.SYNTAX,
                                "PROMPT 步骤 '" + name + "' 缺少 varName",
                                "step:" + name,
                                "为 PROMPT 步骤添加 **varName** 声明输出变量名，后续步骤通过 {{varName}} 引用。示例：\n### step: " + name + "\n**type**: prompt\n**varName**: result"));
                    }
                } else if ("template".equals(stepType)) {
                    if (!TEMPLATE_BLOCK_PATTERN.matcher(stepContent).find()) {
                        issues.add(ValidationIssue.error("L06", ValidationCategory.LOGIC,
                                "template 步骤 '" + name + "' 缺少 template",
                                "step:" + name + ".template",
                                "在步骤下方添加 template 代码块作为文本渲染模板（不经过 LLM）。示例：\n```template\n查询结果：{{result}}\n```"));
                    }
                    if (!STEP_VARNAME_ATTR_PATTERN.matcher(stepContent).find()) {
                        issues.add(ValidationIssue.error("S17", ValidationCategory.SYNTAX,
                                "TEMPLATE 步骤 '" + name + "' 缺少 varName",
                                "step:" + name,
                                "为 TEMPLATE 步骤添加 **varName** 声明输出变量名，后续步骤通过 {{varName}} 引用。示例：\n### step: " + name + "\n**type**: template\n**varName**: result"));
                    }
                }
            }
        }
    }
}
