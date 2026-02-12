---
id: test_extended_input
version: 1.0.0
description: 测试扩展 Input Schema 格式的 Skill
intent: 测试扩展输入
---

# skill: test_extended_input

## intent

- 测试扩展输入格式
- test extended input

## input_schema

```yaml
# 简写格式（向后兼容）
simple_field: string

# 扩展格式 - 基本字段
company:
  type: string
  required: true
  description: 公司名称
  placeholder: 请输入公司名称，如：阿里巴巴
  uiHint: text
  label: 公司

# 扩展格式 - 带选项的下拉框
period:
  type: string
  required: true
  description: 分析周期
  options:
    - Q1
    - Q2
    - Q3
    - Q4
    - 年度
  uiHint: select
  default: Q4
  label: 周期

# 扩展格式 - 多选
metrics:
  type: array
  required: false
  description: 分析指标
  options:
    - 营收
    - 利润
    - 现金流
    - 资产负债
  uiHint: multiselect
  label: 指标
  validation:
    minItems: 1
    message: 请至少选择一个分析指标

# 扩展格式 - 数值输入带验证
amount:
  type: number
  required: false
  description: 金额上限
  placeholder: 输入金额（万元）
  uiHint: number
  label: 金额上限
  default: 1000
  validation:
    min: 0
    max: 100000
    message: 金额必须在 0-100000 之间

# 扩展格式 - 多行文本
notes:
  type: string
  required: false
  description: 备注信息
  placeholder: 请输入备注...
  uiHint: textarea
  label: 备注

# 扩展格式 - 布尔值
confirmed:
  type: boolean
  required: true
  description: 确认执行
  uiHint: checkbox
  label: 我确认以上信息正确
  default: false
```

## output_schema

```yaml
echo_result:
  type: object
  description: 输入回显结果
```

## steps

### step: echo_input

**type**: tool
**tool**: log
**varName**: echo_result

```yaml
message: "收到输入: company={{company}}, period={{period}},metrics={{metrics}},amount={{amount}},notes={{notes}},confirmed={{confirmed}}"
```
