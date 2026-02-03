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
    private StepStatus status;
    private Object output;

    /**
     * 创建 Step。
     *
     * @param name Step 名称（Skill 内唯一）
     * @param type Step 类型
     * @param config Step 配置
     */
    public Step(String name, StepType type, StepConfig config) {
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
            throw new IllegalArgumentException(MessageUtil.getMessage("step.config.mismatch", type, config.getStepType()));
        }
        this.name = name.trim();
        this.type = type;
        this.config = config;
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
     * 创建 Compose 类型 Step。
     *
     * @param name Step 名称
     * @param config Compose 配置
     * @return Step 实例
     */
    public static Step compose(String name, ComposeStepConfig config) {
        return new Step(name, StepType.COMPOSE, config);
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
     * 获取 Compose 配置。
     *
     * @return Compose 配置
     * @throws IllegalStateException 如果不是 Compose 类型
     */
    public ComposeStepConfig getComposeConfig() {
        if (type != StepType.COMPOSE) {
            throw new IllegalStateException("Step is not a COMPOSE type: " + type);
        }
        return (ComposeStepConfig) config;
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
     * 判断是否为 Compose 类型。
     *
     * @return 是否为 Compose 类型
     */
    public boolean isCompose() {
        return type == StepType.COMPOSE;
    }

    @Override
    public String toString() {
        return "Step{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", status=" + status +
                '}';
    }
}
