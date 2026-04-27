package com.github.cybellereaper.particleengine.script;

import java.util.List;

/**
 * AST node hierarchy for PEScript. Implemented as a sealed type so the
 * interpreter can switch over node kinds exhaustively.
 */
public sealed interface ScriptNode {
    int line();

    /* --- expressions --- */
    record Literal(Object value, int line) implements ScriptNode {}
    record Identifier(String name, int line) implements ScriptNode {}
    record ListLiteral(List<ScriptNode> elements, int line) implements ScriptNode {}
    record Index(ScriptNode target, ScriptNode index, int line) implements ScriptNode {}
    record Binary(ScriptNode left, ScriptTokenType operator, ScriptNode right, int line) implements ScriptNode {}
    record Logical(ScriptNode left, ScriptTokenType operator, ScriptNode right, int line) implements ScriptNode {}
    record Unary(ScriptTokenType operator, ScriptNode operand, int line) implements ScriptNode {}
    record Assign(String name, ScriptNode value, int line) implements ScriptNode {}
    record IndexAssign(ScriptNode target, ScriptNode index, ScriptNode value, int line) implements ScriptNode {}
    record Call(ScriptNode callee, List<ScriptNode> arguments, int line) implements ScriptNode {}

    /* --- statements --- */
    record Program(List<ScriptNode> statements) implements ScriptNode {
        @Override public int line() { return 0; }
    }
    record Block(List<ScriptNode> statements, int line) implements ScriptNode {}
    record Let(String name, ScriptNode initializer, int line) implements ScriptNode {}
    record If(ScriptNode condition, ScriptNode thenBranch, ScriptNode elseBranch, int line) implements ScriptNode {}
    record While(ScriptNode condition, ScriptNode body, int line) implements ScriptNode {}
    record ForEach(String variable, ScriptNode iterable, ScriptNode body, int line) implements ScriptNode {}
    record Function(String name, List<String> parameters, ScriptNode body, int line) implements ScriptNode {}
    record Return(ScriptNode value, int line) implements ScriptNode {}
    record Wait(ScriptNode ticks, int line) implements ScriptNode {}
    record Break(int line) implements ScriptNode {}
    record Continue(int line) implements ScriptNode {}
    record ExpressionStatement(ScriptNode expression, int line) implements ScriptNode {}
}
