# Aegis Skill Web - 前端项目

## 📋 项目概述

这是 Aegis Skill 平台的前端应用，提供用户友好的界面来管理和执行 AI 技能。

- **技术栈**: React 18 + TypeScript + Vite
- **UI 框架**: Tailwind CSS (通过 CDN)
- **图表库**: Recharts
- **图标库**: Lucide React
- **Markdown 渲染**: React Markdown

---

## 🚀 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问: http://localhost:3000

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

---

## ⚠️ 前后端对齐说明

### 当前状态

前端项目从 `Skill-Engine` 迁移并重命名为 `skill-web`，但存在以下功能差异：

#### ✅ 已完成的修改

1. **项目名称更新**
   - package.json: `name` 字段从 "skill-engine" 改为 "skill-web"
   - 添加项目描述

2. **代码注释**
   - 所有核心文件添加了详细的中文注释
   - 使用 JSDoc 标准注释
   - 标注了前后端差异（使用特殊标签）

3. **前后端对比文档**
   - 创建了 `FRONTEND_BACKEND_DIFF.md` 详细说明差异

#### ⚠️ 前端超前的功能（后端未实现）

| 功能 | 前端调用 | 后端状态 | 优先级 |
|------|---------|---------|-------|
| 获取技能列表 | `GET /api/skill/skills` | ❌ 未实现 | 🔴 高 |
| 多轮交互恢复 | `POST /api/skill/resume` | ❌ 未实现 | 🟡 中 |

#### 🔄 接口参数不一致

| 功能 | 前端期望 | 后端实际 | 影响 |
|------|---------|---------|------|
| 执行技能 | `{skillId, inputs}` | `{skillMarkdown, inputs}` | 🔴 严重 |

---

## 📁 项目结构

```
skill-web/
├── components/          # React 组件
│   ├── DynamicForm.tsx     # 动态表单组件
│   └── ResultRenderer.tsx  # 结果渲染组件
├── services/            # API 服务层
│   ├── skillService.ts    # 技能 API 封装
│   └── mockData.ts        # Mock 数据（临时使用）
├── App.tsx              # 主应用组件
├── types.ts             # TypeScript 类型定义
├── vite.config.ts       # Vite 配置（含代理）
├── package.json         # 项目依赖
└── index.html           # HTML 入口
```

---

## 🔧 配置说明

### Vite 代理配置

开发环境下，前端在 `localhost:3000`，后端在 `localhost:8080`。

代理配置在 `vite.config.ts` 中：

```typescript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
    secure: false,
  }
}
```

所有 `/api/*` 请求会自动转发到后端。

---

## 📝 代码标注说明

为了方便识别前端超前的功能，我们在代码中使用以下标签：

### 标签列表

- `⚠️ [FRONTEND-AHEAD]` - 前端超前功能，后端尚未实现
- `🔄 [NEEDS-ALIGNMENT]` - 需要对齐，前后端接口不一致
- `✅ [ALIGNED]` - 已对齐，前后端一致

### 示例

```typescript
/**
 * 获取技能列表
 *
 * ⚠️ [FRONTEND-AHEAD] 前端超前功能
 * - 后端尚未实现 GET /api/skill/skills 接口
 * - 临时方案：使用 Mock 数据
 */
async getSkills(): Promise<Skill[]> {
  // ...
}
```

---

## 🛠️ 临时解决方案

由于前后端接口不一致，前端需要以下临时处理：

### 1. 使用 Mock 数据

在 `services/mockData.ts` 中定义 Mock 技能数据：

```typescript
export const mockSkills: Skill[] = [
  {
    id: 'financial_analysis',
    description: '财务数据分析',
    intents: ['finance', 'analysis'],
    inputSchema: {
      fields: {
        company: { type: 'string', required: true, description: '公司代码' },
        year: { type: 'number', required: true, description: '年份' }
      }
    }
  }
];
```

### 2. 修改 API 调用

在 `skillService.ts` 中临时修改：

```typescript
async getSkills(): Promise<Skill[]> {
  // ⚠️ 临时使用 Mock 数据
  return mockSkills;

  // 正常实现（后端完成后启用）：
  // const response = await fetch('/api/skill/skills');
  // return await response.json();
}
```

### 3. 参数格式转换

如果后端要求 `skillMarkdown`，前端需要转换：

```typescript
async executeSkill(skillId: string, inputs: any) {
  // 查找对应的 skillMarkdown
  const skill = mockSkills.find(s => s.id === skillId);
  const skillMarkdown = skill ? skill.markdown : '';

  return fetch('/api/skill/execute', {
    method: 'POST',
    body: JSON.stringify({ skillMarkdown, inputs })
  });
}
```

---

## 📋 后端待实现功能清单

### 高优先级（必须实现）

- [ ] **实现 `GET /api/skill/skills` 接口**
  ```java
  @GetMapping("/skills")
  public List<SkillMetadata> listSkills() {
      // 返回所有可用技能的元数据
  }
  ```

- [ ] **修改 `POST /api/skill/execute` 支持 skillId**
  ```java
  @PostMapping("/execute")
  public SkillExecuteResponse executeById(@RequestParam String skillId,
                                          @RequestBody Map<String, Object> inputs) {
      // 根据 skillId 查找并执行技能
  }
  ```

### 中优先级（增强功能）

- [ ] **实现会话管理**
  ```java
  @Service
  public class ExecutionManager {
      private Map<String, ExecutionContext> sessions;

      public SkillExecuteResponse resume(String executionId,
                                          Map<String, Object> userInput) {
          // 恢复会话并继续执行
      }
  }
  ```

- [ ] **实现 `POST /api/skill/resume` 接口**
  ```java
  @PostMapping("/resume")
  public SkillExecuteResponse resume(@RequestBody ResumeRequest request) {
      // 恢复多轮交互执行
  }
  ```

### 低优先级（优化体验）

- [ ] 扩展 `SkillExecuteResponse` DTO
  - 添加 `executionId` 字段
  - 添加 `awaitMessage` 字段
  - 添加 `inputSchema` 字段（中间步骤）

---

## 🎯 开发建议

### 前端开发者

1. **使用 Mock 数据开发**
   - 先实现 UI 和交互逻辑
   - 使用 `services/mockData.ts` 中的数据
   - 等后端接口完成后再对接

2. **错误处理**
   - 所有 API 调用都使用 try-catch
   - 向用户显示友好的错误提示

3. **加载状态**
   - 使用 `isLoading` 状态显示加载动画
   - 提供良好的用户反馈

### 后端开发者

1. **优先实现高优先级接口**
   - GET /api/skill/skills
   - 修改 /api/skill/execute 支持 skillId

2. **参考前端类型定义**
   - 查看 `types.ts` 中的接口定义
   - 确保返回格式一致

3. **测试接口**
   - 使用 Postman 或 curl 测试
   - 验证参数格式和返回值

---

## 📚 相关文档

- [前后端差异详细分析](./FRONTEND_BACKEND_DIFF.md)
- [后端 API 文档](../aegis-skill-api/README.md)
- [Vite 官方文档](https://vitejs.dev/)

---

## 🤝 贡献指南

1. 确保所有代码都有中文注释
2. 使用 JSDoc 标准注释接口和组件
3. 如需使用超前功能，添加 `⚠️ [FRONTEND-AHEAD]` 标签
4. 提交前测试前后端对接

---

## 📞 联系方式

如有问题或建议，请联系项目维护者。

---

**最后更新**: 2026-02-06
**版本**: 1.0.0
