package com.github.cybellereaper.particleengine.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScriptParserTest {

    private ScriptNode.Program parse(String source) {
        return new ScriptParser(new ScriptLexer(source).scanTokens()).parse();
    }

    @Test
    void parsesLetWithInitializer() {
        ScriptNode.Program program = parse("let x = 1 + 2;");
        assertEquals(1, program.statements().size());
        assertInstanceOf(ScriptNode.Let.class, program.statements().get(0));
    }

    @Test
    void parsesFunctionDeclaration() {
        ScriptNode.Program program = parse("fn add(a, b) { return a + b; }");
        ScriptNode.Function fn = (ScriptNode.Function) program.statements().get(0);
        assertEquals("add", fn.name());
        assertEquals(2, fn.parameters().size());
    }

    @Test
    void parsesIfElse() {
        parse("if (1 < 2) { log(\"a\"); } else { log(\"b\"); }");
    }

    @Test
    void parsesForLoop() {
        ScriptNode.Program program = parse("for (i in range(5)) { log(i); }");
        assertInstanceOf(ScriptNode.ForEach.class, program.statements().get(0));
    }

    @Test
    void parsesWhileWaitBreakContinue() {
        parse("while (true) { wait 5; if (false) { break; } else { continue; } }");
    }

    @Test
    void rejectsInvalidAssignment() {
        ScriptError ex = assertThrows(ScriptError.class, () -> parse("1 = 2;"));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid assignment target"));
    }

    @Test
    void rejectsMissingSemicolon() {
        assertThrows(ScriptError.class, () -> parse("let x = 1"));
    }

    @Test
    void parsesListAndIndexing() {
        ScriptNode.Program program = parse("let xs = [1, 2, 3]; let v = xs[1];");
        assertEquals(2, program.statements().size());
    }
}
