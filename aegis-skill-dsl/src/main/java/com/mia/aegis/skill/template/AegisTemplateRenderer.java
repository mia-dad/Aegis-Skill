package com.mia.aegis.skill.template;

import com.mia.aegis.skill.exception.TemplateRenderException;
import com.mia.aegis.skill.executor.context.ExecutionContext;
import com.mia.aegis.skill.template.expr.TemplateExpression;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Aegis 自定义模板渲染器。
 *
 * <p>替代 JMustache，支持以下语法：</p>
 * <ul>
 *   <li>{@code {{variable}}} — 变量替换</li>
 *   <li>{@code {{a.b.c}}} — 嵌套属性访问</li>
 *   <li>{@code {{a + b}}}, {@code {{a * b}}} — 表达式求值（四则运算）</li>
 *   <li>{@code {{arr[0]}}}, {@code {{arr[#var]}}} — 数组索引</li>
 *   <li>{@code {{#for items}}...{{/for}}} — 循环渲染</li>
 *   <li>{@code {{_}}} — 当前循环元素</li>
 * </ul>
 *
 * @since 0.3.0
 */
public class AegisTemplateRenderer implements TemplateRenderer {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^{}]+?)\\}\\}");

    private final TemplateTokenizer tokenizer;
    private final TemplateExpressionParser expressionParser;
    private final TemplateExpressionEvaluator evaluator;

    public AegisTemplateRenderer() {
        this.tokenizer = new TemplateTokenizer();
        this.expressionParser = new TemplateExpressionParser();
        this.evaluator = new TemplateExpressionEvaluator();
    }

    @Override
    public String render(String template, Map<String, Object> context) throws TemplateRenderException {
        if (template == null || template.isEmpty()) {
            return template;
        }
        if (context == null) {
            context = Collections.<String, Object>emptyMap();
        }

        try {
            List<TemplateTokenizer.Token> tokens = tokenizer.tokenize(template);
            return renderTokens(tokens, 0, tokens.size(), context);
        } catch (TemplateRenderException e) {
            throw e;
        } catch (Exception e) {
            throw new TemplateRenderException(
                    "Failed to render template: " + e.getMessage(), template);
        }
    }

    @Override
    public String render(String template, ExecutionContext context) throws TemplateRenderException {
        if (context == null) {
            return render(template, Collections.<String, Object>emptyMap());
        }
        return render(template, context.buildVariableContext());
    }

    @Override
    public boolean isValid(String template) {
        if (template == null || template.isEmpty()) {
            return true;
        }
        try {
            tokenizer.tokenize(template);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<String> extractVariables(String template) {
        List<String> variables = new ArrayList<String>();
        if (template == null || template.isEmpty()) {
            return variables;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            String inner = matcher.group(1).trim();

            // 跳过 #for xxx 和 /for
            if (inner.startsWith("#for ") || inner.equals("/for")) {
                continue;
            }

            // 提取表达式中的变量名
            List<String> exprVars = expressionParser.extractVariableNames(inner);
            for (String var : exprVars) {
                // 提取根变量名（点号分隔路径的第一段）
                String rootVar = var.contains(".") ? var.split("\\.")[0] : var;
                if (!rootVar.isEmpty() && !rootVar.equals("_")) {
                    // 保留完整路径用于变量引用检查
                    if (!variables.contains(var)) {
                        variables.add(var);
                    }
                }
            }
        }

        return variables;
    }

    /**
     * 渲染 Map 类型的输入模板。
     *
     * <p>递归渲染 Map/List 值中的模板字符串。</p>
     *
     * @param inputTemplate 输入模板映射
     * @param context       上下文数据
     * @return 渲染后的参数映射
     * @throws TemplateRenderException 渲染失败时抛出
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> renderInputTemplate(Map<String, Object> inputTemplate,
                                                    Map<String, Object> context) throws TemplateRenderException {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> entry : inputTemplate.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            result.put(key, renderValue(value, context));
        }
        return result;
    }

    /**
     * 渲染 Map 类型的输入模板（使用 ExecutionContext）。
     *
     * @param inputTemplate 输入模板映射
     * @param context       执行上下文
     * @return 渲染后的参数映射
     * @throws TemplateRenderException 渲染失败时抛出
     */
    public Map<String, Object> renderInputTemplate(Map<String, Object> inputTemplate,
                                                    ExecutionContext context) throws TemplateRenderException {
        return renderInputTemplate(inputTemplate, context.buildVariableContext());
    }

    // ---- 内部实现 ----

    @SuppressWarnings("unchecked")
    private String renderTokens(List<TemplateTokenizer.Token> tokens, int start, int end,
                                Map<String, Object> context) throws TemplateRenderException {
        StringBuilder sb = new StringBuilder();

        int i = start;
        while (i < end) {
            TemplateTokenizer.Token token = tokens.get(i);

            switch (token.getType()) {
                case TEXT:
                    sb.append(token.getContent());
                    i++;
                    break;

                case EXPRESSION:
                    TemplateExpression expr = expressionParser.parse(token.getContent());
                    Object value = evaluator.evaluate(expr, context);
                    sb.append(evaluator.toDisplayString(value));
                    i++;
                    break;

                case FOR_START: {
                    String arrayVarName = token.getContent();

                    // 找到配对的 FOR_END
                    int forEndIdx = findMatchingForEnd(tokens, i + 1, end);
                    if (forEndIdx == -1) {
                        throw new TemplateRenderException(
                                "未找到 {{/for}} 来匹配 {{#for " + arrayVarName + "}}",
                                "");
                    }

                    // 提取循环体 tokens
                    int bodyStart = i + 1;
                    int bodyEnd = forEndIdx;

                    // 从 context 获取数组
                    Object arrayObj = resolveVariable(arrayVarName, context);
                    if (arrayObj instanceof List) {
                        List<Object> list = (List<Object>) arrayObj;
                        for (Object element : list) {
                            // 创建子 context
                            Map<String, Object> childContext = new LinkedHashMap<String, Object>(context);
                            // 设置 _ 为当前元素
                            childContext.put("_", element);
                            // 如果元素是 Map，平铺字段到子 context
                            if (element instanceof Map) {
                                Map<String, Object> elementMap = (Map<String, Object>) element;
                                childContext.putAll(elementMap);
                            }
                            // 递归渲染循环体
                            sb.append(renderTokens(tokens, bodyStart, bodyEnd, childContext));
                        }
                    }
                    // 如果不是 List，则跳过循环体

                    i = forEndIdx + 1; // 跳过 FOR_END
                    break;
                }

                case FOR_END:
                    // 不应该在这里遇到，跳过
                    i++;
                    break;

                default:
                    i++;
                    break;
            }
        }

        return sb.toString();
    }

    private int findMatchingForEnd(List<TemplateTokenizer.Token> tokens, int start, int end) {
        int depth = 1;
        for (int i = start; i < end; i++) {
            TemplateTokenizer.Token token = tokens.get(i);
            if (token.getType() == TemplateTokenizer.TokenType.FOR_START) {
                depth++;
            } else if (token.getType() == TemplateTokenizer.TokenType.FOR_END) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private Object resolveVariable(String path, Map<String, Object> context) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        // 先尝试直接查找（支持 "a.b" 作为 key）
        if (context.containsKey(path)) {
            return context.get(path);
        }

        // 点路径解析
        String[] parts = path.split("\\.");
        Object current = context.get(parts[0]);
        for (int i = 1; i < parts.length && current != null; i++) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(parts[i]);
            } else {
                return null;
            }
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private Object renderValue(Object value, Map<String, Object> context) throws TemplateRenderException {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            String str = (String) value;
            if (str.contains("{{") && str.contains("}}")) {
                return render(str, context);
            }
            return str;
        }

        if (value instanceof Map) {
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            Map<String, Object> map = (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                result.put(entry.getKey(), renderValue(entry.getValue(), context));
            }
            return result;
        }

        if (value instanceof List) {
            List<Object> result = new ArrayList<Object>();
            List<Object> list = (List<Object>) value;
            for (Object item : list) {
                result.add(renderValue(item, context));
            }
            return result;
        }

        return value;
    }
}
