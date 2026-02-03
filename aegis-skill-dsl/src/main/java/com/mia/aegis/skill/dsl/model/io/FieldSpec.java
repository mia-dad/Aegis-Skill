package com.mia.aegis.skill.dsl.model.io;

/**
 * 字段规范定义。
 *
 * <p>用于描述 InputSchema 和 OutputSchema 中的字段属性。</p>
 */
public class FieldSpec {

    private final String type;
    private final boolean required;
    private final String description;

    /**
     * 创建字段规范。
     *
     * @param type 字段类型（string, number, boolean, object, array）
     * @param required 是否必需
     * @param description 字段描述
     */
    public FieldSpec(String type, boolean required, String description) {
        this.type = type;
        this.required = required;
        this.description = description;
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

    @Override
    public String toString() {
        return "FieldSpec{" +
                "type='" + type + '\'' +
                ", required=" + required +
                ", description='" + description + '\'' +
                '}';
    }
}

