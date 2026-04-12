package com.github.cybellereaper.particleengine.bootstrap;

import com.github.cybellereaper.particleengine.api.ParticleEngineApi;
import com.github.cybellereaper.particleengine.command.ParticleEngineCommand;
import com.github.cybellereaper.particleengine.config.EffectConfigLoader;
import com.github.cybellereaper.particleengine.config.LoadResult;
import com.github.cybellereaper.particleengine.debug.DebugService;
import com.github.cybellereaper.particleengine.anchor.AnchorResolver;
import com.github.cybellereaper.particleengine.collision.CollisionService;
import com.github.cybellereaper.particleengine.integration.IntegrationRegistry;
import com.github.cybellereaper.particleengine.math.ShapeRegistry;
import com.github.cybellereaper.particleengine.modifier.ModifierEngine;
import com.github.cybellereaper.particleengine.persistence.NamedEffectStore;
import com.github.cybellereaper.particleengine.registry.ActiveEffectRegistry;
import com.github.cybellereaper.particleengine.registry.TemplateRegistry;
import com.github.cybellereaper.particleengine.runtime.EffectExecutor;
import com.github.cybellereaper.particleengine.runtime.ParticleEngineApiImpl;
import com.github.cybellereaper.particleengine.scheduler.EngineScheduler;
import com.github.cybellereaper.particleengine.timeline.TimelineEngine;
import com.github.cybellereaper.particleengine.trigger.ParticleTriggerListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class EngineBootstrap {
    private final JavaPlugin plugin;
    private final TemplateRegistry templateRegistry;
    private final ActiveEffectRegistry activeRegistry;
    private final EffectConfigLoader configLoader;
    private final ParticleEngineApi api;
    private final EngineScheduler scheduler;
    private final DebugService debugService;

    public EngineBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
        this.templateRegistry = new TemplateRegistry();
        this.activeRegistry = new ActiveEffectRegistry();
        this.configLoader = new EffectConfigLoader(plugin);

        long seed = plugin.getConfig().getLong("engine.deterministic-random-seed", 1337L);
        var executor = new EffectExecutor(new ShapeRegistry(), new ModifierEngine(), new TimelineEngine(),
                new AnchorResolver(), new CollisionService(), seed);

        this.api = new ParticleEngineApiImpl(
                plugin,
                templateRegistry,
                activeRegistry,
                plugin.getConfig().getInt("engine.max-active-effects", 2000)
        );
        this.scheduler = new EngineScheduler(
                plugin,
                activeRegistry,
                executor,
                plugin.getConfig().getLong("engine.tick-budget-nanos", 1_500_000L),
                plugin.getConfig().getDouble("engine.default-view-distance", 48D),
                plugin.getConfig().getInt("engine.lod-throttle-factor", 2)
        );
        this.debugService = new DebugService();
    }

    public void start() {
        reloadTemplates();
        scheduler.start();
        registerCommands();
        plugin.getServer().getPluginManager().registerEvents(new ParticleTriggerListener(api), plugin);

        IntegrationRegistry integrationRegistry = new IntegrationRegistry(plugin.getServer().getPluginManager());
        for (String name : java.util.List.of("WorldGuard", "PlaceholderAPI", "MythicMobs", "ModelEngine", "ItemsAdder")) {
            integrationRegistry.registerPassive(name);
        }
        integrationRegistry.enableAll();
    }

    public void stop() {
        scheduler.stop();
        activeRegistry.snapshot().forEach(effect -> activeRegistry.remove(effect.runtimeId()));
    }

    public ParticleEngineApi api() {
        return api;
    }

    private void registerCommands() {
        NamedEffectStore store = new NamedEffectStore(plugin, plugin.getConfig().getString("storage.named-effects-file", "named-effects.yml"));
        ParticleEngineCommand command = new ParticleEngineCommand(api, debugService, this::reloadAll, store);
        PluginCommand pluginCommand = plugin.getCommand("particleengine");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
    }

    private void reloadAll() {
        plugin.reloadConfig();
        reloadTemplates();
    }

    private void reloadTemplates() {
        LoadResult result = configLoader.loadAll();
        templateRegistry.replaceAll(result.templates());
        if (result.hasErrors()) {
            result.issues().forEach(issue -> plugin.getLogger().warning("[Config] " + issue.path() + " -> " + issue.message()));
        }
        plugin.getLogger().info("Loaded " + result.templates().size() + " effect templates.");
    }
}
