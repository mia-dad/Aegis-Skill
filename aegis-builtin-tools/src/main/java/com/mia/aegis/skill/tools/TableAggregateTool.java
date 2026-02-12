package com.mia.aegis.skill.tools;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.mia.aegis.skill.exception.ToolExecutionException;
import com.mia.aegis.skill.tools.ToolOutputContext;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 表格聚合 Tool。
 *
 * <p>对表格数据执行聚合操作，支持 sum/avg/count 和可选的 group by。</p>
 *
 * <h3>输入参数</h3>
 * <ul>
 *   <li><b>data</b> (List, 必填): 表格数据（List of Map）</li>
 *   <li><b>operation</b> (String, 必填): 聚合操作，支持 sum, avg, count</li>
 *   <li><b>field</b> (String, 必填): 聚合字段名</li>
 *   <li><b>groupBy</b> (String, 可选): 分组字段名</li>
 * </ul>
 *
 * <h3>输出（无分组）</h3>
 * <pre>{@code
 * {
 *   "result": 123.45,
 *   "operation": "sum",
 *   "field": "amount",
 *   "count": 10
 * }
 * }</pre>
 *
 * <h3>输出（有分组）</h3>
 * <pre>{@code
 * {
 *   "result": {
 *     "group1": 100.0,
 *     "group2": 200.0
 *   },
 *   "operation": "sum",
 *   "field": "amount",
 *   "groupBy": "category"
 * }
 * }</pre>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * List<Map<String, Object>> data = Arrays.asList(
 *     createRow("category", "A", "amount", 100),
 *     createRow("category", "A", "amount", 200),
 *     createRow("category", "B", "amount", 150)
 * );
 *
 * Map<String, Object> input = new HashMap<>();
 * input.put("data", data);
 * input.put("operation", "sum");
 * input.put("field", "amount");
 * input.put("groupBy", "category");
 *
 * Object result = tableAggregateTool.execute(input);
 * // result.result = {"A": 300.0, "B": 150.0}
 * }</pre>
 */
@Component
public class TableAggregateTool extends BuiltInTool {

    private static final String NAME = "table_aggregate";
    private static final String DESCRIPTION = "Aggregate table data with sum/avg/count operations";

    private static final Set<String> VALID_OPERATIONS = new HashSet<String>(
            Arrays.asList("sum", "avg", "count")
    );

    /**
     * 构造表格聚合 Tool。
     */
    public TableAggregateTool() {
        super(NAME, DESCRIPTION, Category.DATA_PROCESSING);
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
        if (!(dataObj instanceof List)) {
            return ValidationResult.failure("data must be a List");
        }
        List<?> data = (List<?>) dataObj;
        if (data.isEmpty()) {
            return ValidationResult.failure("data cannot be empty");
        }

        Object operationObj = input.get("operation");
        if (operationObj == null || operationObj.toString().trim().isEmpty()) {
            return ValidationResult.failure("operation is required");
        }
        String operation = operationObj.toString().toLowerCase();
        if (!VALID_OPERATIONS.contains(operation)) {
            return ValidationResult.failure("operation must be one of: sum, avg, count");
        }

        Object fieldObj = input.get("field");
        if (fieldObj == null || fieldObj.toString().trim().isEmpty()) {
            return ValidationResult.failure("field is required");
        }

        return ValidationResult.success();
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void execute(Map<String, Object> input, ToolOutputContext output) throws ToolExecutionException {
        ValidationResult validation = validateInput(input);
        if (!validation.isValid()) {
            throw new ToolExecutionException(NAME, validation.getErrorMessage());
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) input.get("data");
        String operation = input.get("operation").toString().toLowerCase();
        String field = input.get("field").toString().trim();
        String groupBy = input.get("groupBy") != null ? input.get("groupBy").toString().trim() : null;

        try {
            output.put("operation", operation);
            output.put("field", field);

            if (groupBy != null && !groupBy.isEmpty()) {
                // 分组聚合
                Map<String, Object> groupedResult = aggregateGrouped(data, operation, field, groupBy);
                output.put("result", objectMapper.writeValueAsString(groupedResult));
            } else {
                // 全局聚合
                AggregateResult aggResult = aggregateAll(data, operation, field);
                output.put("result", aggResult.value);
                output.put("count", aggResult.count);
            }

        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolExecutionException(NAME, "Aggregation failed: " + e.getMessage(), e);
        }
    }

    private AggregateResult aggregateAll(List<Map<String, Object>> data, String operation, String field)
            throws ToolExecutionException {
        double sum = 0.0;
        int count = 0;

        for (Map<String, Object> row : data) {
            Object value = row.get(field);
            if (value != null) {
                if ("count".equals(operation)) {
                    count++;
                } else {
                    double numValue = toDouble(value, field);
                    sum += numValue;
                    count++;
                }
            }
        }

        double result;
        switch (operation) {
            case "sum":
                result = sum;
                break;
            case "avg":
                result = count > 0 ? sum / count : 0.0;
                break;
            case "count":
                result = count;
                break;
            default:
                throw new ToolExecutionException(NAME, "Unknown operation: " + operation);
        }

        return new AggregateResult(result, count);
    }

    private Map<String, Object> aggregateGrouped(List<Map<String, Object>> data,
                                                 String operation, String field, String groupBy)
            throws ToolExecutionException {
        // 按分组字段分组
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (Map<String, Object> row : data) {
            Object groupValue = row.get(groupBy);
            String groupKey = groupValue != null ? groupValue.toString() : "(null)";

            if (!groups.containsKey(groupKey)) {
                groups.put(groupKey, new ArrayList<Map<String, Object>>());
            }
            groups.get(groupKey).add(row);
        }

        // 对每个分组执行聚合
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : groups.entrySet()) {
            AggregateResult aggResult = aggregateAll(entry.getValue(), operation, field);
            result.put(entry.getKey(), aggResult.value);
        }

        return result;
    }

    private double toDouble(Object value, String field) throws ToolExecutionException {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            throw new ToolExecutionException(NAME,
                    "Cannot convert field '" + field + "' value to number: " + value);
        }
    }

    @Override
    public ToolSchema getInputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<String, ToolSchema.ParameterSpec>();
        params.put("data", ToolSchema.ParameterSpec.required("array", "Table data (list of objects)"));
        params.put("operation", ToolSchema.ParameterSpec.required("string", "Aggregate operation: sum, avg, count"));
        params.put("field", ToolSchema.ParameterSpec.required("string", "Field name to aggregate"));
        params.put("groupBy", ToolSchema.ParameterSpec.optional("string", "Field name to group by"));
        return new ToolSchema(params);
    }

    @Override
    public ToolSchema getOutputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<String, ToolSchema.ParameterSpec>();
        params.put("result", ToolSchema.ParameterSpec.required("object", "Aggregation result (number or grouped map)"));
        params.put("operation", ToolSchema.ParameterSpec.required("string", "Operation performed"));
        params.put("field", ToolSchema.ParameterSpec.required("string", "Aggregated field"));
        params.put("count", ToolSchema.ParameterSpec.optional("integer", "Number of records (non-grouped only)"));
        params.put("groupBy", ToolSchema.ParameterSpec.optional("string", "Group by field (if grouped)"));
        return new ToolSchema(params);
    }

    /**
     * 聚合结果内部类。
     */
    private static class AggregateResult {
        final double value;
        final int count;

        AggregateResult(double value, int count) {
            this.value = value;
            this.count = count;
        }
    }
}
