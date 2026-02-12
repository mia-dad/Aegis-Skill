package com.mia.aegis.skill.document;

import com.mia.aegis.skill.document.builder.DocumentBuilder;
import com.mia.aegis.skill.document.model.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DocumentBuilder 单元测试。
 */
class DocumentBuilderTest {

    @Test
    void testBuildEmptyDocument() {
        Document document = DocumentBuilder.create().build();

        assertThat(document.isEmpty()).isTrue();
        assertThat(document.getBlockCount()).isEqualTo(0);
    }

    @Test
    void testBuildDocumentWithParagraphs() {
        Document document = DocumentBuilder.create()
            .addParagraph("First paragraph")
            .addParagraph("Second paragraph")
            .build();

        assertThat(document.getBlockCount()).isEqualTo(2);
        assertThat(document.getBlocks().get(0)).isInstanceOf(ParagraphBlock.class);
        assertThat(document.getBlocks().get(1)).isInstanceOf(ParagraphBlock.class);

        ParagraphBlock block1 = (ParagraphBlock) document.getBlocks().get(0);
        ParagraphBlock block2 = (ParagraphBlock) document.getBlocks().get(1);

        assertThat(block1.getText()).isEqualTo("First paragraph");
        assertThat(block2.getText()).isEqualTo("Second paragraph");
    }

    @Test
    void testBuildDocumentWithChart() {
        Series series = Series.of("Revenue", Arrays.asList(100, 200, 300));
        ChartSpec chart = ChartSpec.builder()
            .type(ChartSpec.TYPE_BAR)
            .title("Revenue Chart")
            .x("Q1", "Q2", "Q3")
            .addSeries(series)
            .build();

        Document document = DocumentBuilder.create()
            .addChart(chart)
            .build();

        assertThat(document.getBlockCount()).isEqualTo(1);
        assertThat(document.getBlocks().get(0)).isInstanceOf(ChartBlock.class);

        ChartBlock block = (ChartBlock) document.getBlocks().get(0);
        assertThat(block.getChart()).isEqualTo(chart);
    }

    @Test
    void testBuildDocumentWithMixedBlocks() {
        Series series = Series.of("Data", Arrays.asList(1, 2, 3));
        ChartSpec chart = ChartSpec.builder()
            .type(ChartSpec.TYPE_LINE)
            .title("Trend")
            .x("A", "B", "C")
            .addSeries(series)
            .build();

        Document document = DocumentBuilder.create()
            .addParagraph("Introduction")
            .addChart(chart)
            .addParagraph("Conclusion")
            .build();

        assertThat(document.getBlockCount()).isEqualTo(3);
        assertThat(document.getBlocks().get(0)).isInstanceOf(ParagraphBlock.class);
        assertThat(document.getBlocks().get(1)).isInstanceOf(ChartBlock.class);
        assertThat(document.getBlocks().get(2)).isInstanceOf(ParagraphBlock.class);
    }

    @Test
    void testBuilderCanRebuild() {
        DocumentBuilder builder = DocumentBuilder.create();

        Document doc1 = builder.addParagraph("First").build();
        Document doc2 = builder.addParagraph("Second").build();

        assertThat(doc1.getBlockCount()).isEqualTo(1);
        assertThat(doc2.getBlockCount()).isEqualTo(2);
        assertThat(doc1.getBlocks().get(0)).isNotSameAs(doc2.getBlocks().get(0));
    }

    @Test
    void testBuildDocumentProducesImmutableBlocks() {
        Document document = DocumentBuilder.create()
            .addParagraph("Test")
            .build();

        java.util.List<Block> blocks = document.getBlocks();

        assertThatThrownBy(() -> blocks.add(ParagraphBlock.of("Another")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testAddCustomBlock() {
        class CustomBlock extends Block {
            @Override
            public String getType() {
                return "custom";
            }
        }

        Document document = DocumentBuilder.create()
            .addBlock(new CustomBlock())
            .build();

        assertThat(document.getBlockCount()).isEqualTo(1);
        assertThat(document.getBlocks().get(0).getType()).isEqualTo("custom");
    }
}
