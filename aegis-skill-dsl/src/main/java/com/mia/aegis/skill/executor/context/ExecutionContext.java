package com.mia.aegis.skill.executor.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 执行上下文。
 *
 * <p>管理 Skill 执行过程中的输入参数、Step 执行结果和运行时信息。</p>
 */
public class ExecutionContext {

    private final Map<String, Object> input;
    private final Map<String, StepResult> stepResults;
    private final Map<String, Object> metadata;
    private final RuntimeInfo runtime;

    /**
     * 创建执行上下文。
     *
     * @param input 用户输入参数
     */
    public ExecutionContext(Map<String, Object> input) {
        this.input = input != null
                ? Collections.unmodifiableMap(new HashMap<String, Object>(input))
                : Collections.<String, Object>emptyMap();
        this.stepResults = new LinkedHashMap<String, StepResult>();
        this.metadata = new HashMap<String, Object>();
        this.runtime = RuntimeInfo.now();
    }

    /**
     * 创建空的执行上下文。
     *
     * @return ExecutionContext 实例
     */
    public static ExecutionContext empty() {
        return new ExecutionContext(null);
    }

    /**
     * 获取用户输入参数。
     *
     * @return 不可变的输入参数映射
     */
    public Map<String, Object> getInput() {
        return input;
    }

    /**
     * 获取输入参数值。
     *
     * @param key 参数名
     * @return 参数值
     */
    public Object getInputValue(String key) {
        return input.get(key);
    }

    /**
     * 获取输入参数值（带默认值）。
     *
     * @param key 参数名
     * @param defaultValue 默认值
     * @return 参数值或默认值
     */
    public Object getInputValue(String key, Object defaultValue) {
        Object value = input.get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取所有 Step 执行结果。
     *
     * @return 不可变的 Step 结果映射
     */
    public Map<String, StepResult> getStepResults() {
        return Collections.unmodifiableMap(stepResults);
    }

    /**
     * 获取指定 Step 的执行结果。
     *
     * @param stepName Step 名称
     * @return Step 执行结果，不存在返回 null
     */
    public StepResult getStepResult(String stepName) {
        return stepResults.get(stepName);
    }

    /**
     * 获取指定 Step 的输出。
     *
     * @param stepName Step 名称
     * @return Step 输出，不存在或未成功返回 null
     */
    public Object getStepOutput(String stepName) {
        StepResult result = stepResults.get(stepName);
        return result != null && result.isSuccess() ? result.getOutput() : null;
    }

    /**
     * 添加 Step 执行结果。
     *
     * @param result Step 执行结果
     */
    public void addStepResult(StepResult result) {
        stepResults.put(result.getStepName(), result);
    }

    /**
     * 检查 Step 是否已执行。
     *
     * @param stepName Step 名称
     * @return 是否已执行
     */
    public boolean hasStepResult(String stepName) {
        return stepResults.containsKey(stepName);
    }

    /**
     * 获取元数据。
     *
     * @return 不可变的元数据映射
     */
    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * 设置元数据。
     *
     * @param key 键
     * @param value 值
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * 获取运行时信息。
     *
     * @return 运行时信息
     */
    public RuntimeInfo getRuntime() {
        return runtime;
    }

    /**
     * 构建变量上下文用于模板渲染。
     *
     * <p>合并输入参数和 Step 输出，支持 {{variable}} 和 {{step.output}} 语法。</p>
     *
     * @return 变量上下文
     */
    public Map<String, Object> buildVariableContext() {
        Map<String, Object> context = new HashMap<String, Object>(input);

        // 添加 Step 输出
        for (Map.Entry<String, StepResult> entry : stepResults.entrySet()) {
            String stepName = entry.getKey();
            StepResult result = entry.getValue();
            if (result.isSuccess()) {
                context.put(stepName, new StepOutputWrapper(result.getOutput()));
            }
        }

        // 添加 context 命名空间
        Map<String, Object> contextNamespace = new HashMap<String, Object>();
        contextNamespace.put("startTime", runtime.getStartTime());
        contextNamespace.put("elapsed", runtime.getElapsedTime());
        contextNamespace.putAll(metadata);
        context.put("context", contextNamespace);

        return context;
    }

    /**
     * Step 输出包装器，用于支持 {{step.output}} 语法。
     */
    public static class StepOutputWrapper {
        private final Object output;

        public StepOutputWrapper(Object output) {
            this.output = output;
        }

        public Object getOutput() {
            return output;
        }

        @Override
        public String toString() {
            return output != null ? output.toString() : "";
        }
    }

    @Override
    public String toString() {
        return "ExecutionContext{" +
                "inputKeys=" + input.keySet() +
                ", completedSteps=" + stepResults.size() +
                ", runtime=" + runtime +
                '}';
    }
}

