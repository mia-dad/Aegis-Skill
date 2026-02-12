import com.mia.aegis.skill.dsl.parser.MarkdownSkillParser;
import com.mia.aegis.skill.dsl.model.Skill;
import java.nio.file.*;

public class TestSkillFiles {
    public static void main(String[] args) throws Exception {
        MarkdownSkillParser parser = new MarkdownSkillParser();

        // Test await-example.md
        System.out.println("=== Testing await-example.md ===");
        try {
            String awaitContent = new String(Files.readAllBytes(
                Paths.get("../aegis-skill-api/src/main/resources/skills/await-example.md")));
            Skill awaitSkill = parser.parse(awaitContent);
            System.out.println("[OK] await-example.md parsed successfully");
            System.out.println("  - Skill ID: " + awaitSkill.getId());
            System.out.println("  - Steps: " + awaitSkill.getSteps().size());
            awaitSkill.getSteps().forEach(step ->
                System.out.println("    * " + step.getName() + " (" + step.getType() + ")"));
        } catch (Exception e) {
            System.out.println("[FAIL] await-example.md parse error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();

        // Test builtin_tools_demo.md
        System.out.println("=== Testing builtin_tools_demo.md ===");
        try {
            String builtinContent = new String(Files.readAllBytes(
                Paths.get("../aegis-skill-api/src/main/resources/skills/builtin_tools_demo.md")));
            Skill builtinSkill = parser.parse(builtinContent);
            System.out.println("[OK] builtin_tools_demo.md parsed successfully");
            System.out.println("  - Skill ID: " + builtinSkill.getId());
            System.out.println("  - Steps: " + builtinSkill.getSteps().size());
            builtinSkill.getSteps().forEach(step ->
                System.out.println("    * " + step.getName() + " (" + step.getType() + ")"));
        } catch (Exception e) {
            System.out.println("[FAIL] builtin_tools_demo.md parse error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
