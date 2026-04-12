package com.github.cybellereaper.particleengine.emitter;

import com.github.cybellereaper.particleengine.effect.EmitterDefinition;

import java.util.List;

public final class EmitterComposer {
    public List<EmitterDefinition> compose(List<EmitterDefinition> base, List<EmitterDefinition> layeredOverrides) {
        if (layeredOverrides == null || layeredOverrides.isEmpty()) {
            return base;
        }
        var copy = new java.util.ArrayList<>(base);
        copy.addAll(layeredOverrides);
        return List.copyOf(copy);
    }
}
