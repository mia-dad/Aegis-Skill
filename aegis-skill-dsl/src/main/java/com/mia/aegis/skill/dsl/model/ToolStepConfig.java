package com.mia.aegis.skill.dsl.model;

import com.mia.aegis.skill.i18n.MessageUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tool 类型 Step 的配置。
 *
 * <p>包含 Tool 名称和输入参数模板。</p>
 */
public class ToolStepConfig implements StepConfig {

    private final String toolName;
    private final Map<String, String> inputTemplate;

    /**
     * 创建 Tool Step 配置。
     *
     * @param toolName Tool 名称
     * @param inputTemplate 输入参数模板（支持 {{var}} 语法）
     */
    public ToolStepConfig(String toolName, Map<String, String> inputTemplate) {
        if (toolName == null || toolName.trim().isEmpty()) {
            throw new IllegalArgumentException(MessageUtil.getMessage("toolstepconfig.toolname.null"));
        }
        this.toolName = toolName.trim();
        this.inputTemplate = inputTemplate != null
                ? Collections.unmodifiableMap(new LinkedHashMap<String, String>(inputTemplate))
                : Collections.<String, String>emptyMap();
    }

    /**
     * 获取 Tool 名称。
     *
     * @return Tool 名称
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * 获取输入参数模板。
     *
     * @return 不可变的模板映射
     */
    public Map<String, String> getInputTemplate() {
        return inputTemplate;
    }

    @Override
    public StepType getStepType() {
        return StepType.TOOL;
    }

    @Override
    public String toString() {
        return "ToolStepConfig{" +
                "toolName='" + toolName + '\'' +
                ", inputTemplate=" + inputTemplate +
                '}';
    }
}
