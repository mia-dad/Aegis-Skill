package com.mia.aegis.skill.document.builder;

import com.mia.aegis.skill.document.model.*;
import com.mia.aegis.skill.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Document 构建器。
 *
 * <p>提供流式 API 构建 Document 对象，支持图文混排。</p>
 *
 * <h3>使用示例</h3>
 * <pre>
 * Document doc = DocumentBuilder.create()
 *     .addParagraph("分析结果：")
 *     .addChart(chartSpec)
 *     .addParagraph("结论...")
 *     .build();
 * </pre>
 *
 * @since 0.3.0
 */
public final class DocumentBuilder {

    private static final Logger logger = LoggerFactory.getLogger(DocumentBuilder.class);

    private final List<Block> blocks;

    private DocumentBuilder() {
        this.blocks = new ArrayList<>();
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.get("document.build.started"));
        }
    }

    /**
     * 创建新的 DocumentBuilder 实例。
     *
     * @return 新的 DocumentBuilder
     */
    public static DocumentBuilder create() {
        return new DocumentBuilder();
    }

    /**
     * 添加文本块。
     *
     * @param text 文本内容
     * @return this builder
     * @throws NullPointerException 如果 text 为 null
     */
    public DocumentBuilder addParagraph(String text) {
        Objects.requireNonNull(text, "text must not be null");
        if (logger.isTraceEnabled()) {
            logger.trace(Messages.get("builder.adding.paragraph"));
        }
        this.blocks.add(ParagraphBlock.of(text));
        return this;
    }

    /**
     * 添加图表块。
     *
     * @param chart 图表规格
     * @return this builder
     * @throws NullPointerException 如果 chart 为 null
     */
    public DocumentBuilder addChart(ChartSpec chart) {
        Objects.requireNonNull(chart, "chart must not be null");
        if (logger.isTraceEnabled()) {
            logger.trace(Messages.get("builder.adding.chart"));
        }
        this.blocks.add(ChartBlock.of(chart));
        return this;
    }

    /**
     * 添加任意块。
     *
     * @param block 内容块
     * @return this builder
     * @throws NullPointerException 如果 block 为 null
     */
    public DocumentBuilder addBlock(Block block) {
        Objects.requireNonNull(block, "block must not be null");
        if (logger.isTraceEnabled()) {
            logger.trace(Messages.get("builder.adding.block", block.getType()));
        }
        this.blocks.add(block);
        return this;
    }

    /**
     * 构建 Document 对象。
     *
     * <p>可多次调用，每次返回基于当前状态的新 Document 实例。
     * 注意：builder会累积状态，不会在build()后自动重置。</p>
     *
     * @return 不可变的 Document 实例
     */
    public Document build() {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.get("builder.building", blocks.size()));
        }
        // 创建blocks列表的深拷贝，确保每次build()都有独立的block对象
        List<Block> copiedBlocks = new ArrayList<>();
        for (Block block : blocks) {
            if (block instanceof ParagraphBlock) {
                ParagraphBlock pb = (ParagraphBlock) block;
                copiedBlocks.add(ParagraphBlock.of(pb.getText()));
            } else if (block instanceof ChartBlock) {
                ChartBlock cb = (ChartBlock) block;
                copiedBlocks.add(ChartBlock.of(cb.getChart()));
            } else {
                copiedBlocks.add(block);
            }
        }
        Document document = Document.of(copiedBlocks);
        if (logger.isInfoEnabled()) {
            logger.info(Messages.get("document.created", blocks.size()));
        }
        return document;
    }
}
