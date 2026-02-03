package com.mia.aegis.skill.template;


import com.mia.aegis.skill.exception.TemplateRenderException;
import com.mia.aegis.skill.executor.context.ExecutionContext;

import java.util.List;
import java.util.Map;

/**
 * 模板渲染器接口。
 *
 * <p>用于渲染包含 {{variable}} 语法的模板字符串。</p>
 *
 * <p>Supported Syntax:</p>
 * <ul>
 *   <li>{@code {{variable}}} - 引用输入参数</li>
 *   <li>{@code {{step.output}}} - 引用前置 Step 输出</li>
 *   <li>{@code {{context.key}}} - 引用运行时上下文</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * TemplateRenderer renderer = new MustacheTemplateRenderer();
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

