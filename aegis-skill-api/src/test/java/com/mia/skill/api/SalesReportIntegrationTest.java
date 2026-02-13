package com.mia.skill.api;

import com.mia.aegis.skill.dsl.model.Skill;
import com.mia.aegis.skill.executor.context.ExecutionContext;
import com.mia.aegis.skill.executor.context.SkillResult;
import com.mia.aegis.skill.executor.engine.SkillExecutor;
import com.mia.aegis.skill.persistence.repository.SkillRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试：测试 sales_report_generator 技能执行
 */
@SpringBootTest
class SalesReportIntegrationTest {

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private SkillExecutor skillExecutor;

    @Test
    void testSalesReportGenerator() throws Exception {
        // 从 SkillRepository 获取所有技能，找到 sales_report_generator
        Skill targetSkill = null;
        for (Skill skill : skillRepository.findAll()) {
            if ("sales_report_generator".equals(skill.getId())) {
                targetSkill = skill;
                break;
            }
        }

        assertNotNull(targetSkill, "技能 sales_report_generator 应该存在");
        System.out.println("技能ID: " + targetSkill.getId());
        System.out.println("技能描述: " + targetSkill.getDescription());

        // 准备输入
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("region", "华南");
        inputs.put("period", "2024Q1");
        inputs.put("need_suggestions", false);

        // 创建执行上下文
        ExecutionContext executionContext = new ExecutionContext(inputs);

        // 执行技能
        SkillResult result = skillExecutor.execute(targetSkill, executionContext);

        // 输出结果
        System.out.println("\n=== 执行结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("等待: " + result.isAwaiting());
        System.out.println("输出类型: " + (result.getOutput() != null ? result.getOutput().getClass() : "null"));

        if (result.getOutput() instanceof Map) {
            Map<String, Object> outputMap = (Map<String, Object>) result.getOutput();
            System.out.println("输出字段数: " + outputMap.size());
            System.out.println("\n输出字段:");
            for (Map.Entry<String, Object> entry : outputMap.entrySet()) {
                System.out.println("  " + entry.getKey() + " = " + entry.getValue() +
                        " (类型: " + (entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null") + ")");
            }
        }

        if (result.getError() != null) {
            System.err.println("\n执行错误: " + result.getError());
        }

        // 验证结果
        assertTrue(result.isSuccess(), "技能应该执行成功" +
                (result.getError() != null ? ", 错误: " + result.getError() : ""));
    }
}
