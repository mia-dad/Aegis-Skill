package com.mia.aegis.skill.template.expr;

import java.util.Collections;
import java.util.List;

/**
 * 变量引用表达式。
 *
 * <p>表示模板中的变量访问，支持点路径如 {@code a.b.c}。</p>
 *
 * @since 0.3.0
 */
public final class VariableExpr implements TemplateExpression {

    private final List<String> pathSegments;

    /**
     * 创建变量引用表达式。
     *
     * @param pathSegments 路径段列表，如 ["user", "name"] 表示 user.name
     */
    public VariableExpr(List<String> pathSegments) {
        if (pathSegments == null || pathSegments.isEmpty()) {
            throw new IllegalArgumentException("Path segments cannot be null or empty");
        }
        this.pathSegments = Collections.unmodifiableList(pathSegments);
    }

    /**
     * 获取路径段列表。
     *
     * @return 不可变的路径段列表
     */
    public List<String> getPathSegments() {
        return pathSegments;
    }

    /**
     * 获取完整路径字符串。
     *
     * @return 以点号分隔的完整路径
     */
    public String getFullPath() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pathSegments.size(); i++) {
            if (i > 0) sb.append(".");
            sb.append(pathSegments.get(i));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Variable(" + getFullPath() + ")";
    }
}
