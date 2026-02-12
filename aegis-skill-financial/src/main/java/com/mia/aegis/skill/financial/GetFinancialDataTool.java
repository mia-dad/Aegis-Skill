package com.mia.aegis.skill.financial;

import com.mia.aegis.skill.tools.BuiltInTool;
import com.mia.aegis.skill.tools.ToolOutputContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 获取金融数据工具（实际应该对接真实的金融数据API）。
 */
@Component
public class GetFinancialDataTool extends BuiltInTool {

    public GetFinancialDataTool() {
        super("get_financial_data", "获取真实的金融市场数据", Category.DATA_PROCESSING);
    }

    @Override
    public void execute(Map<String, Object> parameters, ToolOutputContext output) {
        // TODO: 对接真实的金融数据 API
        // 目前先使用 mock 数据
        MockFinancialDataTool mockTool = new MockFinancialDataTool();
        mockTool.execute(parameters, output);
    }
}
