package com.github.cybellereaper.particleengine.script;

import java.util.List;

/**
 * Closure produced by a user-defined {@code fn} declaration.
 */
public final class ScriptFunction implements ScriptCallable {
    private final ScriptNode.Function declaration;
    private final ScriptEnvironment closure;

    public ScriptFunction(ScriptNode.Function declaration, ScriptEnvironment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return declaration.parameters().size();
    }

    @Override
    public Object call(ScriptInterpreter interpreter, List<Object> arguments) {
        ScriptEnvironment env = new ScriptEnvironment(closure);
        for (int i = 0; i < declaration.parameters().size(); i++) {
            env.define(declaration.parameters().get(i), arguments.get(i));
        }
        try {
            if (declaration.body() instanceof ScriptNode.Block block) {
                interpreter.executeBlock(block.statements(), env);
            } else {
                interpreter.execute(declaration.body(), env);
            }
        } catch (ScriptInterpreter.ReturnSignal r) {
            return r.value;
        }
        return null;
    }

    @Override
    public String describe() {
        return "<fn " + declaration.name() + ">";
    }
}
