package com.mia.aegis.skill.dsl.model;

import com.mia.aegis.skill.i18n.MessageUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool 类型 Step 的配置。
 *
 * <p>包含 Tool 名称和输入参数模板。</p>
 *
 * <p>inputTemplate 存储原始的 YAML 结构，支持以下类型：</p>
 * <ul>
 *   <li>String - 字符串值，可包含 {{var}} 模板语法</li>
 *   <li>Number - 数字值</li>
 *   <li>Boolean - 布尔值</li>
 *   <li>Map - 嵌套对象结构</li>
 *   <li>List - 数组结构</li>
 * </ul>
 */
public class ToolStepConfig implements StepConfig {

    private final String toolName;
    private final Map<String, Object> inputTemplate;
    /** output_schema 中声明的输出字段名列表（纯可读性，不参与执行逻辑） */
    private final List<String> outputFields;

    /**
     * 创建 Tool Step 配置。
     *
     * @param toolName Tool 名称
     * @param inputTemplate 输入参数模板（支持 {{var}} 语法和嵌套对象）
     */
    public ToolStepConfig(String toolName, Map<String, Object> inputTemplate) {
        this(toolName, inputTemplate, null);
    }

    /**
     * 创建 Tool Step 配置（含 output_schema 声明）。
     *
     * @param toolName      Tool 名称
     * @param inputTemplate 输入参数模板（支持 {{var}} 语法和嵌套对象）
     * @param outputFields  output_schema 中声明的输出字段名列表（可为 null）
     */
    public ToolStepConfig(String toolName, Map<String, Object> inputTemplate, List<String> outputFields) {
        if (toolName == null || toolName.trim().isEmpty()) {
            throw new IllegalArgumentException(MessageUtil.getMessage("toolstepconfig.toolname.null"));
        }
        this.toolName = toolName.trim();
        this.inputTemplate = inputTemplate != null
                ? Collections.unmodifiableMap(new LinkedHashMap<String, Object>(inputTemplate))
                : Collections.<String, Object>emptyMap();
        this.outputFields = outputFields != null
                ? Collections.unmodifiableList(new ArrayList<String>(outputFields))
                : null;
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
    public Map<String, Object> getInputTemplate() {
        return inputTemplate;
    }

    /**
     * 获取 output_schema 中声明的输出字段名列表。
     *
     * <p>这是纯可读性声明，不参与执行逻辑。实际输出由工具 Java 代码控制。</p>
     *
     * @return 输出字段名列表，未声明时返回 null
     */
    public List<String> getOutputFields() {
        return outputFields;
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
                ", outputFields=" + outputFields +
                '}';
    }
}
