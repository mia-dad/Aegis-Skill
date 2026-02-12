/**
 * Document 数据模型。
 *
 * <p>包含结构化文档的核心数据结构：</p>
 * <ul>
 *   <li>{@link com.mia.aegis.skill.document.model.Document} - 顶层文档容器</li>
 *   <li>{@link com.mia.aegis.skill.document.model.Block} - 内容块抽象基类</li>
 *   <li>{@link com.mia.aegis.skill.document.model.ParagraphBlock} - 文本块</li>
 *   <li>{@link com.mia.aegis.skill.document.model.ChartBlock} - 图表块</li>
 *   <li>{@link com.mia.aegis.skill.document.model.ChartSpec} - 图表规格</li>
 *   <li>{@link com.mia.aegis.skill.document.model.Series} - 数据系列</li>
 * </ul>
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>不可变性：所有类均为 final，字段为 final，集合不可变</li>
 *   <li>JSON 友好：使用 Jackson 注解支持序列化/反序列化</li>
 *   <li>类型安全：通过枚举和常量约束有效值</li>
 * </ul>
 *
 * @since 0.3.0
 */
package com.mia.aegis.skill.document.model;
