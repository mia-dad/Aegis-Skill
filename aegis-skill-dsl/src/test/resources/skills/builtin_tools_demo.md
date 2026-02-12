# skill: builtin_tools_demo

## version

1.0.0

**description**: 演示 Aegis 内置 Tools 的使用方法

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

output_format:
  type: string
  required: false
  description: 输出格式（txt/excel/ppt）
  default: txt
```

## steps

### step: read_source_data

**type**: tool
**tool**: builtin_read_file

```yaml
path: "{{data_file}}"
output_schema:
  path:
    type: string
  content:
    type: string
```

### step: process_data

**type**: prompt
**varName**: report_content

```prompt
根据以下数据生成{{report_type}}报告：

{{content}}

要求：
1. 提取关键指标
2. 计算汇总数据
3. 生成简洁的报告摘要
```

### step: write_txt_report

**type**: tool
**tool**: builtin_write_file

```yaml
when:
  expr: "{{output_format}} == 'txt'"
path: "output/{{report_type}}_report.md"
content: "{{report_content}}"
```

### step: prepare_excel_data

**type**: tool
**tool**: builtin_read_file

```yaml
when:
  expr: "{{output_format}} == 'excel'"
path: "{{data_file}}"
```

### step: write_excel_report

**type**: tool
**tool**: builtin_write_excel

```yaml
when:
  expr: "{{output_format}} == 'excel'"
path: "output/{{report_type}}_report.xlsx"
data: "{{excel_data}}"
sheetName: "Report"
```

### step: prepare_ppt_model

**type**: tool
**tool**: builtin_prepare_ppt_model

```yaml
when:
  expr: "{{output_format}} == 'ppt'"
data:
  title: "{{report_type}} Report"
  content: "{{report_content}}"
  date: "{{current_date}}"
```

### step: render_ppt_report

**type**: tool
**tool**: builtin_render_ppt

```yaml
when:
  expr: "{{output_format}} == 'ppt'"
templatePath: "templates/report_template.pptx"
outputPath: "output/{{report_type}}_report.pptx"
model: "{{ppt_model}}"
```

### step: compose_result

**type**: template
**varName**: result_summary

```template
报告生成完成！

类型: {{report_type}}
格式: {{output_format}}

{{#txt_result}}
文本报告已保存至: output/{{report_type}}_report.md
{{/txt_result}}

{{#excel_result}}
Excel 报告已保存至: output/{{report_type}}_report.xlsx
{{/excel_result}}

{{#ppt_result}}
PPT 报告已保存至: output/{{report_type}}_report.pptx
{{/ppt_result}}
```

## output_schema

```yaml
level:
  type: string
  description: 提醒级别
title:
  type: string
  description: 提醒标题
content:
  type: string
  description: 报告生成结果
output_path:
  type: string
  description: 输出文件路径
```
