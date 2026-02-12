package com.mia.aegis.skill.dsl.validator;

import com.mia.aegis.skill.dsl.model.*;
import com.mia.aegis.skill.dsl.model.io.FieldSpec;
import com.mia.aegis.skill.dsl.model.io.InputSchema;
import com.mia.aegis.skill.dsl.model.io.OutputContract;
import com.mia.aegis.skill.exception.SkillValidationException;
import com.mia.aegis.skill.i18n.MessageUtil;
import com.mia.aegis.skill.template.AegisTemplateRenderer;
import com.mia.aegis.skill.template.TemplateRenderer;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 默认 Skill 校验器实现。
 *
 * <p>执行以下校验：</p>
 * <ul>
 *   <li>Skill ID 格式校验 ({@code ^[a-z][a-z0-9_]*$})</li>
 *   <li>Step 名称唯一性校验</li>
 *   <li>变量引用可解析性校验</li>
 *   <li>变量循环依赖检测</li>
 *   <li>Step 配置完整性校验</li>
 * </ul>
 */
public class DefaultSkillValidator implements SkillValidator {

    private static final Pattern SKILL_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*$");
    private static final Pattern STEP_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+(\\.\\d+)?$");

    private final TemplateRenderer templateRenderer;

    /**
     * 创建校验器实例。
     */
    public DefaultSkillValidator() {
        this.templateRenderer = new AegisTemplateRenderer();
    }

    @Override
    public void validate(Skill skill) throws SkillValidationException {
        List<String> errors = validateAndCollectErrors(skill);
        if (!errors.isEmpty()) {
            throw new SkillValidationException(errors);
        }
    }

    @Override
    public List<String> validateAndCollectErrors(Skill skill) {
        List<String> errors = new ArrayList<String>();

        if (skill == null) {
            errors.add(MessageUtil.getMessage("skill.null"));
            return errors;
        }

        // 1. 校验 Skill ID 格式
        validateSkillId(skill.getId(), errors);

        // 1.5 校验 version 格式
        validateVersion(skill.getVersion(), errors);

        // 2. 校验 Steps 不为空
        if (skill.getSteps() == null || skill.getSteps().isEmpty()) {
            errors.add(MessageUtil.getMessage("skill.steps.empty"));
            return errors;
        }

        // 3. 校验 output_schema 存在性（必需字段）
        if (skill.getOutputContract() == null) {
            errors.add("缺少必需的 output_schema 定义");
            return errors;
        }

        // 5. 校验 Step 名称唯一性
        validateStepNameUniqueness(skill.getSteps(), errors);

        // 6. 收集所有 varName 并校验
        Set<String> stepNames = collectStepNames(skill.getSteps());
        Set<String> inputFields = collectInputFields(skill.getInputSchema());
        Set<String> varNames = collectVarNames(skill.getSteps());

        // 7. 校验每个 Step 的配置
        for (Step step : skill.getSteps()) {
            validateStep(step, stepNames, inputFields, varNames, errors);
        }

        // 8. 检测循环依赖
        detectCircularDependencies(skill.getSteps(), errors);

        return errors;
    }

    @Override
    public boolean isValid(Skill skill) {
        return validateAndCollectErrors(skill).isEmpty();
    }

    private void validateSkillId(String id, List<String> errors) {
        if (id == null || id.isEmpty()) {
            errors.add(MessageUtil.getMessage("skill.id.empty"));
            return;
        }
        if (!SKILL_ID_PATTERN.matcher(id).matches()) {
            errors.add(MessageUtil.getMessage("skill.id.invalid", id));
        }
    }

    private void validateVersion(String version, List<String> errors) {
        if (version == null || version.isEmpty()) {
            errors.add("缺少必需的 version 字段");
            return;
        }
        if (!VERSION_PATTERN.matcher(version).matches()) {
            errors.add("version 格式无效: '" + version + "'（应为 x.y 或 x.y.z 格式）");
        }
    }

    private void validateStepNameUniqueness(List<Step> steps, List<String> errors) {
        Set<String> names = new HashSet<String>();
        for (Step step : steps) {
            String name = step.getName();
            if (names.contains(name)) {
                errors.add(MessageUtil.getMessage("skill.duplicate.step", name));
            } else {
                names.add(name);
            }
        }
    }

    private Set<String> collectStepNames(List<Step> steps) {
        Set<String> names = new LinkedHashSet<String>();
        for (Step step : steps) {
            names.add(step.getName());
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
        for (Step step : steps) {
            if (step.hasVarName()) {
                varNames.add(step.getVarName());
            }
        }
        return varNames;
    }

    private void validateStep(Step step, Set<String> allStepNames,
                              Set<String> inputFields, Set<String> allVarNames,
                              List<String> errors) {
        String stepName = step.getName();

        // 校验 Step 名称格式
        if (!STEP_NAME_PATTERN.matcher(stepName).matches()) {
            errors.add(MessageUtil.getMessage("skill.step.invalid.name", stepName));
        }

        // 仅 PROMPT 和 TEMPLATE 需要 varName; TOOL 和 AWAIT 不需要
        if (!step.hasVarName() && (step.getType() == StepType.PROMPT || step.getType() == StepType.TEMPLATE)) {
            errors.add(MessageUtil.getMessage("step.varname.required", stepName));
        }

        // 校验 varName 格式和唯一性
        if (step.hasVarName()) {
            String varName = step.getVarName();
            // 格式校验
            if (!STEP_NAME_PATTERN.matcher(varName).matches()) {
                errors.add(MessageUtil.getMessage("step.varname.invalid", stepName, varName));
            }
            // 唯一性校验：不能与 stepName 或 inputField 冲突
            if (allStepNames.contains(varName)) {
                errors.add(MessageUtil.getMessage("step.varname.conflict", stepName, varName));
            }
            if (inputFields.contains(varName)) {
                errors.add(MessageUtil.getMessage("step.varname.conflict", stepName, varName));
            }
            // 检查 varName 在所有 varNames 中是否重复（出现多次）
            int count = 0;
            for (String vn : allVarNames) {
                if (vn.equals(varName)) count++;
            }
            if (count > 1) {
                errors.add(MessageUtil.getMessage("step.varname.conflict", stepName, varName));
            }
        }

        // 根据类型进行特定校验
        switch (step.getType()) {
            case TOOL:
                validateToolStep(step, inputFields, allStepNames, allVarNames, errors);
                break;
            case PROMPT:
                validatePromptStep(step, inputFields, allStepNames, allVarNames, errors);
                break;
            case TEMPLATE:
                validateTemplateStep(step, inputFields, allStepNames, allVarNames, errors);
                break;
            case AWAIT:
                validateAwaitStep(step, errors);
                break;
        }
    }

    private void validateAwaitStep(Step step, List<String> errors) {
        AwaitStepConfig config = step.getAwaitConfig();

        // 校验 message
        if (config.getMessage() == null || config.getMessage().isEmpty()) {
            errors.add(MessageUtil.getMessage("skill.step.await.message.empty", step.getName()));
        }

        // 校验 inputSchema
        InputSchema inputSchema = config.getInputSchema();
        if (inputSchema == null || inputSchema.isEmpty()) {
            errors.add(MessageUtil.getMessage("skill.step.await.inputSchema.empty", step.getName()));
        } else {
            // 校验每个字段的类型定义
            for (Map.Entry<String, FieldSpec> entry : inputSchema.getFields().entrySet()) {
                String fieldName = entry.getKey();
                FieldSpec fieldSpec = entry.getValue();
                if (fieldSpec.getType() == null || fieldSpec.getType().isEmpty()) {
                    errors.add(MessageUtil.getMessage("skill.step.await.field.type.missing", step.getName(), fieldName));
                }
            }
        }
    }

    private void validateToolStep(Step step, Set<String> inputFields,
                                  Set<String> allStepNames, Set<String> allVarNames,
                                  List<String> errors) {
        ToolStepConfig config = step.getToolConfig();

        // 校验 Tool 名称
        if (config.getToolName() == null || config.getToolName().isEmpty()) {
            errors.add(MessageUtil.getMessage("skill.step.tool.name.empty", step.getName()));
        }

        // 校验 output_schema 必填
        if (config.getOutputFields() == null || config.getOutputFields().isEmpty()) {
            errors.add("步骤 '" + step.getName() + "' 的 tool 类型缺少 output_schema 声明");
        }

        // 校验输入模板中的变量引用（支持嵌套对象和数组）
        validateInputTemplate(config.getInputTemplate(), step.getName(), inputFields,
                allStepNames, allVarNames, step.getName(), errors);
    }

    /**
     * 递归校验输入模板中的变量引用。
     *
     * @param template 模板对象（可以是 String、Map、List）
     * @param stepName 步骤名称
     * @param inputFields 输入字段集合
     * @param allStepNames 所有步骤名称
     * @param currentField 当前字段名
     * @param errors 错误列表
     */
    @SuppressWarnings("unchecked")
    private void validateInputTemplate(Object template, String stepName, Set<String> inputFields,
                                       Set<String> allStepNames, Set<String> allVarNames,
                                       String currentField, List<String> errors) {
        if (template == null) {
            return;
        }

        // 字符串：直接校验变量引用
        if (template instanceof String) {
            validateVariableReferences((String) template, stepName, inputFields,
                    allStepNames, allVarNames, currentField, errors);
            return;
        }

        // Map：递归校验每个值
        if (template instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) template;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                validateInputTemplate(entry.getValue(), stepName, inputFields,
                        allStepNames, allVarNames, currentField + "." + entry.getKey(), errors);
            }
            return;
        }

        // List：递归校验每个元素
        if (template instanceof List) {
            List<Object> list = (List<Object>) template;
            for (int i = 0; i < list.size(); i++) {
                validateInputTemplate(list.get(i), stepName, inputFields,
                        allStepNames, allVarNames, currentField + "[" + i + "]", errors);
            }
            return;
        }

        // 其他类型（Number, Boolean 等）：无需校验
    }

    private void validatePromptStep(Step step, Set<String> inputFields,
                                    Set<String> allStepNames, Set<String> allVarNames,
                                    List<String> errors) {
        PromptStepConfig config = step.getPromptConfig();

        // 校验 Prompt 模板
        if (config.getTemplate() == null || config.getTemplate().isEmpty()) {
            errors.add(MessageUtil.getMessage("skill.step.prompt.template.empty", step.getName()));
        }

        // 校验模板中的变量引用
        validateVariableReferences(config.getTemplate(), step.getName(), inputFields,
                allStepNames, allVarNames, step.getName(), errors);
    }

    private void validateTemplateStep(Step step, Set<String> inputFields,
                                      Set<String> allStepNames, Set<String> allVarNames,
                                      List<String> errors) {
        TemplateStepConfig config = step.getTemplateConfig();

        // 校验模板内容非空
        if (config.getTemplate() == null || config.getTemplate().isEmpty()) {
            errors.add(MessageUtil.getMessage("skill.step.template.content.empty", step.getName()));
        }

        // 校验模板中的变量引用
        validateVariableReferences(config.getTemplate(), step.getName(), inputFields,
                allStepNames, allVarNames, step.getName(), errors);
    }

    private void validateVariableReferences(String template, String stepName,
                                            Set<String> inputFields, Set<String> allStepNames,
                                            Set<String> allVarNames,
                                            String currentStep, List<String> errors) {
        if (template == null || template.isEmpty()) {
            return;
        }

        List<String> variables = templateRenderer.extractVariables(template);
        for (String variable : variables) {
            // 检查是否是输入字段
            if (inputFields.contains(variable)) {
                continue;
            }

            // 检查是否是 context.* 引用（需要在步骤引用之前检查）
            if (variable.startsWith("context.")) {
                continue; // context 变量在运行时提供
            }

            // 检查是否是 step.value 引用
            String stepRef = extractStepReference(variable);
            if (stepRef != null) {
                if (!allStepNames.contains(stepRef)) {
                    errors.add(MessageUtil.getMessage("skill.step.variable.unknown.step",
                            stepName, variable, stepRef));
                } else if (stepRef.equals(stepName)) {
                    errors.add(MessageUtil.getMessage("skill.step.variable.self",
                            stepName, variable));
                }
                continue;
            }

            // 检查是否是 varName 引用
            if (allVarNames.contains(variable)) {
                continue;
            }

            // 检查是否是 varName.field 引用（嵌套属性访问）
            String varNameRef = extractStepReference(variable);
            if (varNameRef != null && allVarNames.contains(varNameRef)) {
                continue;
            }

            // 未知变量
            errors.add(MessageUtil.getMessage("skill.step.variable.unknown", stepName, variable));
        }
    }

    private String extractStepReference(String variable) {
        // 解析 step.value 或 step 格式
        if (variable.contains(".")) {
            return variable.split("\\.")[0];
        }
        return null;
    }

    private void detectCircularDependencies(List<Step> steps, List<String> errors) {
        // 构建依赖图
        Map<String, Set<String>> dependencies = new LinkedHashMap<String, Set<String>>();
        for (Step step : steps) {
            Set<String> deps = new LinkedHashSet<String>();
            collectStepDependencies(step, deps);
            dependencies.put(step.getName(), deps);
        }

        // 检测循环
        Set<String> visited = new HashSet<String>();
        Set<String> inStack = new HashSet<String>();

        for (String stepName : dependencies.keySet()) {
            if (hasCycle(stepName, dependencies, visited, inStack, new ArrayList<String>())) {
                // 循环已在 hasCycle 中报告
            }
        }

        // 实际的循环检测在这里实现
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
                errors.add(MessageUtil.getMessage("skill.circular.dependency", cycle.toString()));
                break; // 只报告一个循环
            }
        }
    }

    private void collectStepDependencies(Step step, Set<String> deps) {
        switch (step.getType()) {
            case TOOL:
                ToolStepConfig toolConfig = step.getToolConfig();
                // 递归收集输入模板中的依赖关系
                collectDependenciesFromObject(toolConfig.getInputTemplate(), deps);
                break;
            case PROMPT:
                PromptStepConfig promptConfig = step.getPromptConfig();
                collectDependenciesFromTemplate(promptConfig.getTemplate(), deps);
                break;
            case TEMPLATE:
                TemplateStepConfig templateConfig = step.getTemplateConfig();
                collectDependenciesFromTemplate(templateConfig.getTemplate(), deps);
                break;
        }
    }

    /**
     * 递归从对象中收集步骤依赖关系。
     *
     * @param obj 模板对象（String, Map, List）
     * @param deps 依赖集合
     */
    @SuppressWarnings("unchecked")
    private void collectDependenciesFromObject(Object obj, Set<String> deps) {
        if (obj == null) {
            return;
        }

        // 字符串：直接提取变量
        if (obj instanceof String) {
            collectDependenciesFromTemplate((String) obj, deps);
            return;
        }

        // Map：递归处理每个值
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            for (Object value : map.values()) {
                collectDependenciesFromObject(value, deps);
            }
            return;
        }

        // List：递归处理每个元素
        if (obj instanceof List) {
            List<Object> list = (List<Object>) obj;
            for (Object item : list) {
                collectDependenciesFromObject(item, deps);
            }
            return;
        }

        // 其他类型：无需处理
    }

    private void collectDependenciesFromTemplate(String template, Set<String> deps) {
        if (template == null) {
            return;
        }
        List<String> variables = templateRenderer.extractVariables(template);
        for (String variable : variables) {
            String ref = extractStepReference(variable);
            if (ref != null) {
                deps.add(ref);
            }
        }
    }

    private boolean hasCycle(String node, Map<String, Set<String>> graph,
                             Set<String> visited, Set<String> inStack, List<String> path) {
        if (inStack.contains(node)) {
            return true;
        }
        if (visited.contains(node)) {
            return false;
        }

        visited.add(node);
        inStack.add(node);
        path.add(node);

        Set<String> deps = graph.get(node);
        if (deps != null) {
            for (String dep : deps) {
                if (graph.containsKey(dep) && hasCycle(dep, graph, visited, inStack, path)) {
                    return true;
                }
            }
        }

        inStack.remove(node);
        return false;
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
}