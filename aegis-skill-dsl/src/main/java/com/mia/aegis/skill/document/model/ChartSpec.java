package com.mia.aegis.skill.document.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 图表规格。
 *
 * <p>定义图表的类型、标题、坐标轴和数据系列。</p>
 *
 * <h3>支持的图表类型 (v1)</h3>
 * <ul>
 *   <li>{@code "bar"} - 柱状图</li>
 *   <li>{@code "line"} - 折线图</li>
 * </ul>
 *
 * <h3>JSON 序列化</h3>
 * <pre>
 * {
 *   "type": "bar",
 *   "title": "收入趋势",
 *   "x": ["1月", "2月", "3月"],
 *   "series": [...]
 * }
 * </pre>
 *
 * @since 0.3.0
 */
public final class ChartSpec {

    /**
     * 柱状图类型。
     */
    public static final String TYPE_BAR = "bar";

    /**
     * 折线图类型。
     */
    public static final String TYPE_LINE = "line";

    private final String type;
    private final String title;
    private final List<String> x;
    private final List<Series> series;

    /**
     * 创建图表规格。
     *
     * @param type   图表类型（如 "bar", "line"）
     * @param title  图表标题
     * @param x      X 轴标签列表
     * @param series 数据系列列表
     * @throws NullPointerException 如果任何参数为 null
     * @throws IllegalArgumentException 如果 series 为空
     */
    @JsonCreator
    public ChartSpec(
            @JsonProperty("type") String type,
            @JsonProperty("title") String title,
            @JsonProperty("x") List<String> x,
            @JsonProperty("series") List<Series> series) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(x, "x must not be null");
        Objects.requireNonNull(series, "series must not be null");
        if (series.isEmpty()) {
            throw new IllegalArgumentException("series must contain at least one element");
        }
        this.type = type;
        this.title = title;
        this.x = Collections.unmodifiableList(new ArrayList<>(x));
        this.series = Collections.unmodifiableList(new ArrayList<>(series));
    }

    /**
     * 获取图表类型。
     *
     * @return 图表类型（如 "bar", "line"）
     */
    public String getType() {
        return type;
    }

    /**
     * 获取图表标题。
     *
     * @return 标题文本，不可为 null
     */
    public String getTitle() {
        return title;
    }

    /**
     * 获取 X 轴标签。
     *
     * @return X 轴标签列表，不可为 null
     */
    public List<String> getX() {
        return x;
    }

    /**
     * 获取数据系列。
     *
     * @return 数据系列列表，不可为 null，至少包含 1 项
     */
    public List<Series> getSeries() {
        return series;
    }

    /**
     * 创建 ChartSpec Builder。
     *
     * @return 新的 Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChartSpec chartSpec = (ChartSpec) o;
        return Objects.equals(type, chartSpec.type) &&
               Objects.equals(title, chartSpec.title) &&
               Objects.equals(x, chartSpec.x) &&
               Objects.equals(series, chartSpec.series);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, title, x, series);
    }

    @Override
    public String toString() {
        return "ChartSpec{type='" + type + "', title='" + title + "', x=" + x + ", series=" + series + "}";
    }

    /**
     * ChartSpec 构建器。
     */
    public static final class Builder {
        private String type;
        private String title;
        private List<String> x = new ArrayList<>();
        private List<Series> series = new ArrayList<>();

        private Builder() {}

        /**
         * 设置图表类型。
         *
         * @param type 图表类型（如 "bar", "line"）
         * @return this builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * 设置图表标题。
         *
         * @param title 图表标题
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * 设置 X 轴标签。
         *
         * @param x X 轴标签列表
         * @return this builder
         * @throws NullPointerException 如果 x 为 null
         */
        public Builder x(List<String> x) {
            if (x == null) {
                throw new NullPointerException("x must not be null");
            }
            this.x = new ArrayList<>(x);
            return this;
        }

        /**
         * 设置 X 轴标签（可变参数版本）。
         *
         * @param labels X 轴标签
         * @return this builder
         */
        public Builder x(String... labels) {
            this.x = new ArrayList<>(Arrays.asList(labels));
            return this;
        }

        /**
         * 设置数据系列列表。
         *
         * @param series 数据系列列表
         * @return this builder
         */
        public Builder series(List<Series> series) {
            this.series = new ArrayList<>(series);
            return this;
        }

        /**
         * 添加单个数据系列。
         *
         * @param series 数据系列
         * @return this builder
         */
        public Builder addSeries(Series series) {
            this.series.add(series);
            return this;
        }

        /**
         * 添加数据系列（便捷方法）。
         *
         * @param name 系列名称
         * @param data 数值数据
         * @return this builder
         */
        public Builder addSeries(String name, List<Number> data) {
            this.series.add(new Series(name, data));
            return this;
        }

        /**
         * 构建 ChartSpec 对象。
         *
         * @return 不可变的 ChartSpec 实例
         */
        public ChartSpec build() {
            return new ChartSpec(type, title, x, series);
        }
    }
}
