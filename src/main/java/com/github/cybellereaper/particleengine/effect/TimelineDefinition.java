package com.github.cybellereaper.particleengine.effect;

import java.util.List;

public record TimelineDefinition(int durationTicks, boolean loop, int delayTicks, List<TimelineKeyframe> keyframes) {
}
