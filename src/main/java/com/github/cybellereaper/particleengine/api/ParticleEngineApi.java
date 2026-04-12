package com.github.cybellereaper.particleengine.api;

import com.github.cybellereaper.particleengine.effect.EffectTemplate;
import com.github.cybellereaper.particleengine.runtime.ActiveEffect;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ParticleEngineApi {
    UUID spawn(String templateId, SpawnRequest request);

    boolean stop(UUID runtimeId);

    int stopByTag(String tag);

    Optional<EffectTemplate> findTemplate(String templateId);

    Collection<EffectTemplate> templates();

    Collection<ActiveEffect> activeEffects();

    static ParticleEngineApi noop() {
        return new ParticleEngineApi() {
            @Override public UUID spawn(String templateId, SpawnRequest request) { return new UUID(0L, 0L); }
            @Override public boolean stop(UUID runtimeId) { return false; }
            @Override public int stopByTag(String tag) { return 0; }
            @Override public Optional<EffectTemplate> findTemplate(String templateId) { return Optional.empty(); }
            @Override public Collection<EffectTemplate> templates() { return java.util.List.of(); }
            @Override public Collection<ActiveEffect> activeEffects() { return java.util.List.of(); }
        };
    }
}
