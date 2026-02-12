package com.mia.aegis.skill.document.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * 文本块。
 *
 * <p>包含纯文本或 Markdown 格式的文本内容。
 * 文本原样保存，是否渲染 Markdown 由前端决定。</p>
 *
 * <h3>JSON 序列化</h3>
 * <pre>
 * {
 *   "type": "paragraph",
 *   "text": "这里是一段分析性文本"
 * }
 * </pre>
 *
 * @since 0.3.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ParagraphBlock extends Block {

    /**
     * 固定类型标识符。
     */
    public static final String TYPE = "paragraph";

    private final String text;

    /**
     * 创建文本块。
     *
     * @param text 文本内容，不可为 null（可为空字符串）
     * @throws NullPointerException 如果 text 为 null
     */
    @JsonCreator
    public ParagraphBlock(@JsonProperty("text") String text) {
        Objects.requireNonNull(text, "text must not be null");
        this.text = text;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * 获取文本内容。
     *
     * @return 文本内容，不可为 null（可为空字符串）
     */
    public String getText() {
        return text;
    }

    /**
     * 创建文本块的工厂方法。
     *
     * @param text 文本内容
     * @return 新的 ParagraphBlock 实例
     * @throws NullPointerException 如果 text 为 null
     */
    public static ParagraphBlock of(String text) {
        return new ParagraphBlock(text);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParagraphBlock that = (ParagraphBlock) o;
        return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    @Override
    public String toString() {
        return "ParagraphBlock{type='" + TYPE + "', text='" + text + "'}";
    }
}
