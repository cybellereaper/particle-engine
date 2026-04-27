package com.github.cybellereaper.particleengine.script;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight {@link ScriptHost} used for unit tests so the interpreter can
 * be exercised without a Bukkit server.
 */
public final class TestScriptHost implements ScriptHost {
    public final List<String> logs = new ArrayList<>();
    public final List<SpawnRecord> spawns = new ArrayList<>();
    public final ConcurrentHashMap<UUID, Set<String>> activeTags = new ConcurrentHashMap<>();
    public final Set<String> templates = new HashSet<>();
    public final AtomicInteger waitTickAccumulator = new AtomicInteger();

    @Override
    public synchronized UUID spawn(String templateId, ScriptLocation at, Set<String> tags, Map<String, Object> overrides) {
        UUID id = UUID.randomUUID();
        spawns.add(new SpawnRecord(templateId, at, tags, overrides, id));
        activeTags.put(id, tags);
        return id;
    }

    @Override public synchronized boolean stop(UUID runtimeId) {
        return activeTags.remove(runtimeId) != null;
    }

    @Override public synchronized int stopByTag(String tag) {
        int n = 0;
        for (var entry : new ArrayList<>(activeTags.entrySet())) {
            if (entry.getValue().contains(tag)) {
                activeTags.remove(entry.getKey());
                n++;
            }
        }
        return n;
    }

    @Override public synchronized int stopAll() {
        int n = activeTags.size();
        activeTags.clear();
        return n;
    }

    @Override public boolean pause(UUID runtimeId) { return activeTags.containsKey(runtimeId); }
    @Override public int pauseByTag(String tag) { return 0; }
    @Override public boolean resume(UUID runtimeId) { return activeTags.containsKey(runtimeId); }
    @Override public int resumeByTag(String tag) { return 0; }
    @Override public boolean templateExists(String templateId) { return templates.contains(templateId); }

    @Override public synchronized void log(String message) { logs.add(message); }

    @Override public void waitTicks(int ticks) {
        waitTickAccumulator.addAndGet(ticks);
    }

    public record SpawnRecord(String template, ScriptLocation at, Set<String> tags, Map<String, Object> overrides, UUID id) {}
}
