package com.mia.aegis.skill.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 国际化消息工具类。
 *
 * <p>提供访问国际化消息的便捷方法，支持参数化消息。</p>
 *
 * <p>多模块支持：自动加载所有模块的 i18n/messages.properties 资源文件。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 获取默认语言消息
 * String message = Messages.get("skill.execution.started", skillName);
 *
 * // 获取指定语言消息
 * String message = Messages.get("condition.evaluating", condition, Locale.CHINESE);
 *
 * // 带多个参数
 * String message = Messages.get("skill.execution.step.started", stepName, index, total);
 * }</pre>
 *
 * <h3>消息参数化</h3>
 * <p>支持两种参数化方式：</p>
 * <ol>
 *   <li>MessageFormat 风格：{0}, {1}, {2}...</li>
 * </ol>
 *
 * @since 1.0.0
 */
public final class Messages {

    /**
     * 多模块消息资源 basename 列表。
     *
     * <p>ResourceBundle 会自动从所有 jar 包的 classpath 中加载这些资源文件。</p>
     */
    private static final String[] BASE_NAMES = {
        "i18n/messages",     // 主模块消息 (aegis-skill-dsl)
    };

    private Messages() {
        // 工具类，禁止实例化
    }

    /**
     * 获取默认语言的消息（使用系统默认 Locale）。
     *
     * @param code 消息代码
     * @param args 消息参数
     * @return 格式化后的消息
     */
    public static String get(String code, Object... args) {
        return get(code, Locale.getDefault(), args);
    }

    /**
     * 获取指定语言的消息。
     *
     * @param code 消息代码
     * @param locale 语言设置
     * @param args 消息参数
     * @return 格式化后的消息
     */
    public static String get(String code, Locale locale, Object... args) {
        if (code == null || code.isEmpty()) {
            return "";
        }

        String pattern = getMessagePattern(code, locale);

        if (args == null || args.length == 0) {
            return pattern;
        }

        // 使用 MessageFormat 格式化消息
        return MessageFormat.format(pattern, args);
    }

    /**
     * 从资源文件中获取消息模式。
     *
     * @param code 消息代码
     * @param locale 语言设置
     * @return 消息模式字符串
     */
    private static String getMessagePattern(String code, Locale locale) {
        // 按顺序尝试从各个 basename 加载
        for (String baseName : BASE_NAMES) {
            try {
                ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale);
                if (bundle.containsKey(code)) {
                    return bundle.getString(code);
                }
            } catch (MissingResourceException e) {
                // 继续尝试下一个 basename
            }
        }

        // 未找到消息，返回 code 本身
        return code;
    }

    /**
     * 获取 ResourceBundle（用于传统方式访问）。
     *
     * <p>注意：由于支持多模块，此方法只返回第一个 basename 的 ResourceBundle。</p>
     *
     * @param locale 语言设置
     * @return ResourceBundle
     */
    public static ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle(BASE_NAMES[0], locale);
    }

    /**
     * 格式化消息（使用 MessageFormat）。
     *
     * @param pattern 消息模式
     * @param args 参数
     * @return 格式化后的消息
     */
    public static String format(String pattern, Object... args) {
        return MessageFormat.format(pattern, args);
    }

    /**
     * 获取默认中文消息。
     *
     * @param code 消息代码
     * @param args 消息参数
     * @return 中文消息
     */
    public static String zh(String code, Object... args) {
        return get(code, Locale.SIMPLIFIED_CHINESE, args);
    }

    /**
     * 获取英文消息。
     *
     * @param code 消息代码
     * @param args 消息参数
     * @return 英文消息
     */
    public static String en(String code, Object... args) {
        return get(code, Locale.ENGLISH, args);
    }
}
