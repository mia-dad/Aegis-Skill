package com.mia.aegis.skill.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 文件读取 Tool。
 *
 * <p>读取文件内容，支持 JSON 和纯文本格式。支持两种访问方式：</p>
 * <ul>
 *   <li><b>path</b>: 直接指定文件路径（需在工作目录内，受安全限制）</li>
 *   <li><b>fileId</b>: 使用预注册的文件标识（更安全）</li>
 * </ul>
 *
 * <h3>安全机制</h3>
 * <ul>
 *   <li>路径白名单</li>
 *   <li>路径规范化：防止路径穿越攻击</li>
 *   <li>文件大小限制：默认 10MB</li>
 *   <li>扩展名白名单：可配置允许的文件类型</li>
 * </ul>
 *
 * <h3>输入参数</h3>
 * <ul>
 *   <li><b>path</b> (String, 可选): 文件路径（相对于工作目录）</li>
 *   <li><b>fileId</b> (String, 可选): 文件标识（path 和 fileId 二选一）</li>
 *   <li><b>format</b> (String, 可选): 文件格式，json 或 txt，默认 json</li>
 * </ul>
 *
 * <h3>输出</h3>
 * <pre>{@code
 * {
 *   "path": "/data/reports/financial.json",
 *   "content": { ... } // JSON 对象或 String
 * }
 * }</pre>
 */
@Component
public class ReadFileTool extends AbstractBuiltinTool {

    private static final String NAME = "read_file";
    private static final String DESCRIPTION = "Read file content (JSON or text)";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造文件读取 Tool。
     *
     * @param config 内置 Tools 配置
     */
    public ReadFileTool(BuiltinToolsConfig config) {
        super(config);
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

        // path 和 fileId 至少需要一个
        Object pathObj = input.get("path");
        Object fileIdObj = input.get("fileId");

        if ((pathObj == null || pathObj.toString().trim().isEmpty()) &&
            (fileIdObj == null || fileIdObj.toString().trim().isEmpty())) {
            return ValidationResult.failure("Either 'path' or 'fileId' is required");
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
            String format = getFormat(input);

            File file = new File(path);

            // 验证文件存在且可读
            if (!file.exists()) {
                throw new ToolExecutionException(NAME, "File not found: " + path);
            }
            if (!file.isFile()) {
                throw new ToolExecutionException(NAME, "Not a file: " + path);
            }
            if (!file.canRead()) {
                throw new ToolExecutionException(NAME, "File not readable: " + path);
            }

            // 检查文件大小
            validateFileSize(file, MAX_FILE_SIZE);

            // 读取文件内容
            String content = readFileContent(file);

            // 写入输出上下文
            output.put("path", path);

            // JSON 格式时保持为字符串（内容本身就是 JSON 字符串）
            if ("json".equals(format)) {
                try {
                    // 验证 JSON 有效性
                    objectMapper.readValue(content, Object.class);
                    output.put("content", content);
                } catch (Exception e) {
                    throw new ToolExecutionException(NAME, "Failed to parse JSON: " + e.getMessage(), e);
                }
            } else {
                output.put("content", content);
            }

        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolExecutionException(NAME, "Failed to read file: " + e.getMessage(), e);
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
        errors.put("FILE_NOT_FOUND", "File does not exist at the specified path");
        errors.put("JSON_PARSE_ERROR", "File content is not valid JSON");
        errors.put("PATH_SECURITY", "Path outside working directory");
        errors.put("FILE_TOO_LARGE", "File exceeds maximum size limit (10MB)");
        return Collections.unmodifiableMap(errors);
    }

    @Override
    public ToolSchema getInputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<String, ToolSchema.ParameterSpec>();
        params.put("path", ToolSchema.ParameterSpec.builder("string", "File path (relative to working directory)")
                .example("data/reports/financial.json")
                .build());
        params.put("format", ToolSchema.ParameterSpec.builder("string", "File format")
                .defaultValue("json")
                .options("json", "txt")
                .build());
        return new ToolSchema(params);
    }

    @Override
    public ToolSchema getOutputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<String, ToolSchema.ParameterSpec>();
        params.put("path", ToolSchema.ParameterSpec.required("string", "Resolved file path"));
        params.put("content", ToolSchema.ParameterSpec.required("object", "File content (JSON object or string)"));
        return new ToolSchema(params);
    }

    private String getFormat(Map<String, Object> input) {
        Object formatObj = input.get("format");
        if (formatObj != null) {
            return formatObj.toString().toLowerCase();
        }
        return "json";
    }

    private String readFileContent(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }
}
