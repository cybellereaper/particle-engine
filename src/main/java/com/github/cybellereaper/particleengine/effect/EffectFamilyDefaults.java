package com.github.cybellereaper.particleengine.effect;

import java.util.List;
import java.util.Map;

public final class EffectFamilyDefaults {
    private EffectFamilyDefaults() {}

    public static List<EmitterDefinition> defaultEmittersFor(EffectFamily family) {
        return switch (family) {
            case TRAIL -> List.of(lineEmitter(10, 0.8));
            case AURA -> List.of(sphereEmitter(20, 0.9));
            case CIRCLE -> List.of(circleEmitter(24, 1.0));
            case SPIRAL -> List.of(spiralEmitter(28, 1.2, 2.0, 3.0));
            case HELIX -> List.of(helixEmitter(32, 1.0, 2.0, 2.0));
            case BEAM -> List.of(lineEmitter(16, 3.0));
            case EXPLOSION -> List.of(sphereEmitter(48, 1.8));
            case ORBITAL -> List.of(circleEmitter(24, 1.3));
            case WINGS -> List.of(
                    new EmitterDefinition(ShapeType.BEZIER, 1, 14, Map.of(
                            "p0", List.of(0.0, 0.8, 0.0),
                            "p1", List.of(-0.6, 1.2, -0.2),
                            "p2", List.of(-1.0, 0.8, -0.5),
                            "p3", List.of(-1.3, 0.3, -0.8)
                    )),
                    new EmitterDefinition(ShapeType.BEZIER, 1, 14, Map.of(
                            "p0", List.of(0.0, 0.8, 0.0),
                            "p1", List.of(0.6, 1.2, -0.2),
                            "p2", List.of(1.0, 0.8, -0.5),
                            "p3", List.of(1.3, 0.3, -0.8)
                    ))
            );
            case GLYPH -> List.of(new EmitterDefinition(ShapeType.RING, 1, 24, Map.of("radius", 1.0)));
            case MISSILE -> List.of(lineEmitter(12, 1.4));
            case REGION_FILL -> List.of(new EmitterDefinition(ShapeType.BOX, 1, 48, Map.of("size", List.of(3.0, 2.5, 3.0))));
            case COMPOSITE -> List.of(new EmitterDefinition(ShapeType.POINT, 1, 1, Map.of()));
        };
    }

    private static EmitterDefinition lineEmitter(int points, double zDistance) {
        return new EmitterDefinition(ShapeType.LINE, 1, points, Map.of(
                "from", List.of(0.0, 0.0, 0.0),
                "to", List.of(0.0, 0.0, zDistance)
        ));
    }

    private static EmitterDefinition circleEmitter(int points, double radius) {
        return new EmitterDefinition(ShapeType.CIRCLE, 1, points, Map.of("radius", radius));
    }

    private static EmitterDefinition sphereEmitter(int points, double radius) {
        return new EmitterDefinition(ShapeType.SPHERE, 1, points, Map.of("radius", radius));
    }

    private static EmitterDefinition spiralEmitter(int points, double radius, double height, double turns) {
        return new EmitterDefinition(ShapeType.SPIRAL, 1, points, Map.of(
                "radius", radius,
                "height", height,
                "turns", turns
        ));
    }

    private static EmitterDefinition helixEmitter(int points, double radius, double height, double turns) {
        return new EmitterDefinition(ShapeType.HELIX, 1, points, Map.of(
                "radius", radius,
                "height", height,
                "turns", turns
        ));
    }
}
