package com.github.cybellereaper.particleengine.registry;

import com.github.cybellereaper.particleengine.runtime.ActiveEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ActiveEffectRegistry {
    private final Map<UUID, ActiveEffect> active = new ConcurrentHashMap<>();

    public void add(ActiveEffect effect) { active.put(effect.runtimeId(), effect); }
    public ActiveEffect remove(UUID id) { return active.remove(id); }
    public ActiveEffect get(UUID id) { return active.get(id); }
    public Collection<ActiveEffect> snapshot() { return new ArrayList<>(active.values()); }
    public int size() { return active.size(); }
}
