# skill: simple_tool_test

## description

A simple skill with one tool step for testing.

## input

```yaml
query: string
```

## steps

### step: search

**type**: tool
**tool**: search_api

```yaml
q: "{{query}}"
```

## output

```json
{
  "results": "array"
}
```
