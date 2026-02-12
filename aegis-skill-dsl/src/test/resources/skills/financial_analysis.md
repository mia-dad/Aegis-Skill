# skill: financial_analysis

## version

1.0.0

## description

对企业财务状况进行分析，并生成结构化分析报告。

## intent

- 财务分析
- 财务报表解读

## input_schema

```yaml
company: string
period: string
```

## output_schema

```yaml
report:
  type: string
  description: 财务分析报告
```

## steps

### step: fetch_financial_data

**type**: tool
**tool**: get_financial_data

```yaml
company: "{{company}}"
period: "{{period}}"
output_schema:
  data:
    type: string
    description: 金融数据（JSON 字符串）
```

### step: analyze_data

**type**: prompt
**varName**: report

```prompt
你是一名专业的财务分析师。
以下是企业财务数据：
{{data}}

请给出专业分析，包括：
1. 关键财务指标解读
2. 风险提示
3. 建议措施
```
