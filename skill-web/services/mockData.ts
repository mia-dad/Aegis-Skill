import { Skill } from '../types';

export const MOCK_SKILLS: Skill[] = [
  {
    id: "financial_analysis",
    version: "1.0.0",
    description: "Financial Analysis Report Generator",
    intents: ["financial analysis", "report", "earnings"],
    inputSchema: {
      fields: {
        company: {
          type: "string",
          required: true,
          description: "Target company name (e.g., Apple, Tesla)",
          placeholder: "Enter company name...",
          uiHint: "text"
        },
        period: {
          type: "string",
          required: true,
          description: "Analysis fiscal period",
          options: ["Q1", "Q2", "Q3", "Q4", "FY2024"],
          uiHint: "select",
          defaultValue: "Q1"
        },
        metrics: {
          type: "array",
          required: false,
          description: "Key metrics to include",
          options: ["Revenue", "Net Profit", "Cash Flow", "Debt Ratio"],
          uiHint: "multiselect"
        }
      }
    }
  },
  {
    id: "market_growth_calc",
    version: "1.0.0",
    description: "Market Growth Calculator (Interactive)",
    intents: ["growth", "market size"],
    inputSchema: {
      fields: {
        industry: {
          type: "string",
          required: true,
          uiHint: "text",
          description: "Industry sector"
        },
        initialSize: {
          type: "number",
          required: true,
          uiHint: "number",
          description: "Initial market size ($B)"
        }
      }
    }
  },
  {
    id: "simple_chat",
    version: "1.0.0",
    description: "General Assistant Chat",
    intents: ["chat", "qa"],
    inputSchema: {
      fields: {
        prompt: {
          type: "string",
          required: true,
          uiHint: "textarea",
          placeholder: "Ask me anything...",
          description: "Your question or instruction"
        }
      }
    }
  }
];

/**
 * Mock 财务分析结果 — 普通 JSON 数据
 */
export const MOCK_FINANCIAL_RESULT = {
  company: "Apple Inc.",
  period: "Q1 2024",
  revenue: 119.58,
  netProfit: 33.92,
  operatingMargin: "28.4%",
  yoyGrowth: "25%",
  keyTakeaways: [
    "25% YoY Growth observed in Q4",
    "Operating margins expanded by 200bps"
  ],
  quarterlyRevenue: {
    "Q1": 15,
    "Q2": 18,
    "Q3": 19,
    "Q4": 21
  }
};
