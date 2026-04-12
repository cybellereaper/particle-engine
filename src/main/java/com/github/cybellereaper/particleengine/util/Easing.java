package com.github.cybellereaper.particleengine.util;

public final class Easing {
    private Easing() {}

    public static double apply(String easing, double t) {
        return switch (easing == null ? "linear" : easing.toLowerCase()) {
            case "ease_in" -> t * t;
            case "ease_out" -> 1D - Math.pow(1D - t, 2D);
            case "ease_in_out" -> t < 0.5 ? 2D * t * t : 1D - Math.pow(-2D * t + 2D, 2D) / 2D;
            default -> t;
        };
    }
}
