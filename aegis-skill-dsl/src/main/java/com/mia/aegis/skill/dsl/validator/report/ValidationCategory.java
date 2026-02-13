package com.mia.aegis.skill.dsl.validator.report;

/**
 * 校验问题分类。
 */
public enum ValidationCategory {
    /** 语法 & 结构（ID/version/name 格式、必填字段缺失） */
    SYNTAX,
    /** 输入输出 Schema（类型定义、字段声明） */
    SCHEMA,
    /** 逻辑（循环依赖、条件表达式、执行流程） */
    LOGIC,
    /** 工具引用（tool 名称、参数匹配） */
    TOOL,
    /** 数据流（变量引用、数据管道连贯性） */
    DATA_FLOW
}
