# skill: builtin_tools_demo

## version

1.0.0

## description

演示 Aegis 内置 Tools 的使用方法

## input_schema

```yaml
report_type:
  type: string
  required: true
  description: 报告类型（sales/summary）

data_file:
  type: string
  required: true
  description: 数据文件路径
```

## steps

### step: read_source_data

**type**: tool
**tool**: read_file

```yaml
input:
  path: "{{data_file}}"
output_schema:
  content:
    type: string
    description: 文件数据的内容
```

### step: process_data

**type**: template
**varName**: report_content

```template
根据以下数据生成{{report_type}}报告：

{{content}}

要求：
1. 提取关键指标
2. 计算汇总数据
3. 生成简洁的报告摘要
```

### step: write_report

**type**: tool
**tool**: write_file

```yaml
input:
  path: "output/{{report_type}}_report.md"
  content: "{{report_content}}"
  format: txt
output_schema:
  result:
    type: boolean
    description: 写入文件的结果
  path:
    type: string
    description: Resolved file path
```

### step: transform_result

**type**: template
**varName**: result_message

```template
报告生成完成！

类型: {{report_type}}

文本报告已保存至: output/{{report_type}}_report.md
```

## output_schema

```yaml
content:
  type: object
  description: 报告生成结果
path:
  type: string
  description: 输出文件路径
```
