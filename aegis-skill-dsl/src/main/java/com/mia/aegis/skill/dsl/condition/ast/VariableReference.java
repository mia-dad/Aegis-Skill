package com.mia.aegis.skill.dsl.condition.ast;

import com.mia.aegis.skill.dsl.condition.parser.ConditionExpression;
import com.mia.aegis.skill.dsl.condition.parser.ConditionExpressionVisitor;

/**
 * 变量引用节点。
 *
 * <p>表示条件表达式中的变量引用，如 {{focusCode}} 或 {{step1.result}}。
 * 存储的路径不包含 {{ 和 }} 包围符号。</p>
 *
 * <h3>变量解析顺序</h3>
 * <ol>
 *   <li>Step 输出（按 step id）</li>
 *   <li>Skill 输入</li>
 *   <li>全局上下文</li>
 * </ol>
 *
 * @since 0.2.0
 */
public final class VariableReference implements ConditionExpression {

    private final String path;

    /**
     * 创建变量引用。
     *
     * @param path 变量路径，如 "focusCode" 或 "step1.result"
     */
    public VariableReference(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Variable path cannot be null or empty");
        }
        this.path = path.trim();
    }

    /**
     * 获取变量路径。
     *
     * @return 变量路径
     */
    public String getPath() {
        return path;
    }

    /**
     * 判断是否为嵌套路径（包含点号）。
     *
     * @return 如果路径包含点号返回 true
     */
    public boolean isNestedPath() {
        return path.contains(".");
    }

    /**
     * 获取根路径（点号之前的部分）。
     *
     * @return 根路径
     */
    public String getRootPath() {
        int dotIndex = path.indexOf('.');
        return dotIndex >= 0 ? path.substring(0, dotIndex) : path;
    }

    /**
     * 获取子路径（点号之后的部分）。
     *
     * @return 子路径，如果没有点号则返回 null
     */
    public String getSubPath() {
        int dotIndex = path.indexOf('.');
        return dotIndex >= 0 ? path.substring(dotIndex + 1) : null;
    }

    @Override
    public <T> T accept(ConditionExpressionVisitor<T> visitor) {
        return visitor.visitVariable(this);
    }

    @Override
    public String toString() {
        return "{{" + path + "}}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VariableReference)) return false;
        VariableReference that = (VariableReference) obj;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
