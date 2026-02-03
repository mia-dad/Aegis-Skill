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
 * <p>支持 Mustache 语法的模板渲染，包括：</p>
 * <ul>
 *   <li>{@code {{variable}}} - 简单变量替换（不转义）</li>
 *   <li>{@code {{step.output}}} - 嵌套属性访问</li>
 *   <li>{@code {{#section}}...{{/section}}} - 条件/循环块</li>
 * </ul>
 *
 * <p><b>注意：</b>本渲染器默认不进行HTML转义，所有变量输出都保持原始格式。
 * 如需HTML转义，请在模板中使用标准Mustache语法或手动处理。</p>
 */
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
                    .strictSections(true);
        } else {
            this.compiler = Mustache.compiler()
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
     * <p>将 Map 中的每个值作为模板进行渲染。</p>
     *
     * @param inputTemplate 输入模板映射
     * @param context 上下文数据
     * @return 渲染后的参数映射
     * @throws TemplateRenderException 渲染失败时抛出
     */
    public Map<String, Object> renderInputTemplate(Map<String, String> inputTemplate,
                                                   Map<String, Object> context) throws TemplateRenderException {
        Map<String, Object> result = new java.util.LinkedHashMap<String, Object>();
        for (Map.Entry<String, String> entry : inputTemplate.entrySet()) {
            String key = entry.getKey();
            String template = entry.getValue();
            String rendered = render(template, context);
            result.put(key, rendered);
        }
        return result;
    }

    /**
     * 渲染 Map 类型的输入模板（使用 ExecutionContext）。
     *
     * @param inputTemplate 输入模板映射
     * @param context 执行上下文
     * @return 渲染后的参数映射
     * @throws TemplateRenderException 渲染失败时抛出
     */
    public Map<String, Object> renderInputTemplate(Map<String, String> inputTemplate,
                                                   ExecutionContext context) throws TemplateRenderException {
        return renderInputTemplate(inputTemplate, context.buildVariableContext());
    }
}

