package com.mia.aegis.skill.tools;

import com.mia.aegis.skill.exception.ToolExecutionException;
import com.mia.aegis.skill.tools.ToolOutputContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 日志 Tool。
 *
 * <p>输出调试日志，支持 debug/info/warn 级别。</p>
 *
 * <h3>输入参数</h3>
 * <ul>
 *   <li><b>level</b> (String, 可选, 默认 info): 日志级别 (debug, info, warn)</li>
 *   <li><b>message</b> (String, 必填): 日志消息</li>
 *   <li><b>data</b> (Object, 可选): 附加数据</li>
 * </ul>
 *
 * <h3>输出</h3>
 * <pre>{@code
 * {
 *   "logged": true,
 *   "level": "info",
 *   "message": "Processing complete",
 *   "timestamp": "2025-12-27T14:30:00Z"
 * }
 * }</pre>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * Map<String, Object> input = new HashMap<>();
 * input.put("level", "info");
 * input.put("message", "Processing started");
 * input.put("data", Collections.singletonMap("itemCount", 100));
 *
 * Object result = logTool.execute(input);
 * }</pre>
 */
@Component
public class LogTool extends BuiltInTool {

    private static final Logger logger = LoggerFactory.getLogger(LogTool.class);

    private static final String NAME = "log";
    private static final String DESCRIPTION = "Output debug/info/warn logs";
    private static final String DEFAULT_LEVEL = "info";

    private static final Set<String> VALID_LEVELS = new HashSet<String>(
            Arrays.asList("debug", "info", "warn")
    );

    private final List<LogEntry> logBuffer;
    private final boolean bufferLogs;

    /**
     * 构造日志 Tool（不缓存日志）。
     */
    public LogTool() {
        this(false);
    }

    /**
     * 构造日志 Tool。
     *
     * @param bufferLogs 是否缓存日志（用于测试）
     */
    public LogTool(boolean bufferLogs) {
        super(NAME, DESCRIPTION, Category.OBSERVABILITY);
        this.bufferLogs = bufferLogs;
        this.logBuffer = bufferLogs ? new ArrayList<LogEntry>() : null;
    }

    @Override
    public ValidationResult validateInput(Map<String, Object> input) {
        if (input == null) {
            return ValidationResult.failure("Input cannot be null");
        }

        Object levelObj = input.get("level");
        String level = (levelObj == null || levelObj.toString().trim().isEmpty())
                ? DEFAULT_LEVEL
                : levelObj.toString().toLowerCase();
        if (!VALID_LEVELS.contains(level)) {
            return ValidationResult.failure("level must be one of: debug, info, warn");
        }

        Object messageObj = input.get("message");
        if (messageObj == null || messageObj.toString().trim().isEmpty()) {
            return ValidationResult.failure("message is required");
        }

        return ValidationResult.success();
    }

    @Override
    public void execute(Map<String, Object> input, ToolOutputContext output) throws ToolExecutionException {
        ValidationResult validation = validateInput(input);
        if (!validation.isValid()) {
            throw new ToolExecutionException(NAME, validation.getErrorMessage());
        }

        Object levelObj = input.get("level");
        String level = (levelObj == null || levelObj.toString().trim().isEmpty())
                ? DEFAULT_LEVEL
                : levelObj.toString().toLowerCase();
        String message = input.get("message").toString();
        Object data = input.get("data");
        String timestamp = Instant.now().toString();

        try {
            // 创建日志条目
            LogEntry entry = new LogEntry(level, message, data, timestamp);

            // 使用 SLF4J 输出日志
            outputLog(entry);

            // 如果缓存模式，保存日志
            if (bufferLogs && logBuffer != null) {
                logBuffer.add(entry);
            }

            // 构建结果
            output.put("logged", true);
            output.put("level", level);
            output.put("message", message);
            output.put("timestamp", timestamp);
            if (data != null) {
                output.put("data", String.valueOf(data));
            }

        } catch (Exception e) {
            throw new ToolExecutionException(NAME, "Failed to log: " + e.getMessage(), e);
        }
    }

    private void outputLog(LogEntry entry) {
        String logMessage = entry.message;
        if (entry.data != null) {
            logMessage = logMessage + " | data=" + entry.data;
        }

        switch (entry.level) {
            case "debug":
                logger.debug(logMessage);
                break;
            case "warn":
                logger.warn(logMessage);
                break;
            default:
                logger.info(logMessage);
        }
    }

    @Override
    public Map<String, String> getErrorDescriptions() {
        Map<String, String> errors = new LinkedHashMap<String, String>();
        errors.put("INVALID_LEVEL", "Log level must be one of: debug, info, warn");
        errors.put("MESSAGE_REQUIRED", "Log message is required and cannot be empty");
        return Collections.unmodifiableMap(errors);
    }

    @Override
    public ToolSchema getInputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<String, ToolSchema.ParameterSpec>();
        params.put("level", ToolSchema.ParameterSpec.builder("string", "Log level")
                .defaultValue("info")
                .options("debug", "info", "warn")
                .build());
        params.put("message", ToolSchema.ParameterSpec.builder("string", "Log message")
                .required()
                .build());
        params.put("data", ToolSchema.ParameterSpec.optional("object", "Additional data"));
        return new ToolSchema(params);
    }

    @Override
    public ToolSchema getOutputSchema() {
        Map<String, ToolSchema.ParameterSpec> params = new LinkedHashMap<String, ToolSchema.ParameterSpec>();
        params.put("logged", ToolSchema.ParameterSpec.required("boolean", "Whether log was output"));
        params.put("level", ToolSchema.ParameterSpec.required("string", "Log level"));
        params.put("message", ToolSchema.ParameterSpec.required("string", "Log message"));
        params.put("timestamp", ToolSchema.ParameterSpec.required("string", "Log timestamp (ISO 8601)"));
        params.put("data", ToolSchema.ParameterSpec.optional("object", "Additional data"));
        return new ToolSchema(params);
    }

    /**
     * 获取缓存的日志（仅在 bufferLogs=true 时有效）。
     *
     * @return 日志列表
     */
    public List<LogEntry> getLogBuffer() {
        return logBuffer != null ? Collections.unmodifiableList(logBuffer) : Collections.<LogEntry>emptyList();
    }

    /**
     * 清空日志缓存。
     */
    public void clearLogBuffer() {
        if (logBuffer != null) {
            logBuffer.clear();
        }
    }

    /**
     * 日志条目。
     */
    public static class LogEntry {
        public final String level;
        public final String message;
        public final Object data;
        public final String timestamp;

        public LogEntry(String level, String message, Object data, String timestamp) {
            this.level = level;
            this.message = message;
            this.data = data;
            this.timestamp = timestamp;
        }
    }
}

