package com.mia.aegis.skill.tools;

import com.mia.aegis.skill.i18n.MessageUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认 Tool 注册表实现。
 * 支持 ServiceLoader 自动发现和手动注册。
 */
public class DefaultToolRegistry implements ToolRegistry {

    private final Map<String, ToolProvider> providers = new ConcurrentHashMap<String, ToolProvider>();

    /**
     * 创建空的注册表。
     */
    public DefaultToolRegistry() {
    }

    /**
     * 创建注册表并自动加载 SPI 发现的 Tool Provider。
     *
     * @param loadFromServiceLoader 是否从 ServiceLoader 加载
     */
    public DefaultToolRegistry(boolean loadFromServiceLoader) {
        if (loadFromServiceLoader) {
            loadFromServiceLoader();
        }
    }

    /**
     * 从 ServiceLoader 加载 Tool Provider。
     */
    public void loadFromServiceLoader() {
        ServiceLoader<ToolProvider> loader = ServiceLoader.load(ToolProvider.class);
        for (ToolProvider provider : loader) {
            register(provider);
        }
    }

    @Override
    public void register(ToolProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException(MessageUtil.getMessage("toolregistry.provider.null"));
        }
        String name = provider.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(MessageUtil.getMessage("toolregistry.name.null"));
        }
        providers.put(name, provider);
    }

    @Override
    public boolean unregister(String toolName) {
        if (toolName == null) {
            return false;
        }
        return providers.remove(toolName) != null;
    }

    @Override
    public Optional<ToolProvider> find(String toolName) {
        if (toolName == null) {
            return Optional.empty();
        }
        ToolProvider provider = providers.get(toolName);
        if (provider != null) {
            return Optional.of(provider);
        }
        return Optional.empty();
    }

    @Override
    public boolean contains(String toolName) {
        return toolName != null && providers.containsKey(toolName);
    }

    @Override
    public List<String> listTools() {
        return new ArrayList<String>(providers.keySet());
    }

    @Override
    public List<ToolProvider> listProviders() {
        return new ArrayList<ToolProvider>(providers.values());
    }

    @Override
    public void clear() {
        providers.clear();
    }

    /**
     * 获取已注册的 Tool 数量。
     *
     * @return Tool 数量
     */
    public int size() {
        return providers.size();
    }
}
