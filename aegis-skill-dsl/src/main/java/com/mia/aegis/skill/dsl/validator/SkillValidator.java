package com.mia.aegis.skill.dsl.validator;

import com.mia.aegis.skill.dsl.model.Skill;
import com.mia.aegis.skill.exception.SkillValidationException;

import java.util.List;

/**
 * Skill 校验器接口。
 *
 * <p>用于校验解析后的 Skill 对象是否符合规范。</p>
 */
public interface SkillValidator {

    /**
     * 校验 Skill 对象。
     *
     * @param skill 要校验的 Skill
     * @throws SkillValidationException 校验失败时抛出
     */
    void validate(Skill skill) throws SkillValidationException;

    /**
     * 校验 Skill 对象并返回所有错误。
     *
     * @param skill 要校验的 Skill
     * @return 错误列表，如果校验通过则返回空列表
     */
    List<String> validateAndCollectErrors(Skill skill);

    /**
     * 检查 Skill 是否有效。
     *
     * @param skill 要校验的 Skill
     * @return 如果校验通过返回 true
     */
    boolean isValid(Skill skill);
}
