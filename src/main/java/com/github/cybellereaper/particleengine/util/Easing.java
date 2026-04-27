package com.github.cybellereaper.particleengine.util;

/**
 * Collection of easing curves used by the timeline engine and keyframed value
 * resolvers. All easings expect {@code t} in the range {@code [0, 1]} and
 * return a remapped value in the same range (with the exception of certain
 * overshooting easings such as {@code back} or {@code elastic}, which
 * intentionally exceed the endpoints).
 */
public final class Easing {
    private Easing() {}

    private static final double BACK_C1 = 1.70158D;
    private static final double BACK_C2 = BACK_C1 * 1.525D;
    private static final double BACK_C3 = BACK_C1 + 1D;
    private static final double ELASTIC_C4 = (2D * Math.PI) / 3D;
    private static final double ELASTIC_C5 = (2D * Math.PI) / 4.5D;

    public static double apply(String easing, double t) {
        return switch (easing == null ? "linear" : easing.toLowerCase()) {
            case "ease_in", "ease_in_quad" -> t * t;
            case "ease_out", "ease_out_quad" -> 1D - Math.pow(1D - t, 2D);
            case "ease_in_out", "ease_in_out_quad" -> t < 0.5D ? 2D * t * t : 1D - Math.pow(-2D * t + 2D, 2D) / 2D;
            case "ease_in_cubic" -> t * t * t;
            case "ease_out_cubic" -> 1D - Math.pow(1D - t, 3D);
            case "ease_in_out_cubic" -> t < 0.5D ? 4D * t * t * t : 1D - Math.pow(-2D * t + 2D, 3D) / 2D;
            case "ease_in_quart" -> t * t * t * t;
            case "ease_out_quart" -> 1D - Math.pow(1D - t, 4D);
            case "ease_in_out_quart" -> t < 0.5D ? 8D * t * t * t * t : 1D - Math.pow(-2D * t + 2D, 4D) / 2D;
            case "ease_in_sine" -> 1D - Math.cos((t * Math.PI) / 2D);
            case "ease_out_sine" -> Math.sin((t * Math.PI) / 2D);
            case "ease_in_out_sine" -> -(Math.cos(Math.PI * t) - 1D) / 2D;
            case "ease_in_expo" -> t == 0D ? 0D : Math.pow(2D, 10D * t - 10D);
            case "ease_out_expo" -> t == 1D ? 1D : 1D - Math.pow(2D, -10D * t);
            case "ease_in_out_expo" -> easeInOutExpo(t);
            case "ease_in_circ" -> 1D - Math.sqrt(1D - Math.pow(t, 2D));
            case "ease_out_circ" -> Math.sqrt(1D - Math.pow(t - 1D, 2D));
            case "ease_in_out_circ" -> easeInOutCirc(t);
            case "ease_in_back" -> BACK_C3 * t * t * t - BACK_C1 * t * t;
            case "ease_out_back" -> 1D + BACK_C3 * Math.pow(t - 1D, 3D) + BACK_C1 * Math.pow(t - 1D, 2D);
            case "ease_in_out_back" -> easeInOutBack(t);
            case "ease_in_elastic" -> easeInElastic(t);
            case "ease_out_elastic" -> easeOutElastic(t);
            case "ease_in_out_elastic" -> easeInOutElastic(t);
            case "ease_in_bounce" -> 1D - bounceOut(1D - t);
            case "ease_out_bounce" -> bounceOut(t);
            case "ease_in_out_bounce" -> t < 0.5D ? (1D - bounceOut(1D - 2D * t)) / 2D : (1D + bounceOut(2D * t - 1D)) / 2D;
            case "step" -> t < 1D ? 0D : 1D;
            case "smoothstep" -> t * t * (3D - 2D * t);
            case "smootherstep" -> t * t * t * (t * (t * 6D - 15D) + 10D);
            default -> t;
        };
    }

    private static double easeInOutExpo(double t) {
        if (t == 0D) return 0D;
        if (t == 1D) return 1D;
        return t < 0.5D
                ? Math.pow(2D, 20D * t - 10D) / 2D
                : (2D - Math.pow(2D, -20D * t + 10D)) / 2D;
    }

    private static double easeInOutCirc(double t) {
        return t < 0.5D
                ? (1D - Math.sqrt(1D - Math.pow(2D * t, 2D))) / 2D
                : (Math.sqrt(1D - Math.pow(-2D * t + 2D, 2D)) + 1D) / 2D;
    }

    private static double easeInOutBack(double t) {
        return t < 0.5D
                ? (Math.pow(2D * t, 2D) * ((BACK_C2 + 1D) * 2D * t - BACK_C2)) / 2D
                : (Math.pow(2D * t - 2D, 2D) * ((BACK_C2 + 1D) * (t * 2D - 2D) + BACK_C2) + 2D) / 2D;
    }

    private static double easeInElastic(double t) {
        if (t == 0D) return 0D;
        if (t == 1D) return 1D;
        return -Math.pow(2D, 10D * t - 10D) * Math.sin((t * 10D - 10.75D) * ELASTIC_C4);
    }

    private static double easeOutElastic(double t) {
        if (t == 0D) return 0D;
        if (t == 1D) return 1D;
        return Math.pow(2D, -10D * t) * Math.sin((t * 10D - 0.75D) * ELASTIC_C4) + 1D;
    }

    private static double easeInOutElastic(double t) {
        if (t == 0D) return 0D;
        if (t == 1D) return 1D;
        return t < 0.5D
                ? -(Math.pow(2D, 20D * t - 10D) * Math.sin((20D * t - 11.125D) * ELASTIC_C5)) / 2D
                : (Math.pow(2D, -20D * t + 10D) * Math.sin((20D * t - 11.125D) * ELASTIC_C5)) / 2D + 1D;
    }

    private static double bounceOut(double t) {
        double n1 = 7.5625D;
        double d1 = 2.75D;
        if (t < 1D / d1) {
            return n1 * t * t;
        } else if (t < 2D / d1) {
            double t2 = t - 1.5D / d1;
            return n1 * t2 * t2 + 0.75D;
        } else if (t < 2.5D / d1) {
            double t2 = t - 2.25D / d1;
            return n1 * t2 * t2 + 0.9375D;
        }
        double t2 = t - 2.625D / d1;
        return n1 * t2 * t2 + 0.984375D;
    }
}
