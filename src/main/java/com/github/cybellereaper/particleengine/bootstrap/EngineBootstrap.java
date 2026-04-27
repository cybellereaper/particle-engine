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
import com.github.cybellereaper.particleengine.script.BukkitScriptHost;
import com.github.cybellereaper.particleengine.script.ScriptHost;
import com.github.cybellereaper.particleengine.script.ScriptLoader;
import com.github.cybellereaper.particleengine.script.ScriptRuntime;
import com.github.cybellereaper.particleengine.timeline.TimelineEngine;
import com.github.cybellereaper.particleengine.trigger.ParticleTriggerListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class EngineBootstrap {
    private final JavaPlugin plugin;
    private final TemplateRegistry templateRegistry;
    private final ActiveEffectRegistry activeRegistry;
    private final EffectConfigLoader configLoader;
    private final ParticleEngineApi api;
    private final EngineScheduler scheduler;
    private final DebugService debugService;
    private final ScriptRuntime scriptRuntime;
    private final ScriptLoader scriptLoader;
    private final ScriptHost scriptHost;
    private BukkitTask scriptTickTask;

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
        this.scriptRuntime = new ScriptRuntime(plugin.getLogger());
        this.scriptRuntime.setDefaultInstructionBudget(plugin.getConfig().getLong("scripts.instruction-budget", 1_000_000L));
        this.scriptLoader = new ScriptLoader(plugin);
        this.scriptHost = new BukkitScriptHost(api, plugin.getLogger(), scriptRuntime, plugin.getConfig().getString("scripts.default-world"));
    }

    public void start() {
        reloadTemplates();
        scriptLoader.reload();
        scheduler.start();
        startScriptTick();
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
        if (scriptTickTask != null) {
            scriptTickTask.cancel();
            scriptTickTask = null;
        }
        scriptRuntime.shutdown();
        activeRegistry.snapshot().forEach(effect -> activeRegistry.remove(effect.runtimeId()));
    }

    public ParticleEngineApi api() {
        return api;
    }

    public ScriptRuntime scriptRuntime() {
        return scriptRuntime;
    }

    public ScriptLoader scriptLoader() {
        return scriptLoader;
    }

    public ScriptHost scriptHost() {
        return scriptHost;
    }

    private void registerCommands() {
        NamedEffectStore store = new NamedEffectStore(plugin, plugin.getConfig().getString("storage.named-effects-file", "named-effects.yml"));
        ParticleEngineCommand command = new ParticleEngineCommand(api, debugService, this::reloadAll, store, scriptRuntime, scriptLoader, scriptHost);
        PluginCommand pluginCommand = plugin.getCommand("particleengine");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
    }

    private void startScriptTick() {
        scriptTickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, scriptRuntime::tick, 1L, 1L);
    }

    private void reloadAll() {
        plugin.reloadConfig();
        reloadTemplates();
        scriptLoader.reload();
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
