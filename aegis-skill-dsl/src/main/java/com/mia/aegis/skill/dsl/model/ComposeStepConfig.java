package com.mia.aegis.skill.dsl.model;

import com.mia.aegis.skill.i18n.MessageUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compose 类型 Step 的配置。
 *
 * <p>包含要组合的数据源引用列表。</p>
 */
public class ComposeStepConfig implements StepConfig {

    private final List<String> sources;

    /**
     * 创建 Compose Step 配置。
     *
     * @param sources 源数据引用列表（如 "step_name.output"）
     */
    public ComposeStepConfig(List<String> sources) {
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException(MessageUtil.getMessage("composestepconfig.sources.null"));
        }
        this.sources = Collections.unmodifiableList(new ArrayList<String>(sources));
    }

    /**
     * 获取源数据引用列表。
     *
     * @return 不可变的源引用列表
     */
    public List<String> getSources() {
        return sources;
    }

    @Override
    public StepType getStepType() {
        return StepType.COMPOSE;
    }

    @Override
    public String toString() {
        return "ComposeStepConfig{sources=" + sources + '}';
    }
}
