package com.mia.aegis.skill.tools.config;

import java.util.Map;

/**
 * 工具配置基类。
 *
 * <p>所有需要配置参数的工具应继承此类，并使用 @ConfigurationProperties 注解。</p>
 *
 * <h3>配置文件命名规范</h3>
 * <pre>
 * # 工具项目配置文件: application-{tool-name}.properties
 * # 例如: application-get-financial-data.properties
 *
 * # 配置前缀规范
 * aegis.tools.{tool-name}.{property-name}=value
 * </pre>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * &#64;ConfigurationProperties(prefix = "aegis.tools.get-financial-data")
 * &#64;Component
 * public class GetFinancialDataToolConfig extends ToolConfig {
 *     private String defaultPeriod = "2024Q4";
 *     private String currency = "CNY";
 *     private boolean enableCache = true;
 *
 *     // getters and setters
 * }
 * }</pre>
 *
 * <h3>配置文件示例</h3>
 * <pre>
 * # application-get-financial-data.properties
 * aegis.tools.get-financial-data.default-period=2024Q4
 * aegis.tools.get-financial-data.currency=CNY
 * aegis.tools.get-financial-data.enable-cache=true
 * </pre>
 *
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 * @see org.springframework.stereotype.Component
 */
public abstract class ToolConfig {

    /**
     * 获取配置描述信息。
     *
     * @return 配置描述
     */
    public String getDescription() {
        return this.getClass().getSimpleName();
    }

    /**
     * 验证配置是否有效。
     *
     * <p>子类可以覆盖此方法添加自定义验证逻辑。</p>
     *
     * @return 验证结果
     */
    public boolean isValid() {
        return true;
    }

    /**
     * 获取配置错误消息。
     *
     * <p>当 isValid() 返回 false 时，此方法返回错误描述。</p>
     *
     * @return 错误消息
     */
    public String getErrorMessage() {
        return "Configuration validation failed";
    }

    /**
     * 将配置转换为 Map 格式（用于调试和日志）。
     *
     * @return 配置属性 Map
     */
    public Map<String, Object> toMap() {
        // 子类可以覆盖此方法提供自定义实现
        return java.util.Collections.emptyMap();
    }
}
