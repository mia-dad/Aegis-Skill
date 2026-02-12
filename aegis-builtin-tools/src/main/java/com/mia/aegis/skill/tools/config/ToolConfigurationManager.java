package com.mia.aegis.skill.tools.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具配置管理器（简化版）。
 *
 * <p>负责管理和访问工具配置参数。</p>
 *
 * <h3>配置文件加载规则</h3>
 * <p>配置通过 Spring Boot 的 <code>&#64;ConfigurationProperties</code> 自动绑定。</p>
 *
 * <h3>配置前缀规范</h3>
 * <pre>
 * aegis.tools.{tool-name}.{property-name}=value
 * </pre>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 自动注入配置 Bean
 * &#64;Autowired
 * private GetFinancialDataToolConfig config;
 *
 * // 使用配置
 * String period = config.getDefaultPeriod();
 * </pre>
 */
@Component
public class ToolConfigurationManager {

    private static final Logger logger = LoggerFactory.getLogger(ToolConfigurationManager.class);

    @Autowired
    private ApplicationContext applicationContext;

    // 配置缓存：configClass -> config object
    private final Map<Class<?>, Object> configCache = new HashMap<Class<?>, Object>();

    /**
     * 初始化配置管理器。
     */
    public void initialize() {
        logger.info("Initializing Tool Configuration Manager...");
        logLoadedConfigurations();
    }

    /**
     * 获取工具配置对象。
     *
     * @param configClass 配置类
     * @param <T> 配置类型
     * @return 配置对象
     */
    public <T> T getConfig(Class<T> configClass) {
        @SuppressWarnings("unchecked")
        T cached = (T) configCache.get(configClass);
        if (cached != null) {
            return cached;
        }

        // 从 Spring 容器中查找配置 Bean
        try {
            Map<String, T> beans = applicationContext.getBeansOfType(configClass);
            if (!beans.isEmpty()) {
                T config = beans.values().iterator().next();
                configCache.put(configClass, config);
                logger.debug("Loaded config from Spring context: {}", configClass.getSimpleName());
                return config;
            }
        } catch (Exception e) {
            logger.debug("No config bean found: {}", e.getMessage());
        }

        // 创建默认配置实例
        try {
            T config = configClass.getDeclaredConstructor().newInstance();
            configCache.put(configClass, config);
            logger.debug("Created default config: {}", configClass.getSimpleName());
            return config;
        } catch (Exception e) {
            logger.warn("Failed to create config '{}': {}", configClass.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * 记录已加载的配置。
     */
    private void logLoadedConfigurations() {
        logger.info("Tool Configuration Manager initialized");
        logger.info("Configuration loading through @ConfigurationProperties is enabled");
        logger.info("Config files should follow pattern: application-{tool-name}-tools.properties");
    }
}
