package com.mia.aegis.skill.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ToolSchema 的单元测试。
 *
 * 测试覆盖：
 * - 创建 Schema
 * - 参数获取
 * - 参数存在性检查
 * - ParameterSpec 工厂方法
 * - 边界情况
 */
@DisplayName("ToolSchema 测试")
class ToolSchemaTest {

    @Test
    @DisplayName("应该创建包含参数的ToolSchema")
    void shouldCreateToolSchemaWithParameters() {
        Map<String, ToolSchema.ParameterSpec> parameters = new HashMap<String, ToolSchema.ParameterSpec>();
        parameters.put("param1", ToolSchema.ParameterSpec.required("string", "First parameter"));
        parameters.put("param2", ToolSchema.ParameterSpec.optional("integer", "Second parameter"));

        ToolSchema schema = new ToolSchema(parameters);

        assertThat(schema.getParameters()).hasSize(2);
        assertThat(schema.getParameters()).containsKeys("param1", "param2");
    }

    @Test
    @DisplayName("应该创建空的ToolSchema")
    void shouldCreateEmptyToolSchema() {
        ToolSchema schema = ToolSchema.empty();

        assertThat(schema.getParameters()).isEmpty();
    }

    @Test
    @DisplayName("应该创建空ToolSchema当传入null")
    void shouldCreateEmptyToolSchemaWhenNullPassed() {
        ToolSchema schema = new ToolSchema(null);

        assertThat(schema.getParameters()).isEmpty();
    }

    @Test
    @DisplayName("应该返回不可修改的参数映射")
    void shouldReturnUnmodifiableParameterMap() {
        Map<String, ToolSchema.ParameterSpec> parameters = new HashMap<String, ToolSchema.ParameterSpec>();
        parameters.put("param1", ToolSchema.ParameterSpec.required("string", "First parameter"));

        ToolSchema schema = new ToolSchema(parameters);

        assertThatThrownBy(() -> schema.getParameters().put("param2", null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("应该能够获取指定的参数规范")
    void shouldGetSpecificParameterSpec() {
        Map<String, ToolSchema.ParameterSpec> parameters = new HashMap<String, ToolSchema.ParameterSpec>();
        ToolSchema.ParameterSpec spec = ToolSchema.ParameterSpec.required("string", "First parameter");
        parameters.put("param1", spec);

        ToolSchema schema = new ToolSchema(parameters);

        assertThat(schema.getParameter("param1")).isSameAs(spec);
    }

    @Test
    @DisplayName("获取不存在的参数应该返回null")
    void shouldReturnNullWhenGettingNonExistentParameter() {
        ToolSchema schema = ToolSchema.empty();

        assertThat(schema.getParameter("nonexistent")).isNull();
    }

    @Test
    @DisplayName("应该正确检查参数是否存在")
    void shouldCheckParameterExistence() {
        Map<String, ToolSchema.ParameterSpec> parameters = new HashMap<String, ToolSchema.ParameterSpec>();
        parameters.put("param1", ToolSchema.ParameterSpec.required("string", "First parameter"));

        ToolSchema schema = new ToolSchema(parameters);

        assertThat(schema.hasParameter("param1")).isTrue();
        assertThat(schema.hasParameter("nonexistent")).isFalse();
        assertThat(schema.hasParameter(null)).isFalse();
    }

    @Test
    @DisplayName("toString应该正确表示ToolSchema")
    void toStringShouldRepresentToolSchemaCorrectly() {
        Map<String, ToolSchema.ParameterSpec> parameters = new HashMap<String, ToolSchema.ParameterSpec>();
        parameters.put("param1", ToolSchema.ParameterSpec.required("string", "First parameter"));
        parameters.put("param2", ToolSchema.ParameterSpec.optional("integer", "Second parameter"));

        ToolSchema schema = new ToolSchema(parameters);

        assertThat(schema.toString()).isEqualTo("ToolSchema{parameters=[param1, param2]}");
    }

    @Test
    @DisplayName("toString应该正确表示空的ToolSchema")
    void toStringShouldRepresentEmptyToolSchemaCorrectly() {
        ToolSchema schema = ToolSchema.empty();

        assertThat(schema.toString()).isEqualTo("ToolSchema{parameters=[]}");
    }

    // ParameterSpec 测试

    @Test
    @DisplayName("应该创建必需的参数规范")
    void shouldCreateRequiredParameterSpec() {
        ToolSchema.ParameterSpec spec = ToolSchema.ParameterSpec.required("string", "A required parameter");

        assertThat(spec.getType()).isEqualTo("string");
        assertThat(spec.getDescription()).isEqualTo("A required parameter");
        assertThat(spec.isRequired()).isTrue();
    }

    @Test
    @DisplayName("应该创建可选的参数规范")
    void shouldCreateOptionalParameterSpec() {
        ToolSchema.ParameterSpec spec = ToolSchema.ParameterSpec.optional("integer", "An optional parameter");

        assertThat(spec.getType()).isEqualTo("integer");
        assertThat(spec.getDescription()).isEqualTo("An optional parameter");
        assertThat(spec.isRequired()).isFalse();
    }

    @Test
    @DisplayName("toString应该正确表示ParameterSpec")
    void toStringShouldRepresentParameterSpecCorrectly() {
        ToolSchema.ParameterSpec spec = ToolSchema.ParameterSpec.required("string", "A parameter");

        assertThat(spec.toString()).isEqualTo("ParameterSpec{type='string', required=true}");
    }

    @Test
    @DisplayName("应该支持各种类型的参数")
    void shouldSupportVariousParameterTypes() {
        ToolSchema.ParameterSpec stringSpec = ToolSchema.ParameterSpec.required("string", "desc");
        ToolSchema.ParameterSpec intSpec = ToolSchema.ParameterSpec.required("integer", "desc");
        ToolSchema.ParameterSpec boolSpec = ToolSchema.ParameterSpec.required("boolean", "desc");
        ToolSchema.ParameterSpec arraySpec = ToolSchema.ParameterSpec.required("array", "desc");
        ToolSchema.ParameterSpec objectSpec = ToolSchema.ParameterSpec.required("object", "desc");

        assertThat(stringSpec.getType()).isEqualTo("string");
        assertThat(intSpec.getType()).isEqualTo("integer");
        assertThat(boolSpec.getType()).isEqualTo("boolean");
        assertThat(arraySpec.getType()).isEqualTo("array");
        assertThat(objectSpec.getType()).isEqualTo("object");
    }

    @Test
    @DisplayName("应该保持参数的插入顺序")
    void shouldMaintainParameterInsertionOrder() {
        Map<String, ToolSchema.ParameterSpec> parameters = new HashMap<String, ToolSchema.ParameterSpec>();
        parameters.put("param3", ToolSchema.ParameterSpec.required("string", "Third"));
        parameters.put("param1", ToolSchema.ParameterSpec.required("string", "First"));
        parameters.put("param2", ToolSchema.ParameterSpec.required("string", "Second"));

        ToolSchema schema = new ToolSchema(parameters);

        // LinkedHashMap 应该保持插入顺序
        assertThat(schema.getParameters().keySet())
                .containsExactly("param3", "param1", "param2");
    }
}
