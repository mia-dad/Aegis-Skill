package com.mia.skill.api.dto;

/**
 * 综合校验请求 DTO。
 */
public class ValidateSkillRequest {

    private String markdown;

    public ValidateSkillRequest() {
    }

    public ValidateSkillRequest(String markdown) {
        this.markdown = markdown;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }
}
