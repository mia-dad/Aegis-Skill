# skill: db_query_customers

## version

1.0.0

## description

查询客户列表，支持按条件过滤、排序和分页。

## intent

- 查询客户
- 客户列表
- 搜索客户
- query customers

## input_schema

```yaml
customer_name:
  type: string
  required: false
  description: 按客户名称过滤（精确匹配）

limit:
  type: number
  required: false
  description: 返回记录数上限
  default: 20
```

## output_schema

```yaml
rows:
  type: array
  description: 客户记录列表
  items:
    id:
      type: number
      description: 客户ID
    name:
      type: string
      description: 客户名称
    email:
      type: string
      description: 客户邮箱
    phone:
      type: string
      description: 客户电话
rowCount:
  type: number
  description: 返回的记录数
summary:
  type: string
  description: 查询结果摘要
```

## steps

### step: query_customers

**type**: tool
**tool**: db_select

```yaml
input:
  datasource: "main_db"
  table: "customers"
  columns:
    - id
    - name
    - email
    - phone
  where:
    name: "{{customer_name}}"
  orderBy: "id ASC"
  limit: "{{limit}}"
output_schema:
  rows:
    type: array
    description: 客户记录列表
  rowCount:
    type: number
    description: 返回的记录数
```

### step: build_summary

**type**: template  **varName**: summary

```template
共查询到 {{rowCount}} 条客户记录
```
