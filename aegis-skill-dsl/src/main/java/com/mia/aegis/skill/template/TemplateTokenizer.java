package com.mia.aegis.skill.template;

import java.util.ArrayList;
import java.util.List;

/**
 * 模板分词器。
 *
 * <p>将模板字符串分割为 token 序列：</p>
 * <ul>
 *   <li>{@code TEXT} — 原始文本</li>
 *   <li>{@code EXPRESSION} — {@code {{...}}} 内的表达式</li>
 *   <li>{@code FOR_START} — {@code {{#for varName}}}，记录数组变量名</li>
 *   <li>{@code FOR_END} — {@code {{/for}}}</li>
 * </ul>
 *
 * @since 0.3.0
 */
public class TemplateTokenizer {

    /** Token 类型。 */
    public enum TokenType {
        TEXT, EXPRESSION, FOR_START, FOR_END
    }

    /** 分词后的 Token。 */
    public static class Token {
        private final TokenType type;
        private final String content;

        public Token(TokenType type, String content) {
            this.type = type;
            this.content = content;
        }

        public TokenType getType() { return type; }
        public String getContent() { return content; }

        @Override
        public String toString() {
            return type + "(" + content + ")";
        }
    }

    /**
     * 将模板字符串分割为 token 列表。
     *
     * @param template 模板字符串
     * @return token 列表
     */
    public List<Token> tokenize(String template) {
        List<Token> tokens = new ArrayList<Token>();
        if (template == null || template.isEmpty()) {
            return tokens;
        }

        int pos = 0;
        int len = template.length();

        while (pos < len) {
            int openIdx = template.indexOf("{{", pos);
            if (openIdx == -1) {
                // 剩余全是文本
                tokens.add(new Token(TokenType.TEXT, template.substring(pos)));
                break;
            }

            // {{ 之前的文本
            if (openIdx > pos) {
                tokens.add(new Token(TokenType.TEXT, template.substring(pos, openIdx)));
            }

            // 查找配对的 }}
            int closeIdx = template.indexOf("}}", openIdx + 2);
            if (closeIdx == -1) {
                // 没有配对的 }}，作为文本处理
                tokens.add(new Token(TokenType.TEXT, template.substring(openIdx)));
                break;
            }

            String inner = template.substring(openIdx + 2, closeIdx).trim();

            if (inner.startsWith("#for ")) {
                // {{#for arrayVar}}
                String arrayVar = inner.substring(5).trim();
                tokens.add(new Token(TokenType.FOR_START, arrayVar));
            } else if (inner.equals("/for")) {
                // {{/for}}
                tokens.add(new Token(TokenType.FOR_END, ""));
            } else {
                // 普通表达式
                tokens.add(new Token(TokenType.EXPRESSION, inner));
            }

            pos = closeIdx + 2;
        }

        return tokens;
    }
}
