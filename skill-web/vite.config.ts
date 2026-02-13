// ============================================
// Vite 配置文件
// ============================================
// 本文件定义了 Vite 开发服务器的配置
// 包括 React 插件、开发服务器端口、API 代理等
// ============================================

import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * Vite 配置导出
 *
 * 配置说明：
 * - plugins: 使用 React 插件支持 JSX 和 React 开发
 * - server: 开发服务器配置
 *   - port: 前端开发服务器运行在 3000 端口
 *   - proxy: API 代理配置，将 /api 请求转发到后端
 *
 * 代理工作原理：
 * 1. 前端发起请求: http://localhost:3000/api/skill/execute
 * 2. Vite 检测到 /api 前缀，触发代理规则
 * 3. 请求被转发到: http://localhost:8080/api/skill/execute
 * 4. changeOrigin: 修改请求头的 origin，避免 CORS 问题
 * 5. secure: 不验证 SSL 证书（开发环境）
 *
 * 注意事项：
 * - 确保后端服务运行在 localhost:8080
 * - 如果后端端口不同，需要修改 target 配置
 * - 生产环境部署时，需要使用 Nginx 等反向代理
 */
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,  // 前端开发服务器端口

    // API 代理配置
    proxy: {
      // 代理所有以 /api 开头的请求到后端服务器
      '/api': {
        target: 'http://localhost:8080',  // 后端服务地址
        changeOrigin: true,                // 修改请求头的 origin
        secure: false,                     // 不验证 SSL 证书
      }
    }
  }
});
