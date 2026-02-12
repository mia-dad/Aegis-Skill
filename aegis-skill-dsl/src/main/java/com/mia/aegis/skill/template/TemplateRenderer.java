package com.mia.aegis.skill.template;


import com.mia.aegis.skill.exception.TemplateRenderException;
import com.mia.aegis.skill.executor.context.ExecutionContext;

import java.util.List;
import java.util.Map;

/**
 * 模板渲染器接口。
 *
 * <p>用于渲染包含 Aegis 模板语法的字符串。</p>
 *
 * <p>支持的语法：</p>
 * <ul>
 *   <li>{@code {{variable}}} — 变量替换</li>
 *   <li>{@code {{a.b.c}}} — 嵌套属性访问</li>
 *   <li>{@code {{a + b}}}, {@code {{a * b}}} — 表达式求值（四则运算）</li>
 *   <li>{@code {{arr[0]}}}, {@code {{arr[#var]}}} — 数组索引</li>
 *   <li>{@code {{#for items}}...{{/for}}} — 循环渲染</li>
 *   <li>{@code {{_}}} — 当前循环元素</li>
 * </ul>
 *
 * <p>用法：</p>
 * <pre>{@code
 * TemplateRenderer renderer = new AegisTemplateRenderer();
 * String result = renderer.render("Hello {{name}}!", context);
 * }</pre>
 */
public interface TemplateRenderer {

    /**
     * 使用 Map 上下文渲染模板。
     *
     * @param template 模板字符串
     * @param context 上下文数据
     * @return 渲染后的字符串
     * @throws TemplateRenderException 渲染失败时抛出
     */
    String render(String template, Map<String, Object> context) throws TemplateRenderException;

    /**
     * 使用 ExecutionContext 渲染模板。
     *
     * @param template 模板字符串
     * @param context 执行上下文
     * @return 渲染后的字符串
     * @throws TemplateRenderException 渲染失败时抛出
     */
    String render(String template, ExecutionContext context) throws TemplateRenderException;

    /**
     * 验证模板语法是否正确。
     *
     * @param template 模板字符串
     * @return 如果语法正确返回 true
     */
    boolean isValid(String template);

    /**
     * 提取模板中引用的变量名。
     *
     * @param template 模板字符串
     * @return 变量名列表
     */
    List<String> extractVariables(String template);
}

