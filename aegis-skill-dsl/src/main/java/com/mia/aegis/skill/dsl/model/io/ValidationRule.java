package com.mia.aegis.skill.dsl.model.io;

/**
 * 字段验证规则。
 *
 * <p>用于定义 InputSchema 中字段的验证约束。</p>
 *
 * <p>支持的验证规则：</p>
 * <ul>
 *   <li>pattern - 正则表达式（适用于 string）</li>
 *   <li>min - 最小值/长度</li>
 *   <li>max - 最大值/长度</li>
 *   <li>minItems - 数组最小项数（适用于 array）</li>
   *   <li>maxItems - 数组最大项数（适用于 array）</li>
 *   <li>message - 自定义错误消息</li>
 * </ul>
 */
public class ValidationRule {

    private final String pattern;
    private final Number min;
    private final Number max;
    private final Integer minItems;
    private final Integer maxItems;
    private final String message;

    /**
     * 创建验证规则。
     *
     * @param pattern 正则表达式
     * @param min 最小值/长度
     * @param max 最大值/长度
     * @param minItems 数组最小项数
     * @param maxItems 数组最大项数
     * @param message 自定义错误消息
     */
    public ValidationRule(String pattern, Number min, Number max,
                          Integer minItems, Integer maxItems, String message) {
        this.pattern = pattern;
        this.min = min;
        this.max = max;
        this.minItems = minItems;
        this.maxItems = maxItems;
        this.message = message;
    }

    public String getPattern() {
        return pattern;
    }

    public Number getMin() {
        return min;
    }

    public Number getMax() {
        return max;
    }

    public Integer getMinItems() {
        return minItems;
    }

    public Integer getMaxItems() {
        return maxItems;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ValidationRule{" +
                "pattern='" + pattern + '\'' +
                (min != null ? ", min=" + min : "") +
                (max != null ? ", max=" + max : "") +
                (minItems != null ? ", minItems=" + minItems : "") +
                (maxItems != null ? ", maxItems=" + maxItems : "") +
                (message != null ? ", message='" + message + '\'' : "") +
                '}';
    }
}
