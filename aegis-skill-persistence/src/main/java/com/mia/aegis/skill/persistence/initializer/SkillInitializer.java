package com.mia.aegis.skill.persistence.initializer;

import com.mia.aegis.skill.dsl.model.Skill;
import com.mia.aegis.skill.dsl.parser.SkillParser;
import com.mia.aegis.skill.persistence.model.SkillRecord;
import com.mia.aegis.skill.persistence.model.SkillStatus;
import com.mia.aegis.skill.persistence.repository.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 技能初始化器。
 *
 * <p>在应用启动时扫描 classpath 下的 .md 技能文件，
 * 如果对应的 skillId 在数据库中不存在，则导入入库。</p>
 *
 * <p>已存在的技能不会被覆盖，保证数据库中人工修改过的技能不被重置。</p>
 */
public class SkillInitializer {

    private static final Logger logger = LoggerFactory.getLogger(SkillInitializer.class);

    private final SkillRepository skillRepository;
    private final SkillParser skillParser;

    public SkillInitializer(SkillRepository skillRepository, SkillParser skillParser) {
        this.skillRepository = skillRepository;
        this.skillParser = skillParser;
    }

    /**
     * 从 classpath 资源列表导入技能。
     *
     * <p>对每个 .md 资源：解析 skillId → 检查数据库是否存在 → 不存在则入库。</p>
     *
     * @param resources classpath 资源 InputStream 列表（包含资源名称用于日志）
     */
    public void importFromResources(List<ResourceEntry> resources) {
        int imported = 0;
        int skipped = 0;
        int failed = 0;

        for (ResourceEntry entry : resources) {
            try {
                String markdown = readStream(entry.getInputStream());
                Skill skill = skillParser.parse(markdown);

                if (skill == null || skill.getId() == null) {
                    logger.warn("Skipping resource '{}': failed to parse or missing skill id", entry.getName());
                    failed++;
                    continue;
                }

                // 检查该 skillId + version 是否已存在
                if (skillRepository.exists(skill.getId(), skill.getVersion())) {
                    logger.debug("Skill '{}@{}' already exists in database, skipping", skill.getId(), skill.getVersion());
                    skipped++;
                    continue;
                }

                // 入库
                SkillRecord record = new SkillRecord(skill.getId(), skill.getVersion(), markdown);
                record.setParsedSkill(skill);
                record.setStatus(SkillStatus.PUBLISHED);
                record.setCreatedBy("system");
                skillRepository.save(record);

                logger.info("Imported skill from classpath: {}@{} ({})", skill.getId(), skill.getVersion(), entry.getName());
                imported++;
            } catch (Exception e) {
                logger.error("Failed to import skill from '{}': {}", entry.getName(), e.getMessage());
                failed++;
            }
        }

        logger.info("Skill import completed: {} imported, {} skipped (already exist), {} failed",
                imported, skipped, failed);
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * classpath 资源条目。
     */
    public static class ResourceEntry {
        private final String name;
        private final InputStream inputStream;

        public ResourceEntry(String name, InputStream inputStream) {
            this.name = name;
            this.inputStream = inputStream;
        }

        public String getName() {
            return name;
        }

        public InputStream getInputStream() {
            return inputStream;
        }
    }
}
