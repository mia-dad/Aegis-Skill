package com.mia.aegis.skill.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 国际化消息工具类。
 *
 * <p>提供错误消息的国际化支持，默认使用中文，可通过 {@link #setLocale(Locale)} 切换语言。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 使用默认语言（中文）
 * String message = MessageUtil.getMessage("skill.id.null");
 *
 * // 使用带参数的消息
 * String message = MessageUtil.getMessage("step.config.mismatch", "TOOL", "PROMPT");
 *
 * // 切换到英文
 * MessageUtil.setLocale(Locale.US);
 * }</pre>
 */
public final class MessageUtil {

    private static final String BUNDLE_NAME = "messages";
    private static Locale currentLocale = Locale.SIMPLIFIED_CHINESE; // 默认中文
    private static ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, currentLocale);

    private MessageUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 获取指定键的消息。
     *
     * @param key 消息键
     * @return 本地化的消息
     */
    public static String getMessage(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "???" + key + "???";
        }
    }

    /**
     * 获取指定键的消息，并格式化参数。
     *
     * @param key 消息键
     * @param params 格式化参数
     * @return 本地化并格式化后的消息
     */
    public static String getMessage(String key, Object... params) {
        String pattern = getMessage(key);
        return MessageFormat.format(pattern, params);
    }

    /**
     * 设置当前语言环境。
     *
     * @param locale 语言环境
     */
    public static void setLocale(Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("Locale cannot be null");
        }
        currentLocale = locale;
        bundle = ResourceBundle.getBundle(BUNDLE_NAME, currentLocale);
    }

    /**
     * 获取当前语言环境。
     *
     * @return 当前语言环境
     */
    public static Locale getLocale() {
        return currentLocale;
    }

    /**
     * 重置为默认语言环境（中文）。
     */
    public static void resetToDefault() {
        setLocale(Locale.SIMPLIFIED_CHINESE);
    }
}
