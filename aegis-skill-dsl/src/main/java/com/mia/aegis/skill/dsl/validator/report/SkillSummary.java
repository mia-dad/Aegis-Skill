package com.mia.aegis.skill.dsl.validator.report;

import java.util.Collections;
import java.util.List;

/**
 * 技能摘要信息。
 *
 * <p>帮助 LLM 快速理解技能结构。</p>
 */
public class SkillSummary {

    private final String skillId;
    private final String version;
    private final String description;
    private final int stepCount;
    private final List<String> stepTypes;
    private final int inputFieldCount;
    private final int outputFieldCount;
    private final boolean hasConditionalSteps;
    private final boolean hasAwaitSteps;

    public SkillSummary(String skillId, String version, String description,
                        int stepCount, List<String> stepTypes,
                        int inputFieldCount, int outputFieldCount,
                        boolean hasConditionalSteps, boolean hasAwaitSteps) {
        this.skillId = skillId;
        this.version = version;
        this.description = description;
        this.stepCount = stepCount;
        this.stepTypes = stepTypes != null
                ? Collections.unmodifiableList(stepTypes)
                : Collections.<String>emptyList();
        this.inputFieldCount = inputFieldCount;
        this.outputFieldCount = outputFieldCount;
        this.hasConditionalSteps = hasConditionalSteps;
        this.hasAwaitSteps = hasAwaitSteps;
    }

    public String getSkillId() {
        return skillId;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public int getStepCount() {
        return stepCount;
    }

    public List<String> getStepTypes() {
        return stepTypes;
    }

    public int getInputFieldCount() {
        return inputFieldCount;
    }

    public int getOutputFieldCount() {
        return outputFieldCount;
    }

    public boolean isHasConditionalSteps() {
        return hasConditionalSteps;
    }

    public boolean isHasAwaitSteps() {
        return hasAwaitSteps;
    }

    @Override
    public String toString() {
        return "SkillSummary{" +
                "skillId='" + skillId + '\'' +
                ", version='" + version + '\'' +
                ", stepCount=" + stepCount +
                ", stepTypes=" + stepTypes +
                ", inputFieldCount=" + inputFieldCount +
                ", outputFieldCount=" + outputFieldCount +
                '}';
    }
}
