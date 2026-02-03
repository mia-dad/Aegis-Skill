package com.mia.aegis.skill.tools;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tool 参数 Schema 定义。
 *
 * <p>描述 Tool 的输入或输出参数结构。</p>
 */
public class ToolSchema {

    private final Map<String, ParameterSpec> parameters;

    /**
     * 创建 Tool Schema。
     *
     * @param parameters 参数规范映射
     */
    public ToolSchema(Map<String, ParameterSpec> parameters) {
        this.parameters = parameters != null
                ? Collections.unmodifiableMap(new LinkedHashMap<String, ParameterSpec>(parameters))
                : Collections.<String, ParameterSpec>emptyMap();
    }

    /**
     * 创建空 Schema。
     *
     * @return 空 ToolSchema
     */
    public static ToolSchema empty() {
        return new ToolSchema(null);
    }

    /**
     * 获取所有参数规范。
     *
     * @return 参数规范映射
     */
    public Map<String, ParameterSpec> getParameters() {
        return parameters;
    }

    /**
     * 获取指定参数规范。
     *
     * @param name 参数名
     * @return 参数规范
     */
    public ParameterSpec getParameter(String name) {
        return parameters.get(name);
    }

    /**
     * 检查参数是否存在。
     *
     * @param name 参数名
     * @return 是否存在
     */
    public boolean hasParameter(String name) {
        return parameters.containsKey(name);
    }

    /**
     * 参数规范。
     */
    public static class ParameterSpec {
        private final String type;
        private final String description;
        private final boolean required;

        public ParameterSpec(String type, String description, boolean required) {
            this.type = type;
            this.description = description;
            this.required = required;
        }

        public static ParameterSpec required(String type, String description) {
            return new ParameterSpec(type, description, true);
        }

        public static ParameterSpec optional(String type, String description) {
            return new ParameterSpec(type, description, false);
        }

        public String getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public boolean isRequired() {
            return required;
        }

        @Override
        public String toString() {
            return "ParameterSpec{type='" + type + "', required=" + required + '}';
        }
    }

    @Override
    public String toString() {
        return "ToolSchema{parameters=" + parameters.keySet() + '}';
    }
}
