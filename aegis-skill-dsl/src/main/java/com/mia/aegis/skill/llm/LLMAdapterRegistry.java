package com.mia.aegis.skill.llm;

import java.util.List;
import java.util.Optional;

/**
 * LLM Adapter 注册表接口。
 * 管理 LLMAdapter 的注册和查找。
 */
public interface LLMAdapterRegistry {

    /**
     * 注册 LLM Adapter。
     *
     * @param adapter LLM Adapter 实例
     */
    void register(LLMAdapter adapter);

    /**
     * 取消注册 LLM Adapter。
     *
     * @param name Adapter 名称
     * @return 是否成功取消注册
     */
    boolean unregister(String name);

    /**
     * 查找 LLM Adapter。
     *
     * @param name Adapter 名称
     * @return LLM Adapter（如果存在）
     */
    Optional<LLMAdapter> find(String name);

    /**
     * 获取默认 LLM Adapter。
     *
     * @return 默认 Adapter（第一个注册的或显式设置的）
     */
    Optional<LLMAdapter> getDefault();

    /**
     * 设置默认 LLM Adapter。
     *
     * @param name Adapter 名称
     * @return 是否成功设置
     */
    boolean setDefault(String name);

    /**
     * 检查 Adapter 是否已注册。
     *
     * @param name Adapter 名称
     * @return 是否已注册
     */
    boolean contains(String name);

    /**
     * 获取所有已注册的 Adapter 名称。
     *
     * @return Adapter 名称列表
     */
    List<String> listAdapters();

    /**
     * 获取所有已注册的 LLM Adapter。
     *
     * @return LLM Adapter 列表
     */
    List<LLMAdapter> listAll();

    /**
     * 清空所有注册。
     */
    void clear();
}

