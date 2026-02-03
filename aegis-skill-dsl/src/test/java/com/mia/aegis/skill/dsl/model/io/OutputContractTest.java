package com.mia.aegis.skill.dsl.model.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OutputContract 的单元测试。
 *
 * 测试覆盖：
 * - 正常创建和使用
 * - 静态工厂方法
 * - 不同格式的输出契约
 * - null值处理
 * - 不可变性验证
 */
@DisplayName("OutputContract 测试")
class OutputContractTest {

    @Test
    @DisplayName("应该成功创建包含字段的OutputContract")
    void shouldCreateOutputContractWithFields() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("result", FieldSpec.of("string"));
        fields.put("count", FieldSpec.of("number"));

        OutputContract contract = new OutputContract(fields, OutputFormat.JSON);

        assertThat(contract.getFieldNames()).hasSize(2);
        assertThat(contract.getField("result")).isNotNull();
        assertThat(contract.getField("count")).isNotNull();
        assertThat(contract.getFormat()).isEqualTo(OutputFormat.JSON);
    }

    @Test
    @DisplayName("应该成功创建空的OutputContract")
    void shouldCreateEmptyOutputContract() {
        OutputContract contract = OutputContract.empty();

        assertThat(contract.isEmpty()).isTrue();
        assertThat(contract.getFieldNames()).isEmpty();
        assertThat(contract.getFormat()).isEqualTo(OutputFormat.JSON);
    }

    @Test
    @DisplayName("应该成功创建JSON格式的OutputContract")
    void shouldCreateJsonOutputContract() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("data", FieldSpec.of("string"));

        OutputContract contract = OutputContract.json(fields);

        assertThat(contract.getFormat()).isEqualTo(OutputFormat.JSON);
        assertThat(contract.getFieldNames()).contains("data");
    }

    @Test
    @DisplayName("应该成功创建TEXT格式的OutputContract")
    void shouldCreateTextOutputContract() {
        OutputContract contract = OutputContract.text();

        assertThat(contract.getFormat()).isEqualTo(OutputFormat.TEXT);
        assertThat(contract.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("null字段应该被视为空契约")
    void shouldTreatNullFieldsAsEmptyContract() {
        OutputContract contract = new OutputContract(null, OutputFormat.JSON);

        assertThat(contract.isEmpty()).isTrue();
        assertThat(contract.getFieldNames()).isEmpty();
    }

    @Test
    @DisplayName("null格式应该默认为JSON")
    void shouldDefaultToJsonWhenFormatIsNull() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("result", FieldSpec.of("string"));

        OutputContract contract = new OutputContract(fields, null);

        assertThat(contract.getFormat()).isEqualTo(OutputFormat.JSON);
    }

    @Test
    @DisplayName("应该能够获取指定的字段")
    void shouldGetFieldByName() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        FieldSpec resultSpec = FieldSpec.of("string");
        fields.put("result", resultSpec);

        OutputContract contract = new OutputContract(fields, OutputFormat.JSON);

        assertThat(contract.getField("result")).isEqualTo(resultSpec);
        assertThat(contract.getField("unknown")).isNull();
    }

    @Test
    @DisplayName("字段映射应该是不可变的")
    void fieldsShouldBeUnmodifiable() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("result", FieldSpec.of("string"));

        OutputContract contract = new OutputContract(fields, OutputFormat.JSON);
        Map<String, FieldSpec> contractFields = contract.getFields();

        assertThatThrownBy(() -> contractFields.put("data", FieldSpec.of("string")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("字段名集合应该是不可变的")
    void fieldNamesShouldBeUnmodifiable() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("result", FieldSpec.of("string"));

        OutputContract contract = new OutputContract(fields, OutputFormat.JSON);
        Set<String> fieldNames = contract.getFieldNames();

        assertThatThrownBy(() -> fieldNames.add("data"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("构造函数应该防御性复制字段映射")
    void constructorShouldDefensivelyCopyFields() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("result", FieldSpec.of("string"));

        OutputContract contract = new OutputContract(fields, OutputFormat.JSON);
        fields.put("data", FieldSpec.of("string"));

        assertThat(contract.getField("data")).isNull();
        assertThat(contract.getFieldNames()).hasSize(1);
    }

    @Test
    @DisplayName("getField应该正确判断字段是否存在")
    void getFieldShouldCheckFieldExistence() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("result", FieldSpec.of("string"));

        OutputContract contract = new OutputContract(fields, OutputFormat.JSON);

        assertThat(contract.getField("result")).isNotNull();
        assertThat(contract.getField("unknown")).isNull();
        assertThat(contract.getField("")).isNull();
        assertThat(contract.getField(null)).isNull();
    }

    @Test
    @DisplayName("toString应该包含字段和格式信息")
    void toStringShouldContainFieldAndFormatInfo() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("result", FieldSpec.of("string"));

        OutputContract contract = new OutputContract(fields, OutputFormat.JSON);

        assertThat(contract.toString()).contains("result");
        assertThat(contract.toString()).contains("JSON");
    }

    @Test
    @DisplayName("应该处理复杂的FieldSpec")
    void shouldHandleComplexFieldSpec() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        FieldSpec complexSpec = new FieldSpec("array", true, "结果列表");
        fields.put("results", complexSpec);

        OutputContract contract = new OutputContract(fields, OutputFormat.JSON);

        assertThat(contract.getField("results").getType()).isEqualTo("array");
        assertThat(contract.getField("results").isRequired()).isTrue();
        assertThat(contract.getField("results").getDescription()).isEqualTo("结果列表");
    }

    @Test
    @DisplayName("getFieldNames应该返回所有字段名")
    void getFieldNamesShouldReturnAllFieldNames() {
        Map<String, FieldSpec> fields = new HashMap<String, FieldSpec>();
        fields.put("result", FieldSpec.of("string"));
        fields.put("count", FieldSpec.of("number"));
        fields.put("success", FieldSpec.of("boolean"));

        OutputContract contract = new OutputContract(fields, OutputFormat.JSON);

        assertThat(contract.getFieldNames()).containsExactlyInAnyOrder("result", "count", "success");
    }

    @Test
    @DisplayName("空字段映射的OutputContract应该返回空映射")
    void emptyFieldsMapShouldReturnEmptyMap() {
        Map<String, FieldSpec> emptyFields = new HashMap<String, FieldSpec>();
        OutputContract contract = new OutputContract(emptyFields, OutputFormat.TEXT);

        assertThat(contract.getFields()).isEmpty();
        assertThat(contract.isEmpty()).isTrue();
        assertThat(contract.getFormat()).isEqualTo(OutputFormat.TEXT);
    }

    @Test
    @DisplayName("text()工厂方法应该创建TEXT格式的空契约")
    void textFactoryMethodShouldCreateTextFormatEmptyContract() {
        OutputContract contract = OutputContract.text();

        assertThat(contract.getFormat()).isEqualTo(OutputFormat.TEXT);
        assertThat(contract.getFields()).isEmpty();
        assertThat(contract.isEmpty()).isTrue();
    }
}
