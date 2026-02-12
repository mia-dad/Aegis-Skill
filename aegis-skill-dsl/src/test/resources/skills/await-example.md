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
title:
  type: string
content:
  type: string
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
**varName**: summary

```prompt
订单摘要：
- 订单编号：{{order_id}}
- 商品：{{product_name}}
- 数量：{{quantity}}
- 单价：¥{{unit_price}}
- 总金额：¥{{total}}
```

### step: user_confirmation

**type**: await
**varName**: confirmation

```yaml
message: |
  {{summary}}

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
**varName**: order_result
**when**: "confirmation.confirm == true"

```prompt
订单 {{order_id}} 已确认，总金额 ¥{{total}}。
{{#confirmation.notes}}
用户备注：{{confirmation.notes}}
{{/confirmation.notes}}
```

### step: cancel_order

**type**: prompt
**varName**: cancel_result
**when**: "confirmation.confirm == false"

```prompt
订单 {{order_id}} 已取消。
```

### step: final_output

**type**: prompt
**varName**: content

```prompt
{
  "order_id": "{{order_id}}",
  "total_amount": {{total}},
  "confirmed": {{confirmation.confirm}},
  "user_notes": "{{confirmation.notes}}"
}
```
