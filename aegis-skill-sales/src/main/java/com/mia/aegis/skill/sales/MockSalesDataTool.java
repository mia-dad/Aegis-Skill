package com.mia.aegis.skill.sales;

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
 * 模拟销售数据工具 - 返回聚合后的销售报告数据。
 */
@Component
public class MockSalesDataTool extends BuiltInTool {

    public MockSalesDataTool() {
        super("mock_sales_data", "获取模拟的销售数据报告（聚合后的报告数据，包含总销售额、目标、达成率等）", Category.DATA_PROCESSING);
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void execute(Map<String, Object> parameters, ToolOutputContext output) {
        String region = (String) parameters.getOrDefault("region", "全国");
        String period = (String) parameters.getOrDefault("period", "2024Q1");

        // 生成聚合后的销售报告数据
        Map<String, Object> report = new HashMap<>();

        // 基本信息
        report.put("region", region);
        report.put("period", period);

        // 生成随机的销售数据
        double totalSales = 1000000 + Math.random() * 500000;
        double target = 1200000;
        double lastYearSales = 900000 + Math.random() * 300000;

        // 计算指标
        double achievementRate = (totalSales / target) * 100;
        double growthRate = ((totalSales - lastYearSales) / lastYearSales) * 100;
        boolean targetAchieved = achievementRate >= 100;

        report.put("total_sales", Math.round(totalSales * 100.0) / 100.0);
        report.put("target", target);
        report.put("achievement_rate", Math.round(achievementRate * 100.0) / 100.0);
        report.put("growth_rate", Math.round(growthRate * 100.0) / 100.0);
        report.put("target_achieved", targetAchieved);

        // 添加状态消息
        if (targetAchieved) {
            report.put("status_message", "恭喜！销售目标已达成，业绩优秀！");
        } else {
            report.put("status_message", "销售目标未达成（达成率：" +
                Math.round(achievementRate) + "%），需要加强市场推广和销售力度。");
        }

        // 生成明细数据（可选）
        List<Map<String, Object>> details = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("period", "2026-" + String.format("%02d", (i % 12) + 1));
            record.put("revenue", 80000 + Math.random() * 40000);
            record.put("units_sold", 800 + (int)(Math.random() * 400));
            record.put("product", "Product-" + (char)('A' + (i % 5)));
            details.add(record);
        }
        report.put("details", details);

        // 输出各个基本类型字段到上下文
        output.put("region", region);
        output.put("period", period);
        output.put("total_sales", report.get("total_sales"));
        output.put("target", target);
        output.put("achievement_rate", report.get("achievement_rate"));
        output.put("growth_rate", report.get("growth_rate"));
        output.put("target_achieved", targetAchieved);
        output.put("status_message", report.get("status_message"));

        // 复杂数据序列化为 JSON 字符串
        try {
            output.put("details", objectMapper.writeValueAsString(details));
        } catch (Exception e) {
            throw new ToolExecutionException("mock_sales_data", "Failed to serialize details: " + e.getMessage(), e);
        }
    }
}
