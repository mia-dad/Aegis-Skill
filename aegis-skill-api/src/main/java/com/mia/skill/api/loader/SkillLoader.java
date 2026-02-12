package com.mia.skill.api.loader;

import com.mia.aegis.skill.dsl.model.Skill;
import com.mia.aegis.skill.dsl.parser.SkillParser;
import com.mia.aegis.skill.exception.SkillParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Skill 文件加载器。
 *
 * <p>从 classpath 的 skills 目录自动加载所有 .md 文件并解析为 Skill 对象。</p>
 *
 * <p>核心设计原则：{@code skillId + version} 是唯一标识。同一个 skillId 可能存在多个版本。</p>
 */
@Component
public class SkillLoader {

    private static final Logger logger = LoggerFactory.getLogger(SkillLoader.class);
    private static final String SKILLS_PATTERN = "classpath*:skills/*.md";

    private final SkillParser skillParser;

    public SkillLoader(SkillParser skillParser) {
        this.skillParser = skillParser;
    }

    /**
     * 根据 Skill ID 查找版本号最大的 Skill。
     *
     * @param skillId Skill 唯一标识符
     * @return 该 skillId 下版本号最大的 Skill，不存在返回 null
     */
    public Skill findById(String skillId) {
        if (skillId == null || skillId.trim().isEmpty()) {
            return null;
        }
        Skill latest = null;
        for (Skill skill : loadAllSkills()) {
            if (skillId.equals(skill.getId())) {
                if (latest == null || compareVersions(skill.getVersion(), latest.getVersion()) > 0) {
                    latest = skill;
                }
            }
        }
        return latest;
    }

    /**
     * 根据 Skill ID 和版本号精确查找 Skill。
     *
     * @param skillId Skill 唯一标识符
     * @param version 版本号
     * @return 精确匹配的 Skill，不存在返回 null
     */
    public Skill findById(String skillId, String version) {
        if (skillId == null || skillId.trim().isEmpty()) {
            return null;
        }
        if (version == null || version.trim().isEmpty()) {
            return findById(skillId);
        }
        for (Skill skill : loadAllSkills()) {
            if (skillId.equals(skill.getId()) && version.equals(skill.getVersion())) {
                return skill;
            }
        }
        return null;
    }

    /**
     * 返回指定 skillId 的所有版本（按版本号降序排列）。
     *
     * @param skillId Skill 唯一标识符
     * @return 该 skillId 的所有版本列表，按版本号从高到低排序
     */
    public List<Skill> findAllVersions(String skillId) {
        List<Skill> result = new ArrayList<Skill>();
        if (skillId == null || skillId.trim().isEmpty()) {
            return result;
        }
        for (Skill skill : loadAllSkills()) {
            if (skillId.equals(skill.getId())) {
                result.add(skill);
            }
        }
        // 按版本号降序排列
        result.sort((a, b) -> compareVersions(b.getVersion(), a.getVersion()));
        return result;
    }

    /**
     * 加载所有预制 Skill。
     *
     * @return 解析成功的 Skill 列表
     */
    public List<Skill> loadAllSkills() {
        List<Skill> skills = new ArrayList<Skill>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources = resolver.getResources(SKILLS_PATTERN);
            logger.info("Found {} skill files in classpath:skills/", resources.length);

            for (Resource resource : resources) {
                try {
                    Skill skill = loadSkill(resource);
                    if (skill != null) {
                        skills.add(skill);
                        logger.info("Loaded skill: {} (id={}, version={}, intents={})",
                                resource.getFilename(),
                                skill.getId(),
                                skill.getVersion(),
                                skill.getIntents());
                    }
                } catch (Exception e) {
                    logger.error("Failed to load skill from {}: {}",
                            resource.getFilename(), e.getMessage());
                }
            }

        } catch (IOException e) {
            logger.warn("No skill files found in classpath:skills/ - {}", e.getMessage());
        }

        logger.info("Total {} skills loaded successfully", skills.size());
        return skills;
    }

    /**
     * 按语义化版本号比较两个版本（major.minor.patch）。
     *
     * <p>null 或空字符串被视为最低版本。</p>
     *
     * @param v1 版本号1
     * @param v2 版本号2
     * @return 正数表示 v1 > v2，负数表示 v1 < v2，0 表示相等
     */
    int compareVersions(String v1, String v2) {
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

    /**
     * 从资源加载单个 Skill。
     */
    private Skill loadSkill(Resource resource) throws IOException, SkillParseException {
        try (InputStream is = resource.getInputStream()) {
            return skillParser.parse(is);
        }
    }
}
