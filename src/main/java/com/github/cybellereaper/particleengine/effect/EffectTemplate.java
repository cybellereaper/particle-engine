package com.github.cybellereaper.particleengine.effect;

import org.bukkit.Particle;

import java.util.List;
import java.util.Map;

public record EffectTemplate(
        String id,
        EffectFamily family,
        Particle particle,
        int count,
        int lifetimeTicks,
        AnchorDefinition anchor,
        List<EmitterDefinition> emitters,
        List<ModifierDefinition> modifiers,
        TimelineDefinition timeline,
        List<String> chainedEffects,
        Map<String, Object> metadata
) {
}
