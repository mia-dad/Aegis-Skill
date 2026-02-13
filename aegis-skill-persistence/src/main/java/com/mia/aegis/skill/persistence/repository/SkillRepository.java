package com.mia.aegis.skill.persistence.repository;

import com.mia.aegis.skill.dsl.model.Skill;
import com.mia.aegis.skill.persistence.model.SkillRecord;

import java.util.List;

/**
 * 技能仓储接口。
 *
 * <p>抽象技能的存储和检索操作，支持不同的后端实现（文件系统、数据库等）。</p>
 *
 * <p>核心约束：{@code skillId + version} 构成唯一标识。</p>
 */
public interface SkillRepository {

    // ===== 查询操作 =====

    /**
     * 获取指定 skillId 的最新版本。
     *
     * @param skillId 技能 ID
     * @return 版本号最大的 Skill，不存在返回 null
     */
    Skill findById(String skillId);

    /**
     * 按 skillId + version 精确查找。
     *
     * @param skillId 技能 ID
     * @param version 版本号
     * @return 匹配的 Skill，不存在返回 null
     */
    Skill findById(String skillId, String version);

    /**
     * 获取所有技能（每个 skillId 仅返回最新版本）。
     *
     * @return 技能列表
     */
    List<Skill> findAll();

    /**
     * 获取指定 skillId 的所有版本（按版本号降序）。
     *
     * @param skillId 技能 ID
     * @return 版本列表
     */
    List<Skill> findAllVersions(String skillId);

    // ===== 持久化记录操作 =====

    /**
     * 获取指定 skillId + version 的完整持久化记录。
     *
     * @param skillId 技能 ID
     * @param version 版本号
     * @return 持久化记录，不存在返回 null
     */
    SkillRecord findRecord(String skillId, String version);

    /**
     * 获取所有持久化记录。
     *
     * @return 记录列表
     */
    List<SkillRecord> findAllRecords();

    /**
     * 保存技能记录（新增或更新）。
     *
     * <p>如果相同 skillId + version 已存在，则覆盖更新。</p>
     *
     * @param record 技能记录
     * @return 保存后的记录
     */
    SkillRecord save(SkillRecord record);

    /**
     * 删除指定版本的技能。
     *
     * @param skillId 技能 ID
     * @param version 版本号
     * @return 是否删除成功
     */
    boolean delete(String skillId, String version);

    /**
     * 检查技能是否存在。
     *
     * @param skillId 技能 ID
     * @param version 版本号
     * @return 是否存在
     */
    boolean exists(String skillId, String version);
}
