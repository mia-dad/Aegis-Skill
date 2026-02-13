// ============================================
// 技能服务模块
// ============================================
// 本文件封装了与后端 API 交互的所有逻辑
// 提供技能列表查询、执行、恢复执行等功能
// ============================================

import { Skill, SkillExecuteResponse, ToolInfo, SkillValidationReport } from '../types';

// ============================================
// API 配置
// ============================================

/**
 * API 基础路径
 *
 * 使用空字符串，因为 Vite 代理会自动将请求转发到后端
 * 请求流程：
 * 1. 前端发起: localhost:3000/api/skill/xxx
 * 2. Vite 代理转发: localhost:8080/api/skill/xxx
 * 3. 后端处理并返回
 *
 * 代理配置在 vite.config.ts 中定义
 */
const API_BASE_URL = '';


// ============================================
// 技能服务对象
// ============================================

/**
 * 技能服务对象
 *
 * 封装所有与技能相关的 API 调用
 */
export const skillService = {

  /**
   * 获取技能列表
   *
   * ⚠️ [FRONTEND-AHEAD] 前端超前功能
   * - 后端尚未实现 GET /api/skill/skills 接口
   * - 前端需要在启动时加载所有可用技能，展示在侧边栏
   * - 临时方案：使用 Mock 数据（参见 mockData.ts）
   *
   * @returns {Promise<Skill[]>} 技能列表，每个技能包含 id、description、intents、inputSchema
   *
   * @example
   * const skills = await skillService.getSkills();
   * console.log(skills); // [{ id: 'my_skill', description: '...', ... }]
   *
   * @throws {Error} 当网络请求失败或后端返回错误时抛出异常
   */
  async getSkills(): Promise<Skill[]> {
    try {
      const response = await fetch(`${API_BASE_URL}/api/skill/skills`);

      if (!response.ok) {
        throw new Error(`Failed to fetch skills: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('API Error:', error);
      throw error;
    }
  },

  /**
   * 执行技能
   *
   * 执行指定技能，支持可选的版本号。
   *
   * 请求格式：
   * {
   *   "skillId": "my_skill",
   *   "version": "1.0.0",  // 可选，不传则使用最大版本
   *   "inputs": { "param1": "value1" }
   * }
   *
   * @param {string} skillId - 技能 ID，用于标识要执行的技能
   * @param {any} inputs - 输入参数对象，key 为参数名，value 为参数值
   * @param {string} [version] - 技能版本号（可选），不传则使用该 skillId 的最大版本
   * @returns {Promise<SkillExecuteResponse>} 执行响应，包含状态、结果、错误信息等
   *
   * @example
   * const response = await skillService.executeSkill('financial_analysis', {
   *   company: 'AAPL',
   *   year: 2024
   * });
   *
   * console.log(response);
   * // {
   * //   status: 'COMPLETED',
   * //   success: true,
   * //   output: { ... },
   * //   durationMs: 1234
   * // }
   *
   * @throws {Error} 当网络请求失败、后端返回错误或执行失败时抛出异常
   */
  async executeSkill(skillId: string, inputs: any, version?: string): Promise<SkillExecuteResponse> {
    try {
      const requestBody: any = { skillId, inputs };
      if (version) {
        requestBody.version = version;
      }

      const response = await fetch(`${API_BASE_URL}/api/skill/execute`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Execution failed: ${response.status} - ${errorText || response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('API Error:', error);
      throw error;
    }
  },

  /**
   * 恢复执行（多轮交互）
   *
   * ⚠️ [FRONTEND-AHEAD] 前端超前功能
   * - 后端尚未实现 POST /api/skill/resume 接口
   * - 用于处理需要多次用户输入的技能场景
   * - 例如：输入公司 → 分析中 → 需要确认年份 → 继续分析
   *
   * 工作流程：
   * 1. 首次调用 executeSkill，后端返回 WAITING_FOR_INPUT 状态
   * 2. 前端显示新的输入表单（基于 response.inputSchema）
   * 3. 用户填写后调用 resumeExecution，传入 executionId 和 userInput
   * 4. 后端恢复执行上下文，继续处理
   *
   * @param {string} executionId - 执行会话 ID，由后端在首次执行时返回
   * @param {string} skillId - 技能 ID，标识正在执行的技能
   * @param {any} userInput - 用户在中间步骤输入的数据
   * @param {string} [version] - 技能版本号（可选）
   * @returns {Promise<SkillExecuteResponse>} 更新后的执行响应
   *
   * @example
   * // 首次执行
   * let response = await skillService.executeSkill('multi_step_skill', initialInput);
   *
   * // 检查是否需要继续输入
   * if (response.status === 'WAITING_FOR_INPUT') {
   *   // 显示新表单，用户填写后
   *   const userInput = { year: 2024 };
   *
   *   // 恢复执行
   *   response = await skillService.resumeExecution(
   *     response.executionId,
   *     'multi_step_skill',
   *     userInput
   *   );
   * }
   *
   * @throws {Error} 当网络请求失败或恢复执行失败时抛出异常
   */
  async getTools(category?: string): Promise<ToolInfo[]> {
    try {
      const url = category
        ? `${API_BASE_URL}/api/tools?category=${encodeURIComponent(category)}`
        : `${API_BASE_URL}/api/tools`;
      const response = await fetch(url);

      if (!response.ok) {
        throw new Error(`Failed to fetch tools: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('API Error:', error);
      throw error;
    }
  },

  async getToolCategories(): Promise<string[]> {
    try {
      const response = await fetch(`${API_BASE_URL}/api/tools/categories`);

      if (!response.ok) {
        throw new Error(`Failed to fetch tool categories: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('API Error:', error);
      throw error;
    }
  },

  async validateAllSkills(): Promise<SkillValidationReport[]> {
    try {
      const response = await fetch(`${API_BASE_URL}/api/validate/skills`);
      if (!response.ok) {
        throw new Error(`Failed to validate skills: ${response.statusText}`);
      }
      return await response.json();
    } catch (error) {
      console.error('API Error:', error);
      throw error;
    }
  },

  async validateMarkdown(markdown: string): Promise<SkillValidationReport> {
    try {
      const response = await fetch(`${API_BASE_URL}/api/validate/skill`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ markdown }),
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Validation failed: ${response.status} - ${errorText || response.statusText}`);
      }
      return await response.json();
    } catch (error) {
      console.error('API Error:', error);
      throw error;
    }
  },

  async validateSkill(skillId: string, version?: string): Promise<SkillValidationReport> {
    try {
      const url = version
        ? `${API_BASE_URL}/api/validate/skill/${encodeURIComponent(skillId)}?version=${encodeURIComponent(version)}`
        : `${API_BASE_URL}/api/validate/skill/${encodeURIComponent(skillId)}`;
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`Failed to validate skill: ${response.statusText}`);
      }
      return await response.json();
    } catch (error) {
      console.error('API Error:', error);
      throw error;
    }
  },

  async resumeExecution(executionId: string, skillId: string, userInput: any, version?: string): Promise<SkillExecuteResponse> {
    try {
      const requestBody: any = { executionId, skillId, inputs: userInput };
      if (version) {
        requestBody.version = version;
      }

      const response = await fetch(`${API_BASE_URL}/api/skill/resume`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Resume failed: ${response.status} - ${errorText || response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('API Error:', error);
      throw error;
    }
  }
};


// ============================================
// 使用说明
// ============================================

/**
 * 💡 前端开发注意事项
 *
 * 1. **错误处理**
 *    所有方法都可能抛出异常，建议使用 try-catch 包裹
 *
 *    try {
 *      const skills = await skillService.getSkills();
 *    } catch (error) {
 *      console.error('加载技能失败', error);
 *      // 显示错误提示给用户
 *    }
 *
 * 2. **前后端对齐问题**
 *    - 当前 getSkills() 会失败（后端未实现），建议使用 Mock 数据
 *    - executeSkill() 参数格式不匹配，需要转换逻辑
 *    - resumeExecution() 暂时不可用
 *
 * 3. **临时降级方案**
 *    在 services/mockData.ts 中定义 Mock 技能数据，
 *    前端启动时优先使用 Mock 数据
 *
 * 4. **Vite 代理配置**
 *    确保后端在 localhost:8080 运行
 *    否则 /api 请求会失败
 *    代理配置在 vite.config.ts 中
 */
