package com.github.cybellereaper.particleengine.script;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScriptInterpreterTest {

    private TestScriptHost host;
    private ScriptInterpreter interpreter;

    private void run(String source) {
        host = new TestScriptHost();
        interpreter = new ScriptInterpreter(host);
        var program = new ScriptParser(new ScriptLexer(source).scanTokens()).parse();
        interpreter.execute(program);
    }

    @Test
    void evaluatesArithmetic() {
        run("let x = (2 + 3) * 4 - 1; log(x);");
        assertEquals("19", host.logs.get(0));
    }

    @Test
    void modAndDivisionByZeroFail() {
        ScriptError ex = assertThrows(ScriptError.class, () -> run("log(1 / 0);"));
        assertTrue(ex.getMessage().contains("Division by zero"));
    }

    @Test
    void stringConcatenation() {
        run("log(\"hi \" + 2);");
        assertEquals("hi 2", host.logs.get(0));
    }

    @Test
    void controlFlow() {
        run("""
                let total = 0;
                for (i in range(1, 6)) {
                    total = total + i;
                }
                log(total);
                """);
        assertEquals("15", host.logs.get(0));
    }

    @Test
    void breakAndContinue() {
        run("""
                let total = 0;
                let i = 0;
                while (i < 10) {
                    i = i + 1;
                    if (i == 3) { continue; }
                    if (i == 7) { break; }
                    total = total + i;
                }
                log(total);
                """);
        assertEquals("18", host.logs.get(0));
    }

    @Test
    void userDefinedFunctions() {
        run("""
                fn fact(n) {
                    if (n <= 1) { return 1; }
                    return n * fact(n - 1);
                }
                log(fact(5));
                """);
        assertEquals("120", host.logs.get(0));
    }

    @Test
    void closuresCaptureScope() {
        run("""
                fn makeAdder(n) {
                    fn add(x) { return x + n; }
                    return add;
                }
                let plusTen = makeAdder(10);
                log(plusTen(5));
                """);
        assertEquals("15", host.logs.get(0));
    }

    @Test
    void waitInvokesHostHook() {
        run("wait 7;");
        assertEquals(7, host.waitTickAccumulator.get());
    }

    @Test
    void spawnDispatchesToHost() {
        run("""
                spawn("trail_smoke", at(1, 2, 3), ["demo"]);
                """);
        assertEquals(1, host.spawns.size());
        assertEquals("trail_smoke", host.spawns.get(0).template());
        assertEquals(new ScriptLocation(null, 1, 2, 3), host.spawns.get(0).at());
        assertTrue(host.spawns.get(0).tags().contains("demo"));
    }

    @Test
    void listOpsAndLen() {
        run("""
                let xs = [1, 2, 3];
                push(xs, 4);
                log(len(xs), xs[0], xs[3]);
                """);
        assertEquals("4 1 4", host.logs.get(0));
    }

    @Test
    void mapOps() {
        run("""
                let m = map("a", 1, "b", 2);
                log(m["a"], len(keys(m)));
                """);
        assertEquals("1 2", host.logs.get(0));
    }

    @Test
    void mathBuiltins() {
        run("log(round(sin(0)), floor(2.7), ceil(2.1), pow(2, 10));");
        assertEquals("0 2 3 1024", host.logs.get(0));
    }

    @Test
    void clampAndLerp() {
        run("log(clamp(5, 0, 1), lerp(0, 10, 0.25));");
        assertEquals("1 2.5", host.logs.get(0));
    }

    @Test
    void shortCircuitLogical() {
        run("""
                fn boom() { log("called"); return true; }
                let v = false && boom();
                log(v);
                """);
        assertEquals(List.of("false"), host.logs);
    }

    @Test
    void instructionBudgetEnforced() {
        host = new TestScriptHost();
        interpreter = new ScriptInterpreter(host);
        interpreter.setInstructionBudget(20);
        var program = new ScriptParser(new ScriptLexer("while (true) { let x = 1; }").scanTokens()).parse();
        ScriptError ex = assertThrows(ScriptError.class, () -> interpreter.execute(program));
        assertTrue(ex.getMessage().toLowerCase().contains("instruction budget"));
    }

    @Test
    void undefinedVariableReports() {
        assertThrows(ScriptError.class, () -> run("log(missingVar);"));
    }

    @Test
    void stopByTagInvokesHost() {
        run("""
                spawn("a", at(0, 0, 0), ["x"]);
                spawn("a", at(0, 0, 0), ["x"]);
                let removed = stop_tag("x");
                log(removed);
                """);
        assertEquals("2", host.logs.get(0));
    }
}
