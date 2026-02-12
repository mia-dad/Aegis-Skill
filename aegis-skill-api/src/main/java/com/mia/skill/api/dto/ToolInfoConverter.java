package com.mia.skill.api.dto;

import com.mia.aegis.skill.tools.ToolProvider;
import com.mia.aegis.skill.tools.ToolSchema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ToolProvider 到 ToolInfo DTO 的转换器。
 */
public final class ToolInfoConverter {

    private ToolInfoConverter() {
    }

    /**
     * 将 ToolProvider 转换为 ToolInfo DTO。
     *
     * @param provider ToolProvider 实例
     * @return ToolInfo DTO
     */
    public static ToolInfo convert(ToolProvider provider) {
        if (provider == null) {
            return null;
        }

        String name = provider.getName();
        String description = provider.getDescription();
        String category = provider.getCategory();
        String version = provider.getVersion();
        List<String> tags = provider.getTags();
        Map<String, String> errorDescriptions = provider.getErrorDescriptions();
        String implementationClass = provider.getClass().getName();

        Map<String, Object> inputSchema = convertSchema(provider.getInputSchema());
        Map<String, Object> outputSchema = convertSchema(provider.getOutputSchema());

        return new ToolInfo(
                name, description, category, version,
                tags != null && !tags.isEmpty() ? tags : null,
                inputSchema != null && !inputSchema.isEmpty() ? inputSchema : null,
                outputSchema != null && !outputSchema.isEmpty() ? outputSchema : null,
                errorDescriptions != null && !errorDescriptions.isEmpty() ? errorDescriptions : null,
                implementationClass
        );
    }

    /**
     * 将 ToolSchema 展开为 Map。
     *
     * @param schema ToolSchema
     * @return 参数名到参数详情的 Map
     */
    public static Map<String, Object> convertSchema(ToolSchema schema) {
        if (schema == null) {
            return Collections.emptyMap();
        }

        Map<String, ToolSchema.ParameterSpec> parameters = schema.getParameters();
        if (parameters == null || parameters.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, ToolSchema.ParameterSpec> entry : parameters.entrySet()) {
            result.put(entry.getKey(), convertParameterSpec(entry.getValue()));
        }
        return result;
    }

    /**
     * 将 ParameterSpec 展开为 Map。
     *
     * <p>包含基础字段（type, description, required）和增强字段
     * （defaultValue, options, example, constraints），增强字段仅在非 null 时包含。</p>
     *
     * @param spec ParameterSpec
     * @return 参数详情 Map
     */
    public static Map<String, Object> convertParameterSpec(ToolSchema.ParameterSpec spec) {
        if (spec == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("type", spec.getType());
        map.put("description", spec.getDescription());
        map.put("required", spec.isRequired());

        if (spec.getDefaultValue() != null) {
            map.put("defaultValue", spec.getDefaultValue());
        }
        if (spec.getOptions() != null) {
            map.put("options", spec.getOptions());
        }
        if (spec.getExample() != null) {
            map.put("example", spec.getExample());
        }
        if (spec.getConstraints() != null) {
            map.put("constraints", spec.getConstraints());
        }

        return map;
    }
}
