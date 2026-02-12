package com.mia.aegis.skill.tools;

import org.springframework.stereotype.Component;

import com.mia.aegis.skill.exception.ToolExecutionException;
import com.mia.aegis.skill.tools.ToolOutputContext;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 获取变量 Tool。
 *
 * <p>从执行上下文中获取变量值。</p>
 *
 * <h3>输入参数</h3>
 * <ul>
 *   <li><b>name</b> (String, 必填): 变量名</li>
 *   <li><b>defaultValue</b> (Object, 可选): 变量不存在时的默认值</li>
 * </ul>
 *
 * <h3>输出</h3>
 * <pre>{@code
 * {
 *   "name": "myVar",
 *   "value": "stored value",
 *   "found": true
 * }
 * }</pre>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * Map<String, Object> input = new HashMap<>();
 * input.put("name", "userId");
 * input.put("defaultValue", 0);
 *
 * Object result = getVariableTool.execute(input);
 * }</pre>
 */
@Component
public class GetVariableTool extends BuiltInTool {

    private static final String NAME = "get_variable";
    private static final String DESCRIPTION = "Get a variable from execution context";

    private final Map<String, Object> variableStore;

    /**
     * 构造获取变量 Tool（使用默认存储）。
     */
    public GetVariableTool() {
        this(new ConcurrentHashMap<String, Object>());
    }

    /**
     * 构造获取变量 Tool（使用指定存储）。
     *
     * @param variableStore 变量存储
     */
    public GetVariableTool(Map<String, Object> variableStore) {
        super(NAME, DESCRIPTION, Category.ORCHESTRATION);
        this.variableStore = variableStore != null ? variableStore : new ConcurrentHashMap<String, Object>();
    }

    @Override
    public ValidationResult validateInput(Map<String, Object> input) {
        if (input == null) {
            return ValidationResult.failure("Input cannot be null");
        }

        Object nameObj = input.get("name");
        if (nameObj == null || nameObj.toString().trim().isEmpty()) {
            return ValidationResult.failure("name is required");
        }

        return ValidationResult.success();
    }

    @Override
    public void execute(Map<String, Object> input, ToolOutputContext output) throws ToolExecutionException {
        ValidationResult validation = validateInput(input);
        if (!validation.isValid()) {
            throw new ToolExecutionException(NAME, validation.getErrorMessage());
        }

        String name = input.get("name").toString().trim();
        Object defaultValue = input.get("defaultValue");

        try {
            boolean found = variableStore.containsKey(name);
            Object value = found ? variableStore.get(name) : defaultValue;

            // 写入输出上下文
            output.put("name", name);
            output.put("value", value);
            output.put("found", found);

        } catch (Exception e) {
            throw new ToolExecutionException(NAME, "Failed to get variable: " + e.getMessage(), e);
        }
    }

    @Override
    public ToolSchema getInputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<String, ToolSchema.ParameterSpec>();
        params.put("name", ToolSchema.ParameterSpec.required("string", "Variable name"));
        params.put("defaultValue", ToolSchema.ParameterSpec.optional("object", "Default value if not found"));
        return new ToolSchema(params);
    }

    @Override
    public ToolSchema getOutputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<String, ToolSchema.ParameterSpec>();
        params.put("name", ToolSchema.ParameterSpec.required("string", "Variable name"));
        params.put("value", ToolSchema.ParameterSpec.required("object", "Variable value"));
        params.put("found", ToolSchema.ParameterSpec.required("boolean", "Whether variable was found"));
        return new ToolSchema(params);
    }

    /**
     * 获取变量存储（用于测试和共享）。
     *
     * @return 变量存储
     */
    public Map<String, Object> getVariableStore() {
        return variableStore;
    }
}
