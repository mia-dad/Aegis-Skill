package com.mia.aegis.skill.dsl.validator.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 技能校验报告。
 *
 * <p>不可变类，汇总校验结果，包含技能摘要、所有校验问题和耗时信息。</p>
 */
public class SkillValidationReport {

    private final boolean valid;
    private final SkillSummary summary;
    private final List<ValidationIssue> issues;
    private final long validationTimeMs;

    private SkillValidationReport(boolean valid, SkillSummary summary,
                                  List<ValidationIssue> issues, long validationTimeMs) {
        this.valid = valid;
        this.summary = summary;
        this.issues = issues != null
                ? Collections.unmodifiableList(new ArrayList<ValidationIssue>(issues))
                : Collections.<ValidationIssue>emptyList();
        this.validationTimeMs = validationTimeMs;
    }

    public static SkillValidationReport success(SkillSummary summary,
                                                List<ValidationIssue> issues,
                                                long timeMs) {
        return new SkillValidationReport(true, summary, issues, timeMs);
    }

    public static SkillValidationReport failure(SkillSummary summary,
                                                List<ValidationIssue> issues,
                                                long timeMs) {
        return new SkillValidationReport(false, summary, issues, timeMs);
    }

    public boolean isValid() {
        return valid;
    }

    public SkillSummary getSummary() {
        return summary;
    }

    public List<ValidationIssue> getIssues() {
        return issues;
    }

    public long getValidationTimeMs() {
        return validationTimeMs;
    }

    public List<ValidationIssue> getErrors() {
        return filterByLevel(ValidationLevel.ERROR);
    }

    public List<ValidationIssue> getWarnings() {
        return filterByLevel(ValidationLevel.WARNING);
    }

    public List<ValidationIssue> getSuggestions() {
        return filterByLevel(ValidationLevel.SUGGESTION);
    }

    public List<ValidationIssue> getIssuesByCategory(ValidationCategory category) {
        List<ValidationIssue> result = new ArrayList<ValidationIssue>();
        for (ValidationIssue issue : issues) {
            if (issue.getCategory() == category) {
                result.add(issue);
            }
        }
        return result;
    }

    public int getErrorCount() {
        return getErrors().size();
    }

    public int getWarningCount() {
        return getWarnings().size();
    }

    public int getSuggestionCount() {
        return getSuggestions().size();
    }

    private List<ValidationIssue> filterByLevel(ValidationLevel level) {
        List<ValidationIssue> result = new ArrayList<ValidationIssue>();
        for (ValidationIssue issue : issues) {
            if (issue.getLevel() == level) {
                result.add(issue);
            }
        }
        return result;
    }

    /**
     * 生成 LLM 友好的文本输出。
     */
    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Skill Validation Report ===\n");
        sb.append("Status: ").append(valid ? "VALID" : "INVALID").append("\n");
        sb.append("Time: ").append(validationTimeMs).append("ms\n");

        if (summary != null) {
            sb.append("\n--- Summary ---\n");
            sb.append("Skill ID: ").append(summary.getSkillId()).append("\n");
            sb.append("Version: ").append(summary.getVersion()).append("\n");
            sb.append("Steps: ").append(summary.getStepCount())
              .append(" ").append(summary.getStepTypes()).append("\n");
            sb.append("Input fields: ").append(summary.getInputFieldCount()).append("\n");
            sb.append("Output fields: ").append(summary.getOutputFieldCount()).append("\n");
        }

        if (!issues.isEmpty()) {
            sb.append("\n--- Issues (")
              .append(getErrorCount()).append(" errors, ")
              .append(getWarningCount()).append(" warnings, ")
              .append(getSuggestionCount()).append(" suggestions) ---\n");

            for (ValidationIssue issue : issues) {
                sb.append(formatIssue(issue)).append("\n");
            }
        } else {
            sb.append("\nNo issues found.\n");
        }

        return sb.toString();
    }

    private String formatIssue(ValidationIssue issue) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(issue.getCode()).append("] ");
        sb.append(issue.getLevel()).append(" ");
        sb.append(issue.getMessage());
        if (issue.getLocation() != null) {
            sb.append(" (at ").append(issue.getLocation()).append(")");
        }
        if (issue.getSuggestion() != null) {
            sb.append("\n  -> Fix: ").append(issue.getSuggestion());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "SkillValidationReport{" +
                "valid=" + valid +
                ", errors=" + getErrorCount() +
                ", warnings=" + getWarningCount() +
                ", suggestions=" + getSuggestionCount() +
                ", timeMs=" + validationTimeMs +
                '}';
    }
}
