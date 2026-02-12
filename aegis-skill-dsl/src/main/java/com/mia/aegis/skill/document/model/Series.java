package com.mia.aegis.skill.document.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 图表数据系列。
 *
 * <p>包含系列名称和数值数据。数据可包含 null 值表示缺失。</p>
 *
 * <h3>JSON 序列化</h3>
 * <pre>
 * {
 *   "name": "收入",
 *   "data": [100, 120, null, 150]
 * }
 * </pre>
 *
 * @since 0.3.0
 */
public final class Series {

    private final String name;
    private final List<Number> data;

    /**
     * 创建数据系列。
     *
     * @param name 系列名称，不可为 null 或空
     * @param data 数值数据，不可为 null（可包含 null 元素）
     * @throws NullPointerException 如果 name 或 data 为 null
     * @throws IllegalArgumentException 如果 name 为空
     */
    @JsonCreator
    public Series(
            @JsonProperty("name") String name,
            @JsonProperty("data") List<Number> data) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(data, "data must not be null");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        this.name = name;
        this.data = Collections.unmodifiableList(new ArrayList<>(data));
    }

    /**
     * 获取系列名称。
     *
     * @return 系列名称，不可为 null 或空
     */
    public String getName() {
        return name;
    }

    /**
     * 获取数值数据。
     *
     * @return 数值列表，不可为 null（可包含 null 元素）
     */
    public List<Number> getData() {
        return data;
    }

    /**
     * 创建数据系列的工厂方法。
     *
     * @param name 系列名称
     * @param data 数值数据
     * @return 新的 Series 实例
     */
    public static Series of(String name, List<Number> data) {
        return new Series(name, data);
    }

    /**
     * 创建 Series Builder。
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
        Series series = (Series) o;
        return Objects.equals(name, series.name) && Objects.equals(data, series.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, data);
    }

    @Override
    public String toString() {
        return "Series{name='" + name + "', data=" + data + "}";
    }

    /**
     * Series 构建器。
     */
    public static final class Builder {
        private String name;
        private List<Number> data = new ArrayList<>();

        private Builder() {}

        /**
         * 设置系列名称。
         *
         * @param name 系列名称
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 设置数值数据。
         *
         * @param data 数值数据
         * @return this builder
         */
        public Builder data(List<Number> data) {
            this.data = new ArrayList<>(data);
            return this;
        }

        /**
         * 添加单个数值。
         *
         * @param value 数值（可为 null）
         * @return this builder
         */
        public Builder addData(Number value) {
            this.data.add(value);
            return this;
        }

        /**
         * 构建 Series 对象。
         *
         * @return 不可变的 Series 实例
         */
        public Series build() {
            return new Series(name, data);
        }
    }
}
