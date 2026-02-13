package com.mia.skill.api.config;


import com.mia.aegis.skill.dsl.parser.MarkdownSkillParser;
import com.mia.aegis.skill.dsl.parser.SkillParser;
import com.mia.aegis.skill.dsl.validator.ComprehensiveSkillValidator;
import com.mia.aegis.skill.persistence.initializer.SkillInitializer;
import com.mia.aegis.skill.persistence.repository.SkillRepository;
import com.mia.aegis.skill.persistence.repository.impl.JdbcSkillRepository;
import com.mia.aegis.skill.template.AegisTemplateRenderer;
import com.mia.aegis.skill.executor.engine.DefaultSkillExecutor;
import com.mia.aegis.skill.executor.engine.SkillExecutor;
import com.mia.aegis.skill.i18n.MessageUtil;
import com.mia.aegis.skill.llm.DefaultLLMAdapterRegistry;
import com.mia.aegis.skill.llm.LLMAdapter;
import com.mia.aegis.skill.llm.LLMAdapterRegistry;
import com.mia.aegis.skill.template.MustacheTemplateRenderer;
import com.mia.aegis.skill.template.TemplateRenderer;
import com.mia.aegis.skill.spi.DefaultToolRegistry;
import com.mia.aegis.skill.tools.ToolRegistry;
import com.mia.aegis.skill.tools.config.DataSourceRegistry;
import com.mia.skill.api.dashscope.DashScopeLLMAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Skill 执行环境配置(SpringBoot)。
 */
@Configuration
public class SkillConfig {

    @Bean
    public LLMAdapterRegistry llmAdapterRegistry(
            @Value("${aegis.llm.dashscope.api-key:}") String dashscopeApiKey) {

        LLMAdapterRegistry registry = new DefaultLLMAdapterRegistry();

        // 注册 DashScope Adapter（从配置文件注入 API Key）
        DashScopeLLMAdapter dashscope = new DashScopeLLMAdapter(dashscopeApiKey);
        registry.register(dashscope);

        // 注册 Mock Adapter（作为备用）
        registry.register(new SimpleMockLLMAdapter());

        // 如果 DashScope 可用，设为默认；否则使用 Mock
        if (dashscope.isAvailable()) {
            registry.setDefault("dashscope");
        } else {
            registry.setDefault("mock");
        }

        return registry;
    }

    /**
     * 简单的 Mock LLM Adapter，用于测试。
     */
    static class SimpleMockLLMAdapter implements LLMAdapter {
        @Override
        public String getName() {
            return "mock";
        }

        @Override
        public String[] getSupportedModels() {
            return new String[]{"mock-model"};
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public String invoke(String prompt, Map<String, Object> options) {
            String truncatedPrompt = prompt.length() > 50 ? prompt.substring(0, 50) + "..." : prompt;
            return MessageUtil.getMessage("mock.llm.response", truncatedPrompt);
        }

        @Override
        public CompletableFuture<String> invokeAsync(String prompt, Map<String, Object> options) {
            return CompletableFuture.completedFuture(invoke(prompt, options));
        }
    }

    @Bean
    public ToolRegistry toolRegistry() {
        return new DefaultToolRegistry();
    }

    @Bean
    public TemplateRenderer templateRenderer() {
        return new MustacheTemplateRenderer();
    }

    @Bean
    public SkillParser skillParser() {
        return new MarkdownSkillParser();
    }

    @Bean
    public SkillExecutor skillExecutor(ToolRegistry toolRegistry,
                                       LLMAdapterRegistry llmRegistry,
                                       TemplateRenderer templateRenderer) {
        return new DefaultSkillExecutor(toolRegistry, llmRegistry, templateRenderer);
    }

    @Bean
    public ComprehensiveSkillValidator comprehensiveSkillValidator(SkillParser skillParser,
                                                                   ToolRegistry toolRegistry) {
        return new ComprehensiveSkillValidator(skillParser, new AegisTemplateRenderer(), toolRegistry);
    }

    @Bean
    public SkillRepository skillRepository(DataSourceRegistry dataSourceRegistry,
                                            SkillParser skillParser,
                                            @Value("${aegis.persistence.datasource:main_db}") String datasourceName) {
        Logger log = LoggerFactory.getLogger(SkillConfig.class);
        if (dataSourceRegistry.hasDataSource(datasourceName)) {
            log.info("Initializing JdbcSkillRepository with datasource: {}", datasourceName);
            return new JdbcSkillRepository(dataSourceRegistry.getDataSource(datasourceName), skillParser);
        }
        log.warn("Datasource '{}' not found, SkillRepository will not be available for database operations", datasourceName);
        return null;
    }

    /**
     * 启动时将 classpath 中的 .md 技能文件导入数据库。
     *
     * <p>仅导入数据库中不存在的 skillId+version，不覆盖已有记录。</p>
     */
    @Bean
    public SkillInitializer skillInitializer(SkillRepository skillRepository, SkillParser skillParser) {
        Logger log = LoggerFactory.getLogger(SkillConfig.class);
        SkillInitializer initializer = new SkillInitializer(skillRepository, skillParser);

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:skills/*.md");
            log.info("Found {} skill files in classpath for import", resources.length);

            List<SkillInitializer.ResourceEntry> entries = new ArrayList<SkillInitializer.ResourceEntry>();
            for (Resource resource : resources) {
                if (resource.isReadable()) {
                    entries.add(new SkillInitializer.ResourceEntry(
                            resource.getFilename(), resource.getInputStream()));
                }
            }
            initializer.importFromResources(entries);
        } catch (IOException e) {
            log.warn("Failed to scan classpath skill files: {}", e.getMessage());
        }

        return initializer;
    }
}
