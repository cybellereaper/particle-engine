package com.github.cybellereaper.particleengine.script;

/**
 * Thrown when a script fails at lex, parse, or runtime.
 */
public class ScriptError extends RuntimeException {
    private final int line;
    private final int column;

    public ScriptError(String message, int line, int column) {
        super(formatMessage(message, line, column));
        this.line = line;
        this.column = column;
    }

    public ScriptError(String message) {
        this(message, -1, -1);
    }

    public int line() { return line; }
    public int column() { return column; }

    private static String formatMessage(String message, int line, int column) {
        if (line < 0) return message;
        if (column < 0) return "[line " + line + "] " + message;
        return "[line " + line + ":" + column + "] " + message;
    }
}
