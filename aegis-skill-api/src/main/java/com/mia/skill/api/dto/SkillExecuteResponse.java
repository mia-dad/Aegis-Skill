package com.mia.skill.api.dto;

import java.util.Map;

/**
 * Skill 执行响应。
 */
public class SkillExecuteResponse {

    /** 执行状态: COMPLETED, FAILED, WAITING_FOR_INPUT */
    private String status;

    /** 是否成功 */
    private boolean success;

    /** 是否等待用户输入 */
    private boolean awaiting;

    /** Skill ID */
    private String skillId;

    /** Skill 版本 */
    private String version;

    /** 执行ID（用于 await/resume） */
    private String executionId;

    /** 执行输出 */
    private Object output;

    /** 等待输入时的提示消息 */
    private String awaitMessage;

    /** 等待输入时的 Schema */
    private Map<String, Object> awaitSchema;

    /** 错误信息（如果失败） */
    private String error;

    /** 执行耗时（毫秒） */
    private long durationMs;

    public static SkillExecuteResponse success(String skillId, String version, Object output, long durationMs) {
        SkillExecuteResponse resp = new SkillExecuteResponse();
        resp.status = "COMPLETED";
        resp.success = true;
        resp.awaiting = false;
        resp.skillId = skillId;
        resp.version = version;
        resp.output = output;
        resp.durationMs = durationMs;
        return resp;
    }

    public static SkillExecuteResponse error(String skillId, String version, String error) {
        SkillExecuteResponse resp = new SkillExecuteResponse();
        resp.status = "FAILED";
        resp.success = false;
        resp.awaiting = false;
        resp.skillId = skillId;
        resp.version = version;
        resp.error = error;
        return resp;
    }

    public static SkillExecuteResponse waitingForInput(String skillId, String version, String executionId,
                                                       String awaitMessage, Map<String, Object> awaitSchema,
                                                       long durationMs) {
        SkillExecuteResponse resp = new SkillExecuteResponse();
        resp.status = "WAITING_FOR_INPUT";
        resp.success = false;
        resp.awaiting = true;
        resp.skillId = skillId;
        resp.version = version;
        resp.executionId = executionId;
        resp.awaitMessage = awaitMessage;
        resp.awaitSchema = awaitSchema;
        resp.durationMs = durationMs;
        return resp;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isAwaiting() {
        return awaiting;
    }

    public void setAwaiting(boolean awaiting) {
        this.awaiting = awaiting;
    }

    public String getSkillId() {
        return skillId;
    }

    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public Object getOutput() {
        return output;
    }

    public void setOutput(Object output) {
        this.output = output;
    }

    public String getAwaitMessage() {
        return awaitMessage;
    }

    public void setAwaitMessage(String awaitMessage) {
        this.awaitMessage = awaitMessage;
    }

    public Map<String, Object> getAwaitSchema() {
        return awaitSchema;
    }

    public void setAwaitSchema(Map<String, Object> awaitSchema) {
        this.awaitSchema = awaitSchema;
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
