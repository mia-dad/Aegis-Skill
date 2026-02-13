package com.mia.aegis.skill.persistence.repository.impl;

import com.mia.aegis.skill.dsl.model.Skill;
import com.mia.aegis.skill.dsl.parser.SkillParser;
import com.mia.aegis.skill.persistence.model.SkillRecord;
import com.mia.aegis.skill.persistence.model.SkillStatus;
import com.mia.aegis.skill.persistence.repository.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 基于文件系统的技能仓储实现。
 *
 * <p>从指定目录加载 .md 文件，支持读写操作。
 * 文件名约定：{skillId}.md 或 {skillId}_{version}.md。</p>
 */
public class FileSkillRepository implements SkillRepository {

    private static final Logger logger = LoggerFactory.getLogger(FileSkillRepository.class);

    private final Path skillsDirectory;
    private final SkillParser skillParser;

    /**
     * @param skillsDirectory 技能文件存储目录
     * @param skillParser     Markdown 解析器
     */
    public FileSkillRepository(Path skillsDirectory, SkillParser skillParser) {
        this.skillsDirectory = skillsDirectory;
        this.skillParser = skillParser;
    }

    @Override
    public Skill findById(String skillId) {
        if (skillId == null || skillId.trim().isEmpty()) {
            return null;
        }
        Skill latest = null;
        for (Skill skill : loadAll()) {
            if (skillId.equals(skill.getId())) {
                if (latest == null || compareVersions(skill.getVersion(), latest.getVersion()) > 0) {
                    latest = skill;
                }
            }
        }
        return latest;
    }

    @Override
    public Skill findById(String skillId, String version) {
        if (skillId == null || skillId.trim().isEmpty()) {
            return null;
        }
        if (version == null || version.trim().isEmpty()) {
            return findById(skillId);
        }
        for (Skill skill : loadAll()) {
            if (skillId.equals(skill.getId()) && version.equals(skill.getVersion())) {
                return skill;
            }
        }
        return null;
    }

    @Override
    public List<Skill> findAll() {
        // 每个 skillId 返回最新版本
        Map<String, Skill> latestMap = new LinkedHashMap<String, Skill>();
        for (Skill skill : loadAll()) {
            String id = skill.getId();
            Skill existing = latestMap.get(id);
            if (existing == null || compareVersions(skill.getVersion(), existing.getVersion()) > 0) {
                latestMap.put(id, skill);
            }
        }
        return new ArrayList<Skill>(latestMap.values());
    }

    @Override
    public List<Skill> findAllVersions(String skillId) {
        List<Skill> result = new ArrayList<Skill>();
        if (skillId == null || skillId.trim().isEmpty()) {
            return result;
        }
        for (Skill skill : loadAll()) {
            if (skillId.equals(skill.getId())) {
                result.add(skill);
            }
        }
        result.sort(new Comparator<Skill>() {
            @Override
            public int compare(Skill a, Skill b) {
                return compareVersions(b.getVersion(), a.getVersion());
            }
        });
        return result;
    }

    @Override
    public SkillRecord findRecord(String skillId, String version) {
        if (skillId == null || version == null) {
            return null;
        }
        Path file = resolveFile(skillId, version);
        if (file == null || !Files.exists(file)) {
            return null;
        }
        try {
            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            Skill skill = skillParser.parse(content);
            SkillRecord record = new SkillRecord(skillId, version, content);
            record.setParsedSkill(skill);
            record.setStatus(SkillStatus.PUBLISHED);
            return record;
        } catch (Exception e) {
            logger.error("Failed to load skill record: {}@{} - {}", skillId, version, e.getMessage());
            return null;
        }
    }

    @Override
    public List<SkillRecord> findAllRecords() {
        List<SkillRecord> records = new ArrayList<SkillRecord>();
        for (Path file : listSkillFiles()) {
            try {
                String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                Skill skill = skillParser.parse(content);
                SkillRecord record = new SkillRecord(skill.getId(), skill.getVersion(), content);
                record.setParsedSkill(skill);
                record.setStatus(SkillStatus.PUBLISHED);
                records.add(record);
            } catch (Exception e) {
                logger.warn("Failed to load skill from {}: {}", file.getFileName(), e.getMessage());
            }
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

        String fileName = record.getSkillId() + ".md";
        Path file = skillsDirectory.resolve(fileName);

        try {
            if (!Files.exists(skillsDirectory)) {
                Files.createDirectories(skillsDirectory);
            }
            Files.write(file, record.getMarkdownContent().getBytes(StandardCharsets.UTF_8));
            record.setUpdatedAt(new Date());

            // 解析并缓存
            try {
                Skill skill = skillParser.parse(record.getMarkdownContent());
                record.setParsedSkill(skill);
            } catch (Exception e) {
                logger.warn("Saved skill file but parse failed: {}", e.getMessage());
            }

            logger.info("Saved skill file: {}", file);
            return record;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save skill file: " + file, e);
        }
    }

    @Override
    public boolean delete(String skillId, String version) {
        Path file = resolveFile(skillId, version);
        if (file != null && Files.exists(file)) {
            try {
                Files.delete(file);
                logger.info("Deleted skill file: {}", file);
                return true;
            } catch (IOException e) {
                logger.error("Failed to delete skill file: {}", file, e);
            }
        }
        return false;
    }

    @Override
    public boolean exists(String skillId, String version) {
        return findById(skillId, version) != null;
    }

    // ===== 内部方法 =====

    private List<Skill> loadAll() {
        List<Skill> skills = new ArrayList<Skill>();
        for (Path file : listSkillFiles()) {
            try {
                String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                Skill skill = skillParser.parse(content);
                skills.add(skill);
            } catch (Exception e) {
                logger.warn("Failed to load skill from {}: {}", file.getFileName(), e.getMessage());
            }
        }
        return skills;
    }

    private List<Path> listSkillFiles() {
        List<Path> files = new ArrayList<Path>();
        if (!Files.exists(skillsDirectory) || !Files.isDirectory(skillsDirectory)) {
            return files;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(skillsDirectory, "*.md")) {
            for (Path entry : stream) {
                files.add(entry);
            }
        } catch (IOException e) {
            logger.error("Failed to list skill files in {}: {}", skillsDirectory, e.getMessage());
        }
        return files;
    }

    /**
     * 根据 skillId 和 version 查找对应的文件。
     * 遍历目录中所有 .md 文件，解析后匹配。
     */
    private Path resolveFile(String skillId, String version) {
        for (Path file : listSkillFiles()) {
            try {
                String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                Skill skill = skillParser.parse(content);
                if (skillId.equals(skill.getId()) && version.equals(skill.getVersion())) {
                    return file;
                }
            } catch (Exception e) {
                // skip
            }
        }
        return null;
    }

    private int compareVersions(String v1, String v2) {
        if (v1 == null || v1.isEmpty()) v1 = "0.0.0";
        if (v2 == null || v2.isEmpty()) v2 = "0.0.0";

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLen = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLen; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            if (num1 != num2) {
                return num1 - num2;
            }
        }
        return 0;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
