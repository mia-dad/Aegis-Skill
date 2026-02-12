package com.mia.aegis.skill.dsl.parser;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class ExtractTextTest {
    
    @Test
    void testExtractTextFromBold() {
        String markdown = "**type**: tool\n**tool**: builtin_write_file";
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        
        Paragraph paragraph = (Paragraph) document.getFirstChild();
        
        // 手动提取文本
        StringBuilder sb = new StringBuilder();
        extractTextRecursive(paragraph, sb);
        String result = sb.toString().trim();
        
        System.out.println("Extracted text: [" + result + "]");
        assertThat(result).isNotEmpty();
    }
    
    private void extractTextRecursive(Node node, StringBuilder sb) {
        if (node instanceof Text) {
            sb.append(((Text) node).getLiteral());
        } else if (node instanceof Code) {
            sb.append(((Code) node).getLiteral());
        } else if (node instanceof SoftLineBreak || node instanceof HardLineBreak) {
            sb.append(" ");
        } else {
            Node child = node.getFirstChild();
            while (child != null) {
                extractTextRecursive(child, sb);
                child = child.getNext();
            }
        }
    }
}
