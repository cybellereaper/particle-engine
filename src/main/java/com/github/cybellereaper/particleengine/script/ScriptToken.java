package com.github.cybellereaper.particleengine.script;

/**
 * Single token produced by the {@link ScriptLexer}.
 */
public record ScriptToken(ScriptTokenType type, String lexeme, Object literal, int line, int column) {
    public static ScriptToken eof(int line, int column) {
        return new ScriptToken(ScriptTokenType.EOF, "", null, line, column);
    }

    @Override
    public String toString() {
        return type + "(" + lexeme + ")@" + line + ":" + column;
    }
}
