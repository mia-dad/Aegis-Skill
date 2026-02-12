package com.mia.aegis.skill.document.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 顶层文档容器。
 *
 * <p>Document 是 Aegis Runtime 中 Skill 执行结果的标准表达形式。
 * 它包含一个有序的 Block 列表，支持图文混排输出。</p>
 *
 * <h3>不变量</h3>
 * <ul>
 *   <li>{@code getType()} 必须返回 "document"</li>
 *   <li>{@code getVersion()} 必须返回 "v1"</li>
 *   <li>{@code getBlocks()} 必须返回不可变列表，不可为 null</li>
 * </ul>
 *
 * <h3>JSON 序列化</h3>
 * <pre>
 * {
 *   "type": "document",
 *   "version": "v1",
 *   "blocks": [...]
 * }
 * </pre>
 *
 * @since 0.3.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Document {

    /**
     * 固定类型标识符。
     */
    public static final String TYPE = "document";

    /**
     * 当前版本号。
     */
    public static final String VERSION = "v1";

    private final List<Block> blocks;

    /**
     * 创建 Document。
     *
     * @param blocks 内容块列表，不可为 null
     * @throws NullPointerException 如果 blocks 为 null
     */
    @JsonCreator
    public Document(@JsonProperty("blocks") List<Block> blocks) {
        Objects.requireNonNull(blocks, "blocks must not be null");
        this.blocks = Collections.unmodifiableList(new ArrayList<>(blocks));
    }

    /**
     * 获取文档类型。
     *
     * @return 固定值 "document"
     */
    public String getType() {
        return TYPE;
    }

    /**
     * 获取版本号。
     *
     * @return 固定值 "v1"
     */
    public String getVersion() {
        return VERSION;
    }

    /**
     * 获取内容块列表。
     *
     * @return 不可变的 Block 列表，不可为 null
     */
    public List<Block> getBlocks() {
        return blocks;
    }

    /**
     * 检查文档是否为空。
     *
     * @return 如果 blocks 为空返回 true
     */
    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    /**
     * 获取内容块数量。
     *
     * @return blocks 列表长度
     */
    public int getBlockCount() {
        return blocks.size();
    }

    /**
     * 创建空文档。
     *
     * @return 不含任何 Block 的空文档
     */
    public static Document empty() {
        return new Document(Collections.emptyList());
    }

    /**
     * 根据 Block 列表创建文档。
     *
     * @param blocks 内容块列表
     * @return 新的 Document 实例
     * @throws NullPointerException 如果 blocks 为 null
     */
    public static Document of(List<Block> blocks) {
        return new Document(blocks);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(blocks, document.blocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blocks);
    }

    @Override
    public String toString() {
        return "Document{type='" + TYPE + "', version='" + VERSION + "', blocks=" + blocks + "}";
    }
}
