package com.github.cybellereaper.particleengine.math;

import com.github.cybellereaper.particleengine.effect.EmitterDefinition;

import java.util.List;
import java.util.Random;

public interface ShapeGenerator {
    List<Vec3> generate(EmitterDefinition emitter, int tick, Random random);
}
