# 前后端功能差异分析

## 📊 概述

本文档记录了 `skill-web` 前端与当前后端（`aegis-skill-api`）之间的功能差异。

**生成时间**: 2026-02-06
**后端版本**: 当前 aegis 项目
**前端版本**: 从 Skill-Engine 迁移的 skill-web

---

## 🔍 核心差异

### 1️⃣ **API 接口差异**

#### ✅ 已对齐的功能

| 功能 | 前端调用 | 后端实现 | 状态 |
|------|---------|---------|------|
| 执行技能 | `POST /api/skill/execute` | ✅ 已实现 | ⚠️ **参数格式不同** |

#### ❌ 前端超前的功能（后端未实现）

| 功能 | 前端调用 | 后端实现 | 优先级 | 说明 |
|------|---------|---------|-------|------|
| **获取技能列表** | `GET /api/skill/skills` | ❌ 未实现 | 🔴 高 | 前端启动时调用，用于展示可用技能列表 |
| **恢复执行（多轮交互）** | `POST /api/skill/resume` | ❌ 未实现 | 🟡 中 | 支持需要多次用户输入的技能场景 |

---

## 🔴 **关键差异详解**

### 差异 1: 执行技能 API 参数不匹配

#### 前端期望格式
```json
POST /api/skill/execute
{
  "skillId": "my_skill",        // ← 前端传递 skillId
  "inputs": {
    "param1": "value1"
  }
}
```

#### 后端实际格式
```json
POST /api/skill/execute
{
  "skillMarkdown": "# skill: my_skill\n...",  // ← 后端需要完整的 Markdown 文本
  "inputs": {
    "param1": "value1"
  },
  "adapter": "dashscope"  // 可选
}
```

**影响**: 🔴 **严重** - 前端无法直接调用后端 API

**解决方案**:
1. **方案A（推荐）**: 后端新增 `GET /api/skill/skills` 接口
   - 返回技能列表，包含 skillId 和其他元数据
   - 修改 `/api/skill/execute` 接受 skillId 参数

2. **方案B**: 前端改为传递 skillMarkdown
   - 需要前端存储所有技能的完整 Markdown
   - 前端需要实现技能库管理功能

---

### 差异 2: 技能列表接口缺失

#### 前端调用
```typescript
// skillService.ts
async getSkills(): Promise<Skill[]> {
  const response = await fetch(`${API_BASE_URL}/api/skill/skills`);
  return await response.json();
}
```

#### 前端期望的返回格式
```json
[
  {
    "id": "my_skill",
    "description": "我的技能描述",
    "intents": ["intent1", "intent2"],
    "inputSchema": {
      "fields": {
        "param1": {
          "type": "string",
          "required": true,
          "description": "参数描述"
        }
      }
    }
  }
]
```

#### 后端状态
❌ **未实现** - 当前后端没有提供技能列表查询接口

**影响**: 🔴 **严重** - 前端启动时会加载失败

**解决方案**:
```java
// SkillController.java (需要新增)
@GetMapping("/skills")
public ResponseEntity<List<SkillInfo>> getSkills() {
    // TODO: 从数据库或文件系统加载所有技能
    // 返回技能元数据列表
}
```

---

### 差异 3: 多轮交互（Resume）功能

#### 前端调用
```typescript
async resumeExecution(executionId: string, skillId: string, userInput: any) {
  const response = await fetch(`${API_BASE_URL}/api/skill/resume`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ executionId, skillId, userInput })
  });
}
```

#### 前端使用场景
- 技能执行过程中需要多次用户输入
- 例如：先输入公司名称 → 分析中 → 需要确认年份 → 继续分析

#### 后端状态
❌ **未实现** - 当前后端不支持会话管理和多轮交互

**影响**: 🟡 **中等** - 单次执行的技能不受影响

**实现要点**:
1. 需要引入 **会话管理** 机制
2. 使用 `executionId` 跟踪执行状态
3. 支持中间状态返回（`WAITING_FOR_INPUT`）
4. 后端需要存储执行上下文

---

## 📋 前端类型定义 vs 后端 DTO

### 前端类型定义 (types.ts)

```typescript
// 前端期望
export interface SkillExecuteResponse {
  status: 'COMPLETED' | 'FAILED' | 'WAITING_FOR_INPUT' | 'IDLE';
  success: boolean;
  skillId: string;
  output?: any;
  error?: string;
  executionId?: string;      // ← 多轮交互需要
  awaitMessage?: string;     // ← 等待用户输入时的提示
  inputSchema?: Record<string, FieldSpec>;
  durationMs: number;
}
```

### 后端 DTO (SkillExecuteResponse.java)

```java
// 后端当前实现
public class SkillExecuteResponse {
    private String status;        // COMPLETED, FAILED
    private boolean success;
    private String skillId;
    private Object output;
    private String error;
    private Long durationMs;

    // ❌ 缺少字段：
    // - executionId
    // - awaitMessage
    // - inputSchema (中间步骤的输入参数定义)
}
```

---

## 🎯 优先级建议

### 🔴 高优先级（必须解决）

1. **统一执行 API 参数格式**
   - 后端支持 skillId 参数
   - 或前端改为传递 skillMarkdown

2. **实现技能列表接口**
   - `GET /api/skill/skills`
   - 返回可用技能的元数据

### 🟡 中优先级（影响部分功能）

3. **添加会话管理支持**
   - 实现 `POST /api/skill/resume`
   - 引入 Execution Context 持久化

### 🟢 低优先级（增强体验）

4. **扩展响应类型**
   - 添加 executionId、awaitMessage 等字段
   - 支持更丰富的执行状态

---

## 📌 标注说明

为了方便识别前端超前的功能，在代码中使用以下标注：

```typescript
// ⚠️ [FRONTEND-AHEAD] 前端超前：此功能后端尚未实现
// 🔄 [NEEDS-ALIGNMENT] 需要对齐：前后端接口不一致
// ✅ [ALIGNED] 已对齐：前后端一致
```

---

## 🚀 快速修复方案

### 临时方案（快速验证）

```typescript
// skillService.ts 临时修改
async getSkills(): Promise<Skill[]> {
  // ⚠️ [FRONTEND-AHEAD] 使用 Mock 数据，后端尚未实现
  return mockSkills;  // 从 mockData.ts 导入
}

async executeSkill(skillId: string, inputs: any) {
  // 🔄 [NEEDS-ALIGNMENT] 将 skillId 转换为 skillMarkdown
  const skillMarkdown = mockSkillsData[skillId];  // 查找预定义的 Markdown
  return fetch('/api/skill/execute', {
    method: 'POST',
    body: JSON.stringify({ skillMarkdown, inputs })  // 使用后端期望的格式
  });
}
```

### 长期方案（完整实现）

需要后端新增以下功能：

1. **SkillController 新增接口**
```java
@GetMapping("/skills")
public List<SkillMetadata> listSkills() { ... }

@PostMapping(value = "/execute", params = "byId")
public SkillExecuteResponse executeById(@RequestParam String skillId, @RequestBody Map<String, Object> inputs) { ... }
```

2. **会话管理模块**
```java
@Service
public class ExecutionManager {
    private Map<String, ExecutionContext> sessions = new ConcurrentHashMap<>();

    public SkillExecuteResponse resume(String executionId, Map<String, Object> userInput) {
        // 恢复会话并继续执行
    }
}
```

---

## 📝 TODO

- [ ] 后端实现 `GET /api/skill/skills` 接口
- [ ] 后端支持通过 skillId 执行技能
- [ ] 后端实现会话管理和 resume 功能
- [ ] 扩展 SkillExecuteResponse DTO
- [ ] 前端添加 API 错误处理和降级方案
