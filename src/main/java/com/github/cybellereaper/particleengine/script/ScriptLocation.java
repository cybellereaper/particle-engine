package com.github.cybellereaper.particleengine.script;

/**
 * Plain-data location reference understood by {@link ScriptHost} adapters.
 * The interpreter is intentionally decoupled from Bukkit types so it can be
 * tested without a server runtime.
 */
public record ScriptLocation(String world, double x, double y, double z) {
    public static ScriptLocation of(String world, double x, double y, double z) {
        return new ScriptLocation(world, x, y, z);
    }
}
