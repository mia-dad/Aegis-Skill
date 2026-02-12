package com.mia.aegis.skill.dsl.model.io;

/**
 * 前端展示类型枚举。
 *
 * <p>定义 YAML 格式 output_schema 中 type 字段的可选值，
 * 用于指导前端如何渲染输出内容。</p>
 *
 * @see OutputContract
 */
public enum DisplayType {

    /**
     * 文本内容。
     *
     * <p>以对话形式呈现的文本内容（需要支持 Markdown 格式）。</p>
     */
    TEXT,

    /**
     * 用户提醒/通知。
     *
     * <p>前端渲染为醒目的提示框，支持不同级别（info/success/warning/error）。</p>
     */
    MESSAGE,

    /**
     * 文件下载。
     *
     * <p>前端显示文件下载链接或按钮。</p>
     */
    FILE,

    /**
     * 表格数据。
     *
     * <p>前端使用表格组件展示数据。</p>
     */
    TABLE,

    /**
     * 图表数据。
     *
     * <p>前端使用图表组件渲染（折线图、柱状图、饼图等）。</p>
     */
    CHART,

    /**
     * 图文混排。
     *
     * <p>前端使用富文本文档渲染器展示复杂内容。</p>
     */
    DOCUMENT;

    /**
     * 从字符串解析 DisplayType。
     *
     * @param value 类型字符串（不区分大小写）
     * @return 对应的 DisplayType，如果为空或无法识别返回 null
     */
    public static DisplayType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // 无法识别的类型，返回 null 让调用者处理
            return null;
        }
    }

    /**
     * 检查字符串是否为有效的 DisplayType。
     *
     * @param value 待检查的字符串
     * @return 是否有效
     */
    public static boolean isValid(String value) {
        return fromString(value) != null;
    }
}
