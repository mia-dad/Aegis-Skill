package com.mia.aegis.skill.document;

import com.mia.aegis.skill.document.model.ChartBlock;
import com.mia.aegis.skill.document.model.ChartSpec;
import com.mia.aegis.skill.document.model.Series;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ChartBlock 单元测试。
 */
class ChartBlockTest {

    @Test
    void testCreateChartBlock() {
        Series series = Series.of("Revenue", Arrays.asList(100, 200, 300));
        ChartSpec chart = ChartSpec.builder()
            .type(ChartSpec.TYPE_BAR)
            .title("Revenue Chart")
            .x("Q1", "Q2", "Q3")
            .addSeries(series)
            .build();

        ChartBlock block = ChartBlock.of(chart);

        assertThat(block.getType()).isEqualTo("chart");
        assertThat(block.getChart()).isEqualTo(chart);
    }

    @Test
    void testChartBlockWithNullChartThrowsException() {
        assertThatThrownBy(() -> ChartBlock.of(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("chart must not be null");
    }

    @Test
    void testChartBlockEqualsAndHashCode() {
        Series series1 = Series.of("Data", Arrays.asList(1, 2, 3));
        ChartSpec chart1 = ChartSpec.builder()
            .type(ChartSpec.TYPE_BAR)
            .title("Test")
            .x("A", "B", "C")
            .addSeries(series1)
            .build();

        Series series2 = Series.of("Data", Arrays.asList(1, 2, 3));
        ChartSpec chart2 = ChartSpec.builder()
            .type(ChartSpec.TYPE_BAR)
            .title("Test")
            .x("A", "B", "C")
            .addSeries(series2)
            .build();

        ChartBlock block1 = ChartBlock.of(chart1);
        ChartBlock block2 = ChartBlock.of(chart2);

        assertThat(block1).isEqualTo(block2);
        assertThat(block1.hashCode()).isEqualTo(block2.hashCode());
    }

    @Test
    void testChartBlockToString() {
        Series series = Series.of("Data", Arrays.asList(1, 2, 3));
        ChartSpec chart = ChartSpec.builder()
            .type(ChartSpec.TYPE_BAR)
            .title("Test Chart")
            .x("A", "B", "C")
            .addSeries(series)
            .build();

        ChartBlock block = ChartBlock.of(chart);
        String str = block.toString();

        assertThat(str).contains("type='chart'");
        assertThat(str).contains("chart=");
    }
}
