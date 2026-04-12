package com.github.cybellereaper.particleengine.debug;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DebugService {
    private final Set<UUID> enabled = ConcurrentHashMap.newKeySet();

    public boolean toggle(UUID playerId) {
        if (enabled.contains(playerId)) {
            enabled.remove(playerId);
            return false;
        }
        enabled.add(playerId);
        return true;
    }

    public boolean isEnabled(UUID playerId) {
        return enabled.contains(playerId);
    }
}
