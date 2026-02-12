package com.mia.aegis.skill.tools;


/**
 * 内置 Tool 基类。
 *
 * <p>所有内置 Tool 应继承此抽象类，提供通用功能。</p>
 *
 * <h3>内置 Tool 列表</h3>
 * <table border="1">
 *   <tr><th>类别</th><th>Tool 名称</th><th>描述</th></tr>
 *   <tr><td>数据获取</td><td>http_request</td><td>HTTP GET/POST 请求</td></tr>
 *   <tr><td>数据获取</td><td>read_file</td><td>读取 JSON/TXT 文件</td></tr>
 *   <tr><td>数据获取</td><td>write_file</td><td>写入文件</td></tr>
 *   <tr><td>数据处理</td><td>json_transform</td><td>JSONPath 提取/映射</td></tr>
 *   <tr><td>数据处理</td><td>table_aggregate</td><td>表格聚合（sum/avg/count）</td></tr>
 *   <tr><td>推理辅助</td><td>render_prompt</td><td>模板变量渲染</td></tr>
 *   <tr><td>推理辅助</td><td>validate_schema</td><td>JSON Schema 校验</td></tr>
 *   <tr><td>编排控制</td><td>set_variable</td><td>设置上下文变量</td></tr>
 *   <tr><td>编排控制</td><td>get_variable</td><td>获取上下文变量</td></tr>
 *   <tr><td>可观测性</td><td>log</td><td>日志输出</td></tr>
 * </table>
 *
 * @see ToolProvider
 */
public abstract class BuiltInTool implements ToolProvider {

    private final String name;
    private final String description;
    private final String category;

    /**
     * 构造内置 Tool。
     *
     * @param name        Tool 名称
     * @param description Tool 描述
     * @param category    Tool 类别
     */
    protected BuiltInTool(String name, String description, String category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * 获取 Tool 类别。
     *
     * @return 类别名称
     */
    public String getCategory() {
        return category;
    }

    /**
     * 获取输入 Schema。
     *
     * <p>子类应覆盖此方法提供具体的输入参数规范。</p>
     *
     * @return 输入 Schema
     */
    @Override
    public ToolSchema getInputSchema() {
        return ToolSchema.empty();
    }

    /**
     * 获取输出 Schema。
     *
     * <p>子类应覆盖此方法提供具体的输出参数规范。</p>
     *
     * @return 输出 Schema
     */
    @Override
    public ToolSchema getOutputSchema() {
        return ToolSchema.empty();
    }

    /**
     * Tool 类别常量。
     */
    public static final class Category {
        public static final String DATA_ACCESS = "data_access";
        public static final String DATA_PROCESSING = "data_processing";
        public static final String REASONING_SUPPORT = "reasoning_support";
        public static final String ORCHESTRATION = "orchestration";
        public static final String OBSERVABILITY = "observability";

        private Category() {}
    }
}

