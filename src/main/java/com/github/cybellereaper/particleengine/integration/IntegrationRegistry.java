package com.github.cybellereaper.particleengine.integration;

import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;

public final class IntegrationRegistry {
    private final PluginManager pluginManager;
    private final List<IntegrationHook> hooks = new ArrayList<>();

    public IntegrationRegistry(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public void registerPassive(String pluginName) {
        hooks.add(new IntegrationHook() {
            @Override public String name() { return pluginName; }
            @Override public boolean isAvailable() { return pluginManager.getPlugin(pluginName) != null; }
            @Override public void enable() { }
        });
    }

    public void enableAll() {
        for (IntegrationHook hook : hooks) {
            if (hook.isAvailable()) hook.enable();
        }
    }
}
