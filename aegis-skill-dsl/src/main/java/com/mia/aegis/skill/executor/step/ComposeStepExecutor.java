package com.mia.aegis.skill.executor.step;


import com.mia.aegis.skill.dsl.model.ComposeStepConfig;
import com.mia.aegis.skill.dsl.model.Step;
import com.mia.aegis.skill.dsl.model.StepType;
import com.mia.aegis.skill.exception.SkillExecutionException;
import com.mia.aegis.skill.executor.context.ExecutionContext;
import com.mia.aegis.skill.executor.context.StepResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compose 类型 Step 执行器。
 *
 * <p>负责执行 Compose 类型的 Step：
 * <ol>
 *   <li>解析 sources 引用列表</li>
 *   <li>从执行上下文中获取各源的输出</li>
 *   <li>将多个输出合并为单一结构化对象</li>
 * </ol>
 * </p>
 *
 * <p>支持的 source 引用格式：
 * <ul>
 *   <li>{@code step_name} - 引用整个 Step 输出</li>
 *   <li>{@code step_name.output} - 引用 Step 输出（等同于 step_name）</li>
 *   <li>{@code step_name.output.field} - 引用输出中的特定字段（如果输出是 Map）</li>
 * </ul>
 * </p>
 */
public class ComposeStepExecutor implements StepExecutor {

    /**
     * 创建 ComposeStepExecutor。
     */
    public ComposeStepExecutor() {
    }

    @Override
    public StepResult execute(Step step, ExecutionContext context) throws SkillExecutionException {
        if (!supports(step)) {
            throw new SkillExecutionException(step.getName(),
                    new IllegalArgumentException("ComposeStepExecutor only supports COMPOSE type steps"));
        }

        long startTime = System.currentTimeMillis();
        String stepName = step.getName();

        try {
            ComposeStepConfig config = step.getComposeConfig();
            List<String> sources = config.getSources();

            // 检查所有依赖是否已执行
            List<String> missingDeps = checkMissingDependencies(sources, context);
            if (!missingDeps.isEmpty()) {
                throw new SkillExecutionException(stepName,
                        new IllegalStateException("Missing dependencies: " + missingDeps));
            }

            // 合并所有源数据
            Object composedOutput = composeSources(sources, context);

            long duration = System.currentTimeMillis() - startTime;
            return StepResult.success(stepName, composedOutput, duration);

        } catch (SkillExecutionException e) {
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            throw new SkillExecutionException(stepName, e);
        }
    }

    @Override
    public boolean supports(Step step) {
        return step != null && step.getType() == StepType.COMPOSE;
    }

    /**
     * 检查缺失的依赖。
     *
     * @param sources 源引用列表
     * @param context 执行上下文
     * @return 缺失的依赖列表
     */
    private List<String> checkMissingDependencies(List<String> sources, ExecutionContext context) {
        List<String> missing = new ArrayList<String>();

        for (String source : sources) {
            String stepName = extractStepName(source);
            if (!context.hasStepResult(stepName)) {
                missing.add(stepName);
            } else {
                StepResult result = context.getStepResult(stepName);
                if (!result.isSuccess()) {
                    missing.add(stepName + " (failed)");
                }
            }
        }

        return missing;
    }

    /**
     * 合并多个源数据。
     *
     * @param sources 源引用列表
     * @param context 执行上下文
     * @return 合并后的输出
     */
    private Object composeSources(List<String> sources, ExecutionContext context) {
        // 如果只有一个源，直接返回该源的输出
        if (sources.size() == 1) {
            return resolveSource(sources.get(0), context);
        }

        // 多个源时，合并为 Map
        Map<String, Object> composed = new LinkedHashMap<String, Object>();

        for (String source : sources) {
            String key = extractSourceKey(source);
            Object value = resolveSource(source, context);
            composed.put(key, value);
        }

        return composed;
    }

    /**
     * 解析源引用并获取值。
     *
     * @param source 源引用（如 "step_name" 或 "step_name.output" 或 "step_name.output.field"）
     * @param context 执行上下文
     * @return 源值
     */
    private Object resolveSource(String source, ExecutionContext context) {
        String[] parts = source.split("\\.");
        String stepName = parts[0];

        Object output = context.getStepOutput(stepName);

        // 如果引用包含字段路径，尝试深入获取
        if (parts.length > 1) {
            int startIndex = 1;
            // 如果第二部分是 "output"，跳过它
            if ("output".equals(parts[1])) {
                startIndex = 2;
            }

            // 深入获取嵌套字段
            for (int i = startIndex; i < parts.length && output != null; i++) {
                output = getFieldValue(output, parts[i]);
            }
        }

        return output;
    }

    /**
     * 从对象中获取字段值。
     *
     * @param obj 对象
     * @param fieldName 字段名
     * @return 字段值
     */
    @SuppressWarnings("unchecked")
    private Object getFieldValue(Object obj, String fieldName) {
        if (obj instanceof Map) {
            return ((Map<String, Object>) obj).get(fieldName);
        }
        // 可以扩展支持反射获取 POJO 字段
        return null;
    }

    /**
     * 从源引用中提取 Step 名称。
     *
     * @param source 源引用
     * @return Step 名称
     */
    private String extractStepName(String source) {
        int dotIndex = source.indexOf('.');
        return dotIndex > 0 ? source.substring(0, dotIndex) : source;
    }

    /**
     * 从源引用中提取用于合并的键名。
     *
     * @param source 源引用
     * @return 键名（使用 Step 名称）
     */
    private String extractSourceKey(String source) {
        return extractStepName(source);
    }
}

