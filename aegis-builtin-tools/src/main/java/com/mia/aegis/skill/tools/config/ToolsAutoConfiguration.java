package com.mia.aegis.skill.tools.config;

import com.mia.aegis.skill.tools.ToolProvider;
import com.mia.aegis.skill.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Aegis Tools 自动配置类（改进版）。
 *
 * <h3>自动配置机制</h3>
 * <p>使用 <code>&#64;ComponentScan</code> 自动扫描所有带 <code>&#64;Component</code> 注解的工具类。</p>
 *
 * <h3>工具注册方式</h3>
 * <p>只需在工具实现类上添加 <code>&#64;Component</code> 注解，即可自动注册：</p>
 * <pre>{@code
 * &#64;Component
 * public class MyTool extends BuiltInTool {
 *     public MyTool() {
 *         super("my_tool", "My tool description", Category.DATA_PROCESSING);
 *     }
 *     // ...
 * }
 * }</pre>
 *
 * <h3>扫描范围</h3>
 * <ul>
 *   <li><code>com.mia.aegis.skill.tools</code> - aegis-tools 包</li>
 *   <li>可通过配置添加更多包路径</li>
 * </ul>
 *
 * <h3>配置管理</h3>
 * <p>工具配置通过 <code>&#64;ConfigurationProperties</code> 管理：</p>
 * <pre>
 * # application-my-tool.properties
 * aegis.tools.my-tool.timeout=5000
 * aegis.tools.my-tool.retry-count=3
 * </pre>
 *
 * <h3>禁用自动配置</h3>
 * <pre>
 * # application.properties
 * spring.autoconfigure.exclude=com.mia.aegis.skill.tools.config.ToolsAutoConfiguration
 * </pre>
 *
 * @see Component
 * @see ComponentScan
 * @see ToolProvider
 * @see ToolRegistry
 */
@Configuration
@ConditionalOnClass(ToolRegistry.class)
@ComponentScan(
    basePackages = {
        "com.mia.aegis.skill.tools"
    },
    useDefaultFilters = false,
    includeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ANNOTATION,
            classes = {Component.class}
        )
    }
)
public class ToolsAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ToolsAutoConfiguration.class);

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 自动注册所有带 @Component 注解的工具。
     *
     * <p>在 Spring 容器初始化后自动执行。</p>
     */
    @javax.annotation.PostConstruct
    public void registerAllTools() {
        logger.info("Auto-registering built-in tools from component scan...");

        // 从 Spring 容器中获取所有 ToolProvider Bean
        Collection<ToolProvider> tools = applicationContext.getBeansOfType(ToolProvider.class).values();

        int registeredCount = 0;
        for (ToolProvider tool : tools) {
            try {
                toolRegistry.register(tool);
                registeredCount++;
                logger.info("✓ Registered tool: {} ({})", tool.getName(), tool.getClass().getSimpleName());
            } catch (Exception e) {
                logger.error("✗ Failed to register tool: {} ({}) - {}",
                        tool.getName(), tool.getClass().getSimpleName(), e.getMessage());
            }
        }

        logger.info("Tool registration complete: {} tools registered", registeredCount);
    }
}
