package com.github.cybellereaper.particleengine.trigger;

import com.github.cybellereaper.particleengine.api.ParticleEngineApi;
import com.github.cybellereaper.particleengine.api.SpawnRequest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public final class ParticleTriggerListener implements Listener {
    private final ParticleEngineApi api;

    public ParticleTriggerListener(ParticleEngineApi api) {
        this.api = api;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getPlayer().hasPermission("particleengine.trigger.itemuse") && event.getAction().isRightClick()) {
            if (api.findTemplate("trail_smoke").isPresent()) {
                api.spawn("trail_smoke", SpawnRequest.at(event.getPlayer().getLocation()));
            }
        }
    }
}
