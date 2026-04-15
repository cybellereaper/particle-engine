package com.github.cybellereaper.particleengine.api;

import org.bukkit.Location;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record SpawnRequest(
        UUID initiator,
        Location anchorLocation,
        UUID anchorEntityId,
        Set<String> tags,
        Map<String, Object> overrides
) {
    public static SpawnRequest at(Location location) {
        return new SpawnRequest(null, location, null, Set.of(), Map.of());
    }

    public static SpawnRequest forEntity(UUID entityId) {
        return new SpawnRequest(entityId, null, entityId, Set.of(), Map.of());
    }
}
