/**
 * Document 构建器。
 *
 * <p>提供流式 API 构建 Document 对象：</p>
 * <ul>
 *   <li>{@link com.mia.aegis.skill.document.builder.DocumentBuilder} - 文档构建器</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * Document doc = DocumentBuilder.create()
 *     .addParagraph("标题")
 *     .addChart(chartSpec)
 *     .addParagraph("结论")
 *     .build();
 * }</pre>
 *
 * @since 0.3.0
 */
package com.mia.aegis.skill.document.builder;
