package com.github.cybellereaper.particleengine.runtime;

import com.github.cybellereaper.particleengine.anchor.AnchorResolver;
import com.github.cybellereaper.particleengine.collision.CollisionService;
import com.github.cybellereaper.particleengine.math.ShapeRegistry;
import com.github.cybellereaper.particleengine.math.Vec3;
import com.github.cybellereaper.particleengine.modifier.ModifierEngine;
import com.github.cybellereaper.particleengine.timeline.TimelineCursor;
import com.github.cybellereaper.particleengine.timeline.TimelineEngine;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Random;

public final class EffectExecutor {
    private final ShapeRegistry shapeRegistry;
    private final ModifierEngine modifierEngine;
    private final TimelineEngine timelineEngine;
    private final AnchorResolver anchorResolver;
    private final CollisionService collisionService;
    private final Random deterministicRandom;

    public EffectExecutor(ShapeRegistry shapeRegistry, ModifierEngine modifierEngine, TimelineEngine timelineEngine,
                          AnchorResolver anchorResolver, CollisionService collisionService, long deterministicSeed) {
        this.shapeRegistry = shapeRegistry;
        this.modifierEngine = modifierEngine;
        this.timelineEngine = timelineEngine;
        this.anchorResolver = anchorResolver;
        this.collisionService = collisionService;
        this.deterministicRandom = new Random(deterministicSeed);
    }

    public void tick(ActiveEffect effect, double maxViewDistance, int lodThrottleFactor) {
        var template = effect.template();
        var anchorOpt = anchorResolver.resolve(template, effect.context());
        if (anchorOpt.isEmpty()) return;

        Location anchor = anchorOpt.get();
        World world = anchor.getWorld();
        if (world == null) return;

        TimelineCursor cursor = timelineEngine.resolve(template.timeline(), effect.ageTicks());
        double scale = cursor.parameters().getOrDefault("scale", 1D);

        for (var emitter : template.emitters()) {
            if (effect.ageTicks() % Math.max(1, lodThrottleFactor) != 0 && anchor.getNearbyPlayers(maxViewDistance).size() > 8) {
                continue;
            }
            List<Vec3> points = shapeRegistry.sample(emitter, effect.ageTicks(), deterministicRandom);
            for (Vec3 point : points) {
                Vec3 modified = modifierEngine.apply(template.modifiers(), point.scale(scale), effect.ageTicks());
                Location at = anchor.clone().add(modified.x(), modified.y(), modified.z());
                if (!collisionService.canRenderAt(at)) continue;
                world.spawnParticle(template.particle(), at, template.count(), 0D, 0D, 0D, 0D);
            }
        }
    }
}
