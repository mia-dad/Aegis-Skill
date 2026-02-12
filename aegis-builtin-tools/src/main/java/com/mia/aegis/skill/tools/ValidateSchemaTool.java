package com.mia.aegis.skill.tools;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mia.aegis.skill.exception.ToolExecutionException;
import com.mia.aegis.skill.tools.ToolOutputContext;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * JSON Schema 校验 Tool。
 *
 * <p>使用 JSON Schema 校验数据结构。</p>
 *
 * <h3>输入参数</h3>
 * <ul>
 *   <li><b>data</b> (Object, 必填): 待校验的数据</li>
 *   <li><b>schema</b> (Object, 必填): JSON Schema 定义</li>
 * </ul>
 *
 * <h3>输出（校验成功）</h3>
 * <pre>{@code
 * {
 *   "valid": true,
 *   "errors": []
 * }
 * }</pre>
 *
 * <h3>输出（校验失败）</h3>
 * <pre>{@code
 * {
 *   "valid": false,
 *   "errors": [
 *     "$.name: is missing but it is required",
 *     "$.age: expected type: integer, found: string"
 *   ]
 * }
 * }</pre>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * Map<String, Object> schema = new HashMap<>();
 * schema.put("type", "object");
 * schema.put("required", Arrays.asList("name", "age"));
 * schema.put("properties", Map.of(
 *     "name", Map.of("type", "string"),
 *     "age", Map.of("type", "integer")
 * ));
 *
 * Map<String, Object> data = new HashMap<>();
 * data.put("name", "John");
 * data.put("age", 30);
 *
 * Map<String, Object> input = new HashMap<>();
 * input.put("data", data);
 * input.put("schema", schema);
 *
 * Object result = validateSchemaTool.execute(input);
 * // result.valid = true
 * }</pre>
 */
@Component
public class ValidateSchemaTool extends BuiltInTool {

    private static final String NAME = "validate_schema";
    private static final String DESCRIPTION = "Validate data against JSON Schema";

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;

    /**
     * 构造 JSON Schema 校验 Tool。
     */
    public ValidateSchemaTool() {
        super(NAME, DESCRIPTION, Category.REASONING_SUPPORT);
        this.objectMapper = new ObjectMapper();
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    @Override
    public ValidationResult validateInput(Map<String, Object> input) {
        if (input == null) {
            return ValidationResult.failure("Input cannot be null");
        }

        Object dataObj = input.get("data");
        if (dataObj == null) {
            return ValidationResult.failure("data is required");
        }

        Object schemaObj = input.get("schema");
        if (schemaObj == null) {
            return ValidationResult.failure("schema is required");
        }

        if (!(schemaObj instanceof Map)) {
            return ValidationResult.failure("schema must be a JSON Schema object");
        }

        return ValidationResult.success();
    }

    @Override
    public void execute(Map<String, Object> input, ToolOutputContext output) throws ToolExecutionException {
        ValidationResult validation = validateInput(input);
        if (!validation.isValid()) {
            throw new ToolExecutionException(NAME, validation.getErrorMessage());
        }

        Object data = input.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> schemaMap = (Map<String, Object>) input.get("schema");

        try {
            // 转换为 JsonNode
            JsonNode dataNode = objectMapper.valueToTree(data);
            JsonNode schemaNode = objectMapper.valueToTree(schemaMap);

            // 创建 Schema
            JsonSchema schema = schemaFactory.getSchema(schemaNode);

            // 执行校验
            Set<ValidationMessage> validationErrors = schema.validate(dataNode);

            // 写入输出上下文
            boolean isValid = validationErrors.isEmpty();
            output.put("valid", isValid);

            List<String> errors = new ArrayList<String>();
            for (ValidationMessage error : validationErrors) {
                errors.add(error.getMessage());
            }
            // 序列化 errors 为 JSON 字符串
            output.put("errors", objectMapper.writeValueAsString(errors));

        } catch (Exception e) {
            throw new ToolExecutionException(NAME, "Schema validation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public ToolSchema getInputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<String, ToolSchema.ParameterSpec>();
        params.put("data", ToolSchema.ParameterSpec.required("object", "Data to validate"));
        params.put("schema", ToolSchema.ParameterSpec.required("object", "JSON Schema definition"));
        return new ToolSchema(params);
    }

    @Override
    public ToolSchema getOutputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<String, ToolSchema.ParameterSpec>();
        params.put("valid", ToolSchema.ParameterSpec.required("boolean", "Whether data is valid"));
        params.put("errors", ToolSchema.ParameterSpec.required("array", "List of validation errors"));
        return new ToolSchema(params);
    }
}

