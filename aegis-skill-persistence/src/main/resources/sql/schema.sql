-- ============================================
-- Aegis Skill Persistence - 建表语句
-- 数据库: skills (MySQL)
-- ============================================

CREATE TABLE IF NOT EXISTS `skill` (
    `id`               BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
    `skill_id`         VARCHAR(128)    NOT NULL                 COMMENT '技能 ID（如 order_query）',
    `version`          VARCHAR(32)     NOT NULL                 COMMENT '版本号（如 1.0.0）',
    `description`      VARCHAR(512)    DEFAULT NULL             COMMENT '技能描述',
    `markdown_content` MEDIUMTEXT      NOT NULL                 COMMENT '原始 Markdown 内容',
    `status`           VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT / PUBLISHED / ARCHIVED',
    `created_by`       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    `updated_by`       VARCHAR(64)     DEFAULT NULL             COMMENT '最后修改人',
    `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_skill_version` (`skill_id`, `version`),
    KEY `idx_skill_id` (`skill_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技能持久化表';
