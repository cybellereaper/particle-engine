package com.github.cybellereaper.particleengine.script;

import java.util.List;

/**
 * Anything that can be invoked from a PEScript expression with a list of
 * resolved argument values.
 */
public interface ScriptCallable {
    int arity();

    Object call(ScriptInterpreter interpreter, List<Object> arguments);

    default String describe() {
        return "<fn>";
    }
}
