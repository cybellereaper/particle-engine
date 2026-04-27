package com.github.cybellereaper.particleengine.script;

public enum ScriptTokenType {
    // literals
    NUMBER, STRING, IDENTIFIER, TRUE, FALSE, NULL,

    // single-char punctuation
    LEFT_PAREN, RIGHT_PAREN,
    LEFT_BRACE, RIGHT_BRACE,
    LEFT_BRACKET, RIGHT_BRACKET,
    COMMA, SEMICOLON, COLON, DOT,

    // operators
    PLUS, MINUS, STAR, SLASH, PERCENT,
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,
    AMP_AMP, PIPE_PIPE,

    // keywords
    LET, IF, ELSE, WHILE, FOR, IN, FN, RETURN, BREAK, CONTINUE, WAIT,

    EOF
}
