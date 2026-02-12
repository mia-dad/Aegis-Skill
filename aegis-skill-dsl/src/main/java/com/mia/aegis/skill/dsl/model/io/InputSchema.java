package com.mia.aegis.skill.dsl.model.io;

import com.mia.aegis.skill.i18n.MessageUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 *   输入参数结构定义。
 *
 * <p>描述 Skill 执行所需的输入参数及其类型。</p>
 */
public class InputSchema {

    private final Map<String, FieldSpec> fields;

    /**
     * 创建输入 Schema。
     *
     * @param fields 字段映射
     */
    public InputSchema(Map<String, FieldSpec> fields) {
        this.fields = fields != null
                ? Collections.unmodifiableMap(new LinkedHashMap<String, FieldSpec>(fields))
                : Collections.<String, FieldSpec>emptyMap();
    }

    /**
     * 创建空的输入 Schema。
     *
     * @return 空 InputSchema
     */
    public static InputSchema empty() {
        return new InputSchema(null);
    }

    /**
     * 获取所有字段定义。
     *
     * @return 不可变的字段映射
     */
    public Map<String, FieldSpec> getFields() {
        return fields;
    }

    /**
     * 获取字段名集合。
     *
     * @return 字段名集合
     */
    public Set<String> getFieldNames() {
        return fields.keySet();
    }

    /**
     * 获取指定字段的规范。
     *
     * @param fieldName 字段名
     * @return 字段规范，不存在返回 null
     */
    public FieldSpec getField(String fieldName) {
        return fields.get(fieldName);
    }

    /**
     * 检查字段是否存在。
     *
     * @param fieldName 字段名
     * @return 是否存在
     */
    public boolean hasField(String fieldName) {
        return fields.containsKey(fieldName);
    }

    /**
     * 检查是否为空 Schema。
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return fields.isEmpty();
    }

    @Override
    public String toString() {
        return MessageUtil.getMessage("inputschema.tostring") + fields.toString() + '}';
    }
}

