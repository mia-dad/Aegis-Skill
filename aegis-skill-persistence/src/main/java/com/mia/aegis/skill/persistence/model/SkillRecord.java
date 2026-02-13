package com.mia.aegis.skill.persistence.model;

import com.mia.aegis.skill.dsl.model.Skill;

import java.util.Date;

/**
 * 技能持久化记录。
 *
 * <p>封装 Skill 对象及其持久化元数据，作为存储层的核心实体。
 * 包含原始 Markdown 内容、解析后的 Skill 对象和管理信息。</p>
 *
 * <p>{@code skillId + version} 是唯一标识。</p>
 */
public class SkillRecord {

    private String skillId;
    private String version;
    private String markdownContent;
    private Skill parsedSkill;
    private SkillStatus status;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;
    private String updatedBy;

    public SkillRecord() {
    }

    public SkillRecord(String skillId, String version, String markdownContent) {
        this.skillId = skillId;
        this.version = version;
        this.markdownContent = markdownContent;
        this.status = SkillStatus.DRAFT;
        this.createdAt = new Date();
        this.updatedAt = new Date();
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

    public String getMarkdownContent() {
        return markdownContent;
    }

    public void setMarkdownContent(String markdownContent) {
        this.markdownContent = markdownContent;
    }

    public Skill getParsedSkill() {
        return parsedSkill;
    }

    public void setParsedSkill(Skill parsedSkill) {
        this.parsedSkill = parsedSkill;
    }

    public SkillStatus getStatus() {
        return status;
    }

    public void setStatus(SkillStatus status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * 返回唯一标识键（skillId@version）。
     */
    public String getKey() {
        return skillId + "@" + version;
    }
}
