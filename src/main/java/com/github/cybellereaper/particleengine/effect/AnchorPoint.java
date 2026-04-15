package com.github.cybellereaper.particleengine.effect;

import java.util.Locale;
import java.util.regex.Pattern;

public enum AnchorPoint {
    HEAD,
    BACK,
    FEET;

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-z]+");

    public static AnchorPoint fromSelector(String selector) {
        if (selector == null || selector.isBlank()) return FEET;

        String normalized = selector.trim().toLowerCase(Locale.ROOT);
        for (String token : TOKEN_SPLIT.split(normalized)) {
            if (token.isBlank()) continue;
            if (token.equals("head")) return HEAD;
            if (token.equals("back")) return BACK;
            if (token.equals("feet")) return FEET;
        }
        return FEET;
    }
}
