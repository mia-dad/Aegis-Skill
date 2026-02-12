package com.mia.aegis.skill.tools;



import java.util.Map;

/**
 * @author chenzhixuan
 * @date 2026/1/5 12:59
 * @description 从一个 JSON 对象中，根据选择规则提取子结构
 *
 * input: object        # 原始 JSON（必填）
 * select:              # 选择规则（必填）
 *   path: string       # JSON 路径（简化版）
 */


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mia.aegis.skill.exception.ToolExecutionException;
import com.mia.aegis.skill.tools.ToolOutputContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * JSON Select Tool
 *
 * <p>用于从 JSON 对象中按路径提取子结构。</p>
 *
 * <h3>输入参数</h3>
 * <ul>
 *   <li><b>input</b> (Object, 必填): 原始 JSON 数据</li>
 *   <li><b>select.path</b> (String, 必填): 选择路径，如 a.b.c、items[0].name、regions[*]</li>
 * </ul>
 *
 * <h3>输出</h3>
 * <pre>{@code
 * 任意 JSON 子结构（Object / Array / Primitive）
 * }</pre>
 */
@Component
public class JsonSelectTool extends BuiltInTool {

    private static final Logger logger = LoggerFactory.getLogger(JsonSelectTool.class);

    private static final String NAME = "json_select";
    private static final String DESCRIPTION = "Select part of JSON by path expression";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonSelectTool() {
        super(NAME, DESCRIPTION, Category.DATA_PROCESSING);
    }

    @Override
    public ValidationResult validateInput(Map<String, Object> input) {
        if (input == null) {
            return ValidationResult.failure("Input cannot be null");
        }

        if (!input.containsKey("input")) {
            return ValidationResult.failure("input is required");
        }

        Object selectObj = input.get("select");
        System.err.println("[JsonSelectTool-DEBUG] select 参数类型: " +
                (selectObj != null ? selectObj.getClass().getName() : "null") +
                ", 值: " + selectObj);
        logger.info("[JsonSelectTool] select 参数类型: {}, 值: {}",
                selectObj != null ? selectObj.getClass().getName() : "null", selectObj);

        Map<String, Object> selectMap;

        // 如果 select 是字符串，尝试解析为 JSON
        if (selectObj instanceof String) {
            try {
                selectMap = objectMapper.readValue((String) selectObj, Map.class);
                logger.info("[JsonSelectTool] 解析 select 字符串为 Map: {}", selectMap);
            } catch (Exception e) {
                logger.error("[JsonSelectTool] JSON 解析失败: {}", e.getMessage());
                return ValidationResult.failure("select must be a valid JSON object or string");
            }
        } else if (selectObj instanceof Map) {
            selectMap = (Map<String, Object>) selectObj;
            logger.info("[JsonSelectTool] select 是 Map 类型，直接使用");
        } else {
            // 兜底：尝试将对象序列化为 JSON 字符串再解析
            logger.error("[JsonSelectTool] select 类型错误: {}, 尝试序列化后解析",
                    selectObj != null ? selectObj.getClass().getName() : "null");
            try {
                String jsonStr = objectMapper.writeValueAsString(selectObj);
                logger.info("[JsonSelectTool] 序列化后的 JSON: {}", jsonStr);
                selectMap = objectMapper.readValue(jsonStr, Map.class);
                logger.info("[JsonSelectTool] 解析成功");
            } catch (Exception e) {
                logger.error("[JsonSelectTool] 序列化和解析失败: {}", e.getMessage());
                return ValidationResult.failure("select must be an object or valid JSON string");
            }
        }

        Object path = selectMap.get("path");
        if (path == null || path.toString().trim().isEmpty()) {
            return ValidationResult.failure("select.path is required");
        }

        return ValidationResult.success();
    }

    @Override
    public void execute(Map<String, Object> input, ToolOutputContext output) throws ToolExecutionException {
        logger.debug("[JsonSelectTool] 开始执行，输入参数: {}", input);

        ValidationResult validation = validateInput(input);
        if (!validation.isValid()) {
            logger.error("[JsonSelectTool] 输入验证失败: {}", validation.getErrorMessage());
            throw new ToolExecutionException(NAME, validation.getErrorMessage());
        }

        try {
            Object inputData = input.get("input");
            logger.debug("[JsonSelectTool] input 类型: {}, 值: {}", inputData != null ? inputData.getClass().getName() : "null", inputData);

            // 解析 select 参数
            Object selectObj = input.get("select");
            Map<String, Object> selectMap;

            if (selectObj instanceof String) {
                selectMap = objectMapper.readValue((String) selectObj, Map.class);
                logger.debug("[JsonSelectTool] 解析 select 字符串为 Map: {}", selectMap);
            } else {
                selectMap = (Map<String, Object>) selectObj;
            }

            JsonNode root = objectMapper.valueToTree(inputData);
            String path = selectMap.get("path").toString();

            System.err.println("[JsonSelectTool-EXECUTE] input 类型: " + (inputData != null ? inputData.getClass().getName() : "null"));
            System.err.println("[JsonSelectTool-EXECUTE] root 节点类型: " + root.getNodeType() + ", 是否为 Object: " + root.isObject());
            if (root.isObject()) {
                System.err.println("[JsonSelectTool-EXECUTE] root 字段名: " + iteratorToList(root.fieldNames()));
            }
            System.err.println("[JsonSelectTool-EXECUTE] 选择路径: '" + path + "'");

            logger.debug("[JsonSelectTool] root 节点类型: {}, 是否为 Object: {}", root.getNodeType(), root.isObject());
            if (root.isObject()) {
                logger.debug("[JsonSelectTool] root 字段名: {}", iteratorToList(root.fieldNames()));
            }
            logger.debug("[JsonSelectTool] 选择路径: '{}'", path);

            JsonNode result = selectByPath(root, path);

            logger.debug("[JsonSelectTool] 选择结果类型: {}, 是否为 null: {}", result.getNodeType(), result.isNull());

            // 序列化为 JSON 字符串写入上下文
            String resultJson = objectMapper.writeValueAsString(result);
            logger.info("[JsonSelectTool] 执行成功 - 结果长度: {}", resultJson.length());
            logger.debug("[JsonSelectTool] 输出值: {}", resultJson);

            output.put("result", resultJson);
            output.put("type", result.getNodeType().toString().toLowerCase());
            output.put("path", selectMap.get("path").toString());

        } catch (Exception e) {
            logger.error("[JsonSelectTool] 执行失败: {}", e.getMessage(), e);
            throw new ToolExecutionException(NAME, "json_select failed: " + e.getMessage(), e);
        }
    }

    @Override
    public ToolSchema getInputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<>();
        params.put("input", ToolSchema.ParameterSpec.required("object", "Input JSON"));
        params.put("select", ToolSchema.ParameterSpec.required("object", "Selection rule"));
        return new ToolSchema(params);
    }

    @Override
    public ToolSchema getOutputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<>();
        params.put("result", ToolSchema.ParameterSpec.optional("object", "Selected JSON"));
        return new ToolSchema(params);
    }

    /**
     * 将 Iterator 转换为 List（用于日志记录）。
     */
    private List<String> iteratorToList(Iterator<String> iterator) {
        List<String> list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }

    // -------------------------
    // 核心选择逻辑
    // -------------------------

    private JsonNode selectByPath(JsonNode root, String path) {
        String[] tokens = path.split("\\.");

        List<JsonNode> current = new ArrayList<>();
        current.add(root);

        for (String token : tokens) {
            List<JsonNode> next = new ArrayList<>();

            for (JsonNode node : current) {
                applyToken(node, token, next);
            }

            current = next;
        }

        if (current.isEmpty()) {
            return objectMapper.nullNode();
        }

        if (current.size() == 1) {
            return current.get(0);
        }

        return objectMapper.valueToTree(current);
    }

    private void applyToken(JsonNode node, String token, List<JsonNode> out) {
        if (token.contains("[")) {
            String field = token.substring(0, token.indexOf('['));
            String indexPart = token.substring(token.indexOf('[') + 1, token.indexOf(']'));

            JsonNode arrayNode = field.isEmpty() ? node : node.get(field);
            if (arrayNode == null || !arrayNode.isArray()) {
                return;
            }

            if ("*".equals(indexPart)) {
                arrayNode.forEach(out::add);
            } else {
                int idx = Integer.parseInt(indexPart);
                if (idx >= 0 && idx < arrayNode.size()) {
                    out.add(arrayNode.get(idx));
                }
            }

        } else {
            JsonNode child = node.get(token);
            if (child != null) {
                out.add(child);
            }
        }
    }
}

