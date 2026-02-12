package com.mia.skill.api.dto;


import java.util.HashMap;
import java.util.Map;

/**
 * Skill 执行请求（统一模型）。
 *
 * <p>支持三种使用场景：</p>
 * <ul>
 *   <li>执行 Skill（通过 Markdown 定义）</li>
 *   <li>执行 Skill（通过预定义 ID）</li>
 *   <li>恢复暂停的 Skill 执行</li>
 * </ul>
 *
 * <h3>场景1：执行 Skill（Markdown）</h3>
 * <pre>{@code
 * POST /api/skill/execute
 * {
 *   "skillMarkdown": "# skill: my_skill\n...",
 *   "inputs": { "param1": "value1" },
 *   "adapter": "dashscope"  // 可选
 * }
 * }</pre>
 *
 * <h3>场景2：执行 Skill（ID）</h3>
 * <pre>{@code
 * POST /api/skill/execute
 * {
 *   "skillId": "financial_analysis",
 *   "inputs": { "param1": "value1" },
 *   "adapter": "dashscope"  // 可选
 * }
 * }</pre>
 *
 * <h3>场景3：恢复执行</h3>
 * <pre>{@code
 * POST /api/skill/resume
 * {
 *   "executionId": "exec-550e8400-e29b-41d4-a716-446655440000",
 *   "skillId": "financial_analysis",
 *   "inputs": { "confirmed": true }
 * }
 * }</pre>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>executionId - 执行ID（仅 resume 时必需，execute 时忽略）</li>
 *   <li>skillId - Skill ID（优先级高于 skillMarkdown）</li>
 *   <li>skillMarkdown - Skill Markdown 定义（作为 skillId 的备选）</li>
 *   <li>inputs - 输入参数</li>
 *   <li>adapter - LLM Adapter 名称（可选）</li>
 * </ul>
 */
public class SkillExecuteRequest {

    /**
     * 执行ID（仅 resume 时使用）。
     *
     * <p>当非 null 时表示恢复已暂停的执行。</p>
     */
    private String executionId;

    /**
     * Skill ID（优先级高于 skillMarkdown）。
     *
     * <p>用于从 SkillLoader 加载预定义的 Skill。</p>
     */
    private String skillId;

    /** Skill 定义（Markdown 格式，skillId 的备选） */
    private String skillMarkdown;

    /** 输入参数（execute 或 resume 时使用） */
    private Map<String, Object> inputs = new HashMap<>();

    /**
     * Skill 版本（可选，仅在使用 skillId 方式时生效）。
     *
     * <p>提供 version → 按 skillId+version 精确查找；
     * 未提供 → 使用该 skillId 的最大版本。</p>
     */
    private String version;

    /** 使用的 LLM Adapter 名称（可选） */
    private String adapter;

    /**
     * 获取执行ID。
     *
     * @return executionId
     */
    public String getExecutionId() {
        return executionId;
    }

    /**
     * 设置执行ID。
     *
     * @param executionId 执行ID
     */
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    /**
     * 获取 Skill ID。
     *
     * @return Skill ID
     */
    public String getSkillId() {
        return skillId;
    }

    /**
     * 设置 Skill ID。
     *
     * @param skillId Skill ID
     */
    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }

    public String getSkillMarkdown() {
        return skillMarkdown;
    }

    public void setSkillMarkdown(String skillMarkdown) {
        this.skillMarkdown = skillMarkdown;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs != null ? inputs : new HashMap<>();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAdapter() {
        return adapter;
    }

    public void setAdapter(String adapter) {
        this.adapter = adapter;
    }
}
