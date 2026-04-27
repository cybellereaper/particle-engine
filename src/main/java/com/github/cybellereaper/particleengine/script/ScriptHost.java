package com.github.cybellereaper.particleengine.script;

import java.util.Map;
import java.util.UUID;

/**
 * Bridge between the PEScript interpreter and the surrounding engine. The
 * {@link com.github.cybellereaper.particleengine.api.ParticleEngineApi} is too
 * tied to Bukkit types to be used directly inside the interpreter, so the
 * host abstracts the operations that scripts need.
 *
 * <p>Implementations should be thread-safe, since {@link ScriptInterpreter}
 * may invoke them from a worker thread.
 */
public interface ScriptHost {
    UUID spawn(String templateId, ScriptLocation at, java.util.Set<String> tags, Map<String, Object> overrides);

    boolean stop(UUID runtimeId);

    int stopByTag(String tag);

    int stopAll();

    boolean pause(UUID runtimeId);

    int pauseByTag(String tag);

    boolean resume(UUID runtimeId);

    int resumeByTag(String tag);

    boolean templateExists(String templateId);

    void log(String message);

    /**
     * Called from a script worker thread to wait the requested number of
     * server ticks before continuing execution. Implementations are
     * responsible for resuming on the engine tick boundary.
     */
    void waitTicks(int ticks) throws InterruptedException;
}
