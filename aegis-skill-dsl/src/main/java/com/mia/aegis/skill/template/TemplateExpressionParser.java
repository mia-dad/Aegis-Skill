package com.mia.aegis.skill.template;

import com.mia.aegis.skill.template.expr.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 模板表达式解析器（递归下降）。
 *
 * <p>语法：</p>
 * <pre>
 * expr             = additive ;
 * additive         = multiplicative ( ("+" | "-") multiplicative )* ;
 * multiplicative   = atom ( ("*" | "/") atom )* ;
 * atom             = number | string | "_" | variable_access ;
 * variable_access  = identifier ( "." identifier | "[" index "]" )* ( "." identifier )* ;
 * index            = number | "#" identifier ;
 * </pre>
 *
 * @since 0.3.0
 */
public class TemplateExpressionParser {

    /**
     * 解析表达式字符串。
     *
     * @param expression 表达式（不含 {{ }}）
     * @return AST 节点
     */
    public TemplateExpression parse(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return new VariableExpr(Collections.singletonList(""));
        }
        Lexer lexer = new Lexer(expression.trim());
        TemplateExpression result = parseAdditive(lexer);
        return result;
    }

    /**
     * 从表达式中提取引用的变量名列表。
     *
     * @param expression 表达式字符串
     * @return 变量名列表
     */
    public List<String> extractVariableNames(String expression) {
        List<String> variables = new ArrayList<String>();
        if (expression == null || expression.trim().isEmpty()) {
            return variables;
        }
        try {
            TemplateExpression expr = parse(expression);
            collectVariables(expr, variables);
        } catch (Exception e) {
            // 解析失败时尝试简单提取
        }
        return variables;
    }

    private void collectVariables(TemplateExpression expr, List<String> variables) {
        if (expr instanceof VariableExpr) {
            String path = ((VariableExpr) expr).getFullPath();
            if (!path.isEmpty() && !variables.contains(path)) {
                variables.add(path);
            }
        } else if (expr instanceof ArrayAccessExpr) {
            ArrayAccessExpr aae = (ArrayAccessExpr) expr;
            collectVariables(aae.getArrayExpr(), variables);
            if (aae.isVariableIndex()) {
                String varIdx = aae.getVariableIndex();
                if (!variables.contains(varIdx)) {
                    variables.add(varIdx);
                }
            }
        } else if (expr instanceof ArithmeticExpr) {
            ArithmeticExpr ae = (ArithmeticExpr) expr;
            collectVariables(ae.getLeft(), variables);
            collectVariables(ae.getRight(), variables);
        }
        // NumberLiteralExpr, StringLiteralExpr, CurrentElementExpr → no variables
    }

    // ---- 递归下降解析 ----

    private TemplateExpression parseAdditive(Lexer lexer) {
        TemplateExpression left = parseMultiplicative(lexer);

        while (lexer.hasMore()) {
            lexer.skipWhitespace();
            if (lexer.match('+')) {
                TemplateExpression right = parseMultiplicative(lexer);
                left = new ArithmeticExpr(ArithmeticExpr.Op.ADD, left, right);
            } else if (lexer.match('-')) {
                // 区分减法和负数：如果 '-' 后面紧跟数字且前面是操作符或开头，则是负数
                // 在 additive 层面，'-' 总是减法
                TemplateExpression right = parseMultiplicative(lexer);
                left = new ArithmeticExpr(ArithmeticExpr.Op.SUB, left, right);
            } else {
                break;
            }
        }

        return left;
    }

    private TemplateExpression parseMultiplicative(Lexer lexer) {
        TemplateExpression left = parseAtom(lexer);

        while (lexer.hasMore()) {
            lexer.skipWhitespace();
            if (lexer.match('*')) {
                TemplateExpression right = parseAtom(lexer);
                left = new ArithmeticExpr(ArithmeticExpr.Op.MUL, left, right);
            } else if (lexer.match('/')) {
                TemplateExpression right = parseAtom(lexer);
                left = new ArithmeticExpr(ArithmeticExpr.Op.DIV, left, right);
            } else {
                break;
            }
        }

        return left;
    }

    private TemplateExpression parseAtom(Lexer lexer) {
        lexer.skipWhitespace();

        if (!lexer.hasMore()) {
            return new VariableExpr(Collections.singletonList(""));
        }

        char c = lexer.peek();

        // 数字字面量
        if (Character.isDigit(c)) {
            return parseNumber(lexer);
        }

        // 负数（只在 atom 层面处理 unary minus）
        if (c == '-' && lexer.hasDigitAfterMinus()) {
            return parseNumber(lexer);
        }

        // 字符串字面量
        if (c == '"' || c == '\'') {
            return parseString(lexer);
        }

        // 当前循环元素 _
        if (c == '_' && !lexer.hasIdentCharAfter(1)) {
            lexer.advance();
            return CurrentElementExpr.INSTANCE;
        }

        // 变量/数组访问
        return parseVariableAccess(lexer);
    }

    private NumberLiteralExpr parseNumber(Lexer lexer) {
        int start = lexer.pos;
        if (lexer.peek() == '-') {
            lexer.advance();
        }
        while (lexer.hasMore() && Character.isDigit(lexer.peek())) {
            lexer.advance();
        }
        if (lexer.hasMore() && lexer.peek() == '.') {
            lexer.advance();
            while (lexer.hasMore() && Character.isDigit(lexer.peek())) {
                lexer.advance();
            }
        }
        double val = Double.parseDouble(lexer.input.substring(start, lexer.pos));
        return new NumberLiteralExpr(val);
    }

    private StringLiteralExpr parseString(Lexer lexer) {
        char quote = lexer.peek();
        lexer.advance(); // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (lexer.hasMore() && lexer.peek() != quote) {
            if (lexer.peek() == '\\' && lexer.pos + 1 < lexer.input.length()) {
                lexer.advance(); // skip backslash
            }
            sb.append(lexer.peek());
            lexer.advance();
        }
        if (lexer.hasMore()) {
            lexer.advance(); // skip closing quote
        }
        return new StringLiteralExpr(sb.toString());
    }

    private TemplateExpression parseVariableAccess(Lexer lexer) {
        // 解析标识符
        String ident = lexer.parseIdentifier();
        if (ident == null || ident.isEmpty()) {
            // 无法解析，返回空变量
            return new VariableExpr(Collections.singletonList(""));
        }

        List<String> pathSegments = new ArrayList<String>();
        pathSegments.add(ident);

        TemplateExpression result = null;

        while (lexer.hasMore()) {
            if (lexer.peek() == '.') {
                lexer.advance(); // skip '.'
                String nextIdent = lexer.parseIdentifier();
                if (nextIdent != null && !nextIdent.isEmpty()) {
                    if (result != null) {
                        // 已经是 ArrayAccessExpr，不能再加 dot path 到 pathSegments
                        // 这种情况在 ArrayAccessExpr 的 trailingPath 中处理
                        break;
                    }
                    pathSegments.add(nextIdent);
                } else {
                    break;
                }
            } else if (lexer.peek() == '[') {
                // 数组索引
                lexer.advance(); // skip '['
                lexer.skipWhitespace();

                TemplateExpression arrayExpr;
                if (result != null) {
                    arrayExpr = result;
                } else {
                    arrayExpr = new VariableExpr(new ArrayList<String>(pathSegments));
                }

                if (lexer.hasMore() && lexer.peek() == '#') {
                    // 变量索引 [#var]
                    lexer.advance(); // skip '#'
                    String varName = lexer.parseIdentifier();
                    lexer.skipWhitespace();
                    if (lexer.hasMore() && lexer.peek() == ']') {
                        lexer.advance(); // skip ']'
                    }
                    List<String> trailingPath = parseTrailingDotPath(lexer);
                    result = new ArrayAccessExpr(arrayExpr, varName, trailingPath);
                    pathSegments.clear();
                } else {
                    // 字面量索引 [N]
                    int idx = parseIntIndex(lexer);
                    lexer.skipWhitespace();
                    if (lexer.hasMore() && lexer.peek() == ']') {
                        lexer.advance(); // skip ']'
                    }
                    List<String> trailingPath = parseTrailingDotPath(lexer);
                    result = new ArrayAccessExpr(arrayExpr, idx, trailingPath);
                    pathSegments.clear();
                }
            } else {
                break;
            }
        }

        if (result != null) {
            return result;
        }
        return new VariableExpr(pathSegments);
    }

    private List<String> parseTrailingDotPath(Lexer lexer) {
        List<String> path = new ArrayList<String>();
        while (lexer.hasMore() && lexer.peek() == '.') {
            lexer.advance(); // skip '.'
            String ident = lexer.parseIdentifier();
            if (ident != null && !ident.isEmpty()) {
                path.add(ident);
            } else {
                break;
            }
        }
        return path;
    }

    private int parseIntIndex(Lexer lexer) {
        StringBuilder sb = new StringBuilder();
        while (lexer.hasMore() && Character.isDigit(lexer.peek())) {
            sb.append(lexer.peek());
            lexer.advance();
        }
        if (sb.length() == 0) {
            return 0;
        }
        return Integer.parseInt(sb.toString());
    }

    // ---- 简单 Lexer ----

    private static class Lexer {
        final String input;
        int pos;

        Lexer(String input) {
            this.input = input;
            this.pos = 0;
        }

        boolean hasMore() {
            return pos < input.length();
        }

        char peek() {
            return input.charAt(pos);
        }

        void advance() {
            pos++;
        }

        boolean match(char expected) {
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == expected) {
                pos++;
                return true;
            }
            return false;
        }

        void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }

        boolean hasDigitAfterMinus() {
            return pos + 1 < input.length() && input.charAt(pos) == '-' && Character.isDigit(input.charAt(pos + 1));
        }

        boolean hasIdentCharAfter(int offset) {
            int checkPos = pos + offset;
            return checkPos < input.length() && (Character.isLetterOrDigit(input.charAt(checkPos)) || input.charAt(checkPos) == '_');
        }

        String parseIdentifier() {
            skipWhitespace();
            if (pos >= input.length()) return null;
            char first = input.charAt(pos);
            if (!Character.isLetter(first) && first != '_') return null;
            int start = pos;
            pos++;
            while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) {
                pos++;
            }
            return input.substring(start, pos);
        }
    }
}
