package com.github.cybellereaper.particleengine.util;

import java.util.*;

public final class KeyframeValueResolver {
    private KeyframeValueResolver() {
    }

    public static double resolveNumber(Map<String, Object> params, String key, int tick, double fallback) {
        Object resolved = resolve(params.get(key), tick);
        return asDouble(resolved, fallback);
    }

    public static Object resolveObject(Map<String, Object> params, String key, int tick) {
        return resolve(params.get(key), tick);
    }

    private static Object resolve(Object raw, int tick) {
        if (!(raw instanceof Map<?, ?> rawMap)) {
            return raw;
        }

        Object keyframesRaw = rawMap.get("keyframes");
        if (!(keyframesRaw instanceof List<?> keyframes) || keyframes.isEmpty()) {
            return raw;
        }

        List<KeyframeEntry> entries = parseEntries(keyframes);
        if (entries.isEmpty()) {
            return raw;
        }

        entries.sort(Comparator.comparingInt(KeyframeEntry::tick));
        if (tick <= entries.getFirst().tick()) {
            return entries.getFirst().value();
        }

        KeyframeEntry last = entries.getLast();
        if (tick >= last.tick()) {
            return last.value();
        }

        for (int i = 0; i < entries.size() - 1; i++) {
            KeyframeEntry left = entries.get(i);
            KeyframeEntry right = entries.get(i + 1);
            if (tick < left.tick() || tick > right.tick()) {
                continue;
            }
            if (right.tick() == left.tick()) {
                return right.value();
            }

            double alpha = (tick - left.tick()) / (double) (right.tick() - left.tick());
            alpha = Easing.apply(right.easing(), alpha);
            return interpolate(left.value(), right.value(), alpha);
        }

        return last.value();
    }

    private static List<KeyframeEntry> parseEntries(List<?> rawKeyframes) {
        List<KeyframeEntry> entries = new ArrayList<>();
        for (Object rawFrame : rawKeyframes) {
            if (!(rawFrame instanceof Map<?, ?> frameMap)) {
                continue;
            }
            int tick = asInt(frameMap.get("tick"), 0);
            Object easingValue = frameMap.containsKey("easing") ? frameMap.get("easing") : "linear";
            String easing = String.valueOf(easingValue);
            Object value = frameMap.get("value");
            entries.add(new KeyframeEntry(tick, easing, value));
        }
        return entries;
    }

    private static Object interpolate(Object left, Object right, double alpha) {
        if (left instanceof Number || right instanceof Number) {
            double leftNumber = asDouble(left, asDouble(right, 0D));
            double rightNumber = asDouble(right, leftNumber);
            return leftNumber + ((rightNumber - leftNumber) * alpha);
        }

        if (left instanceof Map<?, ?> leftMap && right instanceof Map<?, ?> rightMap) {
            Map<String, Object> merged = new HashMap<>();
            Set<String> keys = new HashSet<>();
            leftMap.keySet().forEach(k -> keys.add(String.valueOf(k)));
            rightMap.keySet().forEach(k -> keys.add(String.valueOf(k)));
            for (String key : keys) {
                Object leftValue = leftMap.get(key);
                Object rightValue = rightMap.get(key);
                Object chosenRight = rightValue == null ? leftValue : rightValue;
                Object chosenLeft = leftValue == null ? chosenRight : leftValue;
                merged.put(key, interpolate(chosenLeft, chosenRight, alpha));
            }
            return Map.copyOf(merged);
        }

        if (left instanceof List<?> leftList && right instanceof List<?> rightList && leftList.size() == rightList.size()) {
            List<Object> out = new ArrayList<>(leftList.size());
            for (int i = 0; i < leftList.size(); i++) {
                out.add(interpolate(leftList.get(i), rightList.get(i), alpha));
            }
            return List.copyOf(out);
        }

        return alpha < 0.5 ? left : right;
    }

    private static int asInt(Object raw, int fallback) {
        if (raw instanceof Number n) {
            return n.intValue();
        }
        if (raw instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static double asDouble(Object raw, double fallback) {
        if (raw instanceof Number n) {
            return n.doubleValue();
        }
        if (raw instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private record KeyframeEntry(int tick, String easing, Object value) {
    }
}
