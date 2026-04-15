package com.github.cybellereaper.particleengine.effect;

import java.util.Locale;

public enum AnchorPoint {
    HEAD,
    BACK,
    FEET;

    public static AnchorPoint fromSelector(String selector) {
        if (selector == null || selector.isBlank()) return FEET;
        return switch (selector.trim().toLowerCase(Locale.ROOT)) {
            case "head" -> HEAD;
            case "back" -> BACK;
            default -> FEET;
        };
    }
}
