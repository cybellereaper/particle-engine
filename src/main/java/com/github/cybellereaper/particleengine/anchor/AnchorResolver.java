package com.github.cybellereaper.particleengine.anchor;

import com.github.cybellereaper.particleengine.effect.AnchorPoint;
import com.github.cybellereaper.particleengine.effect.AnchorType;
import com.github.cybellereaper.particleengine.effect.EffectTemplate;
import com.github.cybellereaper.particleengine.runtime.EffectRuntimeContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public final class AnchorResolver {
    private final EntityLocator entityLocator;
    private final AnchorLocationCalculator locationCalculator;

    public AnchorResolver() {
        this(new BukkitEntityLocator(), new AnchorLocationCalculator());
    }

    AnchorResolver(EntityLocator entityLocator) {
        this(entityLocator, new AnchorLocationCalculator());
    }

    AnchorResolver(EntityLocator entityLocator, AnchorLocationCalculator locationCalculator) {
        this.entityLocator = entityLocator;
        this.locationCalculator = locationCalculator;
    }

    public Optional<Location> resolve(EffectTemplate template, EffectRuntimeContext context) {
        if (context.anchorLocation() != null) {
            return Optional.of(context.anchorLocation().clone());
        }

        Optional<Entity> entity = resolveAnchorEntity(template, context);
        if (entity.isPresent()) {
            AnchorPoint point = AnchorPoint.fromSelector(template.anchor().selector());
            Entity resolvedEntity = entity.get();
            Location base = resolvedEntity.getLocation();
            return Optional.of(locationCalculator.calculate(base, base.getDirection(), resolvedEntity.getHeight(), point));
        }

        return Optional.empty();
    }

    private Optional<Entity> resolveAnchorEntity(EffectTemplate template, EffectRuntimeContext context) {
        Optional<Entity> fromAnchorId = lookupEntity(context.anchorEntityId());
        if (fromAnchorId.isPresent()) return fromAnchorId;

        if (template.anchor().type() == AnchorType.PLAYER) {
            return lookupPlayer(context.initiator());
        }

        return Optional.empty();
    }

    private Optional<Entity> lookupEntity(UUID entityId) {
        if (entityId == null) return Optional.empty();
        return entityLocator.findEntity(entityId).filter(Entity::isValid);
    }

    private Optional<Entity> lookupPlayer(UUID playerId) {
        if (playerId == null) return Optional.empty();
        return entityLocator.findPlayer(playerId).filter(Entity::isValid).map(Entity.class::cast);
    }

    interface EntityLocator {
        Optional<Entity> findEntity(UUID id);
        Optional<Player> findPlayer(UUID id);
    }

    private static final class BukkitEntityLocator implements EntityLocator {
        @Override
        public Optional<Entity> findEntity(UUID id) {
            return Optional.ofNullable(Bukkit.getEntity(id));
        }

        @Override
        public Optional<Player> findPlayer(UUID id) {
            return Optional.ofNullable(Bukkit.getPlayer(id));
        }
    }
}
