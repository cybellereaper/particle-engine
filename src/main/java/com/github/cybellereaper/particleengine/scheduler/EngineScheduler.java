package com.github.cybellereaper.particleengine.scheduler;

import com.github.cybellereaper.particleengine.registry.ActiveEffectRegistry;
import com.github.cybellereaper.particleengine.runtime.ActiveEffect;
import com.github.cybellereaper.particleengine.runtime.EffectExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class EngineScheduler {
    private final JavaPlugin plugin;
    private final ActiveEffectRegistry activeRegistry;
    private final EffectExecutor executor;
    private final long tickBudgetNanos;
    private final double maxViewDistance;
    private final int lodThrottleFactor;
    private BukkitTask task;

    public EngineScheduler(JavaPlugin plugin, ActiveEffectRegistry activeRegistry, EffectExecutor executor,
                           long tickBudgetNanos, double maxViewDistance, int lodThrottleFactor) {
        this.plugin = plugin;
        this.activeRegistry = activeRegistry;
        this.executor = executor;
        this.tickBudgetNanos = tickBudgetNanos;
        this.maxViewDistance = maxViewDistance;
        this.lodThrottleFactor = lodThrottleFactor;
    }

    public void start() {
        stop();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        long start = System.nanoTime();
        for (ActiveEffect activeEffect : activeRegistry.snapshot()) {
            if (System.nanoTime() - start > tickBudgetNanos) {
                break;
            }
            if (activeEffect.isPaused()) {
                continue;
            }
            executor.tick(activeEffect, maxViewDistance, lodThrottleFactor);
            activeEffect.incrementAge();
            if (activeEffect.isExpired()) {
                activeRegistry.remove(activeEffect.runtimeId());
            }
        }
    }
}
