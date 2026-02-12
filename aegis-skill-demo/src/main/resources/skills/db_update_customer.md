# skill: db_update_customer

## version

1.0.0

## description

根据客户ID更新客户信息。

## intent

- 更新客户
- 修改客户
- 编辑客户
- update customer

## input_schema

```yaml
customer_id:
  type: number
  required: true
  description: 客户ID

customer_name:
  type: string
  required: false
  description: 新的客户名称

email:
  type: string
  required: false
  description: 新的客户邮箱

phone:
  type: string
  required: false
  description: 新的客户电话
```

## output_schema

```yaml
affectedRows:
  type: number
  description: 影响行数
message:
  type: string
  description: 操作结果描述
```

## steps

### step: update_customer

**type**: tool
**tool**: db_update

```yaml
input:
  datasource: "main_db"
  table: "customers"
  set:
    name: "{{customer_name}}"
    email: "{{email}}"
    phone: "{{phone}}"
  where:
    id: "{{customer_id}}"
output_schema:
  affectedRows:
    type: number
    description: 影响行数
```

### step: build_result

**type**: template  **varName**: message

```template
客户 {{customer_id}} 更新完成，影响 {{affectedRows}} 行
```
