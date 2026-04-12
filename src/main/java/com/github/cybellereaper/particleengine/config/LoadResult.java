package com.github.cybellereaper.particleengine.config;

import com.github.cybellereaper.particleengine.effect.EffectTemplate;

import java.util.List;

public record LoadResult(List<EffectTemplate> templates, List<ValidationIssue> issues) {
    public boolean hasErrors() { return !issues.isEmpty(); }
}
