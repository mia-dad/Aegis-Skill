package com.mia.aegis.skill.tools.config;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具配置基类（不依赖 Spring）。
 *
 * <p>所有需要配置参数的工具应继承此类。</p>
 *
 * <h3>配置文件命名规范</h3>
 * <pre>
 * # 工具配置文件: application-{tool-name}-tools.properties
 * # 例如: application-get-financial-data-tools.properties
 *
 * # 配置前缀规范
 * aegis.tools.{tool-name}.{property-name}=value
 * </pre>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 1. 创建配置类
 * public class MyToolConfig extends ToolConfigBase {
 *     private String timeout = "5000";
 *     private boolean enableCache = true;
 *
 *     public String getTimeout() { return timeout; }
 *     public void setTimeout(String timeout) { this.timeout = timeout; }
 *     public boolean isEnableCache() { return enableCache; }
 *     public void setEnableCache(boolean enableCache) { this.enableCache = enableCache; }
 * }
 *
 * // 2. 在工具中使用
 * &#64;Component
 * public class MyTool extends BuiltInTool {
 *     private final MyToolConfig config;
 *
 *     &#64;Autowired(required = false)
 *     public MyTool(MyToolConfig config) {
 *         super("my_tool", "My tool", Category.DATA_PROCESSING);
 *         this.config = config != null ? config : new MyToolConfig();
 *     }
 *
 *     &#64;Override
 *     public Object execute(Map<String, Object> input) {
 *         String timeout = config.getTimeout();
 *         // 使用配置...
 *     }
 * }
 * }</pre>
 *
 * <h3>配置文件示例</h3>
 * <pre>
 * # application-my-tool.properties
 * aegis.tools.my-tool.timeout=5000
 * aegis.tools.my-tool.enable-cache=true
 * </pre>
 */
public abstract class ToolConfigBase {

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
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            java.lang.reflect.Field[] fields = this.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(this);
                map.put(field.getName(), value);
            }
        } catch (IllegalAccessException e) {
            // 忽略
        }
        return map;
    }

    /**
     * 获取配置前缀。
     *
     * <p>默认格式：aegis.tools.{tool-class-simple-name}</p>
     *
     * @return 配置前缀
     */
    public String getPrefix() {
        String className = this.getClass().getSimpleName();
        // 将驼峰命名转换为短横线命名
        return "aegis.tools." + camelToKebab(className);
    }

    /**
     * 将驼峰命名转换为短横线命名。
     */
    private String camelToKebab(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
}
