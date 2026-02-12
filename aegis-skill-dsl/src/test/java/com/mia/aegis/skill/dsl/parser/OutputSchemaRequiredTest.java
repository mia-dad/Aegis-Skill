package com.mia.aegis.skill.dsl.parser;

import com.mia.aegis.skill.dsl.model.io.FieldSpec;
import com.mia.aegis.skill.dsl.model.io.OutputContract;
import com.mia.aegis.skill.dsl.model.Skill;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 output_schema 中 required 字段的解析。
 */
class OutputSchemaRequiredTest {

    @Test
    void testOutputSchemaRequiredFields() throws Exception {
        // 读取 sales_report_generator.md
        MarkdownSkillParser parser = new MarkdownSkillParser();
        Skill skill = parser.parseFile(Paths.get("src/test/resources/skills/sales_report_generator.md"));

        OutputContract outputContract = skill.getOutputContract();
        assertNotNull(outputContract, "OutputContract should not be null");

        // 验证必需字段
        FieldSpec region = outputContract.getFields().get("region");
        assertNotNull(region, "region field should exist");
        assertTrue(region.isRequired(), "region should be required");
        assertEquals("string", region.getType());

        FieldSpec period = outputContract.getFields().get("period");
        assertNotNull(period, "period field should exist");
        assertTrue(period.isRequired(), "period should be required");
        assertEquals("string", period.getType());

        FieldSpec totalSales = outputContract.getFields().get("total_sales");
        assertNotNull(totalSales, "total_sales field should exist");
        assertFalse(totalSales.isRequired(), "total_sales should be optional");
        assertEquals("number", totalSales.getType());

        // 验证可选字段
        FieldSpec title = outputContract.getFields().get("title");
        assertNotNull(title, "title field should exist");
        assertFalse(title.isRequired(), "title should be optional");
        assertEquals("string", title.getType());

        FieldSpec statusMessage = outputContract.getFields().get("status_message");
        assertNotNull(statusMessage, "status_message field should exist");
        assertFalse(statusMessage.isRequired(), "status_message should be optional");
        assertEquals("string", statusMessage.getType());

        FieldSpec suggestions = outputContract.getFields().get("suggestions");
        assertNotNull(suggestions, "suggestions field should exist");
        assertFalse(suggestions.isRequired(), "suggestions should be optional");
        assertEquals("string", suggestions.getType());

        FieldSpec userNotes = outputContract.getFields().get("user_notes");
        assertNotNull(userNotes, "user_notes field should exist");
        assertFalse(userNotes.isRequired(), "user_notes should be optional");
        assertEquals("string", userNotes.getType());
    }

    @Test
    void testOutputSchemaDefaultRequiredIsTrue() throws Exception {
        String markdown = "# skill: test_skill\n" +
                "\n" +
                "## version\n" +
                "\n" +
                "1.0.0\n" +
                "\n" +
                "## description\n" +
                "\n" +
                "测试技能\n" +
                "\n" +
                "## output_schema\n" +
                "\n" +
                "```yaml\n" +
                "field1:\n" +
                "  type: string\n" +
                "  description: 简写格式，默认必需\n" +
                "field2:\n" +
                "  type: string\n" +
                "  required: false\n" +
                "  description: 显式标记为可选\n" +
                "```\n" +
                "\n" +
                "## steps\n" +
                "\n" +
                "### step: test_step\n" +
                "\n" +
                "**type**: prompt\n" +
                "**varName**: field1\n" +
                "\n" +
                "```prompt\n" +
                "测试\n" +
                "```\n";

        MarkdownSkillParser parser = new MarkdownSkillParser();
        Skill skill = parser.parse(markdown);

        OutputContract outputContract = skill.getOutputContract();
        assertNotNull(outputContract);

        // 简写格式默认为必需
        FieldSpec field1 = outputContract.getFields().get("field1");
        assertNotNull(field1, "field1 should exist");
        assertTrue(field1.isRequired(), "field1 should be required by default");

        // 显式标记为可选
        FieldSpec field2 = outputContract.getFields().get("field2");
        assertNotNull(field2, "field2 should exist");
        assertFalse(field2.isRequired(), "field2 should be optional");
    }
}
