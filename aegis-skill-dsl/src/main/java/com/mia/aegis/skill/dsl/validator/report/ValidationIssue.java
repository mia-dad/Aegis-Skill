package com.mia.aegis.skill.dsl.validator.report;

/**
 * 单条校验问题。
 *
 * <p>不可变类，描述一条校验发现的问题，包含编号、级别、分类、消息、位置和修复建议。</p>
 */
public class ValidationIssue {

    private final String code;
    private final ValidationLevel level;
    private final ValidationCategory category;
    private final String message;
    private final String location;
    private final String suggestion;

    public ValidationIssue(String code, ValidationLevel level, ValidationCategory category,
                           String message, String location, String suggestion) {
        this.code = code;
        this.level = level;
        this.category = category;
        this.message = message;
        this.location = location;
        this.suggestion = suggestion;
    }

    public static ValidationIssue error(String code, ValidationCategory category,
                                        String message, String location, String suggestion) {
        return new ValidationIssue(code, ValidationLevel.ERROR, category, message, location, suggestion);
    }

    public static ValidationIssue warning(String code, ValidationCategory category,
                                          String message, String location, String suggestion) {
        return new ValidationIssue(code, ValidationLevel.WARNING, category, message, location, suggestion);
    }

    public static ValidationIssue suggestion(String code, ValidationCategory category,
                                             String message, String location, String suggestion) {
        return new ValidationIssue(code, ValidationLevel.SUGGESTION, category, message, location, suggestion);
    }

    public String getCode() {
        return code;
    }

    public ValidationLevel getLevel() {
        return level;
    }

    public ValidationCategory getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public String getLocation() {
        return location;
    }

    public String getSuggestion() {
        return suggestion;
    }

    @Override
    public String toString() {
        return "[" + code + "] " + level + " (" + category + ") " + message +
                (location != null ? " @ " + location : "") +
                (suggestion != null ? " -> " + suggestion : "");
    }
}
