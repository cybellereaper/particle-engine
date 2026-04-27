package com.github.cybellereaper.particleengine.script;

import java.util.List;
import java.util.Map;

/**
 * Helpers for converting between PEScript runtime values and Java values.
 * PEScript represents:
 * <ul>
 *   <li>numbers as {@link Double}</li>
 *   <li>booleans as {@link Boolean}</li>
 *   <li>strings as {@link String}</li>
 *   <li>lists as {@link List} of values</li>
 *   <li>maps as {@link Map} keyed by string</li>
 *   <li>{@code null} as Java {@code null}</li>
 *   <li>callables as {@link ScriptCallable}</li>
 * </ul>
 */
public final class ScriptValue {
    private ScriptValue() {}

    public static boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0D;
        if (value instanceof String s) return !s.isEmpty();
        if (value instanceof List<?> l) return !l.isEmpty();
        if (value instanceof Map<?, ?> m) return !m.isEmpty();
        return true;
    }

    public static double asNumber(Object value, String context) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof Boolean b) return b ? 1D : 0D;
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ex) {
                throw new ScriptError(context + " expected a number but got string '" + s + "'.");
            }
        }
        throw new ScriptError(context + " expected a number but got " + describe(value) + ".");
    }

    public static int asInt(Object value, String context) {
        return (int) Math.round(asNumber(value, context));
    }

    public static String asString(Object value) {
        if (value == null) return "null";
        if (value instanceof Double d) {
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return Long.toString(d.longValue());
            }
            return d.toString();
        }
        return value.toString();
    }

    public static String describe(Object value) {
        if (value == null) return "null";
        if (value instanceof Number) return "number";
        if (value instanceof String) return "string";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof List<?>) return "list";
        if (value instanceof Map<?, ?>) return "map";
        if (value instanceof ScriptCallable) return "function";
        return value.getClass().getSimpleName();
    }

    public static boolean equals(Object a, Object b) {
        if (a == null || b == null) return a == b;
        if (a instanceof Number na && b instanceof Number nb) {
            return na.doubleValue() == nb.doubleValue();
        }
        return a.equals(b);
    }
}
