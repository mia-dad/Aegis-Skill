# skill: financial_analysis

## version

1.0.0

## description

财务分析 Skill，用于分析公司财务数据。

## intent

- 财务分析
- financial_analysis
- 财报分析
- 公司分析

## input_schema

```yaml
query: 
  type: string
  required: true
  description: 你需要分析的问题
company:
  type: string
  required: true
  description: 待分析的公司
period:
  type: string
  required: true
  description: 分析的周期(如2025年)
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
**varName**: financial_data

```yaml
company: "{{company}}"
period: "{{period}}"
```

### step: analyze_data

**type**: prompt
**varName**: report

```prompt
你是一位专业的财务分析师。

用户问题：{{query}}

请分析 {{company}} 在 {{period}} 期间的财务数据：
{{financial_data}}

请给出专业、清晰的财务分析报告。
```
