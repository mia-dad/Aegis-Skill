# skill: financial_analysis

## description

对企业财务状况进行分析，并生成结构化分析报告。

## intent

- 财务分析
- 财务报表解读

## input

```yaml
company: string
period: string
```

## steps

### step: fetch_financial_data

**type**: tool
**tool**: get_financial_data

```yaml
company: "{{company}}"
period: "{{period}}"
```

### step: analyze_data

**type**: prompt

```prompt
你是一名专业的财务分析师。
以下是企业财务数据：
{{fetch_financial_data.output}}

请给出专业分析，包括：
1. 关键财务指标解读
2. 风险提示
3. 建议措施
```

## output

```json
{
  "summary": "string",
  "risks": ["string"],
  "suggestions": ["string"]
}
```
