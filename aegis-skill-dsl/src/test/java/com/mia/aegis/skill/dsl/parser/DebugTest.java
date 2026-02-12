package com.mia.aegis.skill.dsl.parser;

import com.mia.aegis.skill.dsl.parser.MarkdownSkillParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DebugTest {
    @Test
    void debugBuiltinToolsFile() throws Exception {
        String content = new String(Files.readAllBytes(
            Paths.get("src/test/resources/skills/builtin_tools_demo.md")));
        
        System.out.println("=== File Content (write_txt_report section) ===");
        String[] lines = content.split("\n");
        boolean found = false;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("step: write_txt_report")) {
                found = true;
            }
            if (found) {
                System.out.println(i + ": " + lines[i]);
                if (i > 55) break; // 打印几行后停止
            }
        }
    }
}
