package com.mia.aegis.skill.tools;

import com.mia.aegis.skill.exception.ToolExecutionException;

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
 *     public Object execute(Map<String, Object> input) {
 *         String company = (String) input.get("company");
 *         // Fetch financial data...
 *         return result;
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
     * @param input 输入参数（已经过模板渲染）
     * @return 执行结果
     * @throws ToolExecutionException 执行失败时抛出
     */
    Object execute(Map<String, Object> input) throws ToolExecutionException;

    /**
     * 异步执行 Tool。
     *
     * <p>默认实现将同步执行包装为 CompletableFuture。</p>
     *
     * @param input 输入参数
     * @return 执行结果的 Future
     */
    default CompletableFuture<Object> executeAsync(Map<String, Object> input) {
        return CompletableFuture.supplyAsync(() -> execute(input));
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
}

