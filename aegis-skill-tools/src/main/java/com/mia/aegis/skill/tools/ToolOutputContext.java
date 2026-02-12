package com.mia.aegis.skill.tools;

/**
 * Tool 输出上下文接口。
 *
 * <p>工具通过此接口将执行结果直接写入技能执行上下文。
 * 每次调用 {@link #put(String, Object)} 会将一个键值对注册到全局变量空间，
 * 后续步骤可通过 {@code {{key}}} 语法引用。</p>
 *
 * <p>值应为基本类型（String、Number、Boolean）或 JSON 字符串。
 * 复杂对象（Map、List 等）应先序列化为 JSON 字符串再写入。</p>
 *
 * <p>示例：</p>
 * <pre>{@code
 * public void execute(Map<String, Object> input, ToolOutputContext output) {
 *     String result = doWork(input);
 *     output.put("result", result);
 *     output.put("status", "success");
 * }
 * }</pre>
 */
public interface ToolOutputContext {

    /**
     * 将键值对写入执行上下文。
     *
     * @param key   变量名（后续步骤通过 {{key}} 引用）
     * @param value 变量值（应为 String、Number、Boolean 或 JSON 字符串）
     */
    void put(String key, Object value);
}
