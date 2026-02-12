package com.mia.aegis.skill.document.validation;

import com.mia.aegis.skill.document.model.Document;

import java.util.List;

/**
 * Document 结构验证器接口。
 *
 * <p>验证 Document 对象的结构完整性，返回错误和警告列表。</p>
 *
 * @since 0.3.0
 */
public interface DocumentValidator {

    /**
     * 验证 Document 结构。
     *
     * @param document 待验证的文档（可为 null）
     * @return 验证结果列表（可能为空表示验证通过）
     */
    List<ValidationResult> validate(Document document);

    /**
     * 检查 Document 是否有效（无 ERROR 级别错误）。
     *
     * @param document 待验证的文档（可为 null）
     * @return 如果无 ERROR 级别错误返回 true
     */
    default boolean isValid(Document document) {
        return validate(document).stream()
            .noneMatch(r -> r.getLevel() == ValidationLevel.ERROR);
    }
}
