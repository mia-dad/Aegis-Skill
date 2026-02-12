# skill: db_insert_customer

## version

1.0.0

## description

插入一条新的客户记录到数据库中。

## intent

- 新增客户
- 创建客户
- 添加客户
- insert customer

## input_schema

```yaml
customer_name:
  type: string
  required: true
  description: 客户名称

email:
  type: string
  required: true
  description: 客户邮箱

phone:
  type: string
  required: false
  description: 客户电话
```

## output_schema

```yaml
affectedRows:
  type: number
  description: 影响行数
generatedKey:
  type: number
  description: 自增主键ID
message:
  type: string
  description: 操作结果描述
```

## steps

### step: insert_customer

**type**: tool
**tool**: db_insert

```yaml
input:
  datasource: "main_db"
  table: "customers"
  fields:
    name: "{{customer_name}}"
    email: "{{email}}"
    phone: "{{phone}}"
output_schema:
  affectedRows:
    type: number
    description: 影响行数
  generatedKey:
    type: number
    description: 自增主键ID
```

### step: build_result

**type**: template  **varName**: message

```template
客户 {{customer_name}} 创建成功，ID: {{generatedKey}}
```
