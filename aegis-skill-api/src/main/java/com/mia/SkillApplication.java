package com.mia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Aegis Skill API 应用入口。
 *
 * <p>提供 REST API 用于测试 LLM Adapter 和 Skill 执行。</p>
 *
 * <h3>启动方式</h3>
 * <pre>
 * # 使用 Maven
 * mvn spring-boot:run -Ddashscope.api.key=sk-xxx
 *
 * # 或打包后运行
 * java -Ddashscope.api.key=sk-xxx -jar target/aegis-skill-api-1.0.0-SNAPSHOT.jar
 * </pre>
 *
 * <h3>API 端点</h3>
 * <ul>
 *   <li>POST /api/llm/invoke - 调用 LLM</li>
 *   <li>POST /api/skill/execute - 执行 Skill</li>
 *   <li>GET /api/adapters - 列出可用 Adapter</li>
 * </ul>
 */

@SpringBootApplication(scanBasePackages = {
    "com.mia"
})
@ConfigurationPropertiesScan(basePackages = {
    "com.mia.aegis.skill",
    "com.mia.skill.api"
})
public class SkillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillApplication.class, args);
    }
}
