package com.mia.aegis.skill.tools.config;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件访问配置。
 *
 * <p>管理文件 ID 到实际路径的映射，提供安全的间接引用机制。</p>
 *
 * <h3>安全机制</h3>
 * <ul>
 *   <li>间接引用：Skill 使用 fileId 而非直接路径</li>
 *   <li>白名单控制：只有注册的文件才能访问</li>
 *   <li>路径规范化：防止路径穿越攻击</li>
 *   <li>可选基础目录：限制访问范围</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * FileAccessConfig config = new FileAccessConfig();
 * config.register("report", "/data/reports/financial.json");
 * config.register("config", "/etc/app/settings.json");
 *
 * // Skill 中使用 fileId
 * String path = config.resolve("report");
 * }</pre>
 */
@Component

public class FileAccessConfig {

    private final Map<String, String> fileRegistry;
    private final String baseDirectory;

    /**
     * 构造文件访问配置（无基础目录限制）。
     */
    public FileAccessConfig() {
        this(null);
    }

    /**
     * 构造文件访问配置（带基础目录限制）。
     *
     * @param baseDirectory 基础目录，所有文件必须在此目录下
     */
    public FileAccessConfig(String baseDirectory) {
        this.fileRegistry = new HashMap<String, String>();
        this.baseDirectory = baseDirectory != null ? normalizeDirectory(baseDirectory) : null;
    }

    /**
     * 注册文件 ID 到路径的映射。
     *
     * @param fileId   文件标识（用于 Skill 引用）
     * @param filePath 实际文件路径
     * @throws IllegalArgumentException 如果 fileId 为空或路径无效
     * @throws SecurityException        如果路径不在基础目录内
     */
    public void register(String fileId, String filePath) {
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new IllegalArgumentException("fileId cannot be null or empty");
        }
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("filePath cannot be null or empty");
        }

        String normalizedPath = normalizePath(filePath);
        validatePathSecurity(normalizedPath);

        fileRegistry.put(fileId.trim(), normalizedPath);
    }

    /**
     * 解析文件 ID 为实际路径。
     *
     * @param fileId 文件标识
     * @return 实际文件路径
     * @throws IllegalArgumentException 如果 fileId 未注册
     */
    public String resolve(String fileId) {
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new IllegalArgumentException("fileId cannot be null or empty");
        }

        String path = fileRegistry.get(fileId.trim());
        if (path == null) {
            throw new IllegalArgumentException("Unknown fileId: " + fileId);
        }

        return path;
    }

    /**
     * 检查文件 ID 是否已注册。
     *
     * @param fileId 文件标识
     * @return 如果已注册返回 true
     */
    public boolean contains(String fileId) {
        return fileId != null && fileRegistry.containsKey(fileId.trim());
    }

    /**
     * 取消注册文件 ID。
     *
     * @param fileId 文件标识
     * @return 如果成功移除返回 true
     */
    public boolean unregister(String fileId) {
        if (fileId == null) {
            return false;
        }
        return fileRegistry.remove(fileId.trim()) != null;
    }

    /**
     * 获取所有已注册的文件 ID。
     *
     * @return 不可变的文件 ID 集合
     */
    public Map<String, String> getAllRegistrations() {
        return Collections.unmodifiableMap(new HashMap<String, String>(fileRegistry));
    }

    /**
     * 获取基础目录。
     *
     * @return 基础目录路径，如果未设置返回 null
     */
    public String getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * 清除所有注册。
     */
    public void clear() {
        fileRegistry.clear();
    }

    /**
     * 规范化文件路径。
     *
     * @param path 原始路径
     * @return 规范化后的绝对路径
     */
    private String normalizePath(String path) {
        try {
            File file = new File(path);
            return file.getCanonicalPath();
        } catch (IOException e) {
            // 无法规范化时返回绝对路径
            return new File(path).getAbsolutePath();
        }
    }

    /**
     * 规范化目录路径。
     */
    private String normalizeDirectory(String dir) {
        String normalized = normalizePath(dir);
        // 确保目录以分隔符结尾
        if (!normalized.endsWith(File.separator)) {
            normalized = normalized + File.separator;
        }
        return normalized;
    }

    /**
     * 验证路径安全性。
     *
     * @param normalizedPath 规范化后的路径
     * @throws SecurityException 如果路径不安全
     */
    private void validatePathSecurity(String normalizedPath) {
        // 检测路径穿越特征
        if (normalizedPath.contains("..")) {
            throw new SecurityException("Path traversal detected: " + normalizedPath);
        }

        // 如果设置了基础目录，验证路径在其内
        if (baseDirectory != null) {
            if (!normalizedPath.startsWith(baseDirectory)) {
                throw new SecurityException(
                        "Path outside allowed directory: " + normalizedPath +
                                " (allowed: " + baseDirectory + ")");
            }
        }
    }
}

