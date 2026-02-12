package com.mia.aegis.skill.financial;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mia.aegis.skill.exception.ToolExecutionException;
import com.mia.aegis.skill.tools.BuiltInTool;
import com.mia.aegis.skill.tools.ToolOutputContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模拟金融数据工具。
 */
@Component
public class MockFinancialDataTool extends BuiltInTool {

    public MockFinancialDataTool() {
        super("mock_financial_data", "获取模拟的金融数据用于测试", Category.DATA_PROCESSING);
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void execute(Map<String, Object> parameters, ToolOutputContext output) {
        String symbol = (String) parameters.getOrDefault("symbol", "AAPL");
        Integer days = (Integer) parameters.getOrDefault("days", 30);

        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("date", "2026-02-" + String.format("%02d", (i % 28) + 1));
            record.put("symbol", symbol);
            record.put("open", 150.0 + Math.random() * 10);
            record.put("high", 155.0 + Math.random() * 10);
            record.put("low", 145.0 + Math.random() * 10);
            record.put("close", 150.0 + Math.random() * 10);
            record.put("volume", 1000000 + (int)(Math.random() * 500000));
            data.add(record);
        }

        // 序列化为 JSON 字符串
        try {
            output.put("data", objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            throw new ToolExecutionException("mock_financial_data", "Failed to serialize data: " + e.getMessage(), e);
        }
    }
}
