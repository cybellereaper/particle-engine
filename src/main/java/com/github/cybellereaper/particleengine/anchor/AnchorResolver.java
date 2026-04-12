package com.github.cybellereaper.particleengine.anchor;

import com.github.cybellereaper.particleengine.effect.AnchorType;
import com.github.cybellereaper.particleengine.effect.EffectTemplate;
import com.github.cybellereaper.particleengine.runtime.EffectRuntimeContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Optional;

public final class AnchorResolver {
    public Optional<Location> resolve(EffectTemplate template, EffectRuntimeContext context) {
        if (context.anchorLocation() != null) {
            return Optional.of(context.anchorLocation().clone());
        }
        if (context.anchorEntityId() != null) {
            Entity entity = Bukkit.getEntity(context.anchorEntityId());
            if (entity != null && entity.isValid()) return Optional.of(entity.getLocation());
        }
        if (template.anchor().type() == AnchorType.COORDINATE && context.anchorLocation() != null) {
            return Optional.of(context.anchorLocation().clone());
        }
        return Optional.empty();
    }
}
