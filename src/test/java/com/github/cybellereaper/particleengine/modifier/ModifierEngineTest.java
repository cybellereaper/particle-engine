package com.github.cybellereaper.particleengine.modifier;

import com.github.cybellereaper.particleengine.effect.ModifierDefinition;
import com.github.cybellereaper.particleengine.math.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModifierEngineTest {
    private final ModifierEngine engine = new ModifierEngine();

    @Test
    void resolvesKeyframedSpinSpeed() {
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
