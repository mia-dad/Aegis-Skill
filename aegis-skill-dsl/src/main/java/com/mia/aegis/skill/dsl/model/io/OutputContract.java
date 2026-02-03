package com.mia.aegis.skill.dsl.model.io;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


/**
 * 输出结构契约定义。
 *
 * <p>描述 Skill 执行结果的结构规范，用于校验输出是否符合预期。</p>
 */
public class OutputContract {


    private final Map<String, FieldSpec> fields;
    private final OutputFormat format;

    /**
     * 创建输出契约。
     *
     * @param fields 字段映射
     * @param format 输出格式
     */
    public OutputContract(Map<String, FieldSpec> fields, OutputFormat format) {
        this.fields = fields != null
                ? Collections.unmodifiableMap(new LinkedHashMap<String, FieldSpec>(fields))
                : Collections.<String, FieldSpec>emptyMap();
        this.format = format != null ? format : OutputFormat.JSON;
    }

    /**
     * 创建 JSON 格式输出契约。
     *
     * @param fields 字段映射
     * @return OutputContract 实例
     */
    public static OutputContract json(Map<String, FieldSpec> fields) {
        return new OutputContract(fields, OutputFormat.JSON);
    }

    /**
     * 创建文本格式输出契约。
     *
     * @return OutputContract 实例
     */
    public static OutputContract text() {
        return new OutputContract(null, OutputFormat.TEXT);
    }

    /**
     * 创建空的输出契约。
     *
     * @return 空 OutputContract
     */
    public static OutputContract empty() {
        return new OutputContract(null, OutputFormat.JSON);
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
     * 获取输出格式。
     *
     * @return 输出格式
     */
    public OutputFormat getFormat() {
        return format;
    }

    /**
     * 检查是否为空契约。
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return fields.isEmpty();
    }

    @Override
    public String toString() {
        return "OutputContract{fields=" + fields + ", format=" + format + '}';
    }
}
