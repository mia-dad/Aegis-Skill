package com.mia.aegis.skill.executor.io;

import com.mia.aegis.skill.dsl.model.io.OutputContract;

/**
        * 输出契约校验器接口。
        *
        * <p>负责校验 Skill 执行结果是否符合声明的 OutputContract。</p>
 */
public interface OutputValidator {

    /**
     * 校验输出是否符合契约。
     *
     * @param output 实际输出对象
     * @param contract 输出契约定义
     * @return 校验结果
     */
    OutputValidationResult validate(Object output, OutputContract contract);
}
