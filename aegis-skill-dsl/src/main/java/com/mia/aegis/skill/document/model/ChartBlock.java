package com.mia.aegis.skill.document.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * 图表块。
 *
 * <p>包含结构化的图表规格。图表的具体渲染由前端负责，
 * 后端仅输出语义化的 Chart Spec。</p>
 *
 * <h3>JSON 序列化</h3>
 * <pre>
 * {
 *   "type": "chart",
 *   "chart": { ... }
 * }
 * </pre>
 *
 * @since 0.3.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ChartBlock extends Block {

    /**
     * 固定类型标识符。
     */
    public static final String TYPE = "chart";

    private final ChartSpec chart;

    /**
     * 创建图表块。
     *
     * @param chart 图表规格，不可为 null
     * @throws NullPointerException 如果 chart 为 null
     */
    @JsonCreator
    public ChartBlock(@JsonProperty("chart") ChartSpec chart) {
        Objects.requireNonNull(chart, "chart must not be null");
        this.chart = chart;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * 获取图表规格。
     *
     * @return Chart Spec 对象，不可为 null
     */
    public ChartSpec getChart() {
        return chart;
    }

    /**
     * 创建图表块的工厂方法。
     *
     * @param chart 图表规格
     * @return 新的 ChartBlock 实例
     * @throws NullPointerException 如果 chart 为 null
     */
    public static ChartBlock of(ChartSpec chart) {
        return new ChartBlock(chart);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChartBlock that = (ChartBlock) o;
        return Objects.equals(chart, that.chart);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chart);
    }

    @Override
    public String toString() {
        return "ChartBlock{type='" + TYPE + "', chart=" + chart + "}";
    }
}
