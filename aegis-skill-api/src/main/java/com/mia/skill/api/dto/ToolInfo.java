package com.mia.skill.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Tool 元信息 DTO。
 *
 * <p>用于返回 Tool 的完整元数据，包含输入输出 Schema、错误描述等。</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolInfo {

    private String name;
    private String description;
    private String category;
    private String version;
    private List<String> tags;
    private Map<String, Object> inputSchema;
    private Map<String, Object> outputSchema;
    private Map<String, String> errorDescriptions;
    private String implementationClass;

    public ToolInfo() {
    }

    public ToolInfo(String name, String description, String category, String version,
                    List<String> tags, Map<String, Object> inputSchema,
                    Map<String, Object> outputSchema, Map<String, String> errorDescriptions,
                    String implementationClass) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.version = version;
        this.tags = tags;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.errorDescriptions = errorDescriptions;
        this.implementationClass = implementationClass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(Map<String, Object> inputSchema) {
        this.inputSchema = inputSchema;
    }

    public Map<String, Object> getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(Map<String, Object> outputSchema) {
        this.outputSchema = outputSchema;
    }

    public Map<String, String> getErrorDescriptions() {
        return errorDescriptions;
    }

    public void setErrorDescriptions(Map<String, String> errorDescriptions) {
        this.errorDescriptions = errorDescriptions;
    }

    public String getImplementationClass() {
        return implementationClass;
    }

    public void setImplementationClass(String implementationClass) {
        this.implementationClass = implementationClass;
    }
}
