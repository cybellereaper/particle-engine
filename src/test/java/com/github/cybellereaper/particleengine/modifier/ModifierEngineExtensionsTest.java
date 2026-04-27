package com.github.cybellereaper.particleengine.modifier;

import com.github.cybellereaper.particleengine.effect.ModifierDefinition;
import com.github.cybellereaper.particleengine.math.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ModifierEngineExtensionsTest {

    private final ModifierEngine engine = new ModifierEngine();

    @Test
    void offsetModifierTranslatesPoint() {
        ModifierDefinition def = new ModifierDefinition("OFFSET", Map.of("x", 1.0, "y", 2.0, "z", 3.0));
        Vec3 result = engine.apply(List.of(def), new Vec3(0, 0, 0), 0);
        assertEquals(new Vec3(1, 2, 3), result);
    }

    @Test
    void gravityIncreasesNegativeYWithTick() {
        ModifierDefinition def = new ModifierDefinition("GRAVITY", Map.of("strength", 0.1));
        Vec3 result = engine.apply(List.of(def), new Vec3(0, 0, 0), 10);
        assertEquals(-1.0, result.y(), 1e-9);
    }

    @Test
    void scaleModifierAppliesUniformAndPerAxis() {
        ModifierDefinition uniform = new ModifierDefinition("SCALE", Map.of("factor", 2.0));
        Vec3 r1 = engine.apply(List.of(uniform), new Vec3(1, 1, 1), 0);
        assertEquals(new Vec3(2, 2, 2), r1);

        ModifierDefinition perAxis = new ModifierDefinition("SCALE", Map.of("x", 2.0, "y", 3.0, "z", 4.0));
        Vec3 r2 = engine.apply(List.of(perAxis), new Vec3(1, 1, 1), 0);
        assertEquals(new Vec3(2, 3, 4), r2);
    }

    @Test
    void waveXyzAreIndependent() {
        ModifierDefinition wx = new ModifierDefinition("WAVE_X", Map.of("amplitude", 1.0, "frequency", Math.PI / 2));
        Vec3 px = engine.apply(List.of(wx), new Vec3(0, 0, 0), 1);
        assertEquals(1.0, px.x(), 1e-9);
        assertEquals(0.0, px.y(), 1e-9);
        assertEquals(0.0, px.z(), 1e-9);

        ModifierDefinition wz = new ModifierDefinition("WAVE_Z", Map.of("amplitude", 1.0, "frequency", Math.PI / 2));
        Vec3 pz = engine.apply(List.of(wz), new Vec3(0, 0, 0), 1);
        assertEquals(0.0, pz.x(), 1e-9);
        assertEquals(0.0, pz.y(), 1e-9);
        assertEquals(1.0, pz.z(), 1e-9);
    }

    @Test
    void pulseScalesPointsRadially() {
        ModifierDefinition def = new ModifierDefinition("PULSE", Map.of("amplitude", 1.0, "frequency", Math.PI / 2));
        Vec3 result = engine.apply(List.of(def), new Vec3(2, 0, 0), 1);
        assertEquals(4.0, result.x(), 1e-9);
    }

    @Test
    void jitterIsBoundedByAmplitude() {
        ModifierEngine seeded = new ModifierEngine(new Random(42));
        ModifierDefinition def = new ModifierDefinition("JITTER", Map.of("amplitude", 0.5));
        for (int i = 0; i < 200; i++) {
            Vec3 r = seeded.apply(List.of(def), new Vec3(0, 0, 0), i);
            assertTrue(Math.abs(r.x()) <= 0.5 + 1e-9);
            assertTrue(Math.abs(r.y()) <= 0.5 + 1e-9);
            assertTrue(Math.abs(r.z()) <= 0.5 + 1e-9);
        }
    }

    @Test
    void spinRollRotatesAroundZ() {
        ModifierDefinition def = new ModifierDefinition("SPIN_ROLL", Map.of("speed", 90.0));
        Vec3 r = engine.apply(List.of(def), new Vec3(1, 0, 0), 1);
        assertEquals(0.0, r.x(), 1e-9);
        assertEquals(1.0, r.y(), 1e-9);
        assertEquals(0.0, r.z(), 1e-9);
    }

    @Test
    void spinPitchRotatesAroundX() {
        ModifierDefinition def = new ModifierDefinition("SPIN_PITCH", Map.of("speed", 90.0));
        Vec3 r = engine.apply(List.of(def), new Vec3(0, 1, 0), 1);
        assertEquals(0.0, r.x(), 1e-9);
        assertEquals(0.0, r.y(), 1e-9);
        assertEquals(1.0, r.z(), 1e-9);
    }

    @Test
    void unknownModifierLeavesPointUnchanged() {
        ModifierDefinition def = new ModifierDefinition("does_not_exist", Map.of());
        Vec3 r = engine.apply(List.of(def), new Vec3(1, 2, 3), 5);
        assertEquals(new Vec3(1, 2, 3), r);
    }

    @Test
    void existingSpinYawStillRespectsKeyframedSpeed() {
        ModifierDefinition spin = new ModifierDefinition(
                "SPIN_YAW",
                Map.of("speed", Map.of("keyframes", List.of(
                        Map.of("tick", 0, "value", 0.0),
                        Map.of("tick", 10, "value", 90.0)
                )))
        );

        Vec3 point = engine.apply(List.of(spin), new Vec3(1, 0, 0), 5);
        assertEquals(-Math.sqrt(2) / 2D, point.x(), 0.0001);
        assertEquals(-Math.sqrt(2) / 2D, point.z(), 0.0001);
    }
}
