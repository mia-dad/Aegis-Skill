# skill: sales_report_generator

## version

1.0.0

## description

销售报告生成器 - 综合演示条件执行、人机交互(await)和结构化文档输出功能

## intent

- 销售报告
- sales report
- 生成销售分析
- 销售数据分析

## input_schema

```yaml
region:
  type: string
  required: false
  description: 销售区域（如：华东、华北、华南）
period:
  type: string
  required: false
  description: 报告期间（如：2024Q1）
need_suggestions:
  type: boolean
  required: false
  default: false
  description: 是否需要改进建议
```

## output_schema

```yaml
title:
  type: string
  required: false
  description: 报告标题
region:
  type: string
  required: true
  description: 销售区域
period:
  type: string
  required: true
  description: 报告期间
total_sales:
  type: number
  required: false
  description: 总销售额
target:
  type: number
  required: false
  description: 销售目标
achievement_rate:
  type: number
  required: false
  description: 达成率
growth_rate:
  type: number
  required: false
  description: 同比增长率
target_achieved:
  type: boolean
  required: false
  description: 是否达成目标
status_message:
  type: string
  required: false
  description: 状态消息
suggestions:
  type: string
  required: false
  description: 改进建议
user_notes:
  type: string
  required: false
  description: 用户备注
sales_output:
  type: string
  required: false
  description: 渲染后的报告文本
```

## steps

### step: request_region

当区域参数为空时，请求用户输入区域。

**type**: await
**varName**: region_input

```yaml
when:
  expr: "{{region}} == null"
message: |
  请选择需要分析的销售区域：
  - 华东
  - 华北
  - 华南
  - 华西
  - 全国
input_schema:
  region:
    type: string
    required: true
    description: 销售区域
```

### step: request_period

当期间参数为空时，请求用户输入期间。

**type**: await
**varName**: period_input

```yaml
when:
  expr: "{{period}} == null"
message: |
  请输入需要分析的报告期间（如 2024Q1、2024H1、2024）：
input_schema:
  period:
    type: string
    required: true
    description: 报告期间
```

### step: fetch_sales_data

获取销售数据（通过 MockSalesDataTool）。

**type**: tool
**tool**: mock_sales_data

```yaml
input:
  region: "{{region}}"
output_schema:
  region:
    type: string
    description: 销售区域
  period:
    type: string
    description: 报告期间
  total_sales:
    type: number
    description: 总销售额
  target:
    type: number
    description: 销售目标
  achievement_rate:
    type: number
    description: 达成率
  growth_rate:
    type: number
    description: 同比增长率
  target_achieved:
    type: boolean
    description: 是否达成目标
  status_message:
    type: string
    description: 状态消息
  details:
    type: string
    description: 明细数据（JSON 字符串）
```

### step: show_success_message

当达成率超过目标时显示成功消息。

**type**: tool
**tool**: log

```yaml
when:
  expr: "{{target_achieved}} == true"
level: info
message: "目标达成，业绩优秀！"
output_schema:
  logged:
    type: boolean
  message:
    type: string
```

### step: show_warning_message

当未达成目标时显示警告消息。

**type**: tool
**tool**: log

```yaml
when:
  expr: "{{target_achieved}} != true"
level: warn
message: "目标未达成，需要关注！"
output_schema:
  logged:
    type: boolean
  message:
    type: string
```

### step: confirm_suggestions

当需要建议时，询问用户是否包含改进建议。

**type**: await
**varName**: suggestion_input

```yaml
when:
  expr: "{{need_suggestions}} == true"
message: |
  您选择了包含改进建议。

  请确认要包含的内容：
input_schema:
  include_analysis:
    type: boolean
    required: true
    description: 是否包含深度分析
  additional_notes:
    type: string
    required: false
    description: 补充说明（可选）
```

### step: generate_suggestions

使用 LLM 生成改进建议。

**type**: prompt
**varName**: suggestions

```yaml
when:
  expr: "{{need_suggestions}} == true"
```

```prompt
你是一位销售分析专家。

基于以下销售数据，请给出3-5条具体的改进建议：

区域：{{region}}
期间：{{period}}
总销售额：{{total_sales}}
目标：{{target}}
达成率：{{achievement_rate}}%
同比增长：{{growth_rate}}%

请用简洁的条目形式列出建议。
```

### step: compose_document

组合最终文档输出。

**type**: template  **varName**: sales_output

```template
region: {{region}}
period: {{period}}
total_sales: {{total_sales}}
target: {{target}}
achievement_rate: {{achievement_rate}}
growth_rate: {{growth_rate}}
target_achieved: {{target_achieved}}
```

### step: compose_document_with_suggestions

组合包含建议的文档输出。

**type**: template  **varName**: sales_output_full

```yaml
when:
  expr: "{{need_suggestions}} == true"
```

```template
region: {{region}}
period: {{period}}
total_sales: {{total_sales}}
target: {{target}}
achievement_rate: {{achievement_rate}}
growth_rate: {{growth_rate}}
target_achieved: {{target_achieved}}
suggestions: {{suggestions}}
```
