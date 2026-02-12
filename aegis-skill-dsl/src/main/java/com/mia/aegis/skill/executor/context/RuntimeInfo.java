package com.mia.aegis.skill.executor.context;

import com.mia.aegis.skill.i18n.MessageUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 运行时信息。
 *
 * <p>记录 Skill 执行过程中的元数据信息。</p>
 */
public class RuntimeInfo {

    private final long startTime;
    private final Map<String, Object> metadata;

    /**
     * 创建运行时信息。
     *
     * @param startTime 开始时间戳
     */
    public RuntimeInfo(long startTime) {
        this.startTime = startTime;
        this.metadata = new HashMap<String, Object>();
    }

    /**
     * 创建当前时间的运行时信息。
     *
     * @return RuntimeInfo 实例
     */
    public static RuntimeInfo now() {
        return new RuntimeInfo(System.currentTimeMillis());
    }

    /**
     * 获取开始时间。
     *
     * @return 开始时间戳
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * 获取已执行时间。
     *
     * @return 已执行毫秒数
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * 获取元数据。
     *
     * @return 不可变的元数据映射
     */
    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * 设置元数据。
     *
     * @param key 键
     * @param value 值
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * 获取元数据值。
     *
     * @param key 键
     * @return 值
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    @Override
    public String toString() {
        return MessageUtil.getMessage("runtimeinfo.tostring") +
                "startTime=" + startTime +
                ", elapsed=" + getElapsedTime() + "ms" +
                '}';
    }
}

