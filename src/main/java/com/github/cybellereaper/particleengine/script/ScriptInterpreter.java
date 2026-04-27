package com.github.cybellereaper.particleengine.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tree-walking interpreter for PEScript. Designed to be runnable on a worker
 * thread (see {@link ScriptRuntime}) so {@code wait} can suspend execution
 * without blocking the main server thread.
 *
 * <p>The interpreter is intentionally small but feature complete enough for
 * typical particle scripting: variables, control flow, user-defined
 * functions, lists, maps, lexical scope, and a host-provided builtin
 * library accessed through the global environment.
 */
public final class ScriptInterpreter {
    private final ScriptEnvironment globals = new ScriptEnvironment();
    private final ScriptHost host;
    private long instructionBudget = 1_000_000L;
    private long instructionCount;

    public ScriptInterpreter(ScriptHost host) {
        this.host = host;
        ScriptBuiltins.install(globals, host);
    }

    public ScriptHost host() {
        return host;
    }

    public ScriptEnvironment globals() {
        return globals;
    }

    public void setInstructionBudget(long budget) {
        this.instructionBudget = budget;
    }

    public void execute(ScriptNode.Program program) {
        instructionCount = 0;
        for (ScriptNode statement : program.statements()) {
            execute(statement, globals);
        }
    }

    void execute(ScriptNode node, ScriptEnvironment env) {
        bumpBudget();
        switch (node) {
            case ScriptNode.Block b -> executeBlock(b.statements(), new ScriptEnvironment(env));
            case ScriptNode.Let l -> {
                Object value = l.initializer() == null ? null : evaluate(l.initializer(), env);
                env.define(l.name(), value);
            }
            case ScriptNode.If i -> {
                if (ScriptValue.isTruthy(evaluate(i.condition(), env))) {
                    execute(i.thenBranch(), env);
                } else if (i.elseBranch() != null) {
                    execute(i.elseBranch(), env);
                }
            }
            case ScriptNode.While w -> {
                while (ScriptValue.isTruthy(evaluate(w.condition(), env))) {
                    try {
                        execute(w.body(), env);
                    } catch (BreakSignal br) {
                        return;
                    } catch (ContinueSignal cs) {
                        // restart loop iteration
                    }
                }
            }
            case ScriptNode.ForEach fe -> {
                Object iterable = evaluate(fe.iterable(), env);
                List<?> values = toIterable(iterable, fe.line());
                ScriptEnvironment loopEnv = new ScriptEnvironment(env);
                loopEnv.define(fe.variable(), null);
                for (Object value : values) {
                    loopEnv.assign(fe.variable(), value);
                    try {
                        execute(fe.body(), loopEnv);
                    } catch (BreakSignal br) {
                        return;
                    } catch (ContinueSignal cs) {
                        // continue
                    }
                }
            }
            case ScriptNode.Function f -> {
                ScriptFunction function = new ScriptFunction(f, env);
                env.define(f.name(), function);
            }
            case ScriptNode.Return r -> {
                Object value = r.value() == null ? null : evaluate(r.value(), env);
                throw new ReturnSignal(value);
            }
            case ScriptNode.Wait w -> {
                int ticks = ScriptValue.asInt(evaluate(w.ticks(), env), "wait");
                if (ticks > 0) {
                    try {
                        host.waitTicks(ticks);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new ScriptError("Script interrupted while waiting.");
                    }
                }
            }
            case ScriptNode.Break ignored -> throw new BreakSignal();
            case ScriptNode.Continue ignored -> throw new ContinueSignal();
            case ScriptNode.ExpressionStatement es -> evaluate(es.expression(), env);
            case ScriptNode.Program p -> {
                for (ScriptNode statement : p.statements()) execute(statement, env);
            }
            default -> throw new ScriptError("Unexpected statement type: " + node.getClass().getSimpleName(), node.line(), -1);
        }
    }

    void executeBlock(List<ScriptNode> statements, ScriptEnvironment env) {
        for (ScriptNode statement : statements) {
            execute(statement, env);
        }
    }

    Object evaluate(ScriptNode node, ScriptEnvironment env) {
        bumpBudget();
        return switch (node) {
            case ScriptNode.Literal l -> l.value();
            case ScriptNode.Identifier id -> env.get(id.name());
            case ScriptNode.ListLiteral list -> {
                List<Object> values = new ArrayList<>(list.elements().size());
                for (ScriptNode element : list.elements()) values.add(evaluate(element, env));
                yield values;
            }
            case ScriptNode.Index idx -> indexInto(evaluate(idx.target(), env), evaluate(idx.index(), env), idx.line());
            case ScriptNode.Unary u -> {
                Object operand = evaluate(u.operand(), env);
                yield switch (u.operator()) {
                    case MINUS -> -ScriptValue.asNumber(operand, "unary -");
                    case BANG -> !ScriptValue.isTruthy(operand);
                    default -> throw new ScriptError("Unknown unary operator.", u.line(), -1);
                };
            }
            case ScriptNode.Binary b -> evalBinary(b, env);
            case ScriptNode.Logical lg -> {
                Object left = evaluate(lg.left(), env);
                if (lg.operator() == ScriptTokenType.PIPE_PIPE) {
                    if (ScriptValue.isTruthy(left)) yield left;
                    yield evaluate(lg.right(), env);
                }
                if (!ScriptValue.isTruthy(left)) yield left;
                yield evaluate(lg.right(), env);
            }
            case ScriptNode.Assign a -> {
                Object value = evaluate(a.value(), env);
                if (env.has(a.name())) {
                    env.assign(a.name(), value);
                } else {
                    env.define(a.name(), value);
                }
                yield value;
            }
            case ScriptNode.IndexAssign ia -> {
                Object target = evaluate(ia.target(), env);
                Object index = evaluate(ia.index(), env);
                Object value = evaluate(ia.value(), env);
                applyIndexAssign(target, index, value, ia.line());
                yield value;
            }
            case ScriptNode.Call c -> performCall(c, env);
            default -> throw new ScriptError("Unsupported expression: " + node.getClass().getSimpleName(), node.line(), -1);
        };
    }

    private Object evalBinary(ScriptNode.Binary b, ScriptEnvironment env) {
        Object left = evaluate(b.left(), env);
        Object right = evaluate(b.right(), env);
        return switch (b.operator()) {
            case PLUS -> {
                if (left instanceof String || right instanceof String) {
                    yield ScriptValue.asString(left) + ScriptValue.asString(right);
                }
                if (left instanceof List<?> ll && right instanceof List<?> rl) {
                    List<Object> merged = new ArrayList<>(ll.size() + rl.size());
                    merged.addAll(ll);
                    merged.addAll(rl);
                    yield merged;
                }
                yield ScriptValue.asNumber(left, "+") + ScriptValue.asNumber(right, "+");
            }
            case MINUS -> ScriptValue.asNumber(left, "-") - ScriptValue.asNumber(right, "-");
            case STAR -> ScriptValue.asNumber(left, "*") * ScriptValue.asNumber(right, "*");
            case SLASH -> {
                double divisor = ScriptValue.asNumber(right, "/");
                if (divisor == 0D) throw new ScriptError("Division by zero.", b.line(), -1);
                yield ScriptValue.asNumber(left, "/") / divisor;
            }
            case PERCENT -> {
                double divisor = ScriptValue.asNumber(right, "%");
                if (divisor == 0D) throw new ScriptError("Modulo by zero.", b.line(), -1);
                yield ScriptValue.asNumber(left, "%") % divisor;
            }
            case GREATER -> ScriptValue.asNumber(left, ">") > ScriptValue.asNumber(right, ">");
            case GREATER_EQUAL -> ScriptValue.asNumber(left, ">=") >= ScriptValue.asNumber(right, ">=");
            case LESS -> ScriptValue.asNumber(left, "<") < ScriptValue.asNumber(right, "<");
            case LESS_EQUAL -> ScriptValue.asNumber(left, "<=") <= ScriptValue.asNumber(right, "<=");
            case EQUAL_EQUAL -> ScriptValue.equals(left, right);
            case BANG_EQUAL -> !ScriptValue.equals(left, right);
            default -> throw new ScriptError("Unknown binary operator: " + b.operator(), b.line(), -1);
        };
    }

    private Object performCall(ScriptNode.Call call, ScriptEnvironment env) {
        Object callee = evaluate(call.callee(), env);
        if (!(callee instanceof ScriptCallable callable)) {
            throw new ScriptError("Cannot call value of type " + ScriptValue.describe(callee) + ".", call.line(), -1);
        }
        List<Object> args = new ArrayList<>(call.arguments().size());
        for (ScriptNode arg : call.arguments()) args.add(evaluate(arg, env));
        if (callable.arity() >= 0 && args.size() != callable.arity()) {
            throw new ScriptError(callable.describe() + " expected " + callable.arity() + " arguments but got " + args.size() + ".", call.line(), -1);
        }
        return callable.call(this, args);
    }

    private Object indexInto(Object target, Object key, int line) {
        if (target instanceof List<?> list) {
            int idx = ScriptValue.asInt(key, "list index");
            if (idx < 0 || idx >= list.size()) {
                throw new ScriptError("List index out of range: " + idx, line, -1);
            }
            return list.get(idx);
        }
        if (target instanceof Map<?, ?> map) {
            return map.get(ScriptValue.asString(key));
        }
        if (target instanceof String s) {
            int idx = ScriptValue.asInt(key, "string index");
            if (idx < 0 || idx >= s.length()) {
                throw new ScriptError("String index out of range: " + idx, line, -1);
            }
            return String.valueOf(s.charAt(idx));
        }
        throw new ScriptError("Cannot index " + ScriptValue.describe(target) + ".", line, -1);
    }

    @SuppressWarnings("unchecked")
    private void applyIndexAssign(Object target, Object key, Object value, int line) {
        if (target instanceof List<?> list && list instanceof ArrayList) {
            int idx = ScriptValue.asInt(key, "list index");
            ArrayList<Object> mutable = (ArrayList<Object>) list;
            if (idx < 0 || idx > mutable.size()) {
                throw new ScriptError("List index out of range: " + idx, line, -1);
            }
            if (idx == mutable.size()) mutable.add(value);
            else mutable.set(idx, value);
            return;
        }
        if (target instanceof Map<?, ?> rawMap && rawMap instanceof HashMap) {
            ((HashMap<String, Object>) rawMap).put(ScriptValue.asString(key), value);
            return;
        }
        throw new ScriptError("Cannot assign to index of " + ScriptValue.describe(target) + ".", line, -1);
    }

    private List<?> toIterable(Object value, int line) {
        if (value instanceof List<?> list) return list;
        if (value instanceof Map<?, ?> map) return new ArrayList<>(map.keySet());
        if (value instanceof String s) {
            List<String> chars = new ArrayList<>(s.length());
            for (int i = 0; i < s.length(); i++) chars.add(String.valueOf(s.charAt(i)));
            return chars;
        }
        if (value instanceof Number n) {
            int upper = (int) Math.floor(n.doubleValue());
            List<Double> range = new ArrayList<>(Math.max(0, upper));
            for (int i = 0; i < upper; i++) range.add((double) i);
            return range;
        }
        throw new ScriptError("Cannot iterate value of type " + ScriptValue.describe(value) + ".", line, -1);
    }

    private void bumpBudget() {
        instructionCount++;
        if (instructionCount > instructionBudget) {
            throw new ScriptError("Script exceeded instruction budget (" + instructionBudget + ").");
        }
    }

    static final class BreakSignal extends RuntimeException {
        BreakSignal() { super(null, null, false, false); }
    }

    static final class ContinueSignal extends RuntimeException {
        ContinueSignal() { super(null, null, false, false); }
    }

    static final class ReturnSignal extends RuntimeException {
        final Object value;
        ReturnSignal(Object value) {
            super(null, null, false, false);
            this.value = value;
        }
    }
}
