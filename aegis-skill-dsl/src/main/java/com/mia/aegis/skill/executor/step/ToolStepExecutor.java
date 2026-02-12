package com.mia.aegis.skill.executor.step;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.mia.aegis.skill.dsl.model.Step;
import com.mia.aegis.skill.dsl.model.StepType;
import com.mia.aegis.skill.dsl.model.ToolStepConfig;
import com.mia.aegis.skill.exception.SkillExecutionException;
import com.mia.aegis.skill.exception.TemplateRenderException;
import com.mia.aegis.skill.exception.ToolExecutionException;
import com.mia.aegis.skill.executor.context.ExecutionContext;
import com.mia.aegis.skill.executor.context.StepResult;
import com.mia.aegis.skill.i18n.MessageUtil;
import com.mia.aegis.skill.template.TemplateRenderer;
import com.mia.aegis.skill.tools.ToolProvider;
import com.mia.aegis.skill.tools.ToolRegistry;
import com.mia.aegis.skill.tools.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tool 类型 Step 执行器。
 *
 * <p>负责执行 Tool 类型的 Step：
 * <ol>
 *   <li>查找 Tool Provider</li>
 *   <li>渲染输入模板</li>
 *   <li>验证输入参数</li>
 *   <li>执行 Tool 并返回结果</li>
 * </ol>
 * </p>
 */
public class ToolStepExecutor implements StepExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ToolStepExecutor.class);
    private final ToolRegistry toolRegistry;

    private final TemplateRenderer templateRenderer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建 ToolStepExecutor。
     *
     * @param toolRegistry Tool 注册表
     * @param templateRenderer 模板渲染器
     */
    public ToolStepExecutor(ToolRegistry toolRegistry, TemplateRenderer templateRenderer) {
        if (toolRegistry == null) {
            throw new IllegalArgumentException(MessageUtil.getMessage("toolstepexecutor.registry.null"));
        }
        if (templateRenderer == null) {
            throw new IllegalArgumentException(MessageUtil.getMessage("toolstepexecutor.renderer.null"));
        }
        this.toolRegistry = toolRegistry;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public StepResult execute(Step step, ExecutionContext context) throws SkillExecutionException {
        if (!supports(step)) {
            throw new SkillExecutionException(MessageUtil.getMessage("toolstepexecutor.unsupported.type"));
        }

        long startTime = System.currentTimeMillis();
        String stepName = step.getName();

        logger.debug("[ToolStepExecutor] 开始执行 Tool 步骤: {}", stepName);

        try {
            ToolStepConfig config = step.getToolConfig();
            String toolName = config.getToolName();
            logger.debug("[ToolStepExecutor] Tool 名称: {}", toolName);

            // 1. 查找 Tool Provider
            Optional<ToolProvider> optionalTool = toolRegistry.find(toolName);
            if (!optionalTool.isPresent()) {
                logger.error("[ToolStepExecutor] Tool 未找到: {}", toolName);
                throw new SkillExecutionException(MessageUtil.getMessage("toolstepexecutor.tool.notfound", toolName));
            }
            ToolProvider tool = optionalTool.get();
            logger.debug("[ToolStepExecutor] 找到 Tool: {}", tool.getClass().getSimpleName());

            // 2. 渲染输入模板
            logger.debug("[ToolStepExecutor] 原始输入模板: {}", config.getInputTemplate());
            for (Map.Entry<String, Object> entry : config.getInputTemplate().entrySet()) {
                logger.debug("[ToolStepExecutor]   {} => {} (type: {})",
                    entry.getKey(), entry.getValue(),
                    entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null");
            }

            Map<String, Object> renderedInput = renderInputTemplate(config.getInputTemplate(), context);
            logger.info("[ToolStepExecutor] 渲染后的输入参数:");
            for (Map.Entry<String, Object> entry : renderedInput.entrySet()) {
                Object value = entry.getValue();
                String type = value != null ? value.getClass().getName() : "null";
                String additionalInfo = "";
                if (value instanceof Map) {
                    additionalInfo = ", Map 键: " + ((Map<?, ?>) value).keySet();
                }
                logger.info("[ToolStepExecutor]   {} => {} (type: {}{})",
                    entry.getKey(),
                    value != null && value.toString().length() > 100 ? value.toString().substring(0, 100) + "..." : value,
                    type,
                    additionalInfo);
            }

            // 3. 验证输入参数
            // 调试：检查 select 参数的类型
            if (renderedInput.containsKey("select")) {
                Object selectObj = renderedInput.get("select");
                logger.info("[ToolStepExecutor] select 参数 - 类型: {}, instanceof String: {}, instanceof Map: {}",
                        selectObj != null ? selectObj.getClass().getName() : "null",
                        selectObj instanceof String,
                        selectObj instanceof java.util.Map);
            }

            ValidationResult validationResult = tool.validateInput(renderedInput);
            if (!validationResult.isValid()) {
                logger.error("[ToolStepExecutor] 输入验证失败: {}", String.join(", ", validationResult.getErrors()));
                throw new SkillExecutionException(MessageUtil.getMessage("toolstepexecutor.validation.failed",
                        String.join(", ", validationResult.getErrors())));
            }
            logger.debug("[ToolStepExecutor] 输入验证通过");

            // 4. 执行 Tool（工具通过 context.put() 直接写入上下文）
            logger.info("[ToolStepExecutor] 执行 Tool: {} - 开始", toolName);
            tool.execute(renderedInput, context);
            logger.info("[ToolStepExecutor] 执行 Tool: {} - 完成", toolName);

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("[ToolStepExecutor] Tool 步骤执行成功: {}, 耗时: {}ms", stepName, duration);
            return StepResult.success(stepName, null, duration);

        } catch (ToolExecutionException e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("[ToolStepExecutor] Tool 执行失败 (ToolExecutionException): {}, 耗时: {}ms", e.getMessage(), duration);
            return StepResult.failed(stepName, MessageUtil.getMessage("toolstepexecutor.execution.error", e.getMessage()), duration);
        } catch (TemplateRenderException e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("[ToolStepExecutor] 模板渲染失败: {}, 耗时: {}ms", e.getMessage(), duration);
            throw new SkillExecutionException("Template render failed: " + e.getMessage(), e);
        } catch (SkillExecutionException e) {
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("[ToolStepExecutor] Tool 步骤执行失败 (Exception): {}, 耗时: {}ms", e.getMessage(), duration);
            throw new SkillExecutionException("Failed to execute tool step: " + stepName, e);
        }
    }

    @Override
    public boolean supports(Step step) {
        return step != null && step.getType() == StepType.TOOL;
    }

    /**
     * 递归渲染输入模板。
     *
     * <p>支持以下数据类型：</p>
     * <ul>
     *   <li>String - 检查是否包含 {{var}} 语法，如果有则渲染</li>
     *   <li>Map - 递归渲染每个值</li>
     *   <li>List - 递归渲染每个元素</li>
     *   <li>其他类型 - 直接返回</li>
     * </ul>
     *
     * @param inputTemplate 输入模板（可能包含嵌套对象）
     * @param context 执行上下文
     * @return 渲染后的输入参数
     * @throws TemplateRenderException 渲染失败时抛出
     */
    private Map<String, Object> renderInputTemplate(Map<String, Object> inputTemplate,
                                                    ExecutionContext context) throws TemplateRenderException {
        if (inputTemplate == null || inputTemplate.isEmpty()) {
            return new HashMap<String, Object>();
        }

        Map<String, Object> renderedInput = new HashMap<String, Object>();
        Map<String, Object> variableContext = context.buildVariableContext();

        for (Map.Entry<String, Object> entry : inputTemplate.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            renderedInput.put(key, renderValue(value, variableContext));
        }

        return renderedInput;
    }

    /**
     * 递归渲染值。
     *
     * @param value 原始值
     * @param variableContext 变量上下文
     * @return 渲染后的值
     * @throws TemplateRenderException 渲染失败时抛出
     */
    @SuppressWarnings("unchecked")
    private Object renderValue(Object value, Map<String, Object> variableContext) throws TemplateRenderException {
        if (value == null) {
            return null;
        }

        // 字符串：检查是否包含模板语法
        if (value instanceof String) {
            String strValue = (String) value;
            if (containsTemplate(strValue)) {
                // 检查是否是简单的变量引用（如 {{var}} 或 {{var.field}}）
                String trimmed = strValue.trim();
                if (trimmed.matches("^\\{\\{\\s*[\\w.]+\\s*\\}\\}$")) {
                    // 简单变量引用：直接返回变量值（支持点号路径）
                    String varPath = trimmed.substring(2, trimmed.length() - 2).trim();
                    Object varValue = resolveVariablePath(varPath, variableContext);
                    logger.debug("[ToolStepExecutor] 简单变量引用: {} => {} (type: {})",
                            strValue, varValue != null ? "找到值" : "null",
                            varValue != null ? varValue.getClass().getSimpleName() : "null");
                    return varValue;
                }
                // 复杂模板：先检查是否是 JSON 格式
                if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                    try {
                        // 尝试解析为 JSON 对象
                        Object jsonObj = objectMapper.readValue(trimmed, Object.class);
                        if (jsonObj instanceof Map) {
                            // 递归渲染 Map 中的值
                            Map<String, Object> renderedMap = new LinkedHashMap<String, Object>();
                            for (Map.Entry<String, Object> entry : ((Map<String, Object>) jsonObj).entrySet()) {
                                renderedMap.put(entry.getKey(), renderValue(entry.getValue(), variableContext));
                            }
                            // 直接返回 Map 对象（不再序列化为 JSON 字符串）
                            return renderedMap;
                        } else if (jsonObj instanceof List) {
                            // 递归渲染 List 中的值
                            List<Object> renderedList = new ArrayList<Object>();
                            for (Object item : (List<Object>) jsonObj) {
                                renderedList.add(renderValue(item, variableContext));
                            }
                            // 直接返回 List 对象（不再序列化为 JSON 字符串）
                            return renderedList;
                        }
                    } catch (Exception e) {
                        logger.debug("[ToolStepExecutor] JSON 解析失败，使用普通模板渲染: {}", e.getMessage());
                    }
                }
                // 普通（非 JSON）复杂模板：渲染后解析为 YAML/JSON
                String rendered = templateRenderer.render(strValue, variableContext);
                logger.trace("[ToolStepExecutor] 复杂模板渲染: {} => {}", strValue, rendered);
                return parseRenderedValue(rendered);
            }
            // 不包含模板语法，直接返回字符串
            return strValue;
        }

        // Map：递归渲染
        if (value instanceof Map) {
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            Map<String, Object> map = (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                result.put(entry.getKey(), renderValue(entry.getValue(), variableContext));
            }
            return result;
        }

        // List：递归渲染
        if (value instanceof List) {
            List<Object> result = new ArrayList<Object>();
            List<Object> list = (List<Object>) value;
            for (Object item : list) {
                result.add(renderValue(item, variableContext));
            }
            return result;
        }

        // 其他类型（Number, Boolean 等）：直接返回
        return value;
    }

    /**
     * 解析变量路径，支持点号分隔的嵌套访问。
     *
     * <p>支持 Map 键访问和 POJO getter 属性访问：</p>
     * <ul>
     *   <li>Map: 通过 {@code map.get(field)} 访问</li>
     *   <li>POJO: 通过反射调用 getter 方法（getField / isField）访问</li>
     * </ul>
     *
     * @param path 变量路径（如 "var" 或 "var.field" 或 "var.field.nested"）
     * @param variableContext 变量上下文
     * @return 解析后的值
     */
    @SuppressWarnings("unchecked")
    private Object resolveVariablePath(String path, Map<String, Object> variableContext) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        // 分割路径
        String[] parts = path.split("\\.");

        // 获取根变量
        Object currentValue = variableContext.get(parts[0]);
        if (currentValue == null) {
            logger.debug("[ToolStepExecutor] 变量不存在: {}, 可用变量: {}", parts[0], variableContext.keySet());
            return null;
        }

        // 遍历路径
        for (int i = 1; i < parts.length; i++) {
            if (currentValue == null) {
                logger.debug("[ToolStepExecutor] 路径解析失败: {} (字段 {} 为 null)", path, parts[i - 1]);
                return null;
            }
            currentValue = resolveField(currentValue, parts[i]);
            if (currentValue == null) {
                logger.debug("[ToolStepExecutor] 路径解析失败: {} (字段 {} 不存在或不可访问)", path, parts[i]);
                return null;
            }
        }

        return currentValue;
    }

    /**
     * 从对象中解析单个字段，支持 Map 和 POJO。
     *
     * @param target 目标对象
     * @param field 字段名
     * @return 字段值，找不到返回 null
     */
    @SuppressWarnings("unchecked")
    private Object resolveField(Object target, String field) {
        // 1. Map 访问
        if (target instanceof Map) {
            return ((Map<String, Object>) target).get(field);
        }

        // 2. POJO getter 访问（反射）
        String capitalized = Character.toUpperCase(field.charAt(0)) + field.substring(1);
        // 尝试 getXxx()
        try {
            java.lang.reflect.Method getter = target.getClass().getMethod("get" + capitalized);
            return getter.invoke(target);
        } catch (NoSuchMethodException e) {
            // 继续尝试 isXxx()
        } catch (Exception e) {
            logger.debug("[ToolStepExecutor] 反射调用 get{}() 失败: {}", capitalized, e.getMessage());
            return null;
        }
        // 尝试 isXxx()（布尔属性）
        try {
            java.lang.reflect.Method isGetter = target.getClass().getMethod("is" + capitalized);
            return isGetter.invoke(target);
        } catch (NoSuchMethodException e) {
            // 继续尝试直接字段访问
        } catch (Exception e) {
            logger.debug("[ToolStepExecutor] 反射调用 is{}() 失败: {}", capitalized, e.getMessage());
            return null;
        }
        // 3. 直接字段访问（public field）
        try {
            java.lang.reflect.Field f = target.getClass().getField(field);
            return f.get(target);
        } catch (NoSuchFieldException e) {
            logger.debug("[ToolStepExecutor] 对象 {} 无法解析字段 '{}'", target.getClass().getSimpleName(), field);
        } catch (Exception e) {
            logger.debug("[ToolStepExecutor] 字段访问 {} 失败: {}", field, e.getMessage());
        }
        return null;
    }

    /**
     * 解析渲染后的值，尝试转换为适当的类型。
     *
     * <p>仅尝试 JSON 解析（严格的 {} / [] 边界）。
     * 不使用 YAML 解析，因为 YAML 会将包含冒号的普通文本误解析为 Map。</p>
     *
     * @param value 字符串值
     * @return 解析后的值
     */
    private Object parseRenderedValue(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        // 尝试解析为 JSON 对象（格式：{"key":"value"}）
        if (trimmed.startsWith("{")) {
            try {
                return objectMapper.readValue(trimmed, Object.class);
            } catch (Exception e) {
                logger.debug("[ToolStepExecutor] JSON 解析失败，保留为字符串: {}", value);
            }
        }

        // 尝试解析为 JSON 数组（格式：[1,2,3]）
        if (trimmed.startsWith("[")) {
            try {
                return objectMapper.readValue(trimmed, Object.class);
            } catch (Exception e) {
                logger.debug("[ToolStepExecutor] JSON 数组解析失败，保留为字符串: {}", value);
            }
        }

        // 默认返回字符串
        return value;
    }

    /**
     * 解析值，尝试转换为适当的类型。
     *
     * @param value 字符串值
     * @return 解析后的值
     */
    private Object parseValue(String value) {
        if (value == null) {
            return null;
        }

        // 尝试解析为数字
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            // 不是数字，继续
        }

        // 布尔值
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }

        // 默认返回字符串
        return value;
    }

    /**
     * 检查字符串是否包含模板语法。
     *
     * @param value 要检查的字符串
     * @return 是否包含模板语法
     */
    private boolean containsTemplate(String value) {
        return value != null && value.contains("{{") && value.contains("}}");
    }
}
