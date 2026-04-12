package com.github.cybellereaper.particleengine;

import com.github.cybellereaper.particleengine.api.ParticleEngineApi;
import com.github.cybellereaper.particleengine.api.ParticleEngineApiProvider;
import com.github.cybellereaper.particleengine.bootstrap.EngineBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

public final class ParticleEnginePlugin extends JavaPlugin {
    private EngineBootstrap bootstrap;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("effects/defaults.yml", false);

        this.bootstrap = new EngineBootstrap(this);
        this.bootstrap.start();
        ParticleEngineApiProvider.set(bootstrap.api());
        getLogger().info("ParticleEngine enabled.");
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.stop();
        }
        ParticleEngineApiProvider.set(ParticleEngineApi.noop());
        getLogger().info("ParticleEngine disabled.");
    }
}
