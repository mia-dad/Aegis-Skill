package com.mia.aegis.skill.template.expr;

import java.util.Collections;
import java.util.List;

/**
 * 数组索引访问表达式。
 *
 * <p>支持两种索引形式：</p>
 * <ul>
 *   <li>字面量索引：{@code arr[2]}</li>
 *   <li>变量索引：{@code arr[#var]}</li>
 * </ul>
 *
 * <p>还支持索引后的字段访问：{@code arr[2].field}</p>
 *
 * @since 0.3.0
 */
public final class ArrayAccessExpr implements TemplateExpression {

    private final TemplateExpression arrayExpr;
    private final int literalIndex;
    private final String variableIndex;
    private final boolean isVariableIndex;
    private final List<String> trailingPath;

    /**
     * 创建字面量索引的数组访问表达式。
     *
     * @param arrayExpr    数组表达式
     * @param literalIndex 字面量索引值
     * @param trailingPath 索引后的字段路径（可为空列表）
     */
    public ArrayAccessExpr(TemplateExpression arrayExpr, int literalIndex, List<String> trailingPath) {
        this.arrayExpr = arrayExpr;
        this.literalIndex = literalIndex;
        this.variableIndex = null;
        this.isVariableIndex = false;
        this.trailingPath = trailingPath != null ? Collections.unmodifiableList(trailingPath) : Collections.<String>emptyList();
    }

    /**
     * 创建变量索引的数组访问表达式。
     *
     * @param arrayExpr     数组表达式
     * @param variableIndex 变量索引名称（不含 # 前缀）
     * @param trailingPath  索引后的字段路径（可为空列表）
     */
    public ArrayAccessExpr(TemplateExpression arrayExpr, String variableIndex, List<String> trailingPath) {
        this.arrayExpr = arrayExpr;
        this.literalIndex = -1;
        this.variableIndex = variableIndex;
        this.isVariableIndex = true;
        this.trailingPath = trailingPath != null ? Collections.unmodifiableList(trailingPath) : Collections.<String>emptyList();
    }

    public TemplateExpression getArrayExpr() {
        return arrayExpr;
    }

    public int getLiteralIndex() {
        return literalIndex;
    }

    public String getVariableIndex() {
        return variableIndex;
    }

    public boolean isVariableIndex() {
        return isVariableIndex;
    }

    public List<String> getTrailingPath() {
        return trailingPath;
    }

    @Override
    public String toString() {
        String idx = isVariableIndex ? "#" + variableIndex : String.valueOf(literalIndex);
        String trail = trailingPath.isEmpty() ? "" : "." + String.join(".", trailingPath);
        return "ArrayAccess(" + arrayExpr + "[" + idx + "]" + trail + ")";
    }
}
