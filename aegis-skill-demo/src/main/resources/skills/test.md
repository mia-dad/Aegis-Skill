# skill: test

## version

1.0.0

**description**: 测试技能

## input_schema

```yaml
query: string
```

## output_schema

```yaml
content:
  type: string
  description: 测试输出
```

## steps

### step: answer

**type**: prompt
**varName**: content

```prompt
测试: {{query}}
```
