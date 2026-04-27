package com.github.cybellereaper.particleengine.runtime;

import com.github.cybellereaper.particleengine.effect.EffectTemplate;

import java.util.Set;
import java.util.UUID;

public final class ActiveEffect {
    private final UUID runtimeId;
    private final EffectTemplate template;
    private final EffectRuntimeContext context;
    private final long createdAtTick;
    private int ageTicks;
    private boolean paused;

    public ActiveEffect(UUID runtimeId, EffectTemplate template, EffectRuntimeContext context, long createdAtTick) {
        this.runtimeId = runtimeId;
        this.template = template;
        this.context = context;
        this.createdAtTick = createdAtTick;
    }

    public UUID runtimeId() { return runtimeId; }
    public EffectTemplate template() { return template; }
    public EffectRuntimeContext context() { return context; }
    public long createdAtTick() { return createdAtTick; }
    public int ageTicks() { return ageTicks; }
    public void incrementAge() { ageTicks++; }
    public boolean isExpired() { return template.lifetimeTicks() > 0 && ageTicks >= template.lifetimeTicks(); }
    public Set<String> tags() { return context.tags(); }

    public boolean isPaused() { return paused; }
    public void pause() { this.paused = true; }
    public void resume() { this.paused = false; }
    public void setPaused(boolean paused) { this.paused = paused; }
}
