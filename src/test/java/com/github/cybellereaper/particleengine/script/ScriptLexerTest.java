package com.github.cybellereaper.particleengine.script;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScriptLexerTest {

    private List<ScriptToken> tokenize(String source) {
        return new ScriptLexer(source).scanTokens();
    }

    @Test
    void scansSinglePunctuation() {
        var tokens = tokenize("(){}[],;:.+-*/%");
        assertEquals(ScriptTokenType.LEFT_PAREN, tokens.get(0).type());
        assertEquals(ScriptTokenType.RIGHT_PAREN, tokens.get(1).type());
        assertEquals(ScriptTokenType.LEFT_BRACE, tokens.get(2).type());
        assertEquals(ScriptTokenType.RIGHT_BRACE, tokens.get(3).type());
        assertEquals(ScriptTokenType.LEFT_BRACKET, tokens.get(4).type());
        assertEquals(ScriptTokenType.RIGHT_BRACKET, tokens.get(5).type());
        assertEquals(ScriptTokenType.COMMA, tokens.get(6).type());
        assertEquals(ScriptTokenType.SEMICOLON, tokens.get(7).type());
        assertEquals(ScriptTokenType.COLON, tokens.get(8).type());
        assertEquals(ScriptTokenType.DOT, tokens.get(9).type());
        assertEquals(ScriptTokenType.PLUS, tokens.get(10).type());
        assertEquals(ScriptTokenType.MINUS, tokens.get(11).type());
        assertEquals(ScriptTokenType.STAR, tokens.get(12).type());
        assertEquals(ScriptTokenType.SLASH, tokens.get(13).type());
        assertEquals(ScriptTokenType.PERCENT, tokens.get(14).type());
        assertEquals(ScriptTokenType.EOF, tokens.get(15).type());
    }

    @Test
    void scansMultiCharOperators() {
        var tokens = tokenize("== != >= <= && ||");
        assertEquals(ScriptTokenType.EQUAL_EQUAL, tokens.get(0).type());
        assertEquals(ScriptTokenType.BANG_EQUAL, tokens.get(1).type());
        assertEquals(ScriptTokenType.GREATER_EQUAL, tokens.get(2).type());
        assertEquals(ScriptTokenType.LESS_EQUAL, tokens.get(3).type());
        assertEquals(ScriptTokenType.AMP_AMP, tokens.get(4).type());
        assertEquals(ScriptTokenType.PIPE_PIPE, tokens.get(5).type());
    }

    @Test
    void recognizesKeywords() {
        var tokens = tokenize("let if else while for in fn return break continue wait true false null");
        ScriptTokenType[] expected = {
                ScriptTokenType.LET, ScriptTokenType.IF, ScriptTokenType.ELSE,
                ScriptTokenType.WHILE, ScriptTokenType.FOR, ScriptTokenType.IN,
                ScriptTokenType.FN, ScriptTokenType.RETURN, ScriptTokenType.BREAK,
                ScriptTokenType.CONTINUE, ScriptTokenType.WAIT,
                ScriptTokenType.TRUE, ScriptTokenType.FALSE, ScriptTokenType.NULL
        };
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], tokens.get(i).type(), "at index " + i);
        }
    }

    @Test
    void parsesNumbersAndStrings() {
        var tokens = tokenize("42 3.14 \"hi\\nthere\"");
        assertEquals(ScriptTokenType.NUMBER, tokens.get(0).type());
        assertEquals(42.0, tokens.get(0).literal());
        assertEquals(3.14, tokens.get(1).literal());
        assertEquals("hi\nthere", tokens.get(2).literal());
    }

    @Test
    void skipsLineAndBlockComments() {
        var tokens = tokenize("// hi\nlet/* in between */ x = 1;");
        assertEquals(ScriptTokenType.LET, tokens.get(0).type());
        assertEquals(ScriptTokenType.IDENTIFIER, tokens.get(1).type());
        assertEquals("x", tokens.get(1).lexeme());
    }

    @Test
    void tracksLineNumbers() {
        var tokens = tokenize("a\nb\nc");
        assertEquals(1, tokens.get(0).line());
        assertEquals(2, tokens.get(1).line());
        assertEquals(3, tokens.get(2).line());
    }

    @Test
    void unterminatedStringRaisesError() {
        ScriptError ex = assertThrows(ScriptError.class, () -> tokenize("\"oops"));
        assertTrue(ex.getMessage().toLowerCase().contains("unterminated"));
    }
}
