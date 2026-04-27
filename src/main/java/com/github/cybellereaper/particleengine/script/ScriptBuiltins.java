package com.github.cybellereaper.particleengine.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Collection of built-in functions available to every PEScript program.
 *
 * <p>Functions registered here cover three categories:
 * <ul>
 *   <li>Engine bridges - {@code spawn}, {@code stop}, {@code stop_tag},
 *       {@code stop_all}, {@code pause}, {@code pause_tag}, {@code resume},
 *       {@code resume_tag}, {@code log}.</li>
 *   <li>Math helpers - {@code sin}, {@code cos}, {@code tan}, {@code abs},
 *       {@code floor}, {@code ceil}, {@code round}, {@code sqrt},
 *       {@code pow}, {@code min}, {@code max}, {@code clamp}, {@code lerp},
 *       {@code random}, {@code randint}.</li>
 *   <li>Collection helpers - {@code len}, {@code range}, {@code list},
 *       {@code map}, {@code push}, {@code keys}, {@code values},
 *       {@code str}, {@code num}, {@code at}, {@code tags}.</li>
 * </ul>
 */
public final class ScriptBuiltins {
    private static final Random SHARED_RANDOM = new Random();

    private ScriptBuiltins() {}

    public static void install(ScriptEnvironment env, ScriptHost host) {
        env.define("spawn", new SpawnCallable(host));
        env.define("stop", host == null ? noop("stop") : varArgs("stop", 1, args -> {
            UUID id = parseUuid(args.get(0));
            return host.stop(id);
        }));
        env.define("stop_tag", host == null ? noop("stop_tag") : varArgs("stop_tag", 1, args -> host.stopByTag(asString(args.get(0)))));
        env.define("stop_all", host == null ? noop("stop_all") : varArgs("stop_all", 0, args -> host.stopAll()));
        env.define("pause", host == null ? noop("pause") : varArgs("pause", 1, args -> host.pause(parseUuid(args.get(0)))));
        env.define("pause_tag", host == null ? noop("pause_tag") : varArgs("pause_tag", 1, args -> host.pauseByTag(asString(args.get(0)))));
        env.define("resume", host == null ? noop("resume") : varArgs("resume", 1, args -> host.resume(parseUuid(args.get(0)))));
        env.define("resume_tag", host == null ? noop("resume_tag") : varArgs("resume_tag", 1, args -> host.resumeByTag(asString(args.get(0)))));
        env.define("log", varArgs("log", -1, args -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(' ');
                sb.append(ScriptValue.asString(args.get(i)));
            }
            if (host != null) host.log(sb.toString());
            return null;
        }));
        env.define("template_exists", host == null ? noop("template_exists") : varArgs("template_exists", 1, args -> host.templateExists(asString(args.get(0)))));

        env.define("at", varArgs("at", -1, args -> {
            if (args.size() == 3) return new ScriptLocation(null, asNumber(args.get(0)), asNumber(args.get(1)), asNumber(args.get(2)));
            if (args.size() == 4) return new ScriptLocation(asString(args.get(0)), asNumber(args.get(1)), asNumber(args.get(2)), asNumber(args.get(3)));
            throw new ScriptError("at(...) expects 3 or 4 arguments (x,y,z) or (world,x,y,z)");
        }));
        env.define("tags", varArgs("tags", -1, args -> {
            Set<String> set = new HashSet<>();
            for (Object arg : args) set.add(ScriptValue.asString(arg));
            return List.copyOf(set);
        }));

        // math
        env.define("sin", unary("sin", Math::sin));
        env.define("cos", unary("cos", Math::cos));
        env.define("tan", unary("tan", Math::tan));
        env.define("asin", unary("asin", Math::asin));
        env.define("acos", unary("acos", Math::acos));
        env.define("atan", unary("atan", Math::atan));
        env.define("abs", unary("abs", Math::abs));
        env.define("floor", unary("floor", Math::floor));
        env.define("ceil", unary("ceil", Math::ceil));
        env.define("round", unary("round", x -> (double) Math.round(x)));
        env.define("sqrt", unary("sqrt", Math::sqrt));
        env.define("sign", unary("sign", Math::signum));
        env.define("pi", varArgs("pi", 0, args -> Math.PI));
        env.define("e", varArgs("e", 0, args -> Math.E));
        env.define("pow", varArgs("pow", 2, args -> Math.pow(asNumber(args.get(0)), asNumber(args.get(1)))));
        env.define("atan2", varArgs("atan2", 2, args -> Math.atan2(asNumber(args.get(0)), asNumber(args.get(1)))));
        env.define("min", varArgs("min", -1, ScriptBuiltins::minFn));
        env.define("max", varArgs("max", -1, ScriptBuiltins::maxFn));
        env.define("clamp", varArgs("clamp", 3, args -> {
            double v = asNumber(args.get(0));
            double lo = asNumber(args.get(1));
            double hi = asNumber(args.get(2));
            return Math.min(Math.max(v, lo), hi);
        }));
        env.define("lerp", varArgs("lerp", 3, args -> {
            double a = asNumber(args.get(0));
            double b = asNumber(args.get(1));
            double t = asNumber(args.get(2));
            return a + (b - a) * t;
        }));
        env.define("random", varArgs("random", -1, args -> {
            if (args.isEmpty()) return SHARED_RANDOM.nextDouble();
            if (args.size() == 1) return SHARED_RANDOM.nextDouble() * asNumber(args.get(0));
            if (args.size() == 2) {
                double lo = asNumber(args.get(0));
                double hi = asNumber(args.get(1));
                return lo + (hi - lo) * SHARED_RANDOM.nextDouble();
            }
            throw new ScriptError("random(...) expects 0, 1, or 2 arguments.");
        }));
        env.define("randint", varArgs("randint", 2, args -> {
            int lo = (int) Math.floor(asNumber(args.get(0)));
            int hi = (int) Math.floor(asNumber(args.get(1)));
            if (hi < lo) throw new ScriptError("randint: high bound must be >= low bound.");
            return (double) (lo + SHARED_RANDOM.nextInt(hi - lo + 1));
        }));

        // collections
        env.define("len", varArgs("len", 1, args -> {
            Object v = args.get(0);
            if (v instanceof List<?> l) return (double) l.size();
            if (v instanceof Map<?, ?> m) return (double) m.size();
            if (v instanceof String s) return (double) s.length();
            if (v == null) return 0D;
            throw new ScriptError("len() not supported on " + ScriptValue.describe(v));
        }));
        env.define("range", varArgs("range", -1, args -> {
            int start;
            int end;
            int step = 1;
            if (args.size() == 1) {
                start = 0;
                end = (int) Math.floor(asNumber(args.get(0)));
            } else if (args.size() == 2) {
                start = (int) Math.floor(asNumber(args.get(0)));
                end = (int) Math.floor(asNumber(args.get(1)));
            } else if (args.size() == 3) {
                start = (int) Math.floor(asNumber(args.get(0)));
                end = (int) Math.floor(asNumber(args.get(1)));
                step = (int) Math.floor(asNumber(args.get(2)));
                if (step == 0) throw new ScriptError("range step cannot be zero.");
            } else {
                throw new ScriptError("range() expects 1-3 arguments.");
            }
            List<Object> out = new ArrayList<>();
            if (step > 0) {
                for (int i = start; i < end; i += step) out.add((double) i);
            } else {
                for (int i = start; i > end; i += step) out.add((double) i);
            }
            return out;
        }));
        env.define("list", varArgs("list", -1, ArrayList::new));
        env.define("map", varArgs("map", -1, args -> {
            if (args.size() % 2 != 0) throw new ScriptError("map() requires key/value pairs.");
            Map<String, Object> out = new HashMap<>();
            for (int i = 0; i < args.size(); i += 2) {
                out.put(ScriptValue.asString(args.get(i)), args.get(i + 1));
            }
            return out;
        }));
        env.define("push", varArgs("push", 2, args -> {
            if (!(args.get(0) instanceof List<?> raw)) {
                throw new ScriptError("push() expects a list as first argument.");
            }
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) raw;
            list.add(args.get(1));
            return list;
        }));
        env.define("keys", varArgs("keys", 1, args -> {
            if (!(args.get(0) instanceof Map<?, ?> map)) {
                throw new ScriptError("keys() expects a map.");
            }
            List<Object> out = new ArrayList<>();
            for (Object k : map.keySet()) out.add(ScriptValue.asString(k));
            return out;
        }));
        env.define("values", varArgs("values", 1, args -> {
            if (!(args.get(0) instanceof Map<?, ?> map)) {
                throw new ScriptError("values() expects a map.");
            }
            return new ArrayList<>(map.values());
        }));
        env.define("str", varArgs("str", 1, args -> ScriptValue.asString(args.get(0))));
        env.define("num", varArgs("num", 1, args -> {
            try {
                return ScriptValue.asNumber(args.get(0), "num");
            } catch (ScriptError ex) {
                return Double.NaN;
            }
        }));
        env.define("contains", varArgs("contains", 2, args -> {
            Object container = args.get(0);
            Object needle = args.get(1);
            if (container instanceof List<?> list) {
                for (Object item : list) if (ScriptValue.equals(item, needle)) return true;
                return false;
            }
            if (container instanceof Map<?, ?> map) return map.containsKey(ScriptValue.asString(needle));
            if (container instanceof String s) return s.contains(ScriptValue.asString(needle));
            throw new ScriptError("contains() not supported on " + ScriptValue.describe(container));
        }));
    }

    private static UUID parseUuid(Object raw) {
        if (raw instanceof UUID id) return id;
        try {
            return UUID.fromString(ScriptValue.asString(raw));
        } catch (IllegalArgumentException ex) {
            throw new ScriptError("Invalid UUID '" + raw + "'.");
        }
    }

    private static double asNumber(Object value) {
        return ScriptValue.asNumber(value, "math");
    }

    private static String asString(Object value) {
        return ScriptValue.asString(value);
    }

    private static Object minFn(List<Object> args) {
        if (args.isEmpty()) throw new ScriptError("min() requires at least one argument.");
        double best = asNumber(args.get(0));
        for (int i = 1; i < args.size(); i++) best = Math.min(best, asNumber(args.get(i)));
        return best;
    }

    private static Object maxFn(List<Object> args) {
        if (args.isEmpty()) throw new ScriptError("max() requires at least one argument.");
        double best = asNumber(args.get(0));
        for (int i = 1; i < args.size(); i++) best = Math.max(best, asNumber(args.get(i)));
        return best;
    }

    private static ScriptCallable unary(String name, Function<Double, Double> fn) {
        return varArgs(name, 1, args -> fn.apply(asNumber(args.get(0))));
    }

    private static ScriptCallable varArgs(String name, int arity, Function<List<Object>, Object> fn) {
        return new ScriptCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(ScriptInterpreter interpreter, List<Object> arguments) {
                return fn.apply(arguments);
            }
            @Override public String describe() { return "<builtin " + name + ">"; }
        };
    }

    private static ScriptCallable noop(String name) {
        return varArgs(name, -1, args -> null);
    }

    private static final class SpawnCallable implements ScriptCallable {
        private final ScriptHost host;

        SpawnCallable(ScriptHost host) {
            this.host = host;
        }

        @Override public int arity() { return -1; }

        @Override
        public Object call(ScriptInterpreter interpreter, List<Object> arguments) {
            if (host == null) return null;
            if (arguments.isEmpty()) {
                throw new ScriptError("spawn() requires at least a template id.");
            }
            String template = ScriptValue.asString(arguments.get(0));
            ScriptLocation location = null;
            Set<String> tagSet = Set.of();
            Map<String, Object> overrides = Map.of();

            for (int i = 1; i < arguments.size(); i++) {
                Object arg = arguments.get(i);
                if (arg instanceof ScriptLocation loc) {
                    location = loc;
                } else if (arg instanceof List<?> list) {
                    Set<String> tmp = new HashSet<>();
                    for (Object item : list) tmp.add(ScriptValue.asString(item));
                    tagSet = Set.copyOf(tmp);
                } else if (arg instanceof Map<?, ?> map) {
                    Map<String, Object> tmp = new HashMap<>();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        tmp.put(ScriptValue.asString(entry.getKey()), entry.getValue());
                    }
                    overrides = Map.copyOf(tmp);
                } else if (arg instanceof String s) {
                    if (s.startsWith("tag:")) {
                        tagSet = Set.of(s.substring(4));
                    }
                }
            }
            UUID id = host.spawn(template, location, tagSet, overrides);
            return id == null ? null : id.toString();
        }

        @Override public String describe() { return "<builtin spawn>"; }
    }
}
