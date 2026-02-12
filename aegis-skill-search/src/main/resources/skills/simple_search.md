# skill: simple_search

## version

1.0.0

## description

简单搜索 Skill，用于搜索相关信息。

## intent

- 搜索
- search
- 查找
- 查询

## input_schema

```yaml
query: string
```

## output_schema

```yaml
executionTime:
  type: integer
  description: 执行时间（毫秒）
resultCount:
  type: integer
  description: 结果数量
query:
  type: string
  description: 搜索查询词
results:
  type: array
  description: 搜索结果列表
  items:
    id:
      type: integer
      description: 结果ID
    type:
      type: string
      description: 结果类型（如：web）
    title:
      type: string
      description: 标题
    url:
      type: string
      description: URL链接
    description:
      type: string
      description: 描述
    published_date:
      type: string
      description: 发布日期
    source:
      type: string
      description: 来源
    website:
      type: string
      description: 网站（可选）
    web_anchor:
      type: string
      description: 网页锚点
    icon:
      type: string
      description: 图标URL
    rerank_score:
      type: number
      description: 重排序分数
    authority_score:
      type: number
      description: 权威性分数
```

## steps

### step: search

**type**: tool
**tool**: builtin_web_search
**varName**: search_result

```yaml
query: "{{query}}"
```
