# skill: simple_tool_test

## version

1.0.0

## description

A simple skill with one tool step for testing.

## input_schema

```yaml
query: string
```

## output_schema

```yaml
result:
  type: object
  description: 搜索结果
```

## steps

### step: search

**type**: tool
**tool**: search_api

```yaml
q: "{{query}}"
output_schema:
  result:
    type: string
    description: 搜索结果
```
