# skill: order_confirmation

## version

1.0.0

**description**: 订单确认示例 - 演示 await step 的人机交互功能

## input_schema

```yaml
order_id:
  type: string
  required: true
  description: 订单编号
product_name:
  type: string
  required: true
  description: 商品名称
quantity:
  type: number
  required: true
  description: 购买数量
unit_price:
  type: number
  required: true
  description: 单价
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
  description: 订单处理结果
```

## steps

### step: calculate_total

**type**: tool
**tool**: variable.set

```yaml
input:
  name: total_amount
  value: "{{quantity * unit_price}}"
```

### step: prepare_summary

**type**: prompt

```prompt
订单摘要：
- 订单编号：{{order_id}}
- 商品：{{product_name}}
- 数量：{{quantity}}
- 单价：¥{{unit_price}}
- 总金额：¥{{calculate_total.output}}
```

### step: user_confirmation

**type**: await

```yaml
message: |
  {{prepare_summary.output}}

  请确认以上订单信息是否正确。
input_schema:
  confirm:
    type: boolean
    required: true
    description: 是否确认订单
  notes:
    type: string
    required: false
    description: 备注信息（可选）
```

### step: process_order

**type**: prompt
when: user_confirmation.confirm == true

```prompt
订单 {{order_id}} 已确认，总金额 ¥{{calculate_total.output}}。
{{#user_confirmation.notes}}
用户备注：{{user_confirmation.notes}}
{{/user_confirmation.notes}}
```

### step: cancel_order

**type**: prompt
when: user_confirmation.confirm == false

```prompt
订单 {{order_id}} 已取消。
```

### step: final_output

**type**: prompt

```prompt
{
  "order_id": "{{order_id}}",
  "total_amount": {{calculate_total.output}},
  "confirmed": {{user_confirmation.confirm}},
  "user_notes": "{{user_confirmation.notes}}"
}
```
