package com.mia.aegis.skill.dsl.validator;

import com.mia.aegis.skill.dsl.model.*;
import com.mia.aegis.skill.dsl.model.io.InputSchema;
import com.mia.aegis.skill.exception.SkillValidationException;
import com.mia.aegis.skill.template.MustacheTemplateRenderer;

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

    private final MustacheTemplateRenderer templateRenderer;

    /**
     * 创建校验器实例。
     */
    public DefaultSkillValidator() {
        this.templateRenderer = new MustacheTemplateRenderer();
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
            errors.add("检验的技能为空");
            return errors;
        }

        // 1. 校验 Skill ID 格式
        validateSkillId(skill.getId(), errors);

        // 2. 校验 Steps 不为空
        if (skill.getSteps() == null || skill.getSteps().isEmpty()) {
            errors.add("技能内容至少存在一个step");
            return errors;
        }

        // 3. 校验 Step 名称唯一性
        validateStepNameUniqueness(skill.getSteps(), errors);

        // 4. 校验每个 Step 的配置
        Set<String> stepNames = collectStepNames(skill.getSteps());
        Set<String> inputFields = collectInputFields(skill.getInputSchema());

        for (Step step : skill.getSteps()) {
            validateStep(step, stepNames, inputFields, errors);
        }

        // 5. 检测循环依赖
        detectCircularDependencies(skill.getSteps(), errors);

        return errors;
    }

    @Override
    public boolean isValid(Skill skill) {
        return validateAndCollectErrors(skill).isEmpty();
    }

    private void validateSkillId(String id, List<String> errors) {
        if (id == null || id.isEmpty()) {
            errors.add("技能ID不能为空");
            return;
        }
        if (!SKILL_ID_PATTERN.matcher(id).matches()) {
            errors.add("Invalid Skill ID format: '" + id + "'. " +
                    "Must match pattern: ^[a-z][a-z0-9_]*$ (lowercase letter start, alphanumeric and underscore)");
        }
    }

    private void validateStepNameUniqueness(List<Step> steps, List<String> errors) {
        Set<String> names = new HashSet<String>();
        for (Step step : steps) {
            String name = step.getName();
            if (names.contains(name)) {
                errors.add("Duplicate step name: '" + name + "'");
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

    private void validateStep(Step step, Set<String> allStepNames,
                              Set<String> inputFields, List<String> errors) {
        String stepName = step.getName();

        // 校验 Step 名称格式
        if (!STEP_NAME_PATTERN.matcher(stepName).matches()) {
            errors.add("Invalid step name format: '" + stepName + "'. " +
                    "Must match pattern: ^[a-z][a-z0-9_]*$");
        }

        // 根据类型进行特定校验
        switch (step.getType()) {
            case TOOL:
                validateToolStep(step, inputFields, allStepNames, errors);
                break;
            case PROMPT:
                validatePromptStep(step, inputFields, allStepNames, errors);
                break;
            case COMPOSE:
                validateComposeStep(step, allStepNames, errors);
                break;
        }
    }

    private void validateToolStep(Step step, Set<String> inputFields,
                                  Set<String> allStepNames, List<String> errors) {
        ToolStepConfig config = step.getToolConfig();

        // 校验 Tool 名称
        if (config.getToolName() == null || config.getToolName().isEmpty()) {
            errors.add("步骤 '" + step.getName() + "': 用到的工具名称不能为空");
        }

        // 校验输入模板中的变量引用
        for (Map.Entry<String, String> entry : config.getInputTemplate().entrySet()) {
            String template = entry.getValue();
            validateVariableReferences(template, step.getName(), inputFields,
                    allStepNames, step.getName(), errors);
        }
    }//

    private void validatePromptStep(Step step, Set<String> inputFields,
                                    Set<String> allStepNames, List<String> errors) {
        PromptStepConfig config = step.getPromptConfig();

        // 校验 Prompt 模板
        if (config.getTemplate() == null || config.getTemplate().isEmpty()) {
            errors.add("步骤 '" + step.getName() + "': 用到的提示词模板不能为空");
        }

        // 校验模板中的变量引用
        validateVariableReferences(config.getTemplate(), step.getName(), inputFields,
                allStepNames, step.getName(), errors);
    }

    private void validateComposeStep(Step step, Set<String> allStepNames, List<String> errors) {
        ComposeStepConfig config = step.getComposeConfig();

        // 校验 sources 中的引用
        for (String source : config.getSources()) {
            String stepRef = extractStepReference(source);
            if (stepRef != null && !allStepNames.contains(stepRef)) {
                errors.add("Step '" + step.getName() + "': Source references unknown step '" + stepRef + "'");
            }
            // 不能引用自己
            if (stepRef != null && stepRef.equals(step.getName())) {
                errors.add("Step '" + step.getName() + "': Cannot reference itself in sources");
            }
        }
    }

    private void validateVariableReferences(String template, String stepName,
                                            Set<String> inputFields, Set<String> allStepNames,
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

            // 检查是否是 context.* 引用（必须在step引用检查之前）
            if (variable.startsWith("context.")) {
                continue; // context 变量在运行时提供
            }

            // 检查是否是 step.output 引用
            String stepRef = extractStepReference(variable);
            if (stepRef != null) {
                if (!allStepNames.contains(stepRef)) {
                    errors.add("Step '" + stepName + "': Variable '" + variable +
                            "' references unknown step '" + stepRef + "'");
                } else if (stepRef.equals(currentStep)) {
                    errors.add("Step '" + stepName + "': Variable '" + variable +
                            "' references current step (self-reference not allowed)");
                }
                continue;
            }

            // 未知变量
            errors.add("Step '" + stepName + "': Unknown variable '" + variable + "'");
        }
    }

    private String extractStepReference(String variable) {
        // 解析 step.output 或 step 格式
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
                errors.add("Circular dependency detected: " + cycle.toString());
                break; // 只报告一个循环
            }
        }
    }

    private void collectStepDependencies(Step step, Set<String> deps) {
        switch (step.getType()) {
            case TOOL:
                ToolStepConfig toolConfig = step.getToolConfig();
                for (String template : toolConfig.getInputTemplate().values()) {
                    collectDependenciesFromTemplate(template, deps);
                }
                break;
            case PROMPT:
                PromptStepConfig promptConfig = step.getPromptConfig();
                collectDependenciesFromTemplate(promptConfig.getTemplate(), deps);
                break;
            case COMPOSE:
                ComposeStepConfig composeConfig = step.getComposeConfig();
                for (String source : composeConfig.getSources()) {
                    String ref = extractStepReference(source);
                    if (ref != null) {
                        deps.add(ref);
                    }
                }
                break;
        }
    }

    private void collectDependenciesFromTemplate(String template, Set<String> deps) {
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

