package com.mia.aegis.skill.document;

import com.mia.aegis.skill.document.model.ChartSpec;
import com.mia.aegis.skill.document.model.Series;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ChartSpec 单元测试。
 */
class ChartSpecTest {

    @Test
    void testCreateBarChartSpec() {
        Series series = Series.of("Revenue", Arrays.asList(100, 200, 300));
        ChartSpec chart = ChartSpec.builder()
            .type(ChartSpec.TYPE_BAR)
            .title("Revenue Chart")
            .x("Q1", "Q2", "Q3")
            .addSeries(series)
            .build();

        assertThat(chart.getType()).isEqualTo(ChartSpec.TYPE_BAR);
        assertThat(chart.getTitle()).isEqualTo("Revenue Chart");
        assertThat(chart.getX()).containsExactly("Q1", "Q2", "Q3");
        assertThat(chart.getSeries()).containsExactly(series);
    }

    @Test
    void testCreateLineChartSpec() {
        Series series = Series.of("Profit", Arrays.asList(50, 150, 250));
        ChartSpec chart = ChartSpec.builder()
            .type(ChartSpec.TYPE_LINE)
            .title("Profit Trend")
            .x("Jan", "Feb", "Mar")
            .addSeries(series)
            .build();

        assertThat(chart.getType()).isEqualTo(ChartSpec.TYPE_LINE);
        assertThat(chart.getTitle()).isEqualTo("Profit Trend");
    }

    @Test
    void testChartSpecWithMultipleSeries() {
        Series series1 = Series.of("Revenue", Arrays.asList(100, 200));
        Series series2 = Series.of("Profit", Arrays.asList(50, 100));

        ChartSpec chart = ChartSpec.builder()
            .type(ChartSpec.TYPE_BAR)
            .title("Financial Chart")
            .x("2023", "2024")
            .series(Arrays.asList(series1, series2))
            .build();

        assertThat(chart.getSeries()).hasSize(2);
        assertThat(chart.getSeries()).containsExactly(series1, series2);
    }

    @Test
    void testChartSpecWithNullTypeThrowsException() {
        assertThatThrownBy(() -> ChartSpec.builder()
            .type(null)
            .title("Test")
            .x("A", "B")
            .addSeries("Series", Arrays.asList(1, 2))
            .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("type must not be null");
    }

    @Test
    void testChartSpecWithNullTitleThrowsException() {
        assertThatThrownBy(() -> ChartSpec.builder()
            .type(ChartSpec.TYPE_BAR)
            .title(null)
            .x("A", "B")
            .addSeries("Series", Arrays.asList(1, 2))
            .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("title must not be null");
    }

    @Test
    void testChartSpecWithNullXThrowsException() {
        assertThatThrownBy(() -> ChartSpec.builder()
            .type(ChartSpec.TYPE_BAR)
            .title("Test")
            .x((java.util.List<String>) null)
            .addSeries("Series", Arrays.asList(1, 2))
            .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("x must not be null");
    }

    @Test
    void testChartSpecWithEmptySeriesThrowsException() {
        assertThatThrownBy(() -> ChartSpec.builder()
            .type(ChartSpec.TYPE_BAR)
            .title("Test")
            .x("A", "B")
            .series(Collections.emptyList())
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("series must contain at least one element");
    }

    @Test
    void testChartSpecEqualsAndHashCode() {
        Series series = Series.of("Data", Arrays.asList(1, 2, 3));

        ChartSpec chart1 = ChartSpec.builder()
            .type(ChartSpec.TYPE_BAR)
            .title("Test")
            .x("A", "B", "C")
            .addSeries(series)
            .build();

        Series series2 = Series.of("Data", Arrays.asList(1, 2, 3));
        ChartSpec chart2 = ChartSpec.builder()
            .type(ChartSpec.TYPE_BAR)
            .title("Test")
            .x("A", "B", "C")
            .addSeries(series2)
            .build();

        assertThat(chart1).isEqualTo(chart2);
        assertThat(chart1.hashCode()).isEqualTo(chart2.hashCode());
    }
}
