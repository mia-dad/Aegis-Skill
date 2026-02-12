package com.mia.aegis.skill.tools.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 内置 Tools 配置类。
 *
 * <p>使用 Spring Boot ConfigurationProperties 注入配置。</p>
 *
 * <p>配置示例（application.properties）：</p>
 * <pre>{@code
 * aegis.builtin-tools.file.working-directory=/data/skills
 * aegis.builtin-tools.file.max-file-size=10485760
 * aegis.builtin-tools.file.allowed-extensions=txt,md,json,yaml,csv
 * aegis.builtin-tools.excel.max-rows=100000
 * aegis.builtin-tools.excel.timeout=30000
 * aegis.builtin-tools.ppt.template-directory=/templates/ppt
 * aegis.builtin-tools.ppt.timeout=60000
 * }</pre>
 */
@Component
@ConfigurationProperties(prefix = "aegis.builtin-tools")
public class BuiltinToolsConfig {

    private FileConfig file = new FileConfig();
    private ExcelConfig excel = new ExcelConfig();
    private PptConfig ppt = new PptConfig();

    public FileConfig getFile() {
        return file;
    }

    public void setFile(FileConfig file) {
        this.file = file;
    }

    public ExcelConfig getExcel() {
        return excel;
    }

    public void setExcel(ExcelConfig excel) {
        this.excel = excel;
    }

    public PptConfig getPpt() {
        return ppt;
    }

    public void setPpt(PptConfig ppt) {
        this.ppt = ppt;
    }

    /**
     * 文件操作配置。
     */
    public static class FileConfig {
        /** 基础工作目录（支持逗号分隔的多个目录） */
        private String workingDirectory = System.getProperty("java.io.tmpdir") + "/aegis-files";

        /** 最大文件大小（字节），默认 10MB */
        private long maxFileSize = 10 * 1024 * 1024;

        /** 允许的文件扩展名 */
        private List<String> allowedExtensions = new ArrayList<String>(
                Arrays.asList("txt", "md", "json", "yaml", "yml", "csv", "xml", "html"));

        /** 默认字符编码 */
        private String defaultEncoding = "UTF-8";

        public String getWorkingDirectory() {
            return workingDirectory;
        }

        public void setWorkingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        /**
         * 获取允许的工作目录列表。
         *
         * <p>将逗号分隔的 workingDirectory 拆分为多个目录路径。
         * 列表中第一个目录作为默认工作目录（用于相对路径解析）。</p>
         *
         * @return 工作目录列表，至少包含一个元素
         */
        public List<String> getWorkingDirectories() {
            List<String> dirs = new ArrayList<String>();
            if (workingDirectory != null) {
                for (String dir : workingDirectory.split(",")) {
                    String trimmed = dir.trim();
                    if (!trimmed.isEmpty()) {
                        dirs.add(trimmed);
                    }
                }
            }
            if (dirs.isEmpty()) {
                dirs.add(System.getProperty("java.io.tmpdir") + "/aegis-files");
            }
            return dirs;
        }

        public long getMaxFileSize() {
            return maxFileSize;
        }

        public void setMaxFileSize(long maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        public List<String> getAllowedExtensions() {
            return allowedExtensions;
        }

        public void setAllowedExtensions(List<String> allowedExtensions) {
            this.allowedExtensions = allowedExtensions;
        }

        public String getDefaultEncoding() {
            return defaultEncoding;
        }

        public void setDefaultEncoding(String defaultEncoding) {
            this.defaultEncoding = defaultEncoding;
        }
    }

    /**
     * Excel 操作配置。
     */
    public static class ExcelConfig {
        /** 最大读取行数 */
        private int maxRows = 100000;

        /** 最大列数 */
        private int maxColumns = 256;

        /** 超时时间（毫秒） */
        private long timeout = 30000;

        /** 日期格式化模式 */
        private String dateFormat = "yyyy-MM-dd";

        public int getMaxRows() {
            return maxRows;
        }

        public void setMaxRows(int maxRows) {
            this.maxRows = maxRows;
        }

        public int getMaxColumns() {
            return maxColumns;
        }

        public void setMaxColumns(int maxColumns) {
            this.maxColumns = maxColumns;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public String getDateFormat() {
            return dateFormat;
        }

        public void setDateFormat(String dateFormat) {
            this.dateFormat = dateFormat;
        }
    }

    /**
     * PPT 操作配置。
     */
    public static class PptConfig {
        /** 模板目录 */
        private String templateDirectory = "/templates/ppt";

        /** 输出目录 */
        private String outputDirectory = "/output/ppt";

        /** 超时时间（毫秒） */
        private long timeout = 60000;

        /** 占位符正则表达式 */
        private String placeholderPattern = "\\{\\{(.+?)\\}\\}";

        public String getTemplateDirectory() {
            return templateDirectory;
        }

        public void setTemplateDirectory(String templateDirectory) {
            this.templateDirectory = templateDirectory;
        }

        public String getOutputDirectory() {
            return outputDirectory;
        }

        public void setOutputDirectory(String outputDirectory) {
            this.outputDirectory = outputDirectory;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public String getPlaceholderPattern() {
            return placeholderPattern;
        }

        public void setPlaceholderPattern(String placeholderPattern) {
            this.placeholderPattern = placeholderPattern;
        }
    }
}

