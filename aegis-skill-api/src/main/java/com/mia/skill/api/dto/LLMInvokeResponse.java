package com.mia.skill.api.dto;

/**
 * 大模型LLM 调用响应。
 */
public class LLMInvokeResponse {

    /** 是否成功 */
    private boolean success;

    /** LLM 响应内容 */
    private String content;

    /** 使用的 Adapter 名称 */
    private String adapter;

    /** 错误信息（如果失败） */
    private String error;

    /** 响应耗时（毫秒） */
    private long durationMs;

    public static LLMInvokeResponse success(String content, String adapter, long durationMs) {
        LLMInvokeResponse resp = new LLMInvokeResponse();
        resp.success = true;
        resp.content = content;
        resp.adapter = adapter;
        resp.durationMs = durationMs;
        return resp;
    }

    public static LLMInvokeResponse error(String error, String adapter) {
        LLMInvokeResponse resp = new LLMInvokeResponse();
        resp.success = false;
        resp.error = error;
        resp.adapter = adapter;
        return resp;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAdapter() {
        return adapter;
    }

    public void setAdapter(String adapter) {
        this.adapter = adapter;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
}