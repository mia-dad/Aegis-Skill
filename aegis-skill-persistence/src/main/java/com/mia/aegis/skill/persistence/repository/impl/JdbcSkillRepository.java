package com.mia.aegis.skill.persistence.repository.impl;

import com.mia.aegis.skill.dsl.model.Skill;
import com.mia.aegis.skill.dsl.parser.SkillParser;
import com.mia.aegis.skill.persistence.model.SkillRecord;
import com.mia.aegis.skill.persistence.model.SkillStatus;
import com.mia.aegis.skill.persistence.repository.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 JDBC 的技能仓储实现。
 *
 * <p>使用原生 JDBC 操作 MySQL skill 表，接受外部注入的 {@link DataSource}。</p>
 */
public class JdbcSkillRepository implements SkillRepository {

    private static final Logger logger = LoggerFactory.getLogger(JdbcSkillRepository.class);

    private final DataSource dataSource;
    private final SkillParser skillParser;

    /**
     * @param dataSource  JDBC 数据源
     * @param skillParser Markdown 解析器，用于将存储的 markdown 解析为 Skill 对象
     */
    public JdbcSkillRepository(DataSource dataSource, SkillParser skillParser) {
        this.dataSource = dataSource;
        this.skillParser = skillParser;
    }

    @Override
    public Skill findById(String skillId) {
        if (skillId == null || skillId.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT markdown_content FROM skill "
                + "WHERE skill_id = ? AND status = 'PUBLISHED' "
                + "ORDER BY "
                + "  CAST(SUBSTRING_INDEX(version, '.', 1) AS UNSIGNED) DESC, "
                + "  CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(version, '.', 2), '.', -1) AS UNSIGNED) DESC, "
                + "  CAST(SUBSTRING_INDEX(version, '.', -1) AS UNSIGNED) DESC "
                + "LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, skillId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return parseMarkdown(rs.getString("markdown_content"));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find skill by id: {}", skillId, e);
        }
        return null;
    }

    @Override
    public Skill findById(String skillId, String version) {
        if (skillId == null || skillId.trim().isEmpty()) {
            return null;
        }
        if (version == null || version.trim().isEmpty()) {
            return findById(skillId);
        }
        String sql = "SELECT markdown_content FROM skill WHERE skill_id = ? AND version = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, skillId);
            ps.setString(2, version);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return parseMarkdown(rs.getString("markdown_content"));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find skill: {}@{}", skillId, version, e);
        }
        return null;
    }

    @Override
    public List<Skill> findAll() {
        // 每个 skillId 取 PUBLISHED 状态的最新版本
        String sql = "SELECT s.markdown_content FROM skill s "
                + "INNER JOIN ("
                + "  SELECT skill_id, MAX(version) AS max_version FROM skill "
                + "  WHERE status = 'PUBLISHED' GROUP BY skill_id"
                + ") latest ON s.skill_id = latest.skill_id AND s.version = latest.max_version";

        List<Skill> skills = new ArrayList<Skill>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Skill skill = parseMarkdown(rs.getString("markdown_content"));
                if (skill != null) {
                    skills.add(skill);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find all skills", e);
        }
        return skills;
    }

    @Override
    public List<Skill> findAllVersions(String skillId) {
        List<Skill> result = new ArrayList<Skill>();
        if (skillId == null || skillId.trim().isEmpty()) {
            return result;
        }
        String sql = "SELECT markdown_content FROM skill WHERE skill_id = ? "
                + "ORDER BY "
                + "  CAST(SUBSTRING_INDEX(version, '.', 1) AS UNSIGNED) DESC, "
                + "  CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(version, '.', 2), '.', -1) AS UNSIGNED) DESC, "
                + "  CAST(SUBSTRING_INDEX(version, '.', -1) AS UNSIGNED) DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, skillId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Skill skill = parseMarkdown(rs.getString("markdown_content"));
                    if (skill != null) {
                        result.add(skill);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find all versions for: {}", skillId, e);
        }
        return result;
    }

    @Override
    public SkillRecord findRecord(String skillId, String version) {
        if (skillId == null || version == null) {
            return null;
        }
        String sql = "SELECT id, skill_id, version, description, markdown_content, "
                + "status, created_by, updated_by, created_at, updated_at "
                + "FROM skill WHERE skill_id = ? AND version = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, skillId);
            ps.setString(2, version);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRecord(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find record: {}@{}", skillId, version, e);
        }
        return null;
    }

    @Override
    public List<SkillRecord> findAllRecords() {
        String sql = "SELECT id, skill_id, version, description, markdown_content, "
                + "status, created_by, updated_by, created_at, updated_at "
                + "FROM skill ORDER BY skill_id, version DESC";

        List<SkillRecord> records = new ArrayList<SkillRecord>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                records.add(mapResultSetToRecord(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find all records", e);
        }
        return records;
    }

    @Override
    public SkillRecord save(SkillRecord record) {
        if (record == null || record.getSkillId() == null || record.getVersion() == null) {
            throw new IllegalArgumentException("SkillRecord must have skillId and version");
        }
        if (record.getMarkdownContent() == null || record.getMarkdownContent().trim().isEmpty()) {
            throw new IllegalArgumentException("SkillRecord must have markdownContent");
        }

        // 尝试解析以获取 description
        String description = record.getParsedSkill() != null
                ? record.getParsedSkill().getDescription()
                : extractDescription(record);

        String status = record.getStatus() != null ? record.getStatus().name() : SkillStatus.DRAFT.name();

        // UPSERT: 存在则更新，不存在则插入
        String sql = "INSERT INTO skill (skill_id, version, description, markdown_content, status, created_by, updated_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE "
                + "  description = VALUES(description), "
                + "  markdown_content = VALUES(markdown_content), "
                + "  status = VALUES(status), "
                + "  updated_by = VALUES(updated_by), "
                + "  updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, record.getSkillId());
            ps.setString(2, record.getVersion());
            ps.setString(3, description);
            ps.setString(4, record.getMarkdownContent());
            ps.setString(5, status);
            ps.setString(6, record.getCreatedBy());
            ps.setString(7, record.getUpdatedBy() != null ? record.getUpdatedBy() : record.getCreatedBy());

            ps.executeUpdate();

            record.setUpdatedAt(new Date());

            // 解析并缓存
            Skill skill = parseMarkdown(record.getMarkdownContent());
            record.setParsedSkill(skill);

            logger.info("Saved skill record: {}@{} [{}]", record.getSkillId(), record.getVersion(), status);
            return record;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save skill record: " + record.getKey(), e);
        }
    }

    @Override
    public boolean delete(String skillId, String version) {
        if (skillId == null || version == null) {
            return false;
        }
        String sql = "DELETE FROM skill WHERE skill_id = ? AND version = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, skillId);
            ps.setString(2, version);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                logger.info("Deleted skill: {}@{}", skillId, version);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Failed to delete skill: {}@{}", skillId, version, e);
        }
        return false;
    }

    @Override
    public boolean exists(String skillId, String version) {
        if (skillId == null || version == null) {
            return false;
        }
        String sql = "SELECT 1 FROM skill WHERE skill_id = ? AND version = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, skillId);
            ps.setString(2, version);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to check skill existence: {}@{}", skillId, version, e);
        }
        return false;
    }

    // ===== 内部方法 =====

    private SkillRecord mapResultSetToRecord(ResultSet rs) throws SQLException {
        SkillRecord record = new SkillRecord();
        record.setSkillId(rs.getString("skill_id"));
        record.setVersion(rs.getString("version"));
        record.setMarkdownContent(rs.getString("markdown_content"));
        record.setStatus(SkillStatus.valueOf(rs.getString("status")));
        record.setCreatedBy(rs.getString("created_by"));
        record.setUpdatedBy(rs.getString("updated_by"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            record.setCreatedAt(new Date(createdAt.getTime()));
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            record.setUpdatedAt(new Date(updatedAt.getTime()));
        }

        // 解析 Skill 对象
        Skill skill = parseMarkdown(record.getMarkdownContent());
        record.setParsedSkill(skill);

        return record;
    }

    private Skill parseMarkdown(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return null;
        }
        try {
            return skillParser.parse(markdown);
        } catch (Exception e) {
            logger.warn("Failed to parse skill markdown: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 尝试从 markdown 内容中提取 description（不依赖完整解析）。
     */
    private String extractDescription(SkillRecord record) {
        try {
            Skill skill = parseMarkdown(record.getMarkdownContent());
            if (skill != null) {
                return skill.getDescription();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
