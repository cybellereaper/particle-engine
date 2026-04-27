package com.github.cybellereaper.particleengine.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasingTest {

    @Test
    void linearReturnsInputUnchanged() {
        assertEquals(0.0, Easing.apply("linear", 0.0), 1e-9);
        assertEquals(0.5, Easing.apply("linear", 0.5), 1e-9);
        assertEquals(1.0, Easing.apply("linear", 1.0), 1e-9);
    }

    @Test
    void unknownEasingFallsBackToLinear() {
        assertEquals(0.42, Easing.apply("nope", 0.42), 1e-9);
        assertEquals(0.42, Easing.apply(null, 0.42), 1e-9);
    }

    @Test
    void smoothstepIsMonotonicAndBounded() {
        double prev = -1D;
        for (int i = 0; i <= 10; i++) {
            double t = i / 10D;
            double v = Easing.apply("smoothstep", t);
            assertTrue(v >= 0.0 && v <= 1.0);
            assertTrue(v >= prev);
            prev = v;
        }
    }

    @Test
    void allCurvesAnchorAtZeroAndOne() {
        String[] easings = {
                "linear", "ease_in", "ease_out", "ease_in_out",
                "ease_in_quad", "ease_out_quad", "ease_in_out_quad",
                "ease_in_cubic", "ease_out_cubic", "ease_in_out_cubic",
                "ease_in_quart", "ease_out_quart", "ease_in_out_quart",
                "ease_in_sine", "ease_out_sine", "ease_in_out_sine",
                "ease_in_expo", "ease_out_expo", "ease_in_out_expo",
                "ease_in_circ", "ease_out_circ", "ease_in_out_circ",
                "ease_in_bounce", "ease_out_bounce", "ease_in_out_bounce",
                "ease_in_elastic", "ease_out_elastic", "ease_in_out_elastic",
                "ease_in_back", "ease_out_back", "ease_in_out_back",
                "step", "smoothstep", "smootherstep"
        };
        for (String name : easings) {
            assertEquals(0.0, Easing.apply(name, 0.0), 1e-6, name + " at t=0");
            assertEquals(1.0, Easing.apply(name, 1.0), 1e-6, name + " at t=1");
        }
    }

    @Test
    void bounceOutReachesPeaks() {
        double mid = Easing.apply("ease_out_bounce", 0.5);
        assertTrue(mid > 0.7 && mid < 0.85, "bounce midpoint should be around 0.77 but was " + mid);
    }

    @Test
    void caseInsensitive() {
        assertEquals(Easing.apply("EASE_IN_OUT", 0.3), Easing.apply("ease_in_out", 0.3), 1e-9);
    }
}
