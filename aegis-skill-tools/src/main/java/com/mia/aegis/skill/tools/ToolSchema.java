package com.mia.aegis.skill.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
        private final Object defaultValue;
        private final List<String> options;
        private final String example;
        private final Map<String, Object> constraints;

        /**
         * 基础构造函数（向后兼容）。
         *
         * @param type        参数类型
         * @param description 参数描述
         * @param required    是否必填
         */
        public ParameterSpec(String type, String description, boolean required) {
            this.type = type;
            this.description = description;
            this.required = required;
            this.defaultValue = null;
            this.options = null;
            this.example = null;
            this.constraints = null;
        }

        /**
         * 全参构造函数（仅供 Builder 调用）。
         */
        private ParameterSpec(String type, String description, boolean required,
                              Object defaultValue, List<String> options,
                              String example, Map<String, Object> constraints) {
            this.type = type;
            this.description = description;
            this.required = required;
            this.defaultValue = defaultValue;
            this.options = options != null
                    ? Collections.unmodifiableList(new ArrayList<String>(options))
                    : null;
            this.example = example;
            this.constraints = constraints != null
                    ? Collections.unmodifiableMap(new HashMap<String, Object>(constraints))
                    : null;
        }

        public static ParameterSpec required(String type, String description) {
            return new ParameterSpec(type, description, true);
        }

        public static ParameterSpec optional(String type, String description) {
            return new ParameterSpec(type, description, false);
        }

        /**
         * 创建 Builder。
         *
         * @param type        参数类型
         * @param description 参数描述
         * @return Builder 实例
         */
        public static Builder builder(String type, String description) {
            return new Builder(type, description);
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

        /**
         * 获取默认值。
         *
         * @return 默认值，未设置时返回 null
         */
        public Object getDefaultValue() {
            return defaultValue;
        }

        /**
         * 获取可选值列表。
         *
         * @return 可选值列表，未设置时返回 null
         */
        public List<String> getOptions() {
            return options;
        }

        /**
         * 获取示例值。
         *
         * @return 示例值，未设置时返回 null
         */
        public String getExample() {
            return example;
        }

        /**
         * 获取约束条件。
         *
         * @return 约束条件 Map，未设置时返回 null
         */
        public Map<String, Object> getConstraints() {
            return constraints;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("ParameterSpec{type='").append(type).append("', required=").append(required);
            if (defaultValue != null) {
                sb.append(", defaultValue=").append(defaultValue);
            }
            if (options != null) {
                sb.append(", options=").append(options);
            }
            if (example != null) {
                sb.append(", example='").append(example).append("'");
            }
            if (constraints != null) {
                sb.append(", constraints=").append(constraints);
            }
            sb.append('}');
            return sb.toString();
        }

        /**
         * ParameterSpec Builder。
         *
         * <p>使用示例：</p>
         * <pre>{@code
         * ParameterSpec.builder("string", "File format")
         *     .required()
         *     .defaultValue("json")
         *     .options("json", "txt")
         *     .example("json")
         *     .constraint("pattern", "^(json|txt)$")
         *     .build();
         * }</pre>
         */
        public static class Builder {
            private final String type;
            private final String description;
            private boolean required = false;
            private Object defaultValue;
            private List<String> options;
            private String example;
            private Map<String, Object> constraints;

            private Builder(String type, String description) {
                this.type = type;
                this.description = description;
            }

            /**
             * 标记为必填。
             */
            public Builder required() {
                this.required = true;
                return this;
            }

            /**
             * 标记为可选（默认）。
             */
            public Builder optional() {
                this.required = false;
                return this;
            }

            /**
             * 设置默认值。
             */
            public Builder defaultValue(Object defaultValue) {
                this.defaultValue = defaultValue;
                return this;
            }

            /**
             * 设置可选值列表。
             */
            public Builder options(String... options) {
                this.options = new ArrayList<String>();
                Collections.addAll(this.options, options);
                return this;
            }

            /**
             * 设置可选值列表。
             */
            public Builder options(List<String> options) {
                this.options = options != null ? new ArrayList<String>(options) : null;
                return this;
            }

            /**
             * 设置示例值。
             */
            public Builder example(String example) {
                this.example = example;
                return this;
            }

            /**
             * 添加单个约束。
             */
            public Builder constraint(String key, Object value) {
                if (this.constraints == null) {
                    this.constraints = new LinkedHashMap<String, Object>();
                }
                this.constraints.put(key, value);
                return this;
            }

            /**
             * 设置约束条件 Map。
             */
            public Builder constraints(Map<String, Object> constraints) {
                this.constraints = constraints != null ? new LinkedHashMap<String, Object>(constraints) : null;
                return this;
            }

            /**
             * 构建 ParameterSpec。
             */
            public ParameterSpec build() {
                return new ParameterSpec(type, description, required,
                        defaultValue, options, example, constraints);
            }
        }
    }

    @Override
    public String toString() {
        return "ToolSchema{parameters=" + parameters.keySet() + '}';
    }
}
