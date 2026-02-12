package com.mia.aegis.skill.document;

import com.mia.aegis.skill.document.model.Block;
import com.mia.aegis.skill.document.model.Document;
import com.mia.aegis.skill.document.model.ParagraphBlock;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Document 单元测试。
 */
class DocumentTest {

    @Test
    void testEmptyDocument() {
        Document document = Document.empty();

        assertThat(document.getType()).isEqualTo("document");
        assertThat(document.getVersion()).isEqualTo("v1");
        assertThat(document.isEmpty()).isTrue();
        assertThat(document.getBlockCount()).isEqualTo(0);
    }

    @Test
    void testDocumentWithBlocks() {
        ParagraphBlock block1 = ParagraphBlock.of("First paragraph");
        ParagraphBlock block2 = ParagraphBlock.of("Second paragraph");

        Document document = Document.of(Arrays.asList(block1, block2));

        assertThat(document.getType()).isEqualTo("document");
        assertThat(document.getVersion()).isEqualTo("v1");
        assertThat(document.isEmpty()).isFalse();
        assertThat(document.getBlockCount()).isEqualTo(2);
        assertThat(document.getBlocks()).containsExactly(block1, block2);
    }

    @Test
    void testDocumentImmutability() {
        ParagraphBlock block = ParagraphBlock.of("Test");
        java.util.List<Block> mutableList = new java.util.ArrayList<>();
        mutableList.add(block);

        Document document = Document.of(mutableList);

        // 修改原始列表不应影响 Document
        mutableList.add(ParagraphBlock.of("Another"));

        assertThat(document.getBlockCount()).isEqualTo(1);
    }

    @Test
    void testDocumentEqualsAndHashCode() {
        ParagraphBlock block1 = ParagraphBlock.of("Test");
        ParagraphBlock block2 = ParagraphBlock.of("Test");

        Document doc1 = Document.of(Collections.singletonList(block1));
        Document doc2 = Document.of(Collections.singletonList(block2));

        assertThat(doc1).isEqualTo(doc2);
        assertThat(doc1.hashCode()).isEqualTo(doc2.hashCode());
    }

    @Test
    void testDocumentToString() {
        Document document = Document.empty();
        String str = document.toString();

        assertThat(str).contains("type='document'");
        assertThat(str).contains("version='v1'");
    }
}
