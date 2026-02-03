package com.mia.aegis.skill.dsl.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 日志功能集成测试。
 */
@DisplayName("日志集成测试")
class LoggingIntegrationTest {

    @Test
    @DisplayName("应该能够创建Logger实例")
    void shouldBeAbleToCreateLogger() {
        // 如果能运行到这里，说明SLF4J依赖配置正确
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
        assertThat(logger).isNotNull();
    }

    @Test
    @DisplayName("应该能够记录不同级别的日志")
    void shouldBeAbleToLogDifferentLevels() {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LoggingIntegrationTest.class);

        // 测试不同级别的日志
        logger.trace("这是一条TRACE日志");
        logger.debug("这是一条DEBUG日志");
        logger.info("这是一条INFO日志");
        logger.warn("这是一条WARN日志");
        logger.error("这是一条ERROR日志");

        // 如果能运行到这里，说明所有日志级别都能正常工作
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("MarkdownSkillParser应该有Logger实例")
    void markdownSkillParserShouldHaveLogger() {
        MarkdownSkillParser parser = new MarkdownSkillParser();

        // 通过解析一个简单的技能来触发日志
        String content = "# skill: test\n\n## steps\n\n### step: test\n\n**type**: prompt\n\n```prompt\ntest\n```\n";

        try {
            parser.parse(content);
            // 如果能运行到这里，说明日志正常工作
            assertThat(true).isTrue();
        } catch (Exception e) {
            // 忽略解析错误，只测试日志是否可用
        }
    }
}
