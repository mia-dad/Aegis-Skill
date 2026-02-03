package com.mia.aegis.skill.exception;

import com.mia.aegis.skill.i18n.MessageUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Skill 校验异常。
 *
 * <p>当 Skill 校验失败时抛出，包含所有校验错误。</p>
 */
public class SkillValidationException extends SkillException {

    private final List<String> errors;

    /**
     * 创建校验异常。
     *
     * @param errors 错误列表
     */
    public SkillValidationException(List<String> errors) {
        super(formatMessage(errors));
        this.errors = errors != null
                ? Collections.unmodifiableList(new ArrayList<String>(errors))
                : Collections.<String>emptyList();
    }

    /**
     * 创建单个错误的校验异常。
     *
     * @param error 错误信息
     */
    public SkillValidationException(String error) {
        super(MessageUtil.getMessage("skill.validation.error.single", error));
        List<String> errorList = new ArrayList<String>();
        errorList.add(error);
        this.errors = Collections.unmodifiableList(errorList);
    }

    private static String formatMessage(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return MessageUtil.getMessage("skill.validation.error");
        }
        if (errors.size() == 1) {
            return MessageUtil.getMessage("skill.validation.error.single", errors.get(0));
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append("  ").append(i + 1).append(". ").append(errors.get(i));
        }
        return MessageUtil.getMessage("skill.validation.error.multiple", errors.size(), sb.toString());
    }

    /**
     * 获取所有校验错误。
     *
     * @return 错误列表
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * 获取错误数量。
     *
     * @return 错误数量
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * 获取第一个错误。
     *
     * @return 第一个错误信息
     */
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }
}
