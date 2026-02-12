package com.mia.aegis.skill.llm;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认 LLM Adapter 注册表实现。
 * 支持 ServiceLoader 自动发现和手动注册。
 */
public class DefaultLLMAdapterRegistry implements LLMAdapterRegistry {

    private final Map<String, LLMAdapter> adapters = new ConcurrentHashMap<String, LLMAdapter>();
    private volatile String defaultAdapterName;

    /**
     * 创建空的注册表。
     */
    public DefaultLLMAdapterRegistry() {
    }

    /**
     * 创建注册表并自动加载 SPI 发现的 LLM Adapter。
     *
     * @param loadFromServiceLoader 是否从 ServiceLoader 加载
     */
    public DefaultLLMAdapterRegistry(boolean loadFromServiceLoader) {
        if (loadFromServiceLoader) {
            loadFromServiceLoader();
        }
    }

    /**
     * 从 ServiceLoader 加载 LLM Adapter。
     */
    public void loadFromServiceLoader() {
        ServiceLoader<LLMAdapter> loader = ServiceLoader.load(LLMAdapter.class);
        for (LLMAdapter adapter : loader) {
            register(adapter);
        }
    }

    @Override
    public void register(LLMAdapter adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("LLMAdapter cannot be null");
        }
        String name = adapter.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Adapter name cannot be null or empty");
        }
        adapters.put(name, adapter);

        // 第一个注册的 Adapter 自动成为默认
        if (defaultAdapterName == null) {
            defaultAdapterName = name;
        }
    }

    @Override
    public boolean unregister(String name) {
        if (name == null) {
            return false;
        }
        boolean removed = adapters.remove(name) != null;
        if (removed && name.equals(defaultAdapterName)) {
            // 如果移除的是默认 Adapter，重新选择默认
            defaultAdapterName = adapters.isEmpty() ? null : adapters.keySet().iterator().next();
        }
        return removed;
    }

    @Override
    public Optional<LLMAdapter> find(String name) {
        if (name == null) {
            return Optional.empty();
        }
        LLMAdapter adapter = adapters.get(name);
        if (adapter != null) {
            return Optional.of(adapter);
        }
        return Optional.empty();
    }

    @Override
    public Optional<LLMAdapter> getDefault() {
        if (defaultAdapterName == null) {
            return Optional.empty();
        }
        return find(defaultAdapterName);
    }

    @Override
    public boolean setDefault(String name) {
        if (name == null || !adapters.containsKey(name)) {
            return false;
        }
        defaultAdapterName = name;
        return true;
    }

    @Override
    public boolean contains(String name) {
        return name != null && adapters.containsKey(name);
    }

    @Override
    public List<String> listAdapters() {
        return new ArrayList<String>(adapters.keySet());
    }

    @Override
    public List<LLMAdapter> listAll() {
        return new ArrayList<LLMAdapter>(adapters.values());
    }

    @Override
    public void clear() {
        adapters.clear();
        defaultAdapterName = null;
    }

    /**
     * 获取已注册的 Adapter 数量。
     *
     * @return Adapter 数量
     */
    public int size() {
        return adapters.size();
    }

    /**
     * 获取默认 Adapter 名称。
     *
     * @return 默认 Adapter 名称
     */
    public String getDefaultName() {
        return defaultAdapterName;
    }
}
