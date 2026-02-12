# skill: chat

## version

1.0.0

## description

通用对话 Skill，用于回答用户的各类问题。

## intent

- 聊天
- 问答
- chat
- question

## input_schema

```yaml
prompt: string
```

## output_schema

```yaml
content:
  type: string
  description: AI 助手回答内容
```

## steps

### step: answer

**type**: prompt
**varName**: content

```prompt
你是一个友好的AI助手。请回答用户的问题。

用户问题：{{prompt}}

请给出简洁、准确的回答。
```
