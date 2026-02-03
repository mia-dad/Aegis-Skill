package com.mia.aegis.skill.tools;

import java.util.List;
import java.util.Optional;

/**
 * Tool 注册表接口。
 * 管理 ToolProvider 的注册和查找。
 */
public interface ToolRegistry {

    /**
     * 注册 Tool Provider。
     *
     * @param provider Tool Provider 实例
     */
    void register(ToolProvider provider);

    /**
     * 取消注册 Tool Provider。
     *
     * @param toolName Tool 名称
     * @return 是否成功取消注册
     */
    boolean unregister(String toolName);

    /**
     * 查找 Tool Provider。
     *
     * @param toolName Tool 名称
     * @return Tool Provider（如果存在）
     */
    Optional<ToolProvider> find(String toolName);

    /**
     * 检查 Tool 是否已注册。
     *
     * @param toolName Tool 名称
     * @return 是否已注册
     */
    boolean contains(String toolName);

    /**
     * 获取所有已注册的 Tool 名称。
     *
     * @return Tool 名称列表
     */
    List<String> listTools();

    /**
     * 获取所有已注册的 Tool Provider。
     *
     * @return Tool Provider 列表
     */
    List<ToolProvider> listProviders();

    /**
     * 清空所有注册。
     */
    void clear();
}

