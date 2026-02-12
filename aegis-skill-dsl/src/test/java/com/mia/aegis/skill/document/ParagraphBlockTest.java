package com.mia.aegis.skill.document;

import com.mia.aegis.skill.document.model.ParagraphBlock;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ParagraphBlock 单元测试。
 */
class ParagraphBlockTest {

    @Test
    void testCreateParagraphBlock() {
        ParagraphBlock block = ParagraphBlock.of("Hello, world!");

        assertThat(block.getType()).isEqualTo("paragraph");
        assertThat(block.getText()).isEqualTo("Hello, world!");
    }

    @Test
    void testParagraphBlockWithEmptyText() {
        ParagraphBlock block = ParagraphBlock.of("");

        assertThat(block.getType()).isEqualTo("paragraph");
        assertThat(block.getText()).isEqualTo("");
    }

    @Test
    void testParagraphBlockWithNullTextThrowsException() {
        assertThatThrownBy(() -> ParagraphBlock.of(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("text must not be null");
    }

    @Test
    void testParagraphBlockEqualsAndHashCode() {
        ParagraphBlock block1 = ParagraphBlock.of("Same text");
        ParagraphBlock block2 = ParagraphBlock.of("Same text");
        ParagraphBlock block3 = ParagraphBlock.of("Different text");

        assertThat(block1).isEqualTo(block2);
        assertThat(block1.hashCode()).isEqualTo(block2.hashCode());
        assertThat(block1).isNotEqualTo(block3);
    }

    @Test
    void testParagraphBlockToString() {
        ParagraphBlock block = ParagraphBlock.of("Test");

        String str = block.toString();

        assertThat(str).contains("type='paragraph'");
        assertThat(str).contains("text='Test'");
    }
}
