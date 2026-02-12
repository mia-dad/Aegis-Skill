package com.mia.aegis.skill.dsl.parser;

import com.mia.aegis.skill.dsl.model.Skill;
import com.mia.aegis.skill.dsl.model.Step;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

public class SkillLoadingTest {

    @Test
    void shouldLoadAllSkills() throws Exception {
        MarkdownSkillParser parser = new MarkdownSkillParser();

        String skillsDir = "src/test/resources/skills";
        File dir = new File(skillsDir);
        File[] skillFiles = dir.listFiles((d, name) ->
                name.endsWith(".md") && !name.startsWith("invalid_"));

        assertThat(skillFiles).isNotNull();
        System.out.println("Testing " + skillFiles.length + " skill files...");

        int successCount = 0;
        int failCount = 0;

        for (File file : skillFiles) {
            try {
                String content = new String(Files.readAllBytes(file.toPath()));
                Skill skill = parser.parse(content);

                System.out.println("SUCCESS: " + file.getName() +
                    " - ID: " + skill.getId() +
                    ", Steps: " + skill.getStepCount() +
                    ", OutputFormat: " + skill.getOutputContract().getFormat());

                // Check await steps
                for (Step step : skill.getSteps()) {
                    if (step.getType().name().equals("AWAIT") && step.getAwaitConfig() != null) {
                        assertThat(step.getAwaitConfig().getMessage()).isNotNull();
                        System.out.println("  - Await step '" + step.getName() + "' has message");
                    }
                }

                successCount++;
            } catch (Exception e) {
                System.err.println("FAILED: " + file.getName() + " - " + e.getMessage());
                failCount++;
            }
        }

        System.out.println("\nSummary: Success=" + successCount + ", Failed=" + failCount + ", Total=" + skillFiles.length);
        assertThat(failCount).isZero();
    }
}
