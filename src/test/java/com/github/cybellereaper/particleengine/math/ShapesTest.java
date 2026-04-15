package com.github.cybellereaper.particleengine.math;

import com.github.cybellereaper.particleengine.effect.EmitterDefinition;
import com.github.cybellereaper.particleengine.effect.ShapeType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class ShapesTest {
    @Test
    void lineProducesRequestedPointCount() {
        EmitterDefinition emitter = new EmitterDefinition(ShapeType.LINE, 1, 10, Map.of("from", java.util.List.of(0,0,0), "to", java.util.List.of(0,0,5)));
        assertEquals(10, Shapes.line(emitter, 0, new Random(1)).size());
    }

    @Test
    void circleProducesRequestedPointCount() {
        EmitterDefinition emitter = new EmitterDefinition(ShapeType.CIRCLE, 1, 12, Map.of("radius", 2.0));
        assertEquals(12, Shapes.circle(emitter, 0, new Random(1)).size());
    }

    @Test
    void polylineSamplesAcrossMultipleSegments() {
        EmitterDefinition emitter = new EmitterDefinition(
                ShapeType.POLYLINE,
                1,
                5,
                Map.of("path", java.util.List.of(
                        java.util.List.of(0, 0, 0),
                        java.util.List.of(0, 0, 2),
                        java.util.List.of(0, 2, 2)
                ))
        );

        assertIterableEquals(
                java.util.List.of(
                        new Vec3(0, 0, 0),
                        new Vec3(0, 0, 1),
                        new Vec3(0, 0, 2),
                        new Vec3(0, 1, 2),
                        new Vec3(0, 2, 2)
                ),
                Shapes.polyline(emitter, 0, new Random(1))
        );
    }

    @Test
    void polylineFallsBackToLineWhenPathIsMissing() {
        EmitterDefinition emitter = new EmitterDefinition(
                ShapeType.POLYLINE,
                1,
                3,
                Map.of("from", java.util.List.of(0, 0, 0), "to", java.util.List.of(0, 0, 2))
        );

        assertIterableEquals(
                java.util.List.of(
                        new Vec3(0, 0, 0),
                        new Vec3(0, 0, 1),
                        new Vec3(0, 0, 2)
                ),
                Shapes.polyline(emitter, 0, new Random(1))
        );
    }

    @Test
    void polylineAcceptsPathAsObjectPoints() {
        EmitterDefinition emitter = new EmitterDefinition(
                ShapeType.POLYLINE,
                1,
                3,
                Map.of("path", java.util.List.of(
                        Map.of("x", 0.0, "y", 0.0, "z", -1.0),
                        Map.of("x", 0.0, "y", 0.0, "z", 0.0),
                        Map.of("x", 0.0, "y", 0.0, "z", 1.0)
                ))
        );

        assertIterableEquals(
                java.util.List.of(
                        new Vec3(0, 0, -1),
                        new Vec3(0, 0, 0),
                        new Vec3(0, 0, 1)
                ),
                Shapes.polyline(emitter, 0, new Random(1))
        );
    }

    @Test
    void polylineIgnoresInvalidPathEntries() {
        EmitterDefinition emitter = new EmitterDefinition(
                ShapeType.POLYLINE,
                1,
                3,
                Map.of(
                        "from", java.util.List.of(1, 0, 0),
                        "to", java.util.List.of(1, 0, 2),
                        "path", java.util.List.of(
                                "bad",
                                java.util.List.of(0, 0, 0),
                                Map.of("x", 0.0, "y", 0.0, "z", 2.0)
                        )
                )
        );

        assertIterableEquals(
                java.util.List.of(
                        new Vec3(0, 0, 0),
                        new Vec3(0, 0, 1),
                        new Vec3(0, 0, 2)
                ),
                Shapes.polyline(emitter, 0, new Random(1))
        );
    }

    @Test
    void circleInterpolatesRadiusFromKeyframes() {
        EmitterDefinition emitter = new EmitterDefinition(
                ShapeType.CIRCLE,
                1,
                4,
                Map.of("radius", Map.of(
                        "keyframes", java.util.List.of(
                                Map.of("tick", 0, "value", 0.2),
                                Map.of("tick", 10, "value", 1.0)
                        )
                ))
        );

        java.util.List<Vec3> atTickFive = Shapes.circle(emitter, 5, new Random(1));
        assertEquals(0.6, atTickFive.getFirst().x(), 0.0001);
    }

    @Test
    void polylineInterpolatesPathFromKeyframes() {
        EmitterDefinition emitter = new EmitterDefinition(
                ShapeType.POLYLINE,
                1,
                3,
                Map.of("path", Map.of(
                        "keyframes", java.util.List.of(
                                Map.of(
                                        "tick", 0,
                                        "value", java.util.List.of(
                                                Map.of("x", 0.0, "y", 0.0, "z", 0.0),
                                                Map.of("x", 0.0, "y", 0.0, "z", 2.0)
                                        )
                                ),
                                Map.of(
                                        "tick", 10,
                                        "value", java.util.List.of(
                                                Map.of("x", 0.0, "y", 2.0, "z", 0.0),
                                                Map.of("x", 0.0, "y", 2.0, "z", 2.0)
                                        )
                                )
                        )
                ))
        );

        assertIterableEquals(
                java.util.List.of(
                        new Vec3(0, 1, 0),
                        new Vec3(0, 1, 1),
                        new Vec3(0, 1, 2)
                ),
                Shapes.polyline(emitter, 5, new Random(1))
        );
    }
}
