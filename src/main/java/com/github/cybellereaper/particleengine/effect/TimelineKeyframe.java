package com.github.cybellereaper.particleengine.effect;

import java.util.Map;

public record TimelineKeyframe(int tick, String easing, Map<String, Double> params) {
}
