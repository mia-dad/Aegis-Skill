package com.mia.aegis.skill.tools;



import com.mia.aegis.skill.exception.ToolExecutionException;
import com.mia.aegis.skill.tools.ToolOutputContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设置变量 Tool。
 *
 * <p>在执行上下文中设置变量，供后续步骤使用。</p>
 *
 * <h3>输入参数</h3>
 * <ul>
 *   <li><b>name</b> (String, 必填): 变量名</li>
 *   <li><b>value</b> (Object, 必填): 变量值</li>
 * </ul>
 *
 * <h3>输出</h3>
 * <pre>{@code
 * {
 *   "name": "myVar",
 *   "value": "stored value",
 *   "success": true
 * }
 * }</pre>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * Map<String, Object> input = new HashMap<>();
 * input.put("name", "userId");
 * input.put("value", 12345);
 *
 * Object result = setVariableTool.execute(input);
 * }</pre>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>变量存储在共享的内存存储中</li>
 *   <li>同名变量会被覆盖</li>
 *   <li>变量值支持任意 JSON 可序列化类型</li>
 * </ul>
 */
@Component
public class SetVariableTool extends BuiltInTool {

    private static final Logger logger = LoggerFactory.getLogger(SetVariableTool.class);
    private static final String NAME = "set_variable";
    private static final String DESCRIPTION = "Set a variable in execution context";

    private final Map<String, Object> variableStore;

    /**
     * 构造设置变量 Tool（使用默认存储）。
     */
    public SetVariableTool() {
        this(new ConcurrentHashMap<String, Object>());
    }

    /**
     * 构造设置变量 Tool（使用指定存储）。
     *
     * @param variableStore 变量存储
     */
    public SetVariableTool(Map<String, Object> variableStore) {
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

        // value 可以是 null，表示删除变量
        // 但必须显式提供 value 键
        if (!input.containsKey("value")) {
            return ValidationResult.failure("value is required");
        }

        return ValidationResult.success();
    }

    @Override
    public void execute(Map<String, Object> input, ToolOutputContext output) throws ToolExecutionException {
        logger.debug("[SetVariableTool] 开始执行，输入参数: {}", input);

        ValidationResult validation = validateInput(input);
        if (!validation.isValid()) {
            logger.error("[SetVariableTool] 输入验证失败: {}", validation.getErrorMessage());
            throw new ToolExecutionException(NAME, validation.getErrorMessage());
        }

        String name = input.get("name").toString().trim();
        Object value = input.get("value");

        try {
            // 存储变量
            if (value != null) {
                variableStore.put(name, value);
                logger.debug("[SetVariableTool] 设置变量: name={}, value类型={}", name, value.getClass().getSimpleName());
                logger.trace("[SetVariableTool] 变量值: {}", value);
            } else {
                variableStore.remove(name);
                logger.debug("[SetVariableTool] 删除变量: name={}", name);
            }

            // 以 input 中 name 参数的值作为 key 写入上下文
            output.put(name, value);

            logger.info("[SetVariableTool] 执行成功 - name={}", name);

        } catch (Exception e) {
            logger.error("[SetVariableTool] 执行失败: {}", e.getMessage(), e);
            throw new ToolExecutionException(NAME, "Failed to set variable: " + e.getMessage(), e);
        }
    }

    @Override
    public ToolSchema getInputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<String, ToolSchema.ParameterSpec>();
        params.put("name", ToolSchema.ParameterSpec.required("string", "Variable name"));
        params.put("value", ToolSchema.ParameterSpec.required("object", "Variable value (any JSON type)"));
        return new ToolSchema(params);
    }

    @Override
    public ToolSchema getOutputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<String, ToolSchema.ParameterSpec>();
        params.put("name", ToolSchema.ParameterSpec.required("string", "Variable name"));
        params.put("value", ToolSchema.ParameterSpec.required("object", "Stored value"));
        params.put("success", ToolSchema.ParameterSpec.required("boolean", "Operation success"));
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

