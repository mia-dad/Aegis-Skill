package com.mia.aegis.skill.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mia.aegis.skill.exception.ToolExecutionException;
import com.mia.aegis.skill.tools.ToolOutputContext;
import com.mia.aegis.skill.tools.config.BuiltinToolsConfig;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件写入 Tool。
 *
 * <p>写入内容到文件，支持 JSON 和纯文本格式。支持两种访问方式：</p>
 * <ul>
 *   <li><b>path</b>: 直接指定文件路径（需在工作目录内，受安全限制）</li>
 *   <li><b>fileId</b>: 使用预注册的文件标识（更安全）</li>
 * </ul>
 *
 * <h3>安全机制</h3>
 * <ul>
 *   <li>路径白名单：只能写入配置的工作目录下的文件</li>
 *   <li>路径规范化：防止路径穿越攻击</li>
 *   <li>内容大小限制：默认 10MB</li>
 *   <li>可选备份：覆盖前可创建备份</li>
 * </ul>
 *
 * <h3>输入参数</h3>
 * <ul>
 *   <li><b>path</b> (String, 可选): 文件路径（相对于工作目录）</li>
 *   <li><b>content</b> (Object, 必填): 写入内容（JSON 对象或字符串）</li>
 *   <li><b>format</b> (String, 可选): 文件格式，json 或 txt，默认 json</li>
 *   <li><b>append</b> (Boolean, 可选): 是否追加模式，默认 false</li>
 *   <li><b>createBackup</b> (Boolean, 可选): 是否创建备份，默认 false</li>
 * </ul>
 *
 * <h3>输出</h3>
 * <pre>{@code
 * {
 *   "path": "/data/reports/output.json",
 *   "bytesWritten": 1234,
 *   "backupPath": "/data/reports/output.json.bak" // 如果 createBackup=true
 * }
 * }</pre>
 */
@Component
public class WriteFileTool extends AbstractBuiltinTool {

    private static final String NAME = "write_file";
    private static final String DESCRIPTION = "Write content to file (JSON or text)";
    private static final long MAX_CONTENT_SIZE = 10 * 1024 * 1024; // 10MB

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造文件写入 Tool。
     *
     * @param config 内置 Tools 配置
     */
    public WriteFileTool(BuiltinToolsConfig config) {
        super(config);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public ValidationResult validateInput(Map<String, Object> input) {
        if (input == null) {
            return ValidationResult.failure("Input cannot be null");
        }

        Object pathObj = input.get("path");
        Object fileIdObj = input.get("fileId");

        // path 和 fileId 至少需要一个
        if ((pathObj == null || pathObj.toString().trim().isEmpty()) &&
            (fileIdObj == null || fileIdObj.toString().trim().isEmpty())) {
            return ValidationResult.failure("Either 'path' or 'fileId' is required");
        }

        Object contentObj = input.get("content");
        if (contentObj == null) {
            return ValidationResult.failure("content is required");
        }

        Object formatObj = input.get("format");
        if (formatObj != null) {
            String format = formatObj.toString().toLowerCase();
            if (!format.equals("json") && !format.equals("txt")) {
                return ValidationResult.failure("format must be 'json' or 'txt'");
            }
        }

        return ValidationResult.success();
    }

    @Override
    public void execute(Map<String, Object> input, ToolOutputContext output) throws ToolExecutionException {
        ValidationResult validation = validateInput(input);
        if (!validation.isValid()) {
            throw new ToolExecutionException(NAME, validation.getErrorMessage());
        }

        try {
            // 解析文件路径
            String path = resolvePath(input);
            Object content = input.get("content");
            String format = getFormat(input);
            boolean append = getBoolean(input, "append", false);
            boolean createBackup = getBoolean(input, "createBackup", false);

            File file = new File(path);

            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                ensureDirectoryExists(parentDir);
            }

            // 创建备份
            String backupPath = null;
            if (createBackup && file.exists()) {
                backupPath = createBackup(file);
            }

            // 转换内容为字符串
            String contentStr = convertContent(content, format);

            // 检查内容大小
            if (contentStr.length() > MAX_CONTENT_SIZE) {
                throw new ToolExecutionException(NAME, "Content too large: " + contentStr.length() + " bytes (max: " + MAX_CONTENT_SIZE + ")");
            }

            // 写入文件
            int bytesWritten = writeFile(file, contentStr, append);

            // 写入输出上下文
            output.put("path", path);
            output.put("result", true);


        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolExecutionException(NAME, "Failed to write file: " + e.getMessage(), e);
        }
    }

    /**
     * 解析文件路径。
     *
     * <p>优先使用 path 参数，其次使用 fileId（从配置中解析）。</p>
     */
    private String resolvePath(Map<String, Object> input) throws ToolExecutionException {
        Object pathObj = input.get("path");

        // 如果直接提供了 path
        if (pathObj != null && !pathObj.toString().trim().isEmpty()) {
            String path = pathObj.toString().trim();
            return validatePath(path, config.getFile().getWorkingDirectories());
        }

        // 如果使用 fileId，需要从 FileAccessConfig 获取
        // 注意：这里需要注入 FileAccessConfig，暂时抛出异常
        throw new ToolExecutionException(NAME, "fileId parameter requires FileAccessConfig, please use path parameter instead");
    }

    @Override
    public String getCategory() {
        return "data_access";
    }

    @Override
    public Map<String, String> getErrorDescriptions() {
        Map<String, String> errors = new LinkedHashMap<String, String>();
        errors.put("CONTENT_TOO_LARGE", "Content exceeds maximum size limit (10MB)");
        errors.put("WRITE_FAILED", "Failed to write content to file");
        errors.put("PATH_SECURITY", "Path outside working directory");
        errors.put("CONTENT_REQUIRED", "Content parameter is required");
        return Collections.unmodifiableMap(errors);
    }

    @Override
    public ToolSchema getInputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<String, ToolSchema.ParameterSpec>();
        params.put("path", ToolSchema.ParameterSpec.builder("string", "File path (relative to working directory)")
                .example("data/output/result.json")
                .build());
        params.put("content", ToolSchema.ParameterSpec.builder("object", "Content to write (JSON object or string)")
                .required()
                .build());
        params.put("format", ToolSchema.ParameterSpec.builder("string", "File format")
                .defaultValue("json")
                .options("json", "txt")
                .build());
        params.put("append", ToolSchema.ParameterSpec.builder("boolean", "Append mode")
                .defaultValue(false)
                .build());
        params.put("createBackup", ToolSchema.ParameterSpec.builder("boolean", "Create backup before write")
                .defaultValue(false)
                .build());
        return new ToolSchema(params);
    }

    @Override
    public ToolSchema getOutputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<String, ToolSchema.ParameterSpec>();
        params.put("path", ToolSchema.ParameterSpec.required("string", "Resolved file path"));
        params.put("result", ToolSchema.ParameterSpec.required("boolean", "写入文件的结果"));
        return new ToolSchema(params);
    }

    private String getFormat(Map<String, Object> input) {
        Object formatObj = input.get("format");
        if (formatObj != null) {
            return formatObj.toString().toLowerCase();
        }
        return "json";
    }

    private boolean getBoolean(Map<String, Object> input, String key, boolean defaultValue) {
        Object value = input.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }
        return defaultValue;
    }

    private String convertContent(Object content, String format) throws Exception {
        if ("json".equals(format)) {
            if (content instanceof String) {
                // 验证是有效 JSON
                objectMapper.readTree((String) content);
                return (String) content;
            }
            return objectMapper.writeValueAsString(content);
        } else {
            return content.toString();
        }
    }

    private String createBackup(File file) throws IOException {
        String backupPath = file.getPath() + ".bak";
        File backupFile = new File(backupPath);

        try (InputStream in = new FileInputStream(file);
             OutputStream out = new FileOutputStream(backupFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        return backupPath;
    }

    private int writeFile(File file, String content, boolean append) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        try (FileOutputStream fos = new FileOutputStream(file, append)) {
            fos.write(bytes);
            fos.flush();
        }
        return bytes.length;
    }
}
