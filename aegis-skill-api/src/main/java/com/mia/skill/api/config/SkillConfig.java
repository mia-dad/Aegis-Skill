package com.mia.skill.api.config;


import com.mia.aegis.skill.dsl.parser.MarkdownSkillParser;
import com.mia.aegis.skill.dsl.parser.SkillParser;
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
import com.mia.skill.api.dashscope.DashScopeLLMAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
