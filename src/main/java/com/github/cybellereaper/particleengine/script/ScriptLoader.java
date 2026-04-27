package com.github.cybellereaper.particleengine.script;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Discovers and loads {@code .pes} scripts from the plugin's
 * {@code scripts/} data folder. Each file's contents are cached in memory
 * and indexed by a normalized name (no extension, lower-cased).
 */
public final class ScriptLoader {
    private final Plugin plugin;
    private final Map<String, String> scripts = new HashMap<>();

    public ScriptLoader(Plugin plugin) {
        this.plugin = plugin;
    }

    public Map<String, String> scripts() {
        return Collections.unmodifiableMap(scripts);
    }

    public String find(String name) {
        return scripts.get(name.toLowerCase());
    }

    public boolean has(String name) {
        return scripts.containsKey(name.toLowerCase());
    }

    public int reload() {
        scripts.clear();
        File dir = new File(plugin.getDataFolder(), "scripts");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Unable to create scripts directory.");
            return 0;
        }
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pes") || name.toLowerCase().endsWith(".pescript"));
        if (files == null) return 0;
        for (File file : files) {
            try {
                String source = Files.readString(file.toPath());
                String name = stripExtension(file.getName()).toLowerCase();
                scripts.put(name, source);
            } catch (IOException ex) {
                plugin.getLogger().warning("Failed to read script '" + file.getName() + "': " + ex.getMessage());
            }
        }
        return scripts.size();
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }
}
