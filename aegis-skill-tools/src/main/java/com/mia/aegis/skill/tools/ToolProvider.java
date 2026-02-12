package com.mia.aegis.skill.tools;

import com.mia.aegis.skill.exception.ToolExecutionException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Tool 提供者 SPI 接口。
 *
 * <p>用于注册和执行外部能力（HTTP API、数据库查询、Java Service 等）。
 * 实现类通过 Java ServiceLoader 机制发现。</p>
 *
 * <p>Registration:</p>
 * <p>在 META-INF/services/com.aegis.skill.spi.ToolProvider 中注册实现类。</p>
 *
 * <p>Example Implementation:</p>
 * <pre>{@code
 * public class FinancialDataTool implements ToolProvider {
 *     @Override
 *     public String getName() { return "get_financial_data"; }
 *
 *     @Override
 *     public void execute(Map<String, Object> input, ToolOutputContext output) {
 *         String company = (String) input.get("company");
 *         // Fetch financial data...
 *         output.put("data", resultJson);
 *         output.put("company", company);
 *     }
 * }
 * }</pre>
 */
public interface ToolProvider {

    /**
     * 获取 Tool 名称。
     *
     * @return Tool 名称（唯一标识）
     */
    String getName();

    /**
     * 获取 Tool 描述。
     *
     * @return 人类可读的描述
     */
    String getDescription();

    /**
     * 获取输入参数 Schema。
     *
     * @return 输入参数规范
     */
    ToolSchema getInputSchema();

    /**
     * 获取输出参数 Schema。
     *
     * @return 输出参数规范
     */
    ToolSchema getOutputSchema();

    /**
     * 同步执行 Tool。
     *
     * <p>工具通过 {@code output.put(key, value)} 将结果写入执行上下文，
     * 后续步骤可通过 {@code {{key}}} 语法引用这些变量。</p>
     *
     * @param input  输入参数（已经过模板渲染）
     * @param output 输出上下文，用于写入执行结果
     * @throws ToolExecutionException 执行失败时抛出
     */
    void execute(Map<String, Object> input, ToolOutputContext output) throws ToolExecutionException;

    /**
     * 异步执行 Tool。
     *
     * <p>默认实现将同步执行包装为 CompletableFuture。</p>
     *
     * @param input  输入参数
     * @param output 输出上下文
     * @return Future（完成时无返回值）
     */
    default CompletableFuture<Void> executeAsync(Map<String, Object> input, ToolOutputContext output) {
        return CompletableFuture.runAsync(() -> execute(input, output));
    }

    /**
     * 验证输入参数。
     *
     * <p>默认返回成功，实现类可覆盖此方法添加校验逻辑。</p>
     *
     * @param input 输入参数
     * @return 验证结果
     */
    default ValidationResult validateInput(Map<String, Object> input) {
        return ValidationResult.success();
    }

    /**
     * 获取 Tool 类别。
     *
     * <p>默认返回 "uncategorized"，实现类可覆盖此方法提供具体类别。</p>
     *
     * @return 类别名称
     */
    default String getCategory() {
        return "uncategorized";
    }

    /**
     * 获取 Tool 版本。
     *
     * <p>默认返回 "1.0.0"。</p>
     *
     * @return 版本号
     */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * 获取错误码描述映射。
     *
     * <p>键为错误码（如 "FILE_NOT_FOUND"），值为人类可读的错误描述。
     * 默认返回空 Map。</p>
     *
     * @return 错误码到描述的映射
     */
    default Map<String, String> getErrorDescriptions() {
        return Collections.emptyMap();
    }

    /**
     * 获取 Tool 标签列表。
     *
     * <p>用于分类检索和过滤。默认返回空列表。</p>
     *
     * @return 标签列表
     */
    default List<String> getTags() {
        return Collections.emptyList();
    }
}
