package com.github.cybellereaper.particleengine.runtime;

import com.github.cybellereaper.particleengine.api.ParticleEngineApi;
import com.github.cybellereaper.particleengine.api.SpawnRequest;
import com.github.cybellereaper.particleengine.effect.EffectTemplate;
import com.github.cybellereaper.particleengine.registry.ActiveEffectRegistry;
import com.github.cybellereaper.particleengine.registry.TemplateRegistry;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class ParticleEngineApiImpl implements ParticleEngineApi {
    private final JavaPlugin plugin;
    private final TemplateRegistry templateRegistry;
    private final ActiveEffectRegistry activeRegistry;
    private final int maxActive;

    public ParticleEngineApiImpl(JavaPlugin plugin, TemplateRegistry templateRegistry, ActiveEffectRegistry activeRegistry, int maxActive) {
        this.plugin = plugin;
        this.templateRegistry = templateRegistry;
        this.activeRegistry = activeRegistry;
        this.maxActive = maxActive;
    }

    @Override
    public UUID spawn(String templateId, SpawnRequest request) {
        EffectTemplate template = templateRegistry.find(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown template: " + templateId));

        if (activeRegistry.size() >= maxActive) {
            throw new IllegalStateException("Engine active effect limit reached: " + maxActive);
        }

        UUID runtimeId = UUID.randomUUID();
        var context = new EffectRuntimeContext(request.initiator(), request.anchorLocation(), request.anchorEntityId(), request.tags(), request.overrides());
        activeRegistry.add(new ActiveEffect(runtimeId, template, context, plugin.getServer().getCurrentTick()));
        return runtimeId;
    }

    @Override
    public boolean stop(UUID runtimeId) {
        return activeRegistry.remove(runtimeId) != null;
    }

    @Override
    public int stopByTag(String tag) {
        int removed = 0;
        for (ActiveEffect effect : activeRegistry.snapshot()) {
            if (effect.tags().contains(tag)) {
                activeRegistry.remove(effect.runtimeId());
                removed++;
            }
        }
        return removed;
    }

    @Override
    public int stopAll() {
        int removed = 0;
        for (ActiveEffect effect : activeRegistry.snapshot()) {
            activeRegistry.remove(effect.runtimeId());
            removed++;
        }
        return removed;
    }

    @Override
    public boolean pause(UUID runtimeId) {
        ActiveEffect effect = activeRegistry.get(runtimeId);
        if (effect == null) return false;
        effect.pause();
        return true;
    }

    @Override
    public int pauseByTag(String tag) {
        int affected = 0;
        for (ActiveEffect effect : activeRegistry.snapshot()) {
            if (effect.tags().contains(tag) && !effect.isPaused()) {
                effect.pause();
                affected++;
            }
        }
        return affected;
    }

    @Override
    public boolean resume(UUID runtimeId) {
        ActiveEffect effect = activeRegistry.get(runtimeId);
        if (effect == null) return false;
        effect.resume();
        return true;
    }

    @Override
    public int resumeByTag(String tag) {
        int affected = 0;
        for (ActiveEffect effect : activeRegistry.snapshot()) {
            if (effect.tags().contains(tag) && effect.isPaused()) {
                effect.resume();
                affected++;
            }
        }
        return affected;
    }

    @Override
    public Optional<EffectTemplate> findTemplate(String templateId) {
        return templateRegistry.find(templateId);
    }

    @Override
    public Collection<EffectTemplate> templates() {
        return templateRegistry.all();
    }

    @Override
    public Collection<ActiveEffect> activeEffects() {
        return activeRegistry.snapshot();
    }
}
