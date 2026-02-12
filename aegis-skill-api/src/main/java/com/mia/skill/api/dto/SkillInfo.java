package com.mia.skill.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mia.aegis.skill.dsl.model.io.InputSchema;

import java.util.List;
import java.util.Map;

/**
 * Skill 元信息 DTO。
 *
 * <p>用于返回 Skill 的基本信息，包含完整的 InputSchema 元数据。</p>
 *
 * <p>Jackson 序列化配置：</p>
 * <ul>
 *   <li>null 字段不会被序列化（NON_NULL）</li>
 *   <li>FieldSpec 的所有新字段（placeholder, defaultValue, options, uiHint, label, validation）都会正确输出</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillInfo {

    /** Skill 唯一标识 */
    private String id;

    /** 版本号 */
    private String version;

    /** Skill 描述 */
    private String description;

    /** 支持的意图列表 */
    private List<String> intents;

    /** 输入 Schema */
    private InputSchema inputSchema;

    /** 输出 Schema（平铺字段定义） */
    private Map<String, Object> outputSchema;

    public SkillInfo(String id, String version, String description, List<String> intents,
                     InputSchema inputSchema, Map<String, Object> outputSchema) {
        this.id = id;
        this.version = version;
        this.description = description;
        this.intents = intents;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
    }

    public InputSchema getInputSchema() {
        return inputSchema;
    }

    public Map<String, Object> getOutputSchema() {
        return outputSchema;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getIntents() {
        return intents;
    }



}
