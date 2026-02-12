package com.mia.aegis.skill.dsl.model.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 字段规范定义。
 *
 * <p>用于描述 InputSchema 和 OutputSchema 中的字段属性。</p>
 *
 * <p>支持的扩展属性（007-skill-api-enhancement）：</p>
 * <ul>
 *   <li>placeholder - 输入框占位提示</li>
 *   <li>defaultValue - 默认值</li>
 *   <li>options - 枚举选项列表</li>
 *   <li>uiHint - UI 渲染提示</li>
 *   <li>label - 显示标签</li>
 *   <li>validation - 验证规则</li>
 * </ul>
 */
public class FieldSpec {

    private final String type;
    private final boolean required;
    private final String description;
    private final String placeholder;
    private final Object defaultValue;
    private final List<String> options;
    private final String uiHint;
    private final String label;
    private final ValidationRule validation;

    /**
     * 创建字段规范（基本版本）。
     *
     * @param type 字段类型（string, number, boolean, object, array）
     * @param required 是否必需
     * @param description 字段描述
     */
    public FieldSpec(String type, boolean required, String description) {
        this(type, required, description, null, null, null, null, null, null);
    }

    /**
     * 创建字段规范（扩展版本）。
     *
     * @param type 字段类型
     * @param required 是否必需
     * @param description 字段描述
     * @param placeholder 输入框占位提示
     * @param defaultValue 默认值
     * @param options 枚举选项列表
     * @param uiHint UI 渲染提示
     * @param label 显示标签
     * @param validation 验证规则
     */
    public FieldSpec(String type, boolean required, String description,
                     String placeholder, Object defaultValue, List<String> options,
                     String uiHint, String label, ValidationRule validation) {
        this.type = type;
        this.required = required;
        this.description = description;
        this.placeholder = placeholder;
        this.defaultValue = defaultValue;
        this.options = options != null ? Collections.unmodifiableList(options) : null;
        this.uiHint = uiHint;
        this.label = label;
        this.validation = validation;
    }

    /**
     * 创建必需字段规范。
     *
     * @param type 字段类型
     * @return FieldSpec 实例
     */
    public static FieldSpec required(String type) {
        return new FieldSpec(type, true, null);
    }

    /**
     * 创建可选字段规范。
     *
     * @param type 字段类型
     * @return FieldSpec 实例
     */
    public static FieldSpec optional(String type) {
        return new FieldSpec(type, false, null);
    }

    /**
     * 从类型字符串创建字段规范（默认必需）。
     *
     * @param type 字段类型
     * @return FieldSpec 实例
     */
    public static FieldSpec of(String type) {
        return required(type);
    }

    public String getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDescription() {
        return description;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getUiHint() {
        return uiHint;
    }

    public String getLabel() {
        return label;
    }

    public ValidationRule getValidation() {
        return validation;
    }

    @Override
    public String toString() {
        return "FieldSpec{" +
                "type='" + type + '\'' +
                ", required=" + required +
                ", description='" + description + '\'' +
                (placeholder != null ? ", placeholder='" + placeholder + '\'' : "") +
                (defaultValue != null ? ", defaultValue=" + defaultValue : "") +
                (options != null ? ", options=" + options : "") +
                (uiHint != null ? ", uiHint='" + uiHint + '\'' : "") +
                (label != null ? ", label='" + label + '\'' : "") +
                (validation != null ? ", validation=" + validation : "") +
                '}';
    }
}

