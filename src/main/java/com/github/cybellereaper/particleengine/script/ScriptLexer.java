package com.github.cybellereaper.particleengine.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Hand-rolled lexer for the PEScript scripting language. Produces a stream of
 * {@link ScriptToken}s that the {@link ScriptParser} consumes.
 *
 * <p>The language is whitespace-insensitive, line and block comments are
 * supported ({@code //} to end-of-line and {@code /* ... *\/}), strings are
 * double-quoted with backslash escapes, numbers may be integer or floating
 * point. Identifiers may contain letters, digits and underscores, and may
 * include {@code .} or {@code :} when used as bare paths (e.g.
 * {@code tag:bossFight}) but the lexer prefers safe alphanumeric IDs and lets
 * the parser handle compound expressions.
 */
public final class ScriptLexer {
    private static final Map<String, ScriptTokenType> KEYWORDS = Map.ofEntries(
            Map.entry("let", ScriptTokenType.LET),
            Map.entry("if", ScriptTokenType.IF),
            Map.entry("else", ScriptTokenType.ELSE),
            Map.entry("while", ScriptTokenType.WHILE),
            Map.entry("for", ScriptTokenType.FOR),
            Map.entry("in", ScriptTokenType.IN),
            Map.entry("fn", ScriptTokenType.FN),
            Map.entry("return", ScriptTokenType.RETURN),
            Map.entry("break", ScriptTokenType.BREAK),
            Map.entry("continue", ScriptTokenType.CONTINUE),
            Map.entry("wait", ScriptTokenType.WAIT),
            Map.entry("true", ScriptTokenType.TRUE),
            Map.entry("false", ScriptTokenType.FALSE),
            Map.entry("null", ScriptTokenType.NULL)
    );

    private final String source;
    private final List<ScriptToken> tokens = new ArrayList<>();
    private int start;
    private int current;
    private int line = 1;
    private int lineStart;

    public ScriptLexer(String source) {
        this.source = source;
    }

    public List<ScriptToken> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(ScriptToken.eof(line, column()));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(' -> addToken(ScriptTokenType.LEFT_PAREN);
            case ')' -> addToken(ScriptTokenType.RIGHT_PAREN);
            case '{' -> addToken(ScriptTokenType.LEFT_BRACE);
            case '}' -> addToken(ScriptTokenType.RIGHT_BRACE);
            case '[' -> addToken(ScriptTokenType.LEFT_BRACKET);
            case ']' -> addToken(ScriptTokenType.RIGHT_BRACKET);
            case ',' -> addToken(ScriptTokenType.COMMA);
            case ';' -> addToken(ScriptTokenType.SEMICOLON);
            case ':' -> addToken(ScriptTokenType.COLON);
            case '.' -> addToken(ScriptTokenType.DOT);
            case '+' -> addToken(ScriptTokenType.PLUS);
            case '-' -> addToken(ScriptTokenType.MINUS);
            case '*' -> addToken(ScriptTokenType.STAR);
            case '%' -> addToken(ScriptTokenType.PERCENT);
            case '!' -> addToken(match('=') ? ScriptTokenType.BANG_EQUAL : ScriptTokenType.BANG);
            case '=' -> addToken(match('=') ? ScriptTokenType.EQUAL_EQUAL : ScriptTokenType.EQUAL);
            case '<' -> addToken(match('=') ? ScriptTokenType.LESS_EQUAL : ScriptTokenType.LESS);
            case '>' -> addToken(match('=') ? ScriptTokenType.GREATER_EQUAL : ScriptTokenType.GREATER);
            case '&' -> {
                if (match('&')) addToken(ScriptTokenType.AMP_AMP);
                else throw error("Unexpected '&'.");
            }
            case '|' -> {
                if (match('|')) addToken(ScriptTokenType.PIPE_PIPE);
                else throw error("Unexpected '|'.");
            }
            case '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    consumeBlockComment();
                } else {
                    addToken(ScriptTokenType.SLASH);
                }
            }
            case ' ', '\r', '\t' -> { /* skip */ }
            case '\n' -> {
                line++;
                lineStart = current;
            }
            case '"' -> string();
            default -> {
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    throw error("Unexpected character '" + c + "'.");
                }
            }
        }
    }

    private void consumeBlockComment() {
        int depth = 1;
        while (!isAtEnd() && depth > 0) {
            char c = advance();
            if (c == '\n') {
                line++;
                lineStart = current;
            } else if (c == '/' && peek() == '*') {
                advance();
                depth++;
            } else if (c == '*' && peek() == '/') {
                advance();
                depth--;
            }
        }
    }

    private void string() {
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && peek() != '"') {
            char c = advance();
            if (c == '\\' && !isAtEnd()) {
                char escaped = advance();
                sb.append(switch (escaped) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case 'r' -> '\r';
                    case '\\' -> '\\';
                    case '"' -> '"';
                    case '\'' -> '\'';
                    case '0' -> '\0';
                    default -> escaped;
                });
                continue;
            }
            if (c == '\n') {
                line++;
                lineStart = current;
            }
            sb.append(c);
        }
        if (isAtEnd()) {
            throw error("Unterminated string literal.");
        }
        advance();
        tokens.add(new ScriptToken(ScriptTokenType.STRING, source.substring(start, current), sb.toString(), line, columnAt(start)));
    }

    private void number() {
        while (isDigit(peek())) advance();
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) advance();
        }
        if (peek() == 'e' || peek() == 'E') {
            advance();
            if (peek() == '+' || peek() == '-') advance();
            while (isDigit(peek())) advance();
        }
        String text = source.substring(start, current);
        double value = Double.parseDouble(text);
        tokens.add(new ScriptToken(ScriptTokenType.NUMBER, text, value, line, columnAt(start)));
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);
        ScriptTokenType type = KEYWORDS.getOrDefault(text, ScriptTokenType.IDENTIFIER);
        Object literal = switch (type) {
            case TRUE -> Boolean.TRUE;
            case FALSE -> Boolean.FALSE;
            case NULL -> null;
            default -> null;
        };
        tokens.add(new ScriptToken(type, text, literal, line, columnAt(start)));
    }

    private void addToken(ScriptTokenType type) {
        tokens.add(new ScriptToken(type, source.substring(start, current), null, line, columnAt(start)));
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char advance() { return source.charAt(current++); }
    private char peek() { return isAtEnd() ? '\0' : source.charAt(current); }
    private char peekNext() { return current + 1 >= source.length() ? '\0' : source.charAt(current + 1); }
    private boolean isAtEnd() { return current >= source.length(); }
    private boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private boolean isAlpha(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'; }
    private boolean isAlphaNumeric(char c) { return isDigit(c) || isAlpha(c); }
    private int column() { return current - lineStart + 1; }
    private int columnAt(int pos) { return pos - lineStart + 1; }

    private ScriptError error(String message) {
        return new ScriptError(message, line, column());
    }
}
