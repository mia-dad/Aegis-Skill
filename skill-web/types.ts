// ============================================
// 类型定义文件
// ============================================
// 本文件定义了 Aegis Skill 平台前端的所有 TypeScript 类型接口
// 这些类型与后端 API 保持一致，确保前后端数据交换的规范性
// ============================================

// ============================================
// Skill 相关类型定义
// 对应后端: com.mia.aegis.skill.dsl.model.Skill
// ============================================

/**
 * 技能定义接口
 *
 * 描述一个 AI 技能的元数据和输入参数规范
 *
 * @property id - 技能唯一标识符，必须符合命名规范（小写字母开头，只能包含字母、数字、下划线）
 * @property description - 技能描述，说明技能的功能和用途
 * @property intents - 意图列表，用于技能分类和搜索
 * @property inputSchema - 输入参数结构定义，描述执行该技能所需的参数
 */
/**
 * 输出 Schema 接口
 *
 * 描述技能执行结果的字段定义（平铺格式），用于元数据展示
 * 后端 API 直接返回字段定义 Map，不含 displayType 等前端渲染指令
 */
export type OutputSchema = Record<string, { type: string; description?: string; items?: any }>;

export interface Skill {
  id: string;
  version?: string;
  description: string;
  intents: string[];
  inputSchema: InputSchema;
  outputSchema?: OutputSchema;
}

/**
 * 输入参数结构接口
 *
 * 定义技能执行所需的输入参数集合
 *
 * @property fields - 字段映射表，key 为参数名，value 为字段规范
 */
export interface InputSchema {
  fields: Record<string, FieldSpec>;
}

/**
 * 字段规范接口
 *
 * 描述单个输入参数的详细定义
 *
 * @property type - 参数数据类型，支持：string(字符串)、number(数字)、boolean(布尔)、array(数组)、object(对象)
 * @property required - 是否必填，true 表示必须提供该参数
 * @property description - 参数描述，用于说明参数的含义和用途（可选）
 * @property placeholder - 输入占位符，在 UI 中显示的提示文本（可选）
 * @property defaultValue - 默认值，当用户未输入时使用的值（可选）
 * @property options - 可选值列表，用于 select 或 radio 类型的输入（可选）
 * @property uiHint - UI 渲染提示，指导前端如何渲染该字段（可选）
 * @property validation - 验证规则，定义参数的校验约束（可选）
 */
export interface FieldSpec {
  type: 'string' | 'number' | 'boolean' | 'array' | 'object';
  required: boolean;
  description?: string;
  placeholder?: string;
  defaultValue?: any;
  options?: string[]; // 用于下拉选择或单选框
  uiHint?: 'text' | 'textarea' | 'select' | 'multiselect' | 'checkbox' | 'radio' | 'date' | 'datetime' | 'number' | 'switch';
  validation?: ValidationRule;
}

/**
 * 验证规则接口
 *
 * 定义参数的校验约束，用于前端表单验证
 *
 * @property pattern - 正则表达式模式，用于字符串格式验证（可选）
 * @property min - 最小值约束，适用于数字、数组长度等（可选）
 * @property max - 最大值约束，适用于数字、数组长度等（可选）
 * @property minItems - 数组最小元素数量（可选）
 * @property maxItems - 数组最大元素数量（可选）
 * @property message - 验证失败时的错误提示消息（可选）
 */
export interface ValidationRule {
  pattern?: string;
  min?: number;
  max?: number;
  minItems?: number;
  maxItems?: number;
  message?: string;
}

// ============================================
// Tool 相关类型定义
// 对应后端: com.mia.skill.api.dto.ToolInfo
// ============================================

export interface ToolInfo {
  name: string;
  description: string;
  category?: string;
  tags?: string[];
  inputSchema?: Record<string, any>;
  outputSchema?: Record<string, any>;
  errorDescriptions?: Record<string, string>;
  implementationClass?: string;
}

// ============================================
// Response 相关类型定义
// 对应后端: com.mia.skill.api.dto.SkillExecuteResponse
// ============================================

/**
 * 技能执行响应接口
 *
 * 描述技能执行后的返回结果
 *
 * @property status - 执行状态
 *   - COMPLETED: 执行完成
 *   - FAILED: 执行失败
 *   - WAITING_FOR_INPUT: 等待用户输入（多轮交互场景）
 *   - IDLE: 空闲状态（前端内部状态）
 * @property success - 是否成功
 * @property skillId - 执行的技能 ID
 * @property output - 输出结果，成功时包含执行结果（可选）
 * @property error - 错误信息，失败时包含错误描述（可选）
 * @property executionId - 执行 ID，用于多轮交互场景，标识唯一执行会话（可选）
 * @property awaitMessage - 等待消息，当状态为 WAITING_FOR_INPUT 时，提示用户需要输入的内容（可选）
 * @property inputSchema - 中间步骤的输入参数定义，用于多轮交互中动态获取后续参数（可选）
 * @property durationMs - 执行耗时，单位：毫秒
 */
export interface SkillExecuteResponse {
  status: 'COMPLETED' | 'FAILED' | 'WAITING_FOR_INPUT' | 'IDLE'; // IDLE 用于前端内部状态
  success: boolean;
  skillId: string;
  version?: string;
  output?: any;
  error?: string;
  executionId?: string;
  awaitMessage?: string;
  awaitSchema?: Record<string, FieldSpec>;
  durationMs: number;
}

// ============================================
// 内部应用状态类型定义
// 用于前端状态管理
// ============================================

/**
 * 执行状态接口
 *
 * 管理技能执行过程中的前端状态
 *
 * @property status - 当前执行状态，参考 SkillExecuteResponse.status
 * @property loading - 是否正在加载，用于显示加载动画
 * @property executionId - 当前执行会话 ID（可选）
 * @property currentSchema - 当前需要的输入参数定义，用于多轮交互（可选）
 * @property awaitMessage - 等待用户输入的提示消息（可选）
 * @property result - 执行结果数据（可选）
 * @property error - 错误信息（可选）
 */
export interface ExecutionState {
  status: SkillExecuteResponse['status'];
  loading: boolean;
  executionId?: string;
  currentSchema?: InputSchema;
  awaitMessage?: string;
  result?: any;
  error?: string;
}

// ============================================
// Validation 相关类型定义
// 对应后端: com.mia.aegis.skill.dsl.validator.report.*
// ============================================

export type ValidationLevel = 'ERROR' | 'WARNING' | 'SUGGESTION';
export type ValidationCategory = 'SYNTAX' | 'SCHEMA' | 'LOGIC' | 'TOOL' | 'DATA_FLOW';

export interface ValidationIssue {
  code: string;
  level: ValidationLevel;
  category: ValidationCategory;
  message: string;
  location: string;
  suggestion: string;
}

export interface SkillSummary {
  skillId: string;
  version: string;
  description: string;
  stepCount: number;
  stepTypes: string[];
  inputFieldCount: number;
  outputFieldCount: number;
  hasConditionalSteps: boolean;
  hasAwaitSteps: boolean;
}

export interface SkillValidationReport {
  valid: boolean;
  summary: SkillSummary | null;
  issues: ValidationIssue[];
  validationTimeMs: number;
  errorCount: number;
  warningCount: number;
  suggestionCount: number;
  errors: ValidationIssue[];
  warnings: ValidationIssue[];
  suggestions: ValidationIssue[];
}
