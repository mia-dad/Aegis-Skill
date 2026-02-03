package com.mia.aegis.skill.dsl.parser;


import com.mia.aegis.skill.dsl.model.*;
import com.mia.aegis.skill.dsl.model.io.FieldSpec;
import com.mia.aegis.skill.dsl.model.io.InputSchema;
import com.mia.aegis.skill.dsl.model.io.OutputContract;
import com.mia.aegis.skill.dsl.model.io.OutputFormat;
import com.mia.aegis.skill.exception.SkillParseException;
import com.mia.aegis.skill.i18n.MessageUtil;
import org.commonmark.node.*;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于 CommonMark 的 Markdown Skill 解析器实现。
 *
 * <p>解析 Claude Skills 兼容的 Markdown DSL 格式。</p>
 */
public class MarkdownSkillParser implements SkillParser {

    private static final Logger logger = LoggerFactory.getLogger(MarkdownSkillParser.class);

    private static final Pattern SKILL_ID_PATTERN = Pattern.compile("^skill:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern STEP_NAME_PATTERN = Pattern.compile("^step:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXTENSION_PATTERN = Pattern.compile("^x-aegis-(.+)$", Pattern.CASE_INSENSITIVE);
    // 解析后的文本格式：key: value（CommonMark 会移除 ** 标记）
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("^([a-zA-Z_]+):\\s*(.+)$");

    private final Parser markdownParser;
    private final Yaml yaml;

    /**
     * 创建解析器实例。
     */
    public MarkdownSkillParser() {
        this.markdownParser = Parser.builder()
                .includeSourceSpans(IncludeSourceSpans.BLOCKS)
                .build();
        this.yaml = new Yaml();
    }

    @Override
    public Skill parse(String content) throws SkillParseException {
        if (content == null || content.trim().isEmpty()) {
            throw new SkillParseException(MessageUtil.getMessage("skillparser.content.empty"));
        }

        logger.debug("开始解析技能内容，长度: {} 字符", content.length());
        Node document = markdownParser.parse(content);
        Skill skill = parseDocument(document, content);
        logger.info("成功解析技能: {} (包含 {} 个步骤)", skill.getId(), skill.getStepCount());
        return skill;
    }

    @Override
    public Skill parseFile(Path path) throws SkillParseException {
        logger.debug("从文件解析技能: {}", path);
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return parse(content);
        } catch (IOException e) {
            logger.error("读取技能文件失败: {}", path, e);
            throw new SkillParseException(MessageUtil.getMessage("skillparser.io.error.withfile", path), 0, e);
        }
    }

    @Override
    public Skill parse(InputStream inputStream) throws SkillParseException {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return parse(sb.toString());
        } catch (IOException e) {
            throw new SkillParseException(MessageUtil.getMessage("skillparser.io.error"), 0, e);
        }
    }

    @Override
    public boolean isValid(String content) {
        try {
            parse(content);
            return true;
        } catch (SkillParseException e) {
            return false;
        }
    }

    /**
     *  将输入的 Markdown rawContent文本（比如 # skill: test-skill、## steps、### step: step1）会被解析成一棵 “节点树”；
     * Node 是这棵树所有节点的 “父类”，不同类型的 Markdown 元素对应不同的 Node 子类（比如标题对应 Heading、段落对应 Paragraph、代码块对应 FencedCodeBlock）；
     * 代码里 node = document.getFirstChild(); while (node != null) { node = node.getNext(); } 这段逻辑，就是遍历整棵 AST 节点树，逐个处理每个 Markdown 元素。
     * @param document
     * @param rawContent
     * @return
     * @throws SkillParseException
     */
    private Skill parseDocument(Node document, String rawContent) throws SkillParseException {
        String skillId = null;
        String description = null;
        List<String> intents = new ArrayList<String>();
        InputSchema inputSchema = null;
        List<Step> steps = new ArrayList<Step>();
        OutputContract outputContract = null;
        Map<String, Object> extensions = new LinkedHashMap<String, Object>();

        String currentSection = null;
        StringBuilder currentContent = new StringBuilder();
        List<StepBuilder> stepBuilders = new ArrayList<StepBuilder>();
        StepBuilder currentStepBuilder = null;

        Node node = document.getFirstChild();
        while (node != null) {
            /**
             * Heading 是 Node 的子类，专门代表 Markdown 中的标题元素（以 # 开头的行），核心属性：
             * getLevel()：返回标题级别（1 对应 #、2 对应 ##、3 对应 ###、以此类推）；
             * extractText(heading)：提取标题的文本内容（比如 # skill: test-skill 提取后是 skill: test-skill）。
             */
            if (node instanceof Heading) {
                Heading heading = (Heading) node;
                String headingText = extractText(heading).trim();
                int level = heading.getLevel();

                if (level == 1) {
                    // 解析skill的名字:# skill: <id>
                    Matcher matcher = SKILL_ID_PATTERN.matcher(headingText);
                    if (matcher.matches()) {
                        skillId = matcher.group(1).trim();
                    } else {
                        throw new SkillParseException(
                                MessageUtil.getMessage("skillparser.invalid.header", headingText),
                                getLineNumber(node, rawContent)
                        );
                    }
                } else if (level == 2) {
                    // ## section
                    // 保存之前的内容
                    if (currentSection != null && currentContent.length() > 0) {
                        processSectionContent(currentSection, currentContent.toString(),
                                intents, extensions);
                    }
                    currentSection = headingText.toLowerCase();
                    currentContent = new StringBuilder();

                    // 结束当前 step
                    if (currentStepBuilder != null) {
                        stepBuilders.add(currentStepBuilder);
                        currentStepBuilder = null;
                    }

                //目前只处理第三级,且为steps
                } else if (level == 3 && "steps".equals(currentSection)) {
                    // ### step: <name>
                    Matcher matcher = STEP_NAME_PATTERN.matcher(headingText);
                    if (matcher.matches()) {
                        // 保存之前的 step
                        if (currentStepBuilder != null) {
                            stepBuilders.add(currentStepBuilder);
                        }
                        currentStepBuilder = new StepBuilder(matcher.group(1).trim());
                    }
                }
            }
            //解析代码块（yaml/json），比如 input/output 的配置都存在这里面
            else if (node instanceof FencedCodeBlock) {
                FencedCodeBlock codeBlock = (FencedCodeBlock) node;
                String info = codeBlock.getInfo();
                String code = codeBlock.getLiteral();

                if ("input".equals(currentSection)) {
                    inputSchema = parseInputSchema(code);
                } else if ("output".equals(currentSection)) {
                    outputContract = parseOutputContract(code, info);
                } else if (currentStepBuilder != null) {
                    // Step 内的代码块
                    if ("yaml".equalsIgnoreCase(info)) {
                        currentStepBuilder.setInputYaml(code);
                    } else if ("prompt".equalsIgnoreCase(info)) {
                        currentStepBuilder.setPromptTemplate(code);
                    } else if ("json".equalsIgnoreCase(info) && currentStepBuilder.getType() == null) {
                        // JSON 格式的 input
                        currentStepBuilder.setInputYaml(code);
                    }
                }
            }
            //解析普通文本段落，比如 Step 的属性（type/tool/sources）、description 文本
            else if (node instanceof Paragraph) {
                String paragraphText = extractText(node).trim();
                // 如果在 Step 内，解析 Step 属性
                if (currentStepBuilder != null && "steps".equals(currentSection)) {
                    parseStepAttributes(paragraphText, currentStepBuilder);
                } else if (currentSection != null) {
                    if ("description".equals(currentSection)) {
                        if (description == null) {
                            description = paragraphText;
                        } else {
                            description += "\n" + paragraphText;
                        }
                    } else {
                        currentContent.append(paragraphText).append("\n");
                    }
                }
            }
            //解析无序列表（仅在 ## intent 章节内），提取技能的意图列表
            else if (node instanceof BulletList && "intent".equals(currentSection)) {
                // 解析 intent 列表
                parseIntentList((BulletList) node, intents);
            }

            node = node.getNext();
        }

        // 处理最后的 section
        if (currentSection != null && currentContent.length() > 0) {
            processSectionContent(currentSection, currentContent.toString(), intents, extensions);
        }

        // 添加最后的 step
        if (currentStepBuilder != null) {
            stepBuilders.add(currentStepBuilder);
        }

        // 验证必需字段（在构建Steps之前先验证skillId）
        if (skillId == null) {
            throw new SkillParseException(MessageUtil.getMessage("skillparser.no.header"));
        }
        if (stepBuilders.isEmpty()) {
            throw new SkillParseException(MessageUtil.getMessage("skillparser.no.steps"));
        }

        // 构建 Steps（验证通过后再构建）
        for (StepBuilder builder : stepBuilders) {
            steps.add(builder.build());
        }

        return new Skill(skillId, description, intents, inputSchema, steps, outputContract, extensions);
    }

    private void parseIntentList(BulletList list, List<String> intents) {
        Node item = list.getFirstChild();
        while (item != null) {
            if (item instanceof ListItem) {
                String text = extractText(item).trim();
                if (text.startsWith("- ")) {
                    text = text.substring(2).trim();
                }
                if (!text.isEmpty()) {
                    intents.add(text);
                }
            }
            item = item.getNext();
        }
    }

    private void processSectionContent(String section, String content,
                                       List<String> intents, Map<String, Object> extensions) {
        // 检查是否是扩展字段
        Matcher extMatcher = EXTENSION_PATTERN.matcher(section);
        if (extMatcher.matches()) {
            String key = extMatcher.group(1);
            try {
                Object value = yaml.load(content);
                extensions.put(key, value);
            } catch (Exception e) {
                extensions.put(key, content.trim());
            }
        }
    }

    private void parseStepAttributes(String text, StepBuilder builder) {
        // 文本可能包含多个属性（空格分隔或换行分隔）
        // 例如: "type: tool tool: test_tool" 或 "type: tool\ntool: test_tool"

        // 使用正向查找来匹配 key: value 对
        // 匹配模式：key: 后面跟着的值直到下一个 key: 或字符串结尾
        Pattern pattern = Pattern.compile("(type|tool|sources):\\s*([^:]+?)(?=\\s+(?:type|tool|sources):|$)", Pattern.CASE_INSENSITIVE);

        String normalizedText = text.replace("\n", " ");
        Matcher matcher = pattern.matcher(normalizedText);

        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase();
            String value = matcher.group(2).trim();

            if (value.isEmpty()) continue;

            switch (key) {
                case "type":
                    builder.setType(StepType.fromString(value));
                    break;
                case "tool":
                    builder.setToolName(value);
                    break;
                case "sources":
                    builder.setSources(parseSourcesList(value));
                    break;
            }
        }
    }

    private List<String> parseSourcesList(String value) {
        List<String> sources = new ArrayList<String>();
        // 支持 [a, b, c] 格式
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        String[] parts = value.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                sources.add(trimmed);
            }
        }
        return sources;
    }

    private InputSchema parseInputSchema(String yamlContent) throws SkillParseException {
        try {
            Map<String, Object> rawSchema = yaml.load(yamlContent);
            if (rawSchema == null) {
                return InputSchema.empty();
            }

            Map<String, FieldSpec> fields = new LinkedHashMap<String, FieldSpec>();
            for (Map.Entry<String, Object> entry : rawSchema.entrySet()) {
                String fieldName = entry.getKey();
                Object fieldValue = entry.getValue();

                if (fieldValue instanceof String) {
                    // 简单格式: field: type
                    fields.put(fieldName, FieldSpec.of((String) fieldValue));
                } else if (fieldValue instanceof Map) {
                    // 复杂格式: field: {type: string, required: true, description: ...}
                    @SuppressWarnings("unchecked")
                    Map<String, Object> spec = (Map<String, Object>) fieldValue;
                    String type = (String) spec.get("type");
                    Boolean required = (Boolean) spec.getOrDefault("required", true);
                    String description = (String) spec.get("description");
                    fields.put(fieldName, new FieldSpec(type, required, description));
                }
            }

            return new InputSchema(fields);
        } catch (Exception e) {
            throw new SkillParseException(MessageUtil.getMessage("skillparser.input.error", e.getMessage()));
        }
    }

    private OutputContract parseOutputContract(String content, String format) throws SkillParseException {
        try {
            OutputFormat outputFormat = "json".equalsIgnoreCase(format)
                    ? OutputFormat.JSON
                    : OutputFormat.fromString(format);

            if (content == null || content.trim().isEmpty()) {
                return OutputContract.empty();
            }

            // 尝试解析为 YAML/JSON
            Map<String, Object> rawOutput = yaml.load(content);
            if (rawOutput == null) {
                return new OutputContract(null, outputFormat);
            }

            Map<String, FieldSpec> fields = new LinkedHashMap<String, FieldSpec>();
            parseOutputFields(rawOutput, fields, "");

            return new OutputContract(fields, outputFormat);
        } catch (Exception e) {
            throw new SkillParseException(MessageUtil.getMessage("skillparser.output.error", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private void parseOutputFields(Map<String, Object> schema, Map<String, FieldSpec> fields, String prefix) {
        for (Map.Entry<String, Object> entry : schema.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                fields.put(entry.getKey(), FieldSpec.of((String) value));
            } else if (value instanceof List) {
                fields.put(entry.getKey(), FieldSpec.of("array"));
            } else if (value instanceof Map) {
                fields.put(entry.getKey(), FieldSpec.of("object"));
            }
        }
    }

    private String extractText(Node node) {
        StringBuilder sb = new StringBuilder();
        extractTextRecursive(node, sb);
        return sb.toString();
    }

    private void extractTextRecursive(Node node, StringBuilder sb) {
        if (node instanceof Text) {
            sb.append(((Text) node).getLiteral());
        } else if (node instanceof Code) {
            sb.append(((Code) node).getLiteral());
        } else if (node instanceof SoftLineBreak || node instanceof HardLineBreak) {
            sb.append(" ");
        } else {
            Node child = node.getFirstChild();
            while (child != null) {
                extractTextRecursive(child, sb);
                child = child.getNext();
            }
        }
    }

    private int getLineNumber(Node node, String rawContent) {
        // 1. 优先从 CommonMark 的 SourceSpan 获取精准行号（核心逻辑）
        List<SourceSpan> sourceSpans = node.getSourceSpans();
        if (sourceSpans != null && !sourceSpans.isEmpty()) {
            // SourceSpan 的 lineIndex 是 0-based，转为 1-based 行号（符合用户阅读习惯）
            return sourceSpans.get(0).getLineIndex() + 1;
        }

        // 2. 降级方案：如果没有 SourceSpan，通过文本匹配估算行号（兜底逻辑）
        return estimateLineNumber(node, rawContent);
    }

    /**
     * 兜底方案：提取节点文本，在原始内容中匹配行号（防止 SourceSpan 为空）
     */
    private int estimateLineNumber(Node node, String rawContent) {
        String nodeText = extractText(node).trim();
        if (nodeText.isEmpty()) {
            return 0;
        }

        // 按换行分割原始内容，逐行匹配节点文本（模糊匹配，优先匹配开头）
        String[] lines = rawContent.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            // 匹配规则：行文本包含节点核心文本（忽略 Markdown 标记，比如##、###）
            if (line.contains(nodeText) || nodeText.contains(line)) {
                return i + 1; // 转为 1-based
            }
        }

        // 完全匹配不到时返回 0
        return 0;
    }

    /**
     * Step 构建器（内部类）。
     */
    private static class StepBuilder {
        private final String name;
        private StepType type;
        private String toolName;
        private String inputYaml;
        private String promptTemplate;
        private List<String> sources;

        StepBuilder(String name) {
            this.name = name;
        }

        void setType(StepType type) {
            this.type = type;
        }

        StepType getType() {
            return type;
        }

        void setToolName(String toolName) {
            this.toolName = toolName;
        }

        void setInputYaml(String inputYaml) {
            this.inputYaml = inputYaml;
        }

        void setPromptTemplate(String promptTemplate) {
            this.promptTemplate = promptTemplate;
        }

        void setSources(List<String> sources) {
            this.sources = sources;
        }

        Step build() throws SkillParseException {
            if (type == null) {
                // 推断类型
                if (toolName != null) {
                    type = StepType.TOOL;
                } else if (promptTemplate != null) {
                    type = StepType.PROMPT;
                } else if (sources != null && !sources.isEmpty()) {
                    type = StepType.COMPOSE;
                } else {
                    throw new SkillParseException(MessageUtil.getMessage("skillparser.step.unknown.type", name));
                }
            }

            StepConfig config;
            switch (type) {
                case TOOL:
                    if (toolName == null) {
                        throw new SkillParseException(MessageUtil.getMessage("skillparser.step.missing.tool", name));
                    }
                    Map<String, String> inputTemplate = parseInputTemplate(inputYaml);
                    config = new ToolStepConfig(toolName, inputTemplate);
                    break;

                case PROMPT:
                    if (promptTemplate == null || promptTemplate.isEmpty()) {
                        throw new SkillParseException(MessageUtil.getMessage("skillparser.step.missing.template", name));
                    }
                    config = new PromptStepConfig(promptTemplate);
                    break;

                case COMPOSE:
                    if (sources == null || sources.isEmpty()) {
                        throw new SkillParseException(MessageUtil.getMessage("skillparser.step.missing.sources", name));
                    }
                    config = new ComposeStepConfig(sources);
                    break;

                default:
                    throw new SkillParseException(MessageUtil.getMessage("skillparser.step.unknown.type.value", type));
            }

            return new Step(name, type, config);
        }

        @SuppressWarnings("unchecked")
        private Map<String, String> parseInputTemplate(String yamlContent) {
            if (yamlContent == null || yamlContent.trim().isEmpty()) {
                return Collections.emptyMap();
            }

            try {
                Yaml yaml = new Yaml();
                Map<String, Object> raw = yaml.load(yamlContent);
                if (raw == null) {
                    return Collections.emptyMap();
                }

                Map<String, String> result = new LinkedHashMap<String, String>();
                for (Map.Entry<String, Object> entry : raw.entrySet()) {
                    result.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
                return result;
            } catch (Exception e) {
                // 如果不是 YAML，作为原始模板处理
                Map<String, String> result = new LinkedHashMap<String, String>();
                result.put("_raw", yamlContent);
                return result;
            }
        }
    }

    /**
     * 获取节点的行号（1-based）。
     *
     * @param node 节点
     * @return 行号，未知返回 0
     */
    private static int getLineNumber(Node node) {
        if (node == null) {
            return 0;
        }
        List<SourceSpan> spans = node.getSourceSpans();
        if (spans != null && !spans.isEmpty()) {
            return spans.get(0).getLineIndex() + 1; // 转为 1-based
        }
        return 0;
    }
}

