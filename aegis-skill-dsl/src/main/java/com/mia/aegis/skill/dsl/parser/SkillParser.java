package com.mia.aegis.skill.dsl.parser;

import com.mia.aegis.skill.dsl.model.Skill;
import com.mia.aegis.skill.exception.SkillParseException;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Skill DSL 解析器接口。
 *
 * <p>用于将 Markdown 格式的 Skill 文件解析为 Skill 对象。</p>
 *
 * <p>Supported Formats:</p>
 * <ul>
 *   <li>Markdown 文件，以 {@code # skill: <id>} 开头</li>
 *   <li>包含 {@code ## description}、{@code ## intent}、{@code ## input}、
 *       {@code ## steps}、{@code ## output} 章节</li>
 *   <li>Step 定义使用 {@code ### step: <name>} 格式</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * SkillParser parser = new MarkdownSkillParser();
 * Skill skill = parser.parse(Files.readString(Path.of("my_skill.md")));
 * }</pre>
 *
 * @see Skill
 * @see MarkdownSkillParser
 */
public interface SkillParser {

    /**
     * 从字符串解析 Skill。
     *
     * @param content Skill DSL 内容
     * @return 解析后的 Skill 对象
     * @throws SkillParseException 解析失败时抛出
     */
    Skill parse(String content) throws SkillParseException;

    /**
     * 从文件路径解析 Skill。
     *
     * @param path Skill 文件路径
     * @return 解析后的 Skill 对象
     * @throws SkillParseException 解析失败时抛出
     */
    Skill parseFile(Path path) throws SkillParseException;

    /**
     * 从输入流解析 Skill。
     *
     * @param inputStream Skill 内容输入流
     * @return 解析后的 Skill 对象
     * @throws SkillParseException 解析失败时抛出
     */
    Skill parse(InputStream inputStream) throws SkillParseException;

    /**
     * 验证 Skill DSL 语法是否正确。
     *
     * @param content Skill DSL 内容
     * @return 如果语法正确返回 true
     */
    boolean isValid(String content);
}

