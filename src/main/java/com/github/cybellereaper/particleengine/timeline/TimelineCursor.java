package com.github.cybellereaper.particleengine.timeline;

import java.util.Map;

public record TimelineCursor(int tick, Map<String, Double> parameters) {
}
