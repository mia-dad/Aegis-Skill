package com.mia.skill.api.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * 大模型LLM 调用请求。
 */
public class LLMInvokeRequest {

    /** 提示词 */
    private String prompt;

    /** 使用的 Adapter 名称（可选，默认使用当前默认 Adapter） */
    private String adapter;

    /** 调用选项（如 temperature, max_tokens 等） */
    private Map<String, Object> options = new HashMap<>();

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getAdapter() {
        return adapter;
    }

    public void setAdapter(String adapter) {
        this.adapter = adapter;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options != null ? options : new HashMap<>();
    }
}
