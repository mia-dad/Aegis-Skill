package com.mia.aegis.skill.dsl.parser;

import com.mia.aegis.skill.dsl.model.Skill;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试实际的 Skill 文件是否能正确解析。
 */
public class SkillFilesParsingTest {

    private final MarkdownSkillParser parser = new MarkdownSkillParser();

    @Test
    void testAwaitExampleFile() throws Exception {
        String content = new String(Files.readAllBytes(
            Paths.get("src/test/resources/skills/await-example.md")));

        Skill skill = parser.parse(content);

        assertThat(skill.getId()).isEqualTo("order_confirmation");
        assertThat(skill.getSteps()).hasSize(6);

        // 验证步骤类型
        assertThat(skill.getSteps().get(0).getType()).isEqualTo(com.mia.aegis.skill.dsl.model.StepType.TOOL);
        assertThat(skill.getSteps().get(1).getType()).isEqualTo(com.mia.aegis.skill.dsl.model.StepType.PROMPT);
        assertThat(skill.getSteps().get(2).getType()).isEqualTo(com.mia.aegis.skill.dsl.model.StepType.AWAIT);
    }

    @Test
    void testDbInsertCustomerSkill() throws Exception {
        String content = new String(Files.readAllBytes(
            Paths.get("../aegis-skill-demo/src/main/resources/skills/db_insert_customer.md")));

        Skill skill = parser.parse(content);

        assertThat(skill.getId()).isEqualTo("db_insert_customer");
        assertThat(skill.getSteps()).hasSize(2);
        assertThat(skill.getSteps().get(0).getType()).isEqualTo(com.mia.aegis.skill.dsl.model.StepType.TOOL);
        assertThat(skill.getSteps().get(0).getToolConfig().getToolName()).isEqualTo("db_insert");
        assertThat(skill.getSteps().get(1).getType()).isEqualTo(com.mia.aegis.skill.dsl.model.StepType.TEMPLATE);
    }

    @Test
    void testDbUpdateCustomerSkill() throws Exception {
        String content = new String(Files.readAllBytes(
            Paths.get("../aegis-skill-demo/src/main/resources/skills/db_update_customer.md")));

        Skill skill = parser.parse(content);

        assertThat(skill.getId()).isEqualTo("db_update_customer");
        assertThat(skill.getSteps()).hasSize(2);
        assertThat(skill.getSteps().get(0).getType()).isEqualTo(com.mia.aegis.skill.dsl.model.StepType.TOOL);
        assertThat(skill.getSteps().get(0).getToolConfig().getToolName()).isEqualTo("db_update");
        assertThat(skill.getSteps().get(1).getType()).isEqualTo(com.mia.aegis.skill.dsl.model.StepType.TEMPLATE);
    }

    @Test
    void testDbQueryCustomersSkill() throws Exception {
        String content = new String(Files.readAllBytes(
            Paths.get("../aegis-skill-demo/src/main/resources/skills/db_query_customers.md")));

        Skill skill = parser.parse(content);

        assertThat(skill.getId()).isEqualTo("db_query_customers");
        assertThat(skill.getSteps()).hasSize(2);
        assertThat(skill.getSteps().get(0).getType()).isEqualTo(com.mia.aegis.skill.dsl.model.StepType.TOOL);
        assertThat(skill.getSteps().get(0).getToolConfig().getToolName()).isEqualTo("db_select");
        assertThat(skill.getSteps().get(1).getType()).isEqualTo(com.mia.aegis.skill.dsl.model.StepType.TEMPLATE);
    }

}
