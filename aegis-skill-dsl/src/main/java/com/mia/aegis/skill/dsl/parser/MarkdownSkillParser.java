package com.mia.aegis.skill.dsl.parser;


import com.mia.aegis.skill.dsl.condition.parser.ConditionExpression;
import com.mia.aegis.skill.dsl.condition.parser.ConditionParser;
import com.mia.aegis.skill.dsl.condition.parser.DefaultConditionParser;
import com.mia.aegis.skill.dsl.model.*;
import com.mia.aegis.skill.dsl.model.io.FieldSpec;
import com.mia.aegis.skill.dsl.model.io.InputSchema;
import com.mia.aegis.skill.dsl.model.io.OutputContract;
import com.mia.aegis.skill.dsl.model.io.OutputFormat;
import com.mia.aegis.skill.dsl.model.io.ValidationRule;
import com.mia.aegis.skill.exception.ConditionParseException;
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
    // YAML Frontmatter 分隔符
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\\s*$");
    // Reference 指令模式: <!-- reference: ./path/to/file -->
    private static final Pattern REFERENCE_PATTERN = Pattern.compile(
            "<!--\\s*reference:\\s*([^>]+?)\\s*-->", Pattern.CASE_INSENSITIVE);

    private final Parser markdownParser;
    private final Yaml yaml;
    private final ConditionParser conditionParser;

    /**
     * 创建解析器实例。
     */
    public MarkdownSkillParser() {
        this.markdownParser = Parser.builder()
                .includeSourceSpans(IncludeSourceSpans.BLOCKS)
                .build();
        this.yaml = new Yaml();
        this.conditionParser = new DefaultConditionParser();
    }

    @Override
    public Skill parse(String content) throws SkillParseException {
        if (content == null || content.trim().isEmpty()) {
            throw new SkillParseException("文件内容不能为空");
        }

        // 解析 YAML frontmatter（如果存在）
        Map<String, Object> frontmatter = new LinkedHashMap<String, Object>();
        String markdownContent = content;

        if (content.trim().startsWith("---")) {
            String[] parts = extractFrontmatter(content);
            if (parts != null) {
                String frontmatterYaml = parts[0];
                markdownContent = parts[1];
                try {
                    Map<String, Object> parsed = yaml.load(frontmatterYaml);
                    if (parsed != null) {
                        frontmatter = parsed;
                    }
                } catch (Exception e) {
                    throw new SkillParseException("解析 YAML frontmatter 失败: " + e.getMessage());
                }
            }
        }

        Node document = markdownParser.parse(markdownContent);
        return parseDocument(document, markdownContent, frontmatter);
    }

    /**
     * 提取 YAML frontmatter 和剩余的 Markdown 内容。
     *
     * @param content 完整内容
     * @return [frontmatter, remaining] 数组，如果没有 frontmatter 返回 null
     */
    private String[] extractFrontmatter(String content) {
        String[] lines = content.split("\n", -1);
        if (lines.length < 2 || !lines[0].trim().equals("---")) {
            return null;
        }

        int endIndex = -1;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().equals("---")) {
                endIndex = i;
                break;
            }
        }

        if (endIndex == -1) {
            return null;
        }

        StringBuilder frontmatter = new StringBuilder();
        for (int i = 1; i < endIndex; i++) {
            frontmatter.append(lines[i]).append("\n");
        }

        StringBuilder remaining = new StringBuilder();
        for (int i = endIndex + 1; i < lines.length; i++) {
            remaining.append(lines[i]);
            if (i < lines.length - 1) {
                remaining.append("\n");
            }
        }

        return new String[]{frontmatter.toString(), remaining.toString()};
    }

    @Override
    public Skill parseFile(Path path) throws SkillParseException {
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return parse(content);
        } catch (IOException e) {
            throw new SkillParseException("读取技能文件失败: " + path, 0, e);
        }
    }

    /**
     * 从文件路径解析 Skill，支持 reference 指令解析。
     *
     * <p>与 parseFile 不同，此方法会解析 <!-- reference: path --> 指令，
     * 加载引用文件内容，并将其存储到 extensions.context 中。</p>
     *
     * @param path Skill 文件路径
     * @return 解析后的 Skill，包含 context 和 references 扩展
     * @throws SkillParseException 如果解析失败或引用文件不存在
     */
    public Skill parseFromPath(Path path) throws SkillParseException {
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

            // 解析 reference 指令
            List<Reference> references = parseReferences(content, path.getParent());

            // 加载引用文件内容
            Map<String, Object> context = new LinkedHashMap<String, Object>();
            for (Reference ref : references) {
                Reference loadedRef = loadReferenceContent(ref, path.getParent());
                String refContent = loadedRef.getContent();

                // 根据文件类型处理内容
                Object contextValue;
                switch (loadedRef.getFileType()) {
                    case JSON:
                        try {
                            // 尝试解析 JSON
                            contextValue = yaml.load(refContent);
                        } catch (Exception e) {
                            contextValue = refContent;
                        }
                        break;
                    case YAML:
                        try {
                            // 尝试解析 YAML
                            contextValue = yaml.load(refContent);
                        } catch (Exception e) {
                            contextValue = refContent;
                        }
                        break;
                    default:
                        // MARKDOWN, TEXT 保持原始字符串
                        contextValue = refContent;
                }
                context.put(loadedRef.getName(), contextValue);
            }

            // 先解析 Skill
            Skill skill = parse(content);

            // 如果有 references，创建包含 context 的新 Skill
            if (!references.isEmpty()) {
                Map<String, Object> newExtensions = new LinkedHashMap<String, Object>(skill.getExtensions());
                newExtensions.put("context", context);
                newExtensions.put("references", references);

                return new Skill(
                        skill.getId(),
                        skill.getVersion(),
                        skill.getDescription(),
                        skill.getIntents(),
                        skill.getInputSchema(),
                        skill.getSteps(),
                        skill.getOutputContract(),
                        newExtensions
                );
            }

            return skill;
        } catch (IOException e) {
            throw new SkillParseException("读取技能文件失败: " + path, 0, e);
        }
    }

    /**
     * 从内容中解析 reference 指令。
     *
     * @param content Skill 内容
     * @param basePath 基础路径（用于解析相对路径）
     * @return Reference 列表
     */
    private List<Reference> parseReferences(String content, Path basePath) {
        List<Reference> references = new ArrayList<Reference>();
        Matcher matcher = REFERENCE_PATTERN.matcher(content);

        while (matcher.find()) {
            String refPath = matcher.group(1).trim();
            FileType fileType = FileType.fromPath(refPath);
            references.add(new Reference(refPath, fileType));
        }

        return references;
    }

    /**
     * 加载引用文件内容。
     *
     * @param reference 引用描述
     * @param basePath 基础路径
     * @return 包含内容的 Reference
     * @throws SkillParseException 如果文件不存在或读取失败
     */
    private Reference loadReferenceContent(Reference reference, Path basePath) throws SkillParseException {
        Path refPath = basePath.resolve(reference.getPath()).normalize();

        if (!Files.exists(refPath)) {
            throw new SkillParseException(
                    "找不到引用文件: " + reference.getPath() + " (解析为: " + refPath + ")");
        }

        try {
            String content = new String(Files.readAllBytes(refPath), StandardCharsets.UTF_8);
            return reference.withContent(content);
        } catch (IOException e) {
            throw new SkillParseException(
                    "读取引用文件失败: " + reference.getPath() + " - " + e.getMessage());
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
            throw new SkillParseException("从流读取技能内容失败", 0, e);
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

    private Skill parseDocument(Node document, String rawContent, Map<String, Object> frontmatter) throws SkillParseException {
        // 从 frontmatter 提取字段
        String skillId = frontmatter.containsKey("id") ? String.valueOf(frontmatter.get("id")) : null;
        String version = frontmatter.containsKey("version") ? String.valueOf(frontmatter.get("version")) : null;
        String description = frontmatter.containsKey("description") ? String.valueOf(frontmatter.get("description")) : null;
        String intent = frontmatter.containsKey("intent") ? String.valueOf(frontmatter.get("intent")) : null;
        List<String> intents = new ArrayList<String>();
        InputSchema inputSchema = null;
        List<Step> steps = new ArrayList<Step>();
        OutputContract outputContract = null;
        Map<String, Object> extensions = new LinkedHashMap<String, Object>();

        // 从 frontmatter 提取扩展字段
        for (Map.Entry<String, Object> entry : frontmatter.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("x-aegis-")) {
                extensions.put(key.substring(8), entry.getValue());
            }
        }

        String currentSection = null;
        StringBuilder currentContent = new StringBuilder();
        List<StepBuilder> stepBuilders = new ArrayList<StepBuilder>();
        StepBuilder currentStepBuilder = null;

        Node node = document.getFirstChild();
        while (node != null) {
            if (node instanceof Heading) {
                Heading heading = (Heading) node;
                String headingText = extractText(heading).trim();
                int level = heading.getLevel();

                if (level == 1) {
                    // # skill: <id> - 如果 frontmatter 中已经有 id，则忽略
                    if (skillId == null) {
                        Matcher matcher = SKILL_ID_PATTERN.matcher(headingText);
                        if (matcher.matches()) {
                            skillId = matcher.group(1).trim();
                        } else {
                            throw new SkillParseException(
                                    "无效的Skill文件: " + headingText,
                                    getLineNumber(node, rawContent)
                            );
                        }
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
            } else if (node instanceof FencedCodeBlock) {
                FencedCodeBlock codeBlock = (FencedCodeBlock) node;
                String info = codeBlock.getInfo();
                String code = codeBlock.getLiteral();

                logger.debug("处理 FencedCodeBlock - section: {}, info: '{}', currentStepBuilder: {}",
                    currentSection, info, currentStepBuilder != null ? "存在" : "null");

                if ("input".equals(currentSection) || "input_schema".equals(currentSection)) {
                    inputSchema = parseInputSchema(code);
                } else if ("output".equals(currentSection) || "output_schema".equals(currentSection)) {
                    logger.debug("解析 output_schema - info: '{}', code: '{}'", info, code);
                    outputContract = parseOutputContract(code, info);
                    logger.debug("解析 output_schema 完成 - result: {}", outputContract != null ? outputContract.getFormat() : "null");
                } else if (currentStepBuilder != null) {
                    // Step 内的代码块
                    if ("yaml".equalsIgnoreCase(info)) {
                        logger.debug("识别为 YAML 代码块，调用 parseStepYaml");
                        parseStepYaml(code, currentStepBuilder);
                    } else if ("prompt".equalsIgnoreCase(info)) {
                        currentStepBuilder.setPromptTemplate(code);
                    } else if ("template".equalsIgnoreCase(info)) {
                        currentStepBuilder.setTemplateContent(code);
                    } else if ("json".equalsIgnoreCase(info) && currentStepBuilder.getType() == null) {
                        // JSON 格式的 input
                        parseStepYaml(code, currentStepBuilder);
                    } else {
                        logger.debug("代码块类型不匹配 - info: '{}', builder type: {}", info, currentStepBuilder.getType());
                    }
                }
            } else if (node instanceof Paragraph) {
                String paragraphText = extractText(node).trim();
                // 如果在 Step 内，解析 Step 属性
                if (currentStepBuilder != null && "steps".equals(currentSection)) {
                    parseStepAttributes(paragraphText, currentStepBuilder);
                } else if (currentSection != null) {
                    if ("version".equals(currentSection)) {
                        if (version == null) {
                            version = paragraphText;
                        }
                    } else if ("description".equals(currentSection)) {
                        if (description == null) {
                            description = paragraphText;
                        } else {
                            description += "\n" + paragraphText;
                        }
                    } else {
                        currentContent.append(paragraphText).append("\n");
                    }
                }
            } else if (node instanceof BulletList && "intent".equals(currentSection)) {
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

        // 构建 Steps
        for (StepBuilder builder : stepBuilders) {
            steps.add(builder.build());
        }

        // 验证必需字段
        if (skillId == null) {
            throw new SkillParseException("缺少必需的技能 id（frontmatter 'id' 或 '# skill: <id>' 标题）");
        }
        if (version == null || version.trim().isEmpty()) {
            throw new SkillParseException("缺少必需的 version（frontmatter 'version' 或 '## version' 段落）");
        }
        if (steps.isEmpty()) {
            throw new SkillParseException("技能至少需要一个步骤");
        }

        // output_schema 是可选的，如果未定义则使用默认的 TEXT 格式
        if (outputContract == null) {
            logger.debug("未定义 output_schema，使用默认的 TEXT 格式");
            outputContract = new OutputContract(null, OutputFormat.TEXT, null);
        }

        // 合并 intent 和 intents
        List<String> finalIntents = intents;
        if (intent != null && !intent.isEmpty()) {
            finalIntents = new ArrayList<String>();
            finalIntents.add(intent);
            finalIntents.addAll(intents);
        }

        return new Skill(skillId, version, description, finalIntents, inputSchema, steps, outputContract, extensions);
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

    private void parseStepAttributes(String text, StepBuilder builder) throws SkillParseException {
        // 文本可能包含多个属性（空格分隔或换行分隔）
        // 例如: "type: tool tool: test_tool when: ..."
        // 注意：CommonMark 会移除 ** 标记，所以文本格式是 "type: tool" 而不是 "**type**: tool"

        logger.debug("解析步骤属性 - 原始文本: '{}'", text);

        // 使用正向查找来匹配 key: value 对
        // 匹配模式：key: value 直到下一个 known_key: 或字符串结尾
        Pattern pattern = Pattern.compile("(type|tool|when|varname):\\s*([^:]+?)(?=\\s+(?:type|tool|when|varname):|$)", Pattern.CASE_INSENSITIVE);

        String normalizedText = text.replace("\n", " ");
        Matcher matcher = pattern.matcher(normalizedText);

        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase();
            String value = matcher.group(2).trim();

            logger.debug("匹配到属性 - key: '{}', value: '{}'", key, value);

            if (value.isEmpty()) continue;

            switch (key) {
                case "type":
                    StepType stepType = StepType.fromString(value);
                    logger.debug("设置步骤类型: {} -> {}", value, stepType);
                    builder.setType(stepType);
                    break;
                case "tool":
                    builder.setToolName(value);
                    break;
                case "varname":
                    logger.debug("设置变量别名: {}", value);
                    builder.setVarName(value);
                    break;
                case "when":
                    String expr = unwrapExpression(value);
                    logger.debug("解析 when 条件属性: {}", expr);
                    try {
                        ConditionExpression parsedExpr = conditionParser.parse(expr);
                        builder.setWhenCondition(new WhenCondition(expr, parsedExpr));
                    } catch (ConditionParseException e) {
                        throw new SkillParseException(
                                "解析 when 条件失败: " + e.getMessage());
                    }
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

    /**
     * 解析 Step 的 YAML 配置块。
     *
     * <p>从 YAML 中提取输入模板和 when 条件。对于 await 类型，还会提取 message 和 input_schema。</p>
     *
     * @param yamlContent YAML 内容
     * @param builder Step 构建器
     * @throws SkillParseException 如果解析失败
     */
    @SuppressWarnings("unchecked")
    private void parseStepYaml(String yamlContent, StepBuilder builder) throws SkillParseException {
        if (yamlContent == null || yamlContent.trim().isEmpty()) {
            return;
        }

        Map<String, Object> parsed;
        try {
            logger.debug("开始解析步骤 YAML，内容预览:\n{}", yamlContent);
            parsed = yaml.load(yamlContent);
            logger.debug("解析步骤 YAML - 当前类型: {}, 包含的键: {}",
                builder.getType(), parsed != null ? parsed.keySet() : "null");
        } catch (Exception e) {
            logger.error("YAML 解析失败: {}", e.getMessage(), e);
            logger.error("失败的 YAML 内容:\n{}", yamlContent);
            throw new SkillParseException("YAML 解析失败: " + e.getMessage());
        }

        if (parsed == null) {
            builder.setInputYaml(yamlContent);
            return;
        }

        // 提取 when 条件
        if (parsed.containsKey("when")) {
            Object whenObj = parsed.get("when");
            if (whenObj instanceof Map) {
                Map<String, Object> whenMap = (Map<String, Object>) whenObj;
                if (whenMap.containsKey("expr")) {
                    String expr = String.valueOf(whenMap.get("expr"));
                    expr = unwrapExpression(expr);
                    try {
                        ConditionExpression parsedExpr = conditionParser.parse(expr);
                        builder.setWhenCondition(new WhenCondition(expr, parsedExpr));
                    } catch (ConditionParseException e) {
                        throw new SkillParseException(
                                "解析 when 条件失败: " + e.getMessage());
                    }
                }
            } else if (whenObj instanceof String) {
                // 支持简写格式: when: "{{x}} != null"
                String expr = (String) whenObj;
                expr = unwrapExpression(expr);
                try {
                    ConditionExpression parsedExpr = conditionParser.parse(expr);
                    builder.setWhenCondition(new WhenCondition(expr, parsedExpr));
                } catch (ConditionParseException e) {
                    throw new SkillParseException(
                            "Failed to parse when condition: " + e.getMessage());
                }
            }
        }

        // 根据步骤类型决定如何处理 message 和 input_schema
        // 如果 type 还未设置，尝试从 YAML 内容推断是否为 await 步骤
        boolean isAwaitStep = builder.getType() == StepType.AWAIT ||
            (builder.getType() == null && parsed.containsKey("message") && parsed.containsKey("input_schema"));

        // 如果推断为 await 步骤且 type 未设置，则设置 type
        if (isAwaitStep && builder.getType() == null) {
            logger.debug("从 YAML 内容推断步骤类型为 AWAIT");
            builder.setType(StepType.AWAIT);
        }

        logger.debug("步骤类型判断 - isAwaitStep: {}, contains message: {}, contains input_schema: {}",
            isAwaitStep, parsed.containsKey("message"), parsed.containsKey("input_schema"));

        if (isAwaitStep) {
            // 对于 await 步骤，提取 message 和 input_schema 作为 await 配置
            if (parsed.containsKey("message")) {
                String message = String.valueOf(parsed.get("message"));
                logger.debug("提取 await message: {}", message);
                builder.setAwaitMessage(message);
            } else {
                logger.warn("Await 步骤缺少 message 字段");
            }
            if (parsed.containsKey("input_schema")) {
                Object inputSchemaObj = parsed.get("input_schema");
                if (inputSchemaObj instanceof Map) {
                    InputSchema schema = parseAwaitInputSchema((Map<String, Object>) inputSchemaObj);
                    logger.debug("提取 await input_schema: {}", schema);
                    builder.setAwaitInputSchema(schema);
                }
            }
            // await 步骤不设置 inputYaml,因为所有配置都已提取
            return;
        }

        // 设置输入模板 (移除 when 块)
        if (parsed.containsKey("when")) {
            String cleanedYaml = removeWhenBlock(yamlContent);
            if (!cleanedYaml.trim().isEmpty()) {
                builder.setInputYaml(cleanedYaml);
            }
        } else {
            builder.setInputYaml(yamlContent);
        }
    }

    /**
     * 解析 await step 的 input_schema。
     *
     * <p>支持简写格式和扩展格式，与 parseInputSchema 保持一致。</p>
     *
     * @param rawSchema 原始 schema Map
     * @return InputSchema 对象
     */
    @SuppressWarnings("unchecked")
    private InputSchema parseAwaitInputSchema(Map<String, Object> rawSchema) {
        Map<String, FieldSpec> fields = new LinkedHashMap<String, FieldSpec>();

        for (Map.Entry<String, Object> entry : rawSchema.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();

            if (fieldValue instanceof String) {
                // 简写格式: field: type（向后兼容）
                fields.put(fieldName, FieldSpec.of((String) fieldValue));
            } else if (fieldValue instanceof Map) {
                // 扩展格式: field: {type: string, required: true, ...}
                fields.put(fieldName, parseExtendedFieldSpec((Map<String, Object>) fieldValue));
            }
        }

        return new InputSchema(fields);
    }

    /**
     * 去除表达式外层的引号（如果存在）。
     *
     * <p>支持单引号和双引号，只去除外层引号，保留内部引号。</p>
     *
     * @param expression 表达式字符串
     * @return 去除外层引号后的表达式
     */
    private String unwrapExpression(String expression) {
        if (expression == null || expression.isEmpty()) {
            return expression;
        }

        String trimmed = expression.trim();
        if (trimmed.length() < 2) {
            return expression;
        }

        char first = trimmed.charAt(0);
        char last = trimmed.charAt(trimmed.length() - 1);

        // 检查是否是外层引号（单引号或双引号）
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return trimmed.substring(1, trimmed.length() - 1);
        }

        return expression;
    }

    /**
     * 从 YAML 内容中移除 when 块。
     *
     * @param yamlContent YAML 内容
     * @return 移除 when 块后的内容
     */
    private String removeWhenBlock(String yamlContent) {
        // 处理两种格式：
        // 1. when:\n  expr: "..." 格式
        // 2. when: "..." 简写格式

        String[] lines = yamlContent.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inWhenBlock = false;
        int whenIndent = 0;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("when:")) {
                inWhenBlock = true;
                whenIndent = line.indexOf("when:");

                // 检查是否是简写格式 (when: "...")
                if (trimmed.length() > 5 && trimmed.charAt(5) != ' ' && trimmed.charAt(5) != '\t') {
                    // when: 后面直接跟内容，跳过这一行
                    continue;
                }
                if (trimmed.equals("when:")) {
                    // 块格式，需要跳过后续的缩进行
                    continue;
                }
                // when: 后面有内容但可能是空格，检查剩余内容
                String afterWhen = trimmed.substring(5).trim();
                if (!afterWhen.isEmpty() && !afterWhen.equals(":")) {
                    // when: "expr" 格式
                    continue;
                }
                continue;
            }

            if (inWhenBlock) {
                // 检查是否还在 when 块内（通过缩进判断）
                int currentIndent = 0;
                for (char c : line.toCharArray()) {
                    if (c == ' ' || c == '\t') {
                        currentIndent++;
                    } else {
                        break;
                    }
                }

                if (trimmed.isEmpty() || currentIndent > whenIndent) {
                    // 仍在 when 块内，跳过
                    continue;
                } else {
                    // 已离开 when 块
                    inWhenBlock = false;
                }
            }

            if (!inWhenBlock) {
                if (result.length() > 0) {
                    result.append("\n");
                }
                result.append(line);
            }
        }

        return result.toString();
    }

    /**
     * 从 YAML 内容中移除 await 特有字段 (message, input_schema)。
     *
     * @param yamlContent YAML 内容
     * @return 移除 await 字段后的内容
     */
    private String removeAwaitFields(String yamlContent) {
        String[] lines = yamlContent.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inBlock = false;
        int blockIndent = 0;

        for (String line : lines) {
            String trimmed = line.trim();

            // 检查是否是 message 或 input_schema 字段
            if (trimmed.startsWith("message:") || trimmed.startsWith("input_schema:")) {
                inBlock = true;
                blockIndent = line.indexOf(trimmed.charAt(0));

                // 检查是否是单行值
                String afterColon = trimmed.substring(trimmed.indexOf(':') + 1).trim();
                if (!afterColon.isEmpty() && !afterColon.equals("|") && !afterColon.equals(">")) {
                    // 单行值，跳过这一行后 inBlock = false
                    inBlock = false;
                }
                continue;
            }

            if (inBlock) {
                // 检查是否还在块内（通过缩进判断）
                int currentIndent = 0;
                for (char c : line.toCharArray()) {
                    if (c == ' ' || c == '\t') {
                        currentIndent++;
                    } else {
                        break;
                    }
                }

                if (trimmed.isEmpty() || currentIndent > blockIndent) {
                    // 仍在块内，跳过
                    continue;
                } else {
                    // 已离开块
                    inBlock = false;
                }
            }

            if (!inBlock) {
                if (result.length() > 0) {
                    result.append("\n");
                }
                result.append(line);
            }
        }

        return result.toString();
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
                    // 简写格式: field: type（向后兼容）
                    fields.put(fieldName, FieldSpec.of((String) fieldValue));
                } else if (fieldValue instanceof Map) {
                    // 扩展格式: field: {type: string, required: true, ...}
                    @SuppressWarnings("unchecked")
                    Map<String, Object> spec = (Map<String, Object>) fieldValue;
                    fields.put(fieldName, parseExtendedFieldSpec(spec));
                }
            }

            return new InputSchema(fields);
        } catch (Exception e) {
            throw new SkillParseException("解析 input schema 失败: " + e.getMessage());
        }
    }

    /**
     * 解析扩展格式的 FieldSpec。
     *
     * <p>支持的字段：</p>
     * <ul>
     *   <li>type - 字段类型（必需）</li>
     *   <li>required - 是否必填（默认 true）</li>
     *   <li>description - 字段描述</li>
     *   <li>placeholder - 输入框占位提示</li>
     *   <li>defaultValue - 默认值</li>
     *   <li>options - 枚举选项列表</li>
     *   <li>uiHint - UI 渲染提示</li>
     *   <li>label - 显示标签</li>
     *   <li>validation - 验证规则</li>
     * </ul>
     *
     * @param spec 原始 Map 格式的字段规范
     * @return FieldSpec 实例
     */
    @SuppressWarnings("unchecked")
    private FieldSpec parseExtendedFieldSpec(Map<String, Object> spec) {
        // 基本字段
        String type = (String) spec.get("type");
        Boolean required = spec.containsKey("required") ? (Boolean) spec.get("required") : true;
        String description = (String) spec.get("description");

        // 新增字段（007-skill-api-enhancement）
        String placeholder = (String) spec.get("placeholder");
        Object defaultValue = spec.get("defaultValue");
        String uiHint = (String) spec.get("uiHint");
        String label = (String) spec.get("label");

        // 解析 options 列表
        List<String> options = null;
        if (spec.containsKey("options")) {
            Object optionsObj = spec.get("options");
            if (optionsObj instanceof List) {
                options = new ArrayList<String>();
                for (Object opt : (List<?>) optionsObj) {
                    options.add(String.valueOf(opt));
                }
            }
        }

        // 解析 validation 规则
        ValidationRule validation = null;
        if (spec.containsKey("validation")) {
            Object validationObj = spec.get("validation");
            if (validationObj instanceof Map) {
                validation = parseValidationRule((Map<String, Object>) validationObj);
            }
        }

        return new FieldSpec(type, required, description,
                placeholder, defaultValue, options, uiHint, label, validation);
    }

    /**
     * 解析 ValidationRule。
     *
     * <p>支持的验证规则：</p>
     * <ul>
     *   <li>pattern - 正则表达式（适用于 string）</li>
     *   <li>min - 最小值/长度</li>
     *   <li>max - 最大值/长度</li>
     *   <li>minItems - 数组最小项数（适用于 array）</li>
     *   <li>maxItems - 数组最大项数（适用于 array）</li>
     *   <li>message - 自定义错误消息</li>
     * </ul>
     *
     * @param validationMap 原始 Map 格式的验证规则
     * @return ValidationRule 实例
     */
    private ValidationRule parseValidationRule(Map<String, Object> validationMap) {
        String pattern = (String) validationMap.get("pattern");
        Number min = (Number) validationMap.get("min");
        Number max = (Number) validationMap.get("max");
        Integer minItems = validationMap.containsKey("minItems")
                ? ((Number) validationMap.get("minItems")).intValue() : null;
        Integer maxItems = validationMap.containsKey("maxItems")
                ? ((Number) validationMap.get("maxItems")).intValue() : null;
        String message = (String) validationMap.get("message");

        return new ValidationRule(pattern, min, max, minItems, maxItems, message);
    }

    private OutputContract parseOutputContract(String content, String format) throws SkillParseException {
        try {
            OutputFormat outputFormat = "json".equalsIgnoreCase(format)
                    ? OutputFormat.JSON
                    : OutputFormat.fromString(format);

            if (content == null || content.trim().isEmpty()) {
                return new OutputContract(null, outputFormat);
            }

            // TEXT 格式不需要解析字段，直接返回空字段的契约
            if (outputFormat == OutputFormat.TEXT) {
                return new OutputContract(null, outputFormat);
            }

            // 尝试解析为 YAML/JSON
            Object loaded = yaml.load(content);
            if (loaded == null) {
                return new OutputContract(null, outputFormat);
            }

            // 如果加载结果不是 Map，则无法解析为字段契约
            if (!(loaded instanceof Map)) {
                return new OutputContract(null, outputFormat);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rawOutput = (Map<String, Object>) loaded;

            // 平铺格式：每个顶层 key 就是字段名
            Map<String, FieldSpec> fields = new LinkedHashMap<String, FieldSpec>();
            Map<String, Object> rawProperties = new LinkedHashMap<String, Object>(rawOutput);
            parseOutputFields(rawOutput, fields, "");

            return new OutputContract(fields, outputFormat, rawProperties);
        } catch (SkillParseException e) {
            throw e;
        } catch (Exception e) {
            throw new SkillParseException("解析 output contract 失败: " + e.getMessage());
        }
    }

    /**
     * 解析输出字段。
     *
     * <p>支持两种格式：</p>
     * <ul>
     *   <li>简写格式：field: type</li>
     *   <li>扩展格式：field: {type: string, required: false, description: ...}</li>
     * </ul>
     *
     * @param schema 字段 schema Map
     * @param fields 输出的 FieldSpec Map
     * @param prefix 字段名前缀（用于嵌套对象）
     */
    @SuppressWarnings("unchecked")
    private void parseOutputFields(Map<String, Object> schema, Map<String, FieldSpec> fields, String prefix) {
        for (Map.Entry<String, Object> entry : schema.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();

            if (fieldValue instanceof String) {
                // 简写格式: field: type
                fields.put(fieldName, FieldSpec.of((String) fieldValue));
            } else if (fieldValue instanceof Map) {
                // 扩展格式: field: {type: string, required: false, ...}
                Map<String, Object> spec = (Map<String, Object>) fieldValue;
                fields.put(fieldName, parseExtendedFieldSpec(spec));
            } else if (fieldValue instanceof List) {
                // 数组类型
                fields.put(fieldName, FieldSpec.of("array"));
            } else {
                // 其他类型，默认为 object
                fields.put(fieldName, FieldSpec.of("object"));
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

    private int getLineNumber(Node node, String content) {
        // CommonMark 不直接提供行号，返回估计值
        // 实际实现中可以通过 SourceSpan 获取
        return 0;
    }

    /**
     * Step 构建器（内部类）。
     */
    private class StepBuilder {
        private final String name;
        private StepType type;
        private String toolName;
        private String inputYaml;
        private String promptTemplate;
        private List<String> sources;
        private WhenCondition whenCondition;
        private String templateContent;
        private String varName;
        // Await step 属性
        private String awaitMessage;
        private InputSchema awaitInputSchema;
        // Tool step output_schema 声明的字段名
        private List<String> outputFieldNames;

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

        void setTemplateContent(String templateContent) {
            this.templateContent = templateContent;
        }

        void setSources(List<String> sources) {
            this.sources = sources;
        }

        void setWhenCondition(WhenCondition whenCondition) {
            this.whenCondition = whenCondition;
        }

        void setVarName(String varName) {
            this.varName = varName;
        }

        void setAwaitMessage(String awaitMessage) {
            this.awaitMessage = awaitMessage;
        }

        void setAwaitInputSchema(InputSchema awaitInputSchema) {
            this.awaitInputSchema = awaitInputSchema;
        }

        void setOutputFieldNames(List<String> outputFieldNames) {
            this.outputFieldNames = outputFieldNames;
        }

        Step build() throws SkillParseException {
            if (type == null) {
                // 推断类型
                if (toolName != null) {
                    type = StepType.TOOL;
                } else if (promptTemplate != null) {
                    type = StepType.PROMPT;
                } else if (templateContent != null) {
                    type = StepType.TEMPLATE;
                } else if (awaitMessage != null || awaitInputSchema != null) {
                    type = StepType.AWAIT;
                } else {
                    throw new SkillParseException("无法确定步骤类型: " + name);
                }
            }

            StepConfig config;
            switch (type) {
                case TOOL:
                    if (toolName == null) {
                        throw new SkillParseException("步骤 '" + name + "' 缺少 **tool** 属性");
                    }
                    Map<String, Object> inputTemplate = parseInputTemplate(inputYaml);
                    config = new ToolStepConfig(toolName, inputTemplate, outputFieldNames);
                    break;

                case PROMPT:
                    if (promptTemplate == null || promptTemplate.isEmpty()) {
                        throw new SkillParseException("步骤 '" + name + "' 缺少 prompt 模板");
                    }
                    config = new PromptStepConfig(promptTemplate);
                    break;

                case TEMPLATE:
                    if (templateContent == null || templateContent.isEmpty()) {
                        throw new SkillParseException("步骤 '" + name + "' 缺少 template 模板");
                    }
                    config = new TemplateStepConfig(templateContent);
                    break;

                case AWAIT:
                    if (awaitMessage == null || awaitMessage.isEmpty()) {
                        throw new SkillParseException("步骤 '" + name + "' 缺少 'message' 属性");
                    }
                    if (awaitInputSchema == null || awaitInputSchema.isEmpty()) {
                        throw new SkillParseException("步骤 '" + name + "' 缺少 'input_schema' 属性");
                    }
                    config = new AwaitStepConfig(awaitMessage, awaitInputSchema);
                    break;

                default:
                    throw new SkillParseException("未知的步骤类型: " + type);
            }

            return new Step(name, type, config, whenCondition, varName);
        }

        /**
         * 解析 Tool 输入模板。
         *
         * <p>保持 YAML 原始结构，不序列化对象。
         * 返回的 Map 中，值可以是 String、Number、Boolean、Map、List 等类型。</p>
         *
         * @param yamlContent YAML 配置内容
         * @return 输入参数模板（保持原始类型）
         */
        @SuppressWarnings("unchecked")
        private Map<String, Object> parseInputTemplate(String yamlContent) {
            if (yamlContent == null || yamlContent.trim().isEmpty()) {
                return Collections.emptyMap();
            }

            try {
                // 在 YAML 解析前，保护模板表达式（{{...}}）
                // 防止 SnakeYAML 将其误认为空 Map
                Map<String, String> templateMapping = new HashMap<String, String>();
                String protectedYaml = protectTemplateExpressions(yamlContent, templateMapping);

                logger.debug("[parseInputTemplate] 原始 YAML:\n{}", yamlContent);
                logger.debug("[parseInputTemplate] 保护后的 YAML:\n{}", protectedYaml);
                logger.debug("[parseInputTemplate] 模板映射: {}", templateMapping);

                Yaml yaml = new Yaml();
                Map<String, Object> raw = yaml.load(protectedYaml);
                logger.debug("[parseInputTemplate] YAML 解析结果: {}, 键: {}", raw, raw != null ? raw.keySet() : "null");

                if (raw == null) {
                    return Collections.emptyMap();
                }

                // 调试：打印每个值的类型
                for (Map.Entry<String, Object> entry : raw.entrySet()) {
                    logger.debug("[parseInputTemplate]   {} => 类型: {}, 值: {}",
                            entry.getKey(),
                            entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null",
                            entry.getValue());
                }

                // 提取 output_schema 声明（如果存在），用于可读性，不参与执行逻辑
                if (raw.containsKey("output_schema")) {
                    Object outputSchemaObj = raw.get("output_schema");
                    if (outputSchemaObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> outputSchemaMap = (Map<String, Object>) outputSchemaObj;
                        outputFieldNames = new ArrayList<String>(outputSchemaMap.keySet());
                        logger.debug("[parseInputTemplate] 提取 output_schema 字段: {}", outputFieldNames);
                    }
                    raw = new LinkedHashMap<String, Object>(raw);
                    raw.remove("output_schema");
                }

                // 如果 YAML 包含 "input" 键，提取其内容作为实际输入
                // 这支持格式：
                // input:
                //   datasource: "main_db"
                //   table: "customers"
                if (raw.containsKey("input") && raw.get("input") instanceof Map) {
                    raw = (Map<String, Object>) raw.get("input");
                    logger.debug("[parseInputTemplate] 提取 input 子对象，新的键: {}", raw.keySet());
                }

                // 还原模板表达式
                Object restoredObj = restoreTemplateExpressions(raw, templateMapping);
                Map<String, Object> restored = (Map<String, Object>) restoredObj;
                logger.debug("[parseInputTemplate] 还原后的结果: {}", restored);

                // 返回原始结构，不进行序列化
                // 工具会接收原始对象类型（Map/List），并根据需要进行处理
                return new LinkedHashMap<String, Object>(restored);
            } catch (Exception e) {
                // 如果不是 YAML，作为原始模板处理
                Map<String, Object> result = new LinkedHashMap<String, Object>();
                result.put("_raw", yamlContent);
                return result;
            }
        }

        /**
         * 保护模板表达式，避免被 YAML 解析器误认为 Map。
         *
         * <p>将 {{expression}} 替换为占位符 __TEMPLATE_N__，并记录映射关系。</p>
         *
         * @param yamlContent 原始 YAML 内容
         * @param templateMapping 输出参数，用于存储占位符到原始模板的映射
         * @return 替换后的 YAML 内容
         */
        private String protectTemplateExpressions(String yamlContent, Map<String, String> templateMapping) {
            // 匹配模板表达式：{{variable}}、{{expr}} 等
            Pattern templatePattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
            Matcher matcher = templatePattern.matcher(yamlContent);

            StringBuffer sb = new StringBuffer();
            int index = 0;

            while (matcher.find()) {
                String placeholder = "__TEMPLATE_" + index + "__";
                String originalTemplate = matcher.group(0); // 包含 {{ }}
                templateMapping.put(placeholder, originalTemplate);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
                index++;
            }
            matcher.appendTail(sb);

            return sb.toString();
        }

        /**
         * 还原模板表达式。
         *
         * <p>递归遍历解析结果，将占位符还原为原始的模板表达式。</p>
         *
         * @param obj 解析后的对象（Map、List 或基本类型）
         * @param templateMapping 占位符到原始模板的映射
         * @return 还原后的对象
         */
        @SuppressWarnings("unchecked")
        private Object restoreTemplateExpressions(Object obj, Map<String, String> templateMapping) {
            if (obj == null) {
                return null;
            }

            if (obj instanceof String) {
                String str = (String) obj;
                // 先检查精确匹配
                if (templateMapping.containsKey(str)) {
                    return templateMapping.get(str);
                }
                // 再检查嵌入式占位符（如 "output/__TEMPLATE_0___report.md"）
                for (Map.Entry<String, String> entry : templateMapping.entrySet()) {
                    if (str.contains(entry.getKey())) {
                        str = str.replace(entry.getKey(), entry.getValue());
                    }
                }
                return str;
            }

            if (obj instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) obj;
                Map<String, Object> restored = new LinkedHashMap<String, Object>();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    restored.put(entry.getKey(), restoreTemplateExpressions(entry.getValue(), templateMapping));
                }
                return restored;
            }

            if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                List<Object> restored = new ArrayList<Object>();
                for (Object item : list) {
                    restored.add(restoreTemplateExpressions(item, templateMapping));
                }
                return restored;
            }

            // 其他类型（Number、Boolean 等）直接返回
            return obj;
        }

        /**
         * 将对象转换为 JSON 字符串。
         *
         * @param obj 对象
         * @return JSON 字符串
         */
        @SuppressWarnings("unchecked")
        private String toJson(Object obj) {
            if (obj == null) {
                return "null";
            }
            if (obj instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) obj;
                StringBuilder sb = new StringBuilder("{");
                boolean first = true;
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    if (!first) sb.append(",");
                    sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
                    sb.append(toJson(entry.getValue()));
                    first = false;
                }
                sb.append("}");
                return sb.toString();
            }
            if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                for (Object item : list) {
                    if (!first) sb.append(",");
                    sb.append(toJson(item));
                    first = false;
                }
                sb.append("]");
                return sb.toString();
            }
            if (obj instanceof String) {
                return "\"" + escapeJson((String) obj) + "\"";
            }
            if (obj instanceof Number || obj instanceof Boolean) {
                return obj.toString();
            }
            return "\"" + escapeJson(obj.toString()) + "\"";
        }

        /**
         * 转义 JSON 字符串中的特殊字符。
         */
        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
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

