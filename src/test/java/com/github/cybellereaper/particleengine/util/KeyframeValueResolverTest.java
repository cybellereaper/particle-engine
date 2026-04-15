package com.github.cybellereaper.particleengine.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class KeyframeValueResolverTest {

    @Test
    void interpolatesNumericKeyframes() {
        Map<String, Object> params = Map.of(
                "radius", Map.of(
                        "keyframes", List.of(
                                Map.of("tick", 0, "value", 0.2),
                                Map.of("tick", 10, "value", 1.2)
                        )
                )
        );

        assertEquals(0.7, KeyframeValueResolver.resolveNumber(params, "radius", 5, 0.0), 0.0001);
    }

    @Test
    void interpolatesNestedListAndMapValues() {
        Map<String, Object> params = Map.of(
                "path", Map.of(
                        "keyframes", List.of(
                                Map.of(
                                        "tick", 0,
                                        "value", List.of(
                                                Map.of("x", 0.0, "y", 0.0, "z", 0.0),
                                                Map.of("x", 2.0, "y", 0.0, "z", 0.0)
                                        )
                                ),
                                Map.of(
                                        "tick", 10,
                                        "value", List.of(
                                                Map.of("x", 0.0, "y", 1.0, "z", 0.0),
                                                Map.of("x", 2.0, "y", 1.0, "z", 0.0)
                                        )
                                )
                        )
                )
        );

        Object resolved = KeyframeValueResolver.resolveObject(params, "path", 5);
        List<?> path = assertInstanceOf(List.class, resolved);
        Map<?, ?> p0 = assertInstanceOf(Map.class, path.getFirst());
        assertEquals(0.5, ((Number) p0.get("y")).doubleValue(), 0.0001);
    }

    @Test
    void clampsToBoundaryKeyframes() {
        Map<String, Object> params = Map.of(
                "speed", Map.of(
                        "keyframes", List.of(
                                Map.of("tick", 5, "value", 3.0),
                                Map.of("tick", 10, "value", 1.0)
                        )
                )
        );

        assertEquals(3.0, KeyframeValueResolver.resolveNumber(params, "speed", 0, 0.0), 0.0001);
        assertEquals(1.0, KeyframeValueResolver.resolveNumber(params, "speed", 20, 0.0), 0.0001);
    }
}
