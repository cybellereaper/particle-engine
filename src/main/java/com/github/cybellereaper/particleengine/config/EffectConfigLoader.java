package com.github.cybellereaper.particleengine.config;

import com.github.cybellereaper.particleengine.effect.*;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

public final class EffectConfigLoader {
    private final Plugin plugin;

    public EffectConfigLoader(Plugin plugin) {
        this.plugin = plugin;
    }

    public LoadResult loadAll() {
        List<EffectTemplate> templates = new ArrayList<>();
        List<ValidationIssue> issues = new ArrayList<>();

        File dir = new File(plugin.getDataFolder(), "effects");
        if (!dir.exists() && !dir.mkdirs()) {
            issues.add(new ValidationIssue("effects", "Unable to create effects directory."));
            return new LoadResult(templates, issues);
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return new LoadResult(templates, issues);

        for (File file : files) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection effects = yaml.getConfigurationSection("effects");
            if (effects == null) {
                issues.add(new ValidationIssue(file.getName(), "Missing root 'effects' section."));
                continue;
            }

            for (String id : effects.getKeys(false)) {
                String path = "effects." + id;
                ConfigurationSection section = effects.getConfigurationSection(id);
                if (section == null) {
                    issues.add(new ValidationIssue(path, "Effect entry must be an object."));
                    continue;
                }
                parseTemplate(id, section, path, templates, issues);
            }
        }
        return new LoadResult(List.copyOf(templates), List.copyOf(issues));
    }

    private void parseTemplate(String id, ConfigurationSection section, String path, List<EffectTemplate> out, List<ValidationIssue> issues) {
        EffectFamily family = enumOf(EffectFamily.class, section.getString("family"), EffectFamily.COMPOSITE, path + ".family", issues);
        Particle particle = enumOf(Particle.class, section.getString("particle"), Particle.END_ROD, path + ".particle", issues);
        int count = section.getInt("count", 1);
        int lifetime = section.getInt("lifetimeTicks", 40);

        ConfigurationSection anchorSec = section.getConfigurationSection("anchor");
        AnchorDefinition anchor = new AnchorDefinition(
                enumOf(AnchorType.class, anchorSec != null ? anchorSec.getString("type") : null, AnchorType.COORDINATE, path + ".anchor.type", issues),
                anchorSec != null ? anchorSec.getString("selector", "self") : "self",
                enumOf(CoordinateSpace.class, anchorSec != null ? anchorSec.getString("space") : null, CoordinateSpace.WORLD, path + ".anchor.space", issues)
        );

        List<EmitterDefinition> emitters = new ArrayList<>();
        List<Map<?, ?>> emitterList = section.getMapList("emitters");
        if (emitterList.isEmpty()) {
            emitters.addAll(EffectFamilyDefaults.defaultEmittersFor(family));
        } else {
            for (int i = 0; i < emitterList.size(); i++) {
                Map<?, ?> map = emitterList.get(i);
                String entryPath = path + ".emitters[" + i + "]";
                ShapeType shape = enumOf(ShapeType.class, str(map.get("shape")), ShapeType.POINT, entryPath + ".shape", issues);
                int rate = asInt(map.get("rate"), 1);
                int points = asInt(map.get("points"), 8);
                emitters.add(new EmitterDefinition(shape, Math.max(1, rate), Math.max(1, points), castStringObjectMap(map)));
            }
        }

        List<ModifierDefinition> modifiers = new ArrayList<>();
        for (Map<?, ?> map : section.getMapList("modifiers")) {
            modifiers.add(new ModifierDefinition(str(valueOr(map, "type", "NONE")), castStringObjectMap(map)));
        }

        TimelineDefinition timeline = parseTimeline(section.getConfigurationSection("timeline"), path + ".timeline", issues);
        List<String> chain = section.getStringList("chain");
        out.add(new EffectTemplate(id, family, particle, Math.max(1, count), lifetime, anchor, List.copyOf(emitters), List.copyOf(modifiers), timeline, List.copyOf(chain), Map.of()));
    }

    private TimelineDefinition parseTimeline(ConfigurationSection section, String path, List<ValidationIssue> issues) {
        if (section == null) {
            return new TimelineDefinition(40, false, 0, List.of(new TimelineKeyframe(0, "linear", Map.of("scale", 1D))));
        }
        List<TimelineKeyframe> frames = new ArrayList<>();
        List<Map<?, ?>> mapList = section.getMapList("keyframes");
        if (mapList.isEmpty()) {
            frames.add(new TimelineKeyframe(0, "linear", Map.of("scale", 1D)));
        } else {
            for (int i = 0; i < mapList.size(); i++) {
                Map<?, ?> map = mapList.get(i);
                int tick = asInt(map.get("tick"), 0);
                String easing = str(valueOr(map, "easing", "linear"));
                Map<String, Double> params = new HashMap<>();
                Object rawParams = map.get("params");
                if (rawParams instanceof Map<?, ?> p) {
                    for (Map.Entry<?, ?> entry : p.entrySet()) {
                        params.put(String.valueOf(entry.getKey()), asDouble(entry.getValue(), 0D));
                    }
                }
                frames.add(new TimelineKeyframe(tick, easing, Map.copyOf(params)));
            }
            frames.sort(Comparator.comparingInt(TimelineKeyframe::tick));
        }
        return new TimelineDefinition(section.getInt("durationTicks", 40), section.getBoolean("loop", false), section.getInt("delayTicks", 0), List.copyOf(frames));
    }

    private <T extends Enum<T>> T enumOf(Class<T> type, String raw, T fallback, String path, List<ValidationIssue> issues) {
        if (raw == null) return fallback;
        try {
            return Enum.valueOf(type, raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            issues.add(new ValidationIssue(path, "Invalid value '" + raw + "'. Using " + fallback + '.'));
            return fallback;
        }
    }

    private int asInt(Object raw, int fallback) {
        if (raw instanceof Number n) return n.intValue();
        if (raw instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) { }
        }
        return fallback;
    }

    private double asDouble(Object raw, double fallback) {
        if (raw instanceof Number n) return n.doubleValue();
        if (raw instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) { }
        }
        return fallback;
    }

    private String str(Object raw) {
        return raw == null ? "" : String.valueOf(raw);
    }

    private Object valueOr(Map<?, ?> map, String key, Object fallback) {
        return map.containsKey(key) ? map.get(key) : fallback;
    }

    private Map<String, Object> castStringObjectMap(Map<?, ?> source) {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            map.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return Map.copyOf(map);
    }
}
