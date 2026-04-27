package com.github.cybellereaper.particleengine.script;

import java.util.HashMap;
import java.util.Map;

/**
 * Lexical environment used by {@link ScriptInterpreter}. Variables resolve
 * up the parent chain so that nested blocks can see enclosing scopes.
 */
public final class ScriptEnvironment {
    private final ScriptEnvironment parent;
    private final Map<String, Object> values = new HashMap<>();

    public ScriptEnvironment() {
        this(null);
    }

    public ScriptEnvironment(ScriptEnvironment parent) {
        this.parent = parent;
    }

    public ScriptEnvironment parent() {
        return parent;
    }

    public void define(String name, Object value) {
        values.put(name, value);
    }

    public Object get(String name) {
        if (values.containsKey(name)) return values.get(name);
        if (parent != null) return parent.get(name);
        throw new ScriptError("Undefined variable '" + name + "'.");
    }

    public boolean has(String name) {
        if (values.containsKey(name)) return true;
        return parent != null && parent.has(name);
    }

    public void assign(String name, Object value) {
        if (values.containsKey(name)) {
            values.put(name, value);
            return;
        }
        if (parent != null) {
            parent.assign(name, value);
            return;
        }
        throw new ScriptError("Cannot assign to undefined variable '" + name + "'.");
    }
}
