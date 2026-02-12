# skill: template_demo

## version

1.0.0

## description

Template Step 演示技能，展示纯文本模板渲染能力。

## input_schema

```yaml
order_id:
  type: string
  required: true
  description: "订单编号"
product_name:
  type: string
  required: true
  description: "商品名称"
quantity:
  type: integer
  required: true
  description: "购买数量"
unit_price:
  type: number
  required: true
  description: "单价"
```

## output_schema

```yaml
report:
  type: string
  description: 订单确认文本
```

## steps

### step: calculate_total

**type**: tool
**tool**: calculator

```yaml
operation: "multiply"
a: "{{quantity}}"
b: "{{unit_price}}"
output_schema:
  total:
    type: number
    description: 计算结果
```

### step: compose_message

**type**: template  **varName**: report

```template
===== 订单确认 =====
订单编号：{{order_id}}
商品名称：{{product_name}}
购买数量：{{quantity}}
单价：¥{{unit_price}}
总金额：¥{{total}}
===================
感谢您的购买！
```
