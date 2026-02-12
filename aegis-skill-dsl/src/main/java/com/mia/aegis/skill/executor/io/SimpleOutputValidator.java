package com.mia.aegis.skill.executor.io;

import com.mia.aegis.skill.dsl.model.io.FieldSpec;
import com.mia.aegis.skill.dsl.model.io.OutputContract;
import com.mia.aegis.skill.dsl.model.io.OutputFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 简单输出校验器实现。
 *
 * <p>实现基本的输出契约校验功能：
 * <ul>
 *   <li>必填字段检查</li>
 *   <li>字段类型校验 (string, number, boolean, object, array)</li>
 *   <li>校验结果聚合（收集所有错误）</li>
 * </ul>
 * </p>
 */
public class SimpleOutputValidator implements OutputValidator {

    /**
     * 创建 SimpleOutputValidator。
     */
    public SimpleOutputValidator() {
    }

    @Override
    public OutputValidationResult validate(Object output, OutputContract contract) {
        // 空契约，跳过校验
        if (contract == null) {
            return OutputValidationResult.success();
        }

        List<OutputValidationResult.ValidationError> errors = new ArrayList<OutputValidationResult.ValidationError>();

        // 校验输出格式
        if (contract.getFormat() == OutputFormat.TEXT) {
            // 文本格式只需要输出不为空
            if (output == null) {
                errors.add(OutputValidationResult.ValidationError.nullOutput());
            } else if (!(output instanceof String)) {
                errors.add(OutputValidationResult.ValidationError.invalidOutputType("string", getTypeName(output)));
            }
            return errors.isEmpty() ? OutputValidationResult.success() : OutputValidationResult.failure(errors);
        }

        // JSON 格式校验
        // 空字段契约，跳过校验
        if (contract.isEmpty()) {
            return OutputValidationResult.success();
        }

        if (output == null) {
            // 如果有必填字段，输出不能为 null
            for (Map.Entry<String, FieldSpec> entry : contract.getFields().entrySet()) {
                if (entry.getValue().isRequired()) {
                    errors.add(OutputValidationResult.ValidationError.missingRequired(entry.getKey()));
                }
            }
            if (!errors.isEmpty()) {
                return OutputValidationResult.failure(errors);
            }
            return OutputValidationResult.success();
        }

        // 输出必须是 Map 类型
        if (!(output instanceof Map)) {
            errors.add(OutputValidationResult.ValidationError.invalidOutputType("object", getTypeName(output)));
            return OutputValidationResult.failure(errors);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> outputMap = (Map<String, Object>) output;

        // 校验每个字段
        for (Map.Entry<String, FieldSpec> entry : contract.getFields().entrySet()) {
            String fieldName = entry.getKey();
            FieldSpec fieldSpec = entry.getValue();

            Object fieldValue = outputMap.get(fieldName);

            // 检查必填字段
            if (fieldSpec.isRequired() && fieldValue == null) {
                errors.add(OutputValidationResult.ValidationError.missingRequired(fieldName));
                continue;
            }

            // 如果字段存在，检查类型
            if (fieldValue != null) {
                String expectedType = fieldSpec.getType();
                String actualType = getTypeName(fieldValue);

                if (!isTypeCompatible(fieldValue, expectedType)) {
                    errors.add(OutputValidationResult.ValidationError.typeMismatch(fieldName, expectedType, actualType));
                }
            }
        }

        return errors.isEmpty() ? OutputValidationResult.success() : OutputValidationResult.failure(errors);
    }

    /**
     * 检查值是否与期望类型兼容。
     *
     * @param value 实际值
     * @param expectedType 期望类型
     * @return 是否兼容
     */
    private boolean isTypeCompatible(Object value, String expectedType) {
        if (expectedType == null || value == null) {
            return true;
        }

        String type = expectedType.toLowerCase();

        switch (type) {
            case "string":
                return value instanceof String;

            case "number":
            case "integer":
            case "int":
            case "long":
            case "double":
            case "float":
                return value instanceof Number;

            case "boolean":
            case "bool":
                return value instanceof Boolean;

            case "object":
            case "map":
                // object 向下兼容：允许接收 string/number/boolean 等简单类型
                return value instanceof Map
                    || value instanceof String
                    || value instanceof Number
                    || value instanceof Boolean;

            case "array":
            case "list":
                return value instanceof List || value.getClass().isArray();

            case "any":
                return true;

            default:
                // 未知类型，默认通过
                return true;
        }
    }

    /**
     * 获取值的类型名称。
     *
     * @param value 值
     * @return 类型名称
     */
    private String getTypeName(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof String) {
            return "string";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof Integer || value instanceof Long) {
            return "integer";
        }
        if (value instanceof Number) {
            return "number";
        }
        if (value instanceof Map) {
            return "object";
        }
        if (value instanceof List) {
            return "array";
        }
        if (value.getClass().isArray()) {
            return "array";
        }

        return value.getClass().getSimpleName().toLowerCase();
    }
}
