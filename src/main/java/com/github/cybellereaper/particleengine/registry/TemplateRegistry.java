package com.github.cybellereaper.particleengine.registry;

import com.github.cybellereaper.particleengine.effect.EffectTemplate;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class TemplateRegistry {
    private final Map<String, EffectTemplate> templates = new ConcurrentHashMap<>();

    public void replaceAll(Collection<EffectTemplate> newTemplates) {
        templates.clear();
        for (EffectTemplate template : newTemplates) {
            templates.put(template.id().toLowerCase(), template);
        }
    }

    public Optional<EffectTemplate> find(String id) {
        return Optional.ofNullable(templates.get(id.toLowerCase()));
    }

    public Collection<EffectTemplate> all() { return templates.values(); }
}
