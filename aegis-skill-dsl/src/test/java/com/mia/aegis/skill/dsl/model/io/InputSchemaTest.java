package com.mia.aegis.skill.dsl.model.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * InputSchema 的单元测试。
 *
 * 测试覆盖：
 * - 正常创建和使用
 * - 空Schema的创建
 * - 字段的获取和查询
 * - null值处理
 * - 不可变性验证
 */
@DisplayName("InputSchema 测试")
class InputSchemaTest {

    @Test
    @DisplayName("应该成功创建包含字段的InputSchema")
    void shouldCreateInputSchemaWithFields() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("name", FieldSpec.of("string"));
        fields.put("age", FieldSpec.of("number"));

        InputSchema schema = new InputSchema(fields);

        assertThat(schema.getFieldNames()).hasSize(2);
        assertThat(schema.hasField("name")).isTrue();
        assertThat(schema.hasField("age")).isTrue();
    }

    @Test
    @DisplayName("应该成功创建空的InputSchema")
    void shouldCreateEmptyInputSchema() {
        InputSchema schema = InputSchema.empty();

        assertThat(schema.isEmpty()).isTrue();
        assertThat(schema.getFieldNames()).isEmpty();
    }

    @Test
    @DisplayName("null值应该被视为空Schema")
    void shouldTreatNullAsEmptySchema() {
        InputSchema schema = new InputSchema(null);

        assertThat(schema.isEmpty()).isTrue();
        assertThat(schema.getFieldNames()).isEmpty();
    }

    @Test
    @DisplayName("应该能够获取指定的字段")
    void shouldGetFieldByName() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        FieldSpec nameSpec = FieldSpec.of("string");
        fields.put("name", nameSpec);

        InputSchema schema = new InputSchema(fields);

        assertThat(schema.getField("name")).isEqualTo(nameSpec);
        assertThat(schema.getField("unknown")).isNull();
    }

    @Test
    @DisplayName("字段映射应该是不可变的")
    void fieldsShouldBeUnmodifiable() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("name", FieldSpec.of("string"));

        InputSchema schema = new InputSchema(fields);
        Map<String, FieldSpec> schemaFields = schema.getFields();

        assertThatThrownBy(() -> schemaFields.put("age", FieldSpec.of("number")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("字段名集合应该是不可变的")
    void fieldNamesShouldBeUnmodifiable() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("name", FieldSpec.of("string"));

        InputSchema schema = new InputSchema(fields);
        Set<String> fieldNames = schema.getFieldNames();

        assertThatThrownBy(() -> fieldNames.add("age"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("构造函数应该防御性复制字段映射")
    void constructorShouldDefensivelyCopyFields() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("name", FieldSpec.of("string"));

        InputSchema schema = new InputSchema(fields);
        fields.put("age", FieldSpec.of("number"));

        assertThat(schema.hasField("age")).isFalse();
        assertThat(schema.getFieldNames()).hasSize(1);
    }

    @Test
    @DisplayName("hasField应该正确判断字段是否存在")
    void hasFieldShouldCheckFieldExistence() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("name", FieldSpec.of("string"));

        InputSchema schema = new InputSchema(fields);

        assertThat(schema.hasField("name")).isTrue();
        assertThat(schema.hasField("unknown")).isFalse();
        assertThat(schema.hasField("")).isFalse();
        assertThat(schema.hasField(null)).isFalse();
    }

    @Test
    @DisplayName("toString应该包含字段信息")
    void toStringShouldContainFieldInfo() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("name", FieldSpec.of("string"));

        InputSchema schema = new InputSchema(fields);

        // 匹配实际的中文输出
        assertThat(schema.toString()).contains("技能入参");
        assertThat(schema.toString()).contains("name");
        assertThat(schema.toString()).contains("fields");
    }

    @Test
    @DisplayName("应该处理复杂的FieldSpec")
    void shouldHandleComplexFieldSpec() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        FieldSpec complexSpec = new FieldSpec("object", true, "用户信息");
        fields.put("user", complexSpec);

        InputSchema schema = new InputSchema(fields);

        assertThat(schema.getField("user").getType()).isEqualTo("object");
        assertThat(schema.getField("user").isRequired()).isTrue();
        assertThat(schema.getField("user").getDescription()).isEqualTo("用户信息");
    }

    @Test
    @DisplayName("应该处理可选字段")
    void shouldHandleOptionalFields() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("required", FieldSpec.required("string"));
        fields.put("optional", FieldSpec.optional("string"));

        InputSchema schema = new InputSchema(fields);

        assertThat(schema.getField("required").isRequired()).isTrue();
        assertThat(schema.getField("optional").isRequired()).isFalse();
    }

    @Test
    @DisplayName("getFieldNames应该返回所有字段名")
    void getFieldNamesShouldReturnAllFieldNames() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("name", FieldSpec.of("string"));
        fields.put("age", FieldSpec.of("number"));
        fields.put("email", FieldSpec.of("string"));

        InputSchema schema = new InputSchema(fields);

        assertThat(schema.getFieldNames()).containsExactlyInAnyOrder("name", "age", "email");
    }

    @Test
    @DisplayName("空字段映射的InputSchema应该返回空映射")
    void emptyFieldsMapShouldReturnEmptyMap() {
        Map<String, FieldSpec> emptyFields = new HashMap<String, FieldSpec>();
        InputSchema schema = new InputSchema(emptyFields);

        assertThat(schema.getFields()).isEmpty();
        assertThat(schema.isEmpty()).isTrue();
    }
}
