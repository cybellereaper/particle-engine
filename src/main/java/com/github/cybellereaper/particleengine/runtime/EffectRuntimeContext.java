package com.github.cybellereaper.particleengine.runtime;

import org.bukkit.Location;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record EffectRuntimeContext(UUID initiator, Location anchorLocation, UUID anchorEntityId, Set<String> tags, Map<String, Object> overrides) {
}
