/**
 * 结构化文档输出框架。
 *
 * <h2>概述</h2>
 * <p>本包提供了一套用于 Skill 执行结果标准化表达的文档模型。
 * Document 是图文混排的顶层容器，支持文本段落和图表两种内容块类型。</p>
 *
 * <h2>核心设计</h2>
 * <h3>1. 不可变对象</h3>
 * <p>所有模型类均为 immutable，线程安全。所有集合字段通过
 * {@code Collections.unmodifiableList()} 包装，防止外部修改。</p>
 *
 * <h3>2. 版本化协议</h3>
 * <pre>
 * Document {
 *   "type": "document",
 *   "version": "v1",
 *   "blocks": [...]
 * }
 * </pre>
 * <ul>
 *   <li>type: 固定标识符 "document"</li>
 *   <li>version: 当前版本 "v1"，预留扩展能力</li>
 *   <li>blocks: 内容块列表（ParagraphBlock 或 ChartBlock）</li>
 * </ul>
 *
 * <h3>3. 类型化块（Block）</h3>
 * <p>Block 是抽象基类，通过 Jackson {@code @JsonTypeInfo} 实现多态序列化：</p>
 * <ul>
 *   <li>{@link com.mia.aegis.skill.document.model.ParagraphBlock} - 文本块（支持 Markdown）</li>
 *   <li>{@link com.mia.aegis.skill.document.model.ChartBlock} - 图表块</li>
 * </ul>
 *
 * <h3>4. 语义化图表</h3>
 * <p>ChartBlock 包含结构化的 ChartSpec，前端根据 type 字段选择渲染方式：</p>
 * <ul>
 *   <li>bar - 柱状图</li>
 *   <li>line - 折线图</li>
 * </ul>
 * <p>后端仅输出语义化数据，不绑定具体图表库。</p>
 *
 * <h2>使用示例</h2>
 * <h3>构建文档</h3>
 * <pre>{@code
 * import com.mia.aegis.skill.document.builder.DocumentBuilder;
 * import com.mia.aegis.skill.document.model.*;
 *
 * // 使用 Builder 模式
 * Document doc = DocumentBuilder.create()
 *     .addParagraph("2024年财务分析报告")
 *     .addChart(ChartSpec.builder()
 *         .type(ChartSpec.TYPE_BAR)
 *         .title("收入趋势")
 *         .x("Q1", "Q2", "Q3", "Q4")
 *         .addSeries("收入", Arrays.asList(100, 120, 140, 160))
 *         .build())
 *     .addParagraph("结论：收入保持稳步增长。")
 *     .build();
 * }</pre>
 *
 * <h3>验证文档</h3>
 * <pre>{@code
 * import com.mia.aegis.skill.document.validation.DocumentValidator;
 * import com.mia.aegis.skill.document.validation.DefaultDocumentValidator;
 *
 * DocumentValidator validator = new DefaultDocumentValidator();
 * List<ValidationResult> results = validator.validate(document);
 *
 * if (validator.isValid(document)) {
 *     // 文档有效，继续处理
 * } else {
 *     // 处理错误
 *     results.forEach(r -> System.err.println(r));
 * }
 * }</pre>
 *
 * <h2>包结构</h2>
 * <ul>
 *   <li>{@link com.mia.aegis.skill.document.model} - 数据模型（Document, Block, ChartSpec 等）</li>
 *   <li>{@link com.mia.aegis.skill.document.builder} - 构建器（DocumentBuilder）</li>
 *   <li>{@link com.mia.aegis.skill.document.validation} - 验证器（DocumentValidator）</li>
 * </ul>
 *
 * <h2>前后端协作</h2>
 * <p>本框架遵循"语义化输出"原则：</p>
 * <ul>
 *   <li>后端：输出结构化的 Document JSON</li>
 *   <li>前端：根据 Block.type 字段动态渲染</li>
 * </ul>
 * <p>前端可根据需要选择 Markdown 渲染库（如 React-Markdown）和图表库（如 ECharts、Chart.js）。</p>
 *
 * <h2>扩展指南</h2>
 * <h3>添加新的 Block 类型</h3>
 * <ol>
 *   <li>创建新的 Block 子类（如 TableBlock）</li>
 *   <li>在 {@code Block@JsonSubTypes} 中注册</li>
 *   <li>更新 {@code DefaultDocumentValidator} 添加验证逻辑</li>
 *   <li>在 DocumentBuilder 中添加便捷方法（可选）</li>
 * </ol>
 *
 * <h3>支持新的图表类型</h3>
 * <ol>
 *   <li>在 {@code ChartSpec} 中添加类型常量（如 {@code TYPE_PIE}）</li>
 *   <li>更新 {@code DefaultDocumentValidator} 的类型检查</li>
 *   <li>前端实现对应渲染逻辑</li>
 * </ol>
 *
 * @since 0.3.0
 */
package com.mia.aegis.skill.document;
