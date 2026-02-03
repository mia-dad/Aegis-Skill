package com.mia.aegis.skill.dsl.model;

import com.mia.aegis.skill.dsl.model.io.InputSchema;
import com.mia.aegis.skill.dsl.model.io.OutputContract;
import com.mia.aegis.skill.i18n.MessageUtil;

import java.util.*;

/**
 * 可执行能力契约。
 *
 * <p>Skill 是 Aegis Runtime 的核心抽象，描述了一个完整的可执行能力，
 * 包括输入、执行步骤和输出契约。</p>
 */
public class Skill {

    private final String id;
    private final String description;
    private final List<String> intents;
    private final InputSchema inputSchema;
    private final List<Step> steps;
    private final OutputContract outputContract;
    private final Map<String, Object> extensions;

    /**
     * 创建 Skill。
     *
     * @param id Skill 唯一标识
     * @param description 描述
     * @param intents 意图标签
     * @param inputSchema 输入 Schema
     * @param steps 执行步骤
     * @param outputContract 输出契约
     * @param extensions 扩展字段
     */
    public Skill(String id, String description, List<String> intents,
                 InputSchema inputSchema, List<Step> steps,
                 OutputContract outputContract, Map<String, Object> extensions) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(MessageUtil.getMessage("skill.id.null"));
        }
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException(MessageUtil.getMessage("skill.steps.empty"));
        }

        this.id = id.trim();
        this.description = description;
        this.intents = intents != null
                ? Collections.unmodifiableList(new ArrayList<String>(intents))
                : Collections.<String>emptyList();
        this.inputSchema = inputSchema != null ? inputSchema : InputSchema.empty();
        this.steps = Collections.unmodifiableList(new ArrayList<Step>(steps));
        this.outputContract = outputContract != null ? outputContract : OutputContract.empty();
        this.extensions = extensions != null
                ? Collections.unmodifiableMap(new LinkedHashMap<String, Object>(extensions))
                : Collections.<String, Object>emptyMap();
    }

    /**
     * 获取 Skill ID。
     *
     * @return Skill ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取描述。
     *
     * @return 描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取意图标签列表。
     *
     * @return 意图标签
     */
    public List<String> getIntents() {
        return intents;
    }

    /**
     * 获取输入 Schema。
     *
     * @return 输入 Schema
     */
    public InputSchema getInputSchema() {
        return inputSchema;
    }

    /**
     * 获取执行步骤列表。
     *
     * @return 步骤列表（有序）
     */
    public List<Step> getSteps() {
        return steps;
    }

    /**
     * 获取输出契约。
     *
     * @return 输出契约
     */
    public OutputContract getOutputContract() {
        return outputContract;
    }

    /**
     * 获取扩展字段。
     *
     * @return 扩展字段映射
     */
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    /**
     * 根据名称查找 Step。
     *
     * @param name Step 名称
     * @return 找到的 Step，不存在返回 null
     */
    public Step getStep(String name) {
        for (Step step : steps) {
            if (step.getName().equals(name)) {
                return step;
            }
        }
        return null;
    }

    /**
     * 获取步骤数量。
     *
     * @return 步骤数量
     */
    public int getStepCount() {
        return steps.size();
    }

    /**
     * 获取扩展字段值。
     *
     * @param key 扩展字段键（不含 x-aegis- 前缀）
     * @return 扩展字段值
     */
    public Object getExtension(String key) {
        return extensions.get(key);
    }

    /**
     * 检查是否有扩展字段。
     *
     * @param key 扩展字段键
     * @return 是否存在
     */
    public boolean hasExtension(String key) {
        return extensions.containsKey(key);
    }

    @Override
    public String toString() {
        return "Skill{" +
                "id='" + id + '\'' +
                ", steps=" + steps.size() +
                ", intents=" + intents +
                '}';
    }
}

