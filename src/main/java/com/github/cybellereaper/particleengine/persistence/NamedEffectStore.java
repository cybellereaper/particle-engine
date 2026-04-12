package com.github.cybellereaper.particleengine.persistence;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class NamedEffectStore {
    private final JavaPlugin plugin;
    private final String fileName;

    public NamedEffectStore(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
    }

    public Map<String, String> load() {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) return Map.of();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<String, String> map = new HashMap<>();
        for (String key : yaml.getKeys(false)) {
            map.put(key, yaml.getString(key, ""));
        }
        return map;
    }

    public void save(Map<String, String> namedTemplates) {
        File file = new File(plugin.getDataFolder(), fileName);
        YamlConfiguration yaml = new YamlConfiguration();
        namedTemplates.forEach(yaml::set);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to persist named effects: " + e.getMessage());
        }
    }
}
