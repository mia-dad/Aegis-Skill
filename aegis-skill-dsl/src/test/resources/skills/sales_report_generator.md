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

获取销售数据。

**type**: tool
**tool**: set_variable

```yaml
name: sales_data
value:
  华东:
    region: 华东
    period: 2024Q1
    total_sales: 1580000
    target: 1500000
    achievement_rate: 105.3
    growth_rate: 12.5
    target_achieved: true
    monthly_data:
      labels:
        - "1月"
        - "2月"
        - "3月"
      sales:
        - 480000
        - 520000
        - 580000
    top_products:
      - name: 产品A
        sales: 650000
      - name: 产品B
        sales: 480000
      - name: 产品C
        sales: 450000

  华北:
    region: 华北
    period: 2024Q1
    total_sales: 1120000
    target: 1300000
    achievement_rate: 86.2
    growth_rate: 5.1
    target_achieved: false
    monthly_data:
      labels:
        - "1月"
        - "2月"
        - "3月"
      sales:
        - 360000
        - 370000
        - 390000
    top_products:
      - name: 产品D
        sales: 420000
      - name: 产品E
        sales: 380000
      - name: 产品F
        sales: 320000

  华南:
    region: 华南
    2024Q1:
      period: 2024Q1
      total_sales: 1340000
      target: 1400000
      achievement_rate: 95.7
      growth_rate: 9.8
      target_achieved: false
      monthly_data:
        labels:
          - "1月"
          - "2月"
          - "3月"
        sales:
          - 430000
          - 450000
          - 460000
      top_products:
        - name: 产品G
          sales: 510000
        - name: 产品H
          sales: 430000
        - name: 产品I
          sales: 400000
    2024Q2:
      period: 2024Q2
      total_sales: 1480000
      target: 1450000
      achievement_rate: 102.1
      growth_rate: 15.6
      target_achieved: true
      monthly_data:
        labels:
          - "4月"
          - "5月"
          - "6月"
        sales:
          - 470000
          - 500000
          - 510000
      top_products:
        - name: 产品G
          sales: 580000
        - name: 产品H
          sales: 460000
        - name: 产品M
          sales: 440000

  华西:
    region: 华西
    period: 2024Q1
    total_sales: 780000
    target: 900000
    achievement_rate: 86.7
    growth_rate: 3.2
    target_achieved: false
    monthly_data:
      labels:
        - "1月"
        - "2月"
        - "3月"
      sales:
        - 240000
        - 260000
        - 280000
    top_products:
      - name: 产品J
        sales: 300000
      - name: 产品K
        sales: 260000
      - name: 产品L
        sales: 220000

  全国:
    region: 全国
    period: 2024Q1
    total_sales: 4820000
    target: 5100000
    achievement_rate: 94.5
    growth_rate: 10.2
    target_achieved: false
    monthly_data:
      labels:
        - "1月"
        - "2月"
        - "3月"
      sales:
        - 1510000
        - 1600000
        - 1710000
    top_products:
      - name: 产品A
        sales: 1200000
      - name: 产品G
        sales: 980000
      - name: 产品D
        sales: 860000
```

### step: select_region_data

**type**: tool
**tool**: json_select

```yaml
input: "{{sales_data}}"
select:
  path: "{{region}}"
output_schema:
  result:
    type: string
    description: 选中的区域数据（JSON 字符串）
```

### step: save_region_data

保存区域数据到独立变量，避免被后续 json_select 覆盖。

**type**: template  **varName**: region_data_json

```template
{{result}}
```

### step: select_period_data

**type**: tool
**tool**: json_select

```yaml
input: "{{sales_data}}"
select:
  path: "{{region}}.{{period}}"
output_schema:
  result:
    type: string
    description: 选中的期间数据（JSON 字符串）
```

### step: save_period_data

保存期间数据到独立变量。

**type**: template  **varName**: period_data_json

```template
{{result}}
```

### step: show_success_message

当达成率超过目标时显示成功消息。

**type**: tool
**tool**: log

```yaml
level: info
message: "数据获取完成"
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
数据：{{period_data_json}}

请用简洁的条目形式列出建议。
```

### step: compose_document

组合最终文档输出。

**type**: template  **varName**: sales_output

```template
region: {{region}}
period: {{period}}
data: {{period_data_json}}
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
data: {{period_data_json}}
suggestions: {{suggestions}}
```
