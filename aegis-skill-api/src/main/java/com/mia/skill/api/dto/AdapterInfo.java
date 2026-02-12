package com.mia.skill.api.dto;

/**
 * Adapter 信息。to 页面数据
 */
public class AdapterInfo {

    /** Adapter 名称 */
    private String name;

    /** 支持的模型 */
    private String[] supportedModels;

    /** 是否可用 */
    private boolean available;

    /** 是否为默认 Adapter */
    private boolean isDefault;

    public AdapterInfo(String name, String[] supportedModels, boolean available, boolean isDefault) {
        this.name = name;
        this.supportedModels = supportedModels;
        this.available = available;
        this.isDefault = isDefault;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String[] getSupportedModels() {
        return supportedModels;
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isDefault() {
        return isDefault;
    }
}
