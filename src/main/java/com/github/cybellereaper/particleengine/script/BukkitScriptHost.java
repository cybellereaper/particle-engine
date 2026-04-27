package com.github.cybellereaper.particleengine.script;

import com.github.cybellereaper.particleengine.api.ParticleEngineApi;
import com.github.cybellereaper.particleengine.api.SpawnRequest;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Default {@link ScriptHost} implementation that bridges PEScript calls into
 * the Bukkit-backed {@link ParticleEngineApi}. Most operations are forwarded
 * directly; {@code spawn(...)} builds a {@link SpawnRequest} from the
 * scripting-friendly {@link ScriptLocation}.
 */
public final class BukkitScriptHost implements ScriptHost {
    private final ParticleEngineApi api;
    private final Logger logger;
    private final ScriptRuntime runtime;
    private final String defaultWorld;

    public BukkitScriptHost(ParticleEngineApi api, Logger logger, ScriptRuntime runtime, String defaultWorld) {
        this.api = api;
        this.logger = logger;
        this.runtime = runtime;
        this.defaultWorld = defaultWorld;
    }

    @Override
    public UUID spawn(String templateId, ScriptLocation at, Set<String> tags, Map<String, Object> overrides) {
        Location location = resolveLocation(at);
        SpawnRequest request = new SpawnRequest(null, location, null, tags, overrides);
        return api.spawn(templateId, request);
    }

    @Override
    public boolean stop(UUID runtimeId) { return api.stop(runtimeId); }

    @Override
    public int stopByTag(String tag) { return api.stopByTag(tag); }

    @Override
    public int stopAll() { return api.stopAll(); }

    @Override
    public boolean pause(UUID runtimeId) { return api.pause(runtimeId); }

    @Override
    public int pauseByTag(String tag) { return api.pauseByTag(tag); }

    @Override
    public boolean resume(UUID runtimeId) { return api.resume(runtimeId); }

    @Override
    public int resumeByTag(String tag) { return api.resumeByTag(tag); }

    @Override
    public boolean templateExists(String templateId) { return api.findTemplate(templateId).isPresent(); }

    @Override
    public void log(String message) {
        if (logger != null) logger.info("[script] " + message);
    }

    @Override
    public void waitTicks(int ticks) throws InterruptedException {
        // Real waiting is handled by ScriptRuntime's wrapper; this is only the
        // fallback path used if a script is run outside the runtime (which we
        // never do internally), so we sleep in 50ms-per-tick increments.
        if (runtime != null) {
            runtime.tick();
        }
        Thread.sleep(Math.max(0, ticks) * 50L);
    }

    private Location resolveLocation(ScriptLocation at) {
        if (at == null) return null;
        String worldName = at.world() != null ? at.world() : defaultWorld;
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) {
            throw new ScriptError("Unknown world '" + worldName + "'.");
        }
        return new Location(world, at.x(), at.y(), at.z());
    }
}
