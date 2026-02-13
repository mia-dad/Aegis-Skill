package com.mia.aegis.skill.persistence.model;

/**
 * 技能状态枚举。
 */
public enum SkillStatus {

    /** 草稿状态，编辑中，不可被执行引擎加载 */
    DRAFT,

    /** 已发布，可被执行引擎加载和执行 */
    PUBLISHED,

    /** 已归档，不再使用但保留记录 */
    ARCHIVED
}
