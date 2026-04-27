package com.github.cybellereaper.particleengine.runtime;

import com.github.cybellereaper.particleengine.effect.*;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActiveEffectPauseTest {

    @Test
    void pauseAndResumeToggleFlag() {
        EffectTemplate template = new EffectTemplate(
                "t", EffectFamily.AURA, Particle.CLOUD, 1, 100,
                new AnchorDefinition(AnchorType.COORDINATE, "self", CoordinateSpace.WORLD),
                List.of(new EmitterDefinition(ShapeType.POINT, 1, 1, Map.of())),
                List.of(),
                new TimelineDefinition(20, false, 0, List.of(new TimelineKeyframe(0, "linear", Map.of()))),
                List.of(),
                Map.of()
        );

        ActiveEffect active = new ActiveEffect(UUID.randomUUID(), template,
                new EffectRuntimeContext(null, null, null, Set.of(), Map.of()), 0);

        assertFalse(active.isPaused());
        active.pause();
        assertTrue(active.isPaused());
        active.resume();
        assertFalse(active.isPaused());
        active.setPaused(true);
        assertTrue(active.isPaused());
    }
}
