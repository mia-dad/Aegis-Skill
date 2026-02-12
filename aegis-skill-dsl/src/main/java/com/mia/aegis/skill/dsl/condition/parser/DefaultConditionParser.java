package com.mia.aegis.skill.dsl.condition.parser;

import com.mia.aegis.skill.dsl.condition.ast.*;
import com.mia.aegis.skill.exception.ConditionParseException;

/**
 * Default implementation of ConditionParser using recursive descent parsing.
 *
 * <p>This parser is thread-safe and can be shared across concurrent parse operations.
 * It implements the following grammar:
 *
 * <pre>
 * expression     = or_expr ;
 * or_expr        = and_expr ( "||" and_expr )* ;
 * and_expr       = comparison ( "&&" comparison )* ;
 * comparison     = operand ( ( "==" | "!=" | ">=" | "<=" | ">" | "<" ) operand )? ;
 * operand        = variable | literal ;
 * variable       = "{{" identifier ( "." identifier )* "}}" ;
 * literal        = "null" | "true" | "false" | number_literal | string_literal ;
 * number_literal = ["-"] digit+ ["." digit+] ;
 * string_literal = "'" [^']* "'" | '"' [^"]* '"' ;
 * identifier     = [a-zA-Z_][a-zA-Z0-9_]* ;
 * </pre>
 *
 * @since 0.2.0
 */
public class DefaultConditionParser implements ConditionParser {

    @Override
    public ConditionExpression parse(String expression) throws ConditionParseException {
        if (expression == null) {
            throw new IllegalArgumentException("Expression cannot be null");
        }
        String trimmed = expression.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Expression cannot be empty");
        }

        Lexer lexer = new Lexer(trimmed);
        ConditionExpression result = parseOrExpression(lexer);

        // Ensure we've consumed all input
        if (!lexer.isAtEnd()) {
            throw new ConditionParseException(
                    "Unexpected token after expression",
                    trimmed,
                    lexer.getPosition(),
                    "end of expression",
                    lexer.peek()
            );
        }

        return result;
    }

    private ConditionExpression parseOrExpression(Lexer lexer) {
        ConditionExpression left = parseAndExpression(lexer);

        while (lexer.match("||")) {
            ConditionExpression right = parseAndExpression(lexer);
            left = new BinaryExpression(BinaryOperator.OR, left, right);
        }

        return left;
    }

    private ConditionExpression parseAndExpression(Lexer lexer) {
        ConditionExpression left = parseComparison(lexer);

        while (lexer.match("&&")) {
            ConditionExpression right = parseComparison(lexer);
            left = new BinaryExpression(BinaryOperator.AND, left, right);
        }

        return left;
    }

    private ConditionExpression parseComparison(Lexer lexer) {
        ConditionExpression left = parseOperand(lexer);

        if (lexer.match("==")) {
            ConditionExpression right = parseOperand(lexer);
            return new BinaryExpression(BinaryOperator.EQ, left, right);
        } else if (lexer.match("!=")) {
            ConditionExpression right = parseOperand(lexer);
            return new BinaryExpression(BinaryOperator.NEQ, left, right);
        } else if (lexer.match(">=")) {
            ConditionExpression right = parseOperand(lexer);
            return new BinaryExpression(BinaryOperator.GTE, left, right);
        } else if (lexer.match("<=")) {
            ConditionExpression right = parseOperand(lexer);
            return new BinaryExpression(BinaryOperator.LTE, left, right);
        } else if (lexer.match(">")) {
            ConditionExpression right = parseOperand(lexer);
            return new BinaryExpression(BinaryOperator.GT, left, right);
        } else if (lexer.match("<")) {
            ConditionExpression right = parseOperand(lexer);
            return new BinaryExpression(BinaryOperator.LT, left, right);
        }

        return left;
    }

    private ConditionExpression parseOperand(Lexer lexer) {
        lexer.skipWhitespace();

        // Check for variable reference {{...}}
        if (lexer.match("{{")) {
            return parseVariable(lexer);
        }

        // Check for null literal
        if (lexer.match("null")) {
            return NullLiteral.INSTANCE;
        }

        // Check for boolean literals
        if (lexer.match("true")) {
            return BooleanLiteral.TRUE;
        }
        if (lexer.match("false")) {
            return BooleanLiteral.FALSE;
        }

        // Check for number literals (integer or decimal, possibly negative)
        if (lexer.peekNumber()) {
            double num = lexer.parseNumber();
            return new NumberLiteral(num);
        }

        // Check for string literals
        if (lexer.peek() != null && (lexer.peek().equals("'") || lexer.peek().equals("\""))) {
            return parseStringLiteral(lexer);
        }

        // Check for bare variable reference (without {{...}})
        // Supports: varName, object.field, a.b.c
        return parseBareVariable(lexer);
    }

    private ConditionExpression parseBareVariable(Lexer lexer) {
        String identifier = lexer.parseIdentifier();
        if (identifier == null || identifier.isEmpty()) {
            throw new ConditionParseException(
                    "Expected operand (variable, null, true, false, or string)",
                    lexer.getInput(),
                    lexer.getPosition(),
                    "operand",
                    lexer.peek()
            );
        }

        StringBuilder path = new StringBuilder(identifier);

        // Parse optional dot-separated path segments
        while (lexer.match(".")) {
            String nextIdent = lexer.parseIdentifier();
            if (nextIdent == null || nextIdent.isEmpty()) {
                throw new ConditionParseException(
                        "Expected identifier after '.'",
                        lexer.getInput(),
                        lexer.getPosition(),
                        "identifier",
                        lexer.peek()
                );
            }
            path.append(".").append(nextIdent);
        }

        return new VariableReference(path.toString());
    }

    private ConditionExpression parseVariable(Lexer lexer) {
        int startPos = lexer.getPosition() - 2; // Account for {{ already consumed
        StringBuilder path = new StringBuilder();

        // Parse identifier
        String identifier = lexer.parseIdentifier();
        if (identifier == null || identifier.isEmpty()) {
            throw new ConditionParseException(
                    "Expected variable name after '{{'",
                    lexer.getInput(),
                    lexer.getPosition(),
                    "identifier",
                    lexer.peek()
            );
        }
        path.append(identifier);

        // Parse optional nested path segments
        while (lexer.match(".")) {
            identifier = lexer.parseIdentifier();
            if (identifier == null || identifier.isEmpty()) {
                throw new ConditionParseException(
                        "Expected identifier after '.'",
                        lexer.getInput(),
                        lexer.getPosition(),
                        "identifier",
                        lexer.peek()
                );
            }
            path.append(".").append(identifier);
        }

        // Expect closing }}
        if (!lexer.match("}}")) {
            throw new ConditionParseException(
                    "Expected '}}' to close variable reference",
                    lexer.getInput(),
                    lexer.getPosition(),
                    "}}",
                    lexer.peek()
            );
        }

        return new VariableReference(path.toString());
    }

    private ConditionExpression parseStringLiteral(Lexer lexer) {
        char quote = lexer.consume();
        int startPos = lexer.getPosition() - 1;
        StringBuilder value = new StringBuilder();

        while (!lexer.isAtEnd()) {
            char c = lexer.peekChar();
            if (c == quote) {
                lexer.consume();
                return new StringLiteral(value.toString());
            }
            value.append(lexer.consume());
        }

        throw new ConditionParseException(
                "Unterminated string literal",
                lexer.getInput(),
                startPos,
                String.valueOf(quote),
                "end of input"
        );
    }

    /**
     * Simple lexer for tokenizing condition expressions.
     */
    private static class Lexer {
        private final String input;
        private int position;

        Lexer(String input) {
            this.input = input;
            this.position = 0;
        }

        String getInput() {
            return input;
        }

        int getPosition() {
            return position;
        }

        boolean isAtEnd() {
            skipWhitespace();
            return position >= input.length();
        }

        void skipWhitespace() {
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
                position++;
            }
        }

        String peek() {
            skipWhitespace();
            if (position >= input.length()) {
                return null;
            }
            return String.valueOf(input.charAt(position));
        }

        char peekChar() {
            return input.charAt(position);
        }

        char consume() {
            return input.charAt(position++);
        }

        boolean peekNumber() {
            skipWhitespace();
            if (position >= input.length()) return false;
            char c = input.charAt(position);
            if (Character.isDigit(c)) return true;
            if (c == '-' && position + 1 < input.length() && Character.isDigit(input.charAt(position + 1))) return true;
            return false;
        }

        double parseNumber() {
            skipWhitespace();
            int start = position;
            if (position < input.length() && input.charAt(position) == '-') position++;
            while (position < input.length() && Character.isDigit(input.charAt(position))) position++;
            if (position < input.length() && input.charAt(position) == '.') {
                position++;
                while (position < input.length() && Character.isDigit(input.charAt(position))) position++;
            }
            return Double.parseDouble(input.substring(start, position));
        }

        boolean match(String expected) {
            skipWhitespace();
            if (position + expected.length() > input.length()) {
                return false;
            }

            // For keywords like 'null', 'true', 'false', ensure they're not part of identifier
            if (expected.equals("null") || expected.equals("true") || expected.equals("false")) {
                if (input.substring(position).startsWith(expected)) {
                    int endPos = position + expected.length();
                    if (endPos >= input.length() || !isIdentifierChar(input.charAt(endPos))) {
                        position += expected.length();
                        return true;
                    }
                }
                return false;
            }

            // For > and <, ensure they're not part of >= or <=
            if (expected.equals(">") || expected.equals("<")) {
                if (input.substring(position).startsWith(expected)) {
                    int endPos = position + expected.length();
                    if (endPos < input.length() && input.charAt(endPos) == '=') {
                        return false; // It's actually >= or <=
                    }
                    position += expected.length();
                    return true;
                }
                return false;
            }

            if (input.substring(position).startsWith(expected)) {
                position += expected.length();
                return true;
            }
            return false;
        }

        String parseIdentifier() {
            skipWhitespace();
            if (position >= input.length()) {
                return null;
            }

            char first = input.charAt(position);
            if (!isIdentifierStart(first)) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(first);
            position++;

            while (position < input.length() && isIdentifierChar(input.charAt(position))) {
                sb.append(input.charAt(position));
                position++;
            }

            return sb.toString();
        }

        private boolean isIdentifierStart(char c) {
            return Character.isLetter(c) || c == '_';
        }

        private boolean isIdentifierChar(char c) {
            return Character.isLetterOrDigit(c) || c == '_';
        }
    }
}
