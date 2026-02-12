package com.mia.aegis.skill.dsl.model;

import com.mia.aegis.skill.i18n.MessageUtil;

/**
 * Skill 内的单个执行步骤。
 *
 * <p>Step 是 Skill 执行的基本单元，包含名称、类型、配置和运行时状态。</p>
 */
public class Step {

    private final String name;
    private final StepType type;
    private final StepConfig config;
    /** 条件表达式，可为 null 表示无条件执行 */
    private final WhenCondition whenCondition;
    /** 变量别名，设置后步骤输出以此名称直接存入上下文，可为 null */
    private final String varName;

    private StepStatus status;
    private Object output;

    /**
     * 创建 Step（无条件）。
     *
     * @param name Step 名称（Skill 内唯一）
     * @param type Step 类型
     * @param config Step 配置
     */
    public Step(String name, StepType type, StepConfig config) {
        this(name, type, config, null, null);
    }

    /**
     * 创建 Step（带条件）。
     *
     * @param name Step 名称（Skill 内唯一）
     * @param type Step 类型
     * @param config Step 配置
     * @param whenCondition 条件表达式，为 null 时表示无条件执行
     */
    public Step(String name, StepType type, StepConfig config, WhenCondition whenCondition) {
        this(name, type, config, whenCondition, null);
    }

    /**
     * 创建 Step（带条件和变量别名）。
     *
     * @param name Step 名称（Skill 内唯一）
     * @param type Step 类型
     * @param config Step 配置
     * @param whenCondition 条件表达式，为 null 时表示无条件执行
     * @param varName 变量别名，为 null 时使用默认的 StepOutputWrapper 包装
     */
    public Step(String name, StepType type, StepConfig config, WhenCondition whenCondition, String varName) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(MessageUtil.getMessage("step.name.null"));
        }
        if (type == null) {
            throw new IllegalArgumentException(MessageUtil.getMessage("step.type.null"));
        }
        if (config == null) {
            throw new IllegalArgumentException(MessageUtil.getMessage("step.config.null"));
        }
        if (config.getStepType() != type) {
            throw new IllegalArgumentException(MessageUtil.getMessage("step.config.mismatch",
                    type, config.getStepType()));
        }
        this.name = name.trim();
        this.type = type;
        this.config = config;
        this.whenCondition = whenCondition;
        this.varName = varName != null ? varName.trim() : null;
        this.status = StepStatus.PENDING;
        this.output = null;
    }

    /**
     * 创建 Tool 类型 Step。
     *
     * @param name Step 名称
     * @param config Tool 配置
     * @return Step 实例
     */
    public static Step tool(String name, ToolStepConfig config) {
        return new Step(name, StepType.TOOL, config);
    }

    /**
     * 创建 Tool 类型 Step（带变量别名）。
     *
     * @param name Step 名称
     * @param config Tool 配置
     * @param varName 变量别名
     * @return Step 实例
     */
    public static Step tool(String name, ToolStepConfig config, String varName) {
        return new Step(name, StepType.TOOL, config, null, varName);
    }

    /**
     * 创建 Prompt 类型 Step。
     *
     * @param name Step 名称
     * @param config Prompt 配置
     * @return Step 实例
     */
    public static Step prompt(String name, PromptStepConfig config) {
        return new Step(name, StepType.PROMPT, config);
    }

    /**
     * 创建 Prompt 类型 Step（带变量别名）。
     *
     * @param name Step 名称
     * @param config Prompt 配置
     * @param varName 变量别名
     * @return Step 实例
     */
    public static Step prompt(String name, PromptStepConfig config, String varName) {
        return new Step(name, StepType.PROMPT, config, null, varName);
    }

    /**
     * 创建 Await 类型 Step。
     *
     * @param name Step 名称
     * @param config Await 配置
     * @return Step 实例
     */
    public static Step await(String name, AwaitStepConfig config) {
        return new Step(name, StepType.AWAIT, config);
    }

    /**
     * 创建 Await 类型 Step（带条件）。
     *
     * @param name Step 名称
     * @param config Await 配置
     * @param whenCondition 条件表达式
     * @return Step 实例
     */
    public static Step await(String name, AwaitStepConfig config, WhenCondition whenCondition) {
        return new Step(name, StepType.AWAIT, config, whenCondition);
    }

    /**
     * 创建 Await 类型 Step（带变量别名）。
     *
     * @param name Step 名称
     * @param config Await 配置
     * @param varName 变量别名
     * @return Step 实例
     * @deprecated Await 步骤不再需要 varName，使用 {@link #await(String, AwaitStepConfig)} 代替
     */
    @Deprecated
    public static Step await(String name, AwaitStepConfig config, String varName) {
        return new Step(name, StepType.AWAIT, config, null, varName);
    }

    /**
     * 创建 Template 类型 Step。
     *
     * @param name Step 名称
     * @param config Template 配置
     * @return Step 实例
     */
    public static Step template(String name, TemplateStepConfig config) {
        return new Step(name, StepType.TEMPLATE, config);
    }

    /**
     * 创建 Template 类型 Step（带条件）。
     *
     * @param name Step 名称
     * @param config Template 配置
     * @param whenCondition 条件表达式
     * @return Step 实例
     */
    public static Step template(String name, TemplateStepConfig config, WhenCondition whenCondition) {
        return new Step(name, StepType.TEMPLATE, config, whenCondition);
    }

    /**
     * 创建 Template 类型 Step（带变量别名）。
     *
     * @param name Step 名称
     * @param config Template 配置
     * @param varName 变量别名
     * @return Step 实例
     */
    public static Step template(String name, TemplateStepConfig config, String varName) {
        return new Step(name, StepType.TEMPLATE, config, null, varName);
    }

    /**
     * 获取 Step 名称。
     *
     * @return Step 名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取 Step 类型。
     *
     * @return Step 类型
     */
    public StepType getType() {
        return type;
    }

    /**
     * 获取 Step 配置。
     *
     * @return Step 配置
     */
    public StepConfig getConfig() {
        return config;
    }

    /**
     * 获取条件表达式。
     *
     * @return 条件表达式，如果没有条件则返回 null
     */
    public WhenCondition getWhenCondition() {
        return whenCondition;
    }

    /**
     * 判断是否有条件表达式。
     *
     * @return 如果有条件表达式返回 true
     */
    public boolean hasWhenCondition() {
        return whenCondition != null;
    }

    /**
     * 获取变量别名。
     *
     * @return 变量别名，如果没有设置则返回 null
     */
    public String getVarName() {
        return varName;
    }

    /**
     * 判断是否有变量别名。
     *
     * @return 如果有变量别名返回 true
     */
    public boolean hasVarName() {
        return varName != null && !varName.isEmpty();
    }

    /**
     * 获取 Tool 配置。
     *
     * @return Tool 配置
     * @throws IllegalStateException 如果不是 Tool 类型
     */
    public ToolStepConfig getToolConfig() {
        if (type != StepType.TOOL) {
            throw new IllegalStateException("Step is not a TOOL type: " + type);
        }
        return (ToolStepConfig) config;
    }

    /**
     * 获取 Prompt 配置。
     *
     * @return Prompt 配置
     * @throws IllegalStateException 如果不是 Prompt 类型
     */
    public PromptStepConfig getPromptConfig() {
        if (type != StepType.PROMPT) {
            throw new IllegalStateException("Step is not a PROMPT type: " + type);
        }
        return (PromptStepConfig) config;
    }

    /**
     * 获取执行状态。
     *
     * @return 执行状态
     */
    public StepStatus getStatus() {
        return status;
    }

    /**
     * 设置执行状态。
     *
     * @param status 新状态
     */
    public void setStatus(StepStatus status) {
        this.status = status;
    }

    /**
     * 获取执行输出。
     *
     * @return 执行输出，未执行完成时返回 null
     */
    public Object getOutput() {
        return output;
    }

    /**
     * 设置执行输出。
     *
     * @param output 执行输出
     */
    public void setOutput(Object output) {
        this.output = output;
    }

    /**
     * 判断是否为 Tool 类型。
     *
     * @return 是否为 Tool 类型
     */
    public boolean isTool() {
        return type == StepType.TOOL;
    }

    /**
     * 判断是否为 Prompt 类型。
     *
     * @return 是否为 Prompt 类型
     */
    public boolean isPrompt() {
        return type == StepType.PROMPT;
    }

    /**
     * 判断是否为 Await 类型。
     *
     * @return 是否为 Await 类型
     */
    public boolean isAwait() {
        return type == StepType.AWAIT;
    }

    /**
     * 判断是否为 Template 类型。
     *
     * @return 是否为 Template 类型
     */
    public boolean isTemplate() {
        return type == StepType.TEMPLATE;
    }

    /**
     * 获取 Await 配置。
     *
     * @return Await 配置
     * @throws IllegalStateException 如果不是 Await 类型
     */
    public AwaitStepConfig getAwaitConfig() {
        if (type != StepType.AWAIT) {
            throw new IllegalStateException("Step is not an AWAIT type: " + type);
        }
        return (AwaitStepConfig) config;
    }

    /**
     * 获取 Template 配置。
     *
     * @return Template 配置
     * @throws IllegalStateException 如果不是 Template 类型
     */
    public TemplateStepConfig getTemplateConfig() {
        if (type != StepType.TEMPLATE) {
            throw new IllegalStateException("Step is not a TEMPLATE type: " + type);
        }
        return (TemplateStepConfig) config;
    }

    @Override
    public String toString() {
        return "Step{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", status=" + status +
                (whenCondition != null ? ", when=" + whenCondition : "") +
                (varName != null ? ", varName=" + varName : "") +
                '}';
    }
}
