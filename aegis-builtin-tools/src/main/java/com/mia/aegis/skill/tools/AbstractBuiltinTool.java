package com.mia.aegis.skill.tools;

import com.mia.aegis.skill.tools.config.BuiltinToolsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 内置 Tool 抽象基类。
 *
 * <p>提供所有内置 Tool 共享的通用功能：</p>
 * <ul>
 *   <li>输入参数校验</li>
 *   <li>路径安全校验（Canonical Path + 白名单）</li>
 *   <li>工作目录解析</li>
 * </ul>
 *
 * @author chenzhixuan
 */
public abstract class AbstractBuiltinTool implements ToolProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final BuiltinToolsConfig config;

    /**
     * 构造函数。
     *
     * @param config 内置 Tools 配置
     */
    protected AbstractBuiltinTool(BuiltinToolsConfig config) {
        this.config = config;
    }

    /**
     * 校验输入参数。
     *
     * @param input 输入参数 Map
     * @param requiredKeys 必需的参数名列表
     * @throws IllegalArgumentException 如果缺少必需参数或参数类型错误
     */
    protected void validateInput(Map<String, Object> input, String... requiredKeys) {
        if (input == null) {
            logger.warn("输入参数为空");
            throw new IllegalArgumentException("输入参数不能为空");
        }

        for (String key : requiredKeys) {
            if (!input.containsKey(key)) {
                logger.warn("缺少必需参数: {}", key);
                throw new IllegalArgumentException("缺少必需参数: " + key);
            }
            Object value = input.get(key);
            if (value == null) {
                logger.warn("参数值为 null: {}", key);
                throw new IllegalArgumentException("参数值不能为 null: " + key);
            }
        }
    }

    /**
     * 校验文件路径安全性。
     *
     * <p>使用 Canonical Path 防止路径遍历攻击，并检查路径是否在允许的工作目录下。</p>
     *
     * @param path 待校验的路径
     * @param workingDirectory 允许的工作目录
     * @return 规范化后的绝对路径
     * @throws SecurityException 如果路径不安全（路径遍历攻击等）
     * @throws IllegalArgumentException 如果路径为空
     */
    protected String validatePath(String path, String workingDirectory) {
        if (path == null || path.trim().isEmpty()) {
            logger.warn("路径参数为空");
            throw new IllegalArgumentException("路径不能为空");
        }

        try {
            // 解析工作目录的规范路径
            File workDir = new File(workingDirectory);
            String canonicalWorkDir = workDir.getCanonicalPath();

            // 解析目标路径（可能是相对路径或绝对路径）
            File targetFile;
            if (new File(path).isAbsolute()) {
                targetFile = new File(path);
            } else {
                targetFile = new File(workingDirectory, path);
            }
            String canonicalTarget = targetFile.getCanonicalPath();

            // 检查目标路径是否在工作目录下（白名单校验）
            if (!canonicalTarget.startsWith(canonicalWorkDir)) {
                logger.warn("路径安全校验失败: 目标路径 [{}] 不在允许的工作目录 [{}] 下", path, workingDirectory);
                throw new SecurityException(
                        "路径安全校验失败: 目标路径 [" + path + "] 不在允许的工作目录 [" + workingDirectory + "] 下");
            }

            logger.debug("路径校验通过: {} -> {}", path, canonicalTarget);
            return canonicalTarget;

        } catch (IOException e) {
            logger.error("路径规范化失败: {}", e.getMessage());
            throw new SecurityException("路径规范化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 校验文件路径安全性（多目录白名单）。
     *
     * <p>检查路径是否在任一允许的工作目录下。
     * 对于相对路径，使用列表中第一个目录作为基准进行解析。</p>
     *
     * @param path 待校验的路径
     * @param workingDirectories 允许的工作目录列表
     * @return 规范化后的绝对路径
     * @throws SecurityException 如果路径不在任何允许的目录内
     * @throws IllegalArgumentException 如果路径为空或目录列表为空
     */
    protected String validatePath(String path, List<String> workingDirectories) {
        if (path == null || path.trim().isEmpty()) {
            logger.warn("路径参数为空");
            throw new IllegalArgumentException("路径不能为空");
        }
        if (workingDirectories == null || workingDirectories.isEmpty()) {
            logger.warn("工作目录列表为空");
            throw new IllegalArgumentException("工作目录列表不能为空");
        }

        try {
            // 解析目标路径
            File targetFile;
            if (new File(path).isAbsolute()) {
                targetFile = new File(path);
            } else {
                // 相对路径使用第一个目录作为基准
                targetFile = new File(workingDirectories.get(0), path);
            }
            String canonicalTarget = targetFile.getCanonicalPath();

            // 检查目标路径是否在任一允许的目录下
            List<String> canonicalDirs = new ArrayList<String>();
            for (String dir : workingDirectories) {
                String canonicalDir = new File(dir).getCanonicalPath();
                if (!canonicalDir.endsWith(File.separator)) {
                    canonicalDir = canonicalDir + File.separator;
                }
                if (canonicalTarget.startsWith(canonicalDir) || canonicalTarget.equals(canonicalDir.substring(0, canonicalDir.length() - 1))) {
                    logger.debug("路径校验通过: {} -> {} (匹配目录: {})", path, canonicalTarget, dir);
                    return canonicalTarget;
                }
                canonicalDirs.add(canonicalDir);
            }

            logger.warn("路径安全校验失败: 目标路径 [{}] 不在任何允许的工作目录下 {}", path, workingDirectories);
            throw new SecurityException(
                    "路径安全校验失败: 目标路径 [" + path + "] 不在允许的工作目录 " + workingDirectories + " 下");

        } catch (IOException e) {
            logger.error("路径规范化失败: {}", e.getMessage());
            throw new SecurityException("路径规范化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 校验文件扩展名。
     *
     * @param path 文件路径
     * @param allowedExtensions 允许的扩展名列表
     * @throws SecurityException 如果扩展名不在允许列表中
     */
    protected void validateExtension(String path, List<String> allowedExtensions) {
        if (allowedExtensions == null || allowedExtensions.isEmpty()) {
            return; // 未配置限制，允许所有
        }

        String extension = getFileExtension(path);
        if (extension.isEmpty()) {
            logger.warn("文件无扩展名: {}", path);
            throw new SecurityException("文件必须有扩展名");
        }

        boolean allowed = false;
        for (String ext : allowedExtensions) {
            if (ext.equalsIgnoreCase(extension)) {
                allowed = true;
                break;
            }
        }

        if (!allowed) {
            logger.warn("不允许的文件扩展名: {}, 允许的扩展名: {}", extension, allowedExtensions);
            throw new SecurityException(
                    "不允许的文件扩展名: " + extension + "，允许的扩展名: " + allowedExtensions);
        }
    }

    /**
     * 解析工作目录。
     *
     * <p>优先使用输入参数中的 workingDirectory，否则使用配置的默认工作目录。</p>
     *
     * @param input 输入参数
     * @param defaultWorkingDirectory 默认工作目录
     * @return 解析后的工作目录路径
     */
    protected String resolveWorkingDirectory(Map<String, Object> input, String defaultWorkingDirectory) {
        Object customDir = input.get("workingDirectory");
        if (customDir != null && customDir instanceof String) {
            String customDirStr = (String) customDir;
            if (!customDirStr.trim().isEmpty()) {
                return customDirStr.trim();
            }
        }
        return defaultWorkingDirectory;
    }

    /**
     * 获取文件扩展名（不含点号）。
     *
     * @param path 文件路径
     * @return 扩展名（小写），如果无扩展名则返回空字符串
     */
    protected String getFileExtension(String path) {
        if (path == null) {
            return "";
        }
        int lastDot = path.lastIndexOf('.');
        int lastSeparator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (lastDot > lastSeparator && lastDot < path.length() - 1) {
            return path.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 校验文件大小。
     *
     * @param file 文件对象
     * @param maxSize 最大允许大小（字节）
     * @throws IllegalArgumentException 如果文件过大
     */
    protected void validateFileSize(File file, long maxSize) {
        if (file.length() > maxSize) {
            logger.warn("文件过大: {} 字节，最大允许: {} 字节, 文件: {}", file.length(), maxSize, file.getAbsolutePath());
            throw new IllegalArgumentException(
                    "文件过大: " + file.length() + " 字节，最大允许: " + maxSize + " 字节");
        }
    }

    /**
     * 确保目录存在，如不存在则创建。
     *
     * @param directory 目录路径
     * @throws IOException 如果无法创建目录
     */
    protected void ensureDirectoryExists(File directory) throws IOException {
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                logger.error("无法创建目录: {}", directory.getAbsolutePath());
                throw new IOException("无法创建目录: " + directory.getAbsolutePath());
            }
            logger.debug("已创建目录: {}", directory.getAbsolutePath());
        } else if (!directory.isDirectory()) {
            logger.error("路径已存在但不是目录: {}", directory.getAbsolutePath());
            throw new IOException("路径已存在但不是目录: " + directory.getAbsolutePath());
        }
    }
}

