package com.mia.aegis.skill.template;


import com.mia.aegis.skill.exception.TemplateRenderException;
import com.mia.aegis.skill.executor.context.ExecutionContext;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheException;
import com.samskivert.mustache.Template;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于 JMustache 的模板渲染器实现。
 *
 * @deprecated 请使用 {@link AegisTemplateRenderer} 代替。此类保留以兼容旧代码，后续版本将移除。
 */
@Deprecated
public class MustacheTemplateRenderer implements TemplateRenderer {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^{}#/]+?)\\}\\}");

    private final Mustache.Compiler compiler;
    private final boolean escapeHtml;

    /**
     * 创建默认配置的渲染器（不进行HTML转义）。
     */
    public MustacheTemplateRenderer() {
        this(false);
    }

    /**
     * 创建自定义配置的渲染器。
     *
     * @param escapeHtml 是否启用HTML转义（默认false）
     */
    public MustacheTemplateRenderer(boolean escapeHtml) {
        this.escapeHtml = escapeHtml;
        this.compiler = Mustache.compiler()
                .escapeHTML(escapeHtml)
                .nullValue("")
                .defaultValue("");
    }

    /**
     * 创建严格模式的渲染器。
     *
     * @param strictMode 是否启用严格模式
     * @param escapeHtml 是否启用HTML转义
     */
    public MustacheTemplateRenderer(boolean strictMode, boolean escapeHtml) {
        this.escapeHtml = escapeHtml;
        if (strictMode) {
            this.compiler = Mustache.compiler()
                    .escapeHTML(escapeHtml)
                    .strictSections(true);
        } else {
            this.compiler = Mustache.compiler()
                    .escapeHTML(escapeHtml)
                    .nullValue("")
                    .defaultValue("");
        }
    }

    @Override
    public String render(String template, Map<String, Object> context) throws TemplateRenderException {
        if (template == null || template.isEmpty()) {
            return template;
        }
        if (context == null) {
            context = java.util.Collections.emptyMap();
        }

        try {
            Template compiled = compiler.compile(template);
            String result = compiled.execute(context);

            // 如果不需要HTML转义，反转义HTML实体
            if (!escapeHtml) {
                result = unescapeHtml(result);
            }

            return result;
        } catch (MustacheException e) {
            throw new TemplateRenderException(
                    "Failed to render template: " + e.getMessage(),
                    template
            );
        }
    }

    /**
     * 反转义HTML实体字符。
     *
     * @param html HTML字符串
     * @return 反转义后的字符串
     */
    private String unescapeHtml(String html) {
        if (html == null) {
            return null;
        }

        return html.replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&quot;", "\"")
                   .replace("&#x27;", "'")
                   .replace("&#39;", "'")
                   .replace("&amp;", "&");
    }

    @Override
    public String render(String template, ExecutionContext context) throws TemplateRenderException {
        if (context == null) {
            return render(template, java.util.Collections.<String, Object>emptyMap());
        }
        return render(template, context.buildVariableContext());
    }

    @Override
    public boolean isValid(String template) {
        if (template == null || template.isEmpty()) {
            return true;
        }

        try {
            compiler.compile(template);
            return true;
        } catch (MustacheException e) {
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
            String variable = matcher.group(1).trim();
            // 忽略注释（以!开头）和section标记（以#或/开头）
            if (!variable.isEmpty() &&
                !variable.startsWith("!") &&
                !variable.startsWith("#") &&
                !variable.startsWith("/") &&
                !variable.startsWith("^")) {
                if (!variables.contains(variable)) {
                    variables.add(variable);
                }
            }
        }

        return variables;
    }

    /**
     * 渲染 Map 类型的输入模板。
     *
     * <p>支持简写格式和扩展格式：</p>
     * <ul>
     *   <li>简写格式：Map<String, String> - 每个值作为模板渲染</li>
     *   <li>扩展格式：Map<String, Object> - 递归处理嵌套对象</li>
     * </ul>
     *
     * @param inputTemplate 输入模板映射
     * @param context 上下文数据
     * @return 渲染后的参数映射
     * @throws TemplateRenderException 渲染失败时抛出
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> renderInputTemplate(Map<String, Object> inputTemplate,
                                                   Map<String, Object> context) throws TemplateRenderException {
        Map<String, Object> result = new java.util.LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> entry : inputTemplate.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            result.put(key, renderValue(value, context));
        }
        return result;
    }

    /**
     * 渲染单个值（支持嵌套对象和数组）。
     */
    @SuppressWarnings("unchecked")
    private Object renderValue(Object value, Map<String, Object> context) throws TemplateRenderException {
        if (value == null) {
            return null;
        }

        // 字符串：作为模板渲染
        if (value instanceof String) {
            String template = (String) value;
            return render(template, context);
        }

        // Map：递归处理
        if (value instanceof Map) {
            Map<String, Object> result = new java.util.LinkedHashMap<String, Object>();
            Map<String, Object> map = (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                result.put(entry.getKey(), renderValue(entry.getValue(), context));
            }
            return result;
        }

        // List：递归处理
        if (value instanceof List) {
            List<Object> result = new java.util.ArrayList<Object>();
            List<Object> list = (List<Object>) value;
            for (Object item : list) {
                result.add(renderValue(item, context));
            }
            return result;
        }

        // 其他类型（Number, Boolean 等）：直接返回
        return value;
    }

    /**
     * 渲染 Map 类型的输入模板（使用 ExecutionContext）。
     *
     * @param inputTemplate 输入模板映射
     * @param context 执行上下文
     * @return 渲染后的参数映射
     * @throws TemplateRenderException 渲染失败时抛出
     */
    public Map<String, Object> renderInputTemplate(Map<String, Object> inputTemplate,
                                                   ExecutionContext context) throws TemplateRenderException {
        return renderInputTemplate(inputTemplate, context.buildVariableContext());
    }
}

