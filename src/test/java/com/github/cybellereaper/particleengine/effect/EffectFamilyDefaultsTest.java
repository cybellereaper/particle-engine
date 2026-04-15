package com.github.cybellereaper.particleengine.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EffectFamilyDefaultsTest {

    @Test
    void everyFamilyHasDefaultEmitterSupport() {
        for (EffectFamily family : EffectFamily.values()) {
            assertFalse(EffectFamilyDefaults.defaultEmittersFor(family).isEmpty(), () -> family + " should have at least one default emitter");
        }
    }

    @Test
    void wingsFamilyProducesSymmetricBezierEmitters() {
        var emitters = EffectFamilyDefaults.defaultEmittersFor(EffectFamily.WINGS);

        assertEquals(2, emitters.size());
        assertEquals(ShapeType.BEZIER, emitters.get(0).shape());
        assertEquals(ShapeType.BEZIER, emitters.get(1).shape());
    }
}
