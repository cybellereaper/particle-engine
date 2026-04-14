package com.github.cybellereaper.particleengine.math;

import com.github.cybellereaper.particleengine.effect.EmitterDefinition;
import com.github.cybellereaper.particleengine.effect.ShapeType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ShapeRegistry {
    private final Map<ShapeType, ShapeGenerator> generators = new EnumMap<>(ShapeType.class);

    public ShapeRegistry() {
        register(ShapeType.POINT, Shapes::point);
        register(ShapeType.LINE, Shapes::line);
        register(ShapeType.RING, Shapes::circle);
        register(ShapeType.CIRCLE, Shapes::circle);
        register(ShapeType.SPHERE, Shapes::sphere);
        register(ShapeType.CYLINDER, Shapes::cylinder);
        register(ShapeType.BOX, Shapes::box);
        register(ShapeType.CONE, Shapes::cone);
        register(ShapeType.HELIX, Shapes::helix);
        register(ShapeType.SPIRAL, Shapes::spiral);
        register(ShapeType.BEZIER, Shapes::bezier);
        register(ShapeType.POLYLINE, Shapes::polyline);
    }

    public void register(ShapeType type, ShapeGenerator generator) {
        generators.put(type, generator);
    }

    public List<Vec3> sample(EmitterDefinition emitter, int tick, Random random) {
        return generators.getOrDefault(emitter.shape(), Shapes::point).generate(emitter, tick, random);
    }
}
