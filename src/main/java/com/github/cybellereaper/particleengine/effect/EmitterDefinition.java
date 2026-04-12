package com.github.cybellereaper.particleengine.effect;

import java.util.Map;

public record EmitterDefinition(ShapeType shape, int rate, int points, Map<String, Object> params) {
}
