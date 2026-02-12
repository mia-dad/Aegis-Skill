package com.mia.aegis.skill.document;

import com.mia.aegis.skill.document.model.Series;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Series 单元测试。
 */
class SeriesTest {

    @Test
    void testCreateSeries() {
        Series series = Series.of("Revenue", Arrays.asList(100, 200, 300));

        assertThat(series.getName()).isEqualTo("Revenue");
        assertThat(series.getData()).containsExactly(100, 200, 300);
    }

    @Test
    void testSeriesWithNullData() {
        Series series = Series.of("Revenue", Collections.singletonList(null));

        assertThat(series.getName()).isEqualTo("Revenue");
        assertThat(series.getData()).containsExactly((Number) null);
    }

    @Test
    void testSeriesWithNullNameThrowsException() {
        assertThatThrownBy(() -> Series.of(null, Arrays.asList(100, 200)))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("name must not be null");
    }

    @Test
    void testSeriesWithEmptyNameThrowsException() {
        assertThatThrownBy(() -> Series.of("", Arrays.asList(100, 200)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("name must not be empty");
    }

    @Test
    void testSeriesWithNullDataListThrowsException() {
        assertThatThrownBy(() -> Series.of("Revenue", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("data must not be null");
    }

    @Test
    void testSeriesImmutability() {
        java.util.List<Number> mutableList = new java.util.ArrayList<>(Arrays.asList(100, 200));
        Series series = Series.of("Revenue", mutableList);

        // 修改原始列表不应影响 Series
        mutableList.add(300);

        assertThat(series.getData()).hasSize(2);
        assertThat(series.getData()).containsExactly(100, 200);
    }

    @Test
    void testSeriesBuilder() {
        Series series = Series.builder()
            .name("Profit")
            .addData(10)
            .addData(20)
            .addData(30)
            .build();

        assertThat(series.getName()).isEqualTo("Profit");
        assertThat(series.getData()).containsExactly(10, 20, 30);
    }

    @Test
    void testSeriesEqualsAndHashCode() {
        Series series1 = Series.of("Revenue", Arrays.asList(100, 200));
        Series series2 = Series.of("Revenue", Arrays.asList(100, 200));

        assertThat(series1).isEqualTo(series2);
        assertThat(series1.hashCode()).isEqualTo(series2.hashCode());
    }
}
