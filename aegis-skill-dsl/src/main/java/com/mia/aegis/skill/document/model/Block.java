package com.mia.aegis.skill.document.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 文档内容块抽象基类。
 *
 * <p>Block 是文档的最小渲染单元。每个 Block 通过 {@code type} 字段
 * 标识其具体类型，前端根据类型选择渲染方式。</p>
 *
 * <h3>已知类型 (v1)</h3>
 * <ul>
 *   <li>{@code "paragraph"} - 文本块，参见 {@link ParagraphBlock}</li>
 *   <li>{@code "chart"} - 图表块，参见 {@link ChartBlock}</li>
 * </ul>
 *
 * @since 0.3.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ParagraphBlock.class, name = "paragraph"),
    @JsonSubTypes.Type(value = ChartBlock.class, name = "chart")
})
public abstract class Block {

    /**
     * 获取块类型。
     *
     * @return 块类型标识符（如 "paragraph", "chart"）
     */
    public abstract String getType();
}
