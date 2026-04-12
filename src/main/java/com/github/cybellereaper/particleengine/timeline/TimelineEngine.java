package com.github.cybellereaper.particleengine.timeline;

import com.github.cybellereaper.particleengine.effect.TimelineDefinition;
import com.github.cybellereaper.particleengine.effect.TimelineKeyframe;
import com.github.cybellereaper.particleengine.util.Easing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TimelineEngine {
    public TimelineCursor resolve(TimelineDefinition definition, int rawTick) {
        if (definition == null || definition.keyframes().isEmpty()) {
            return new TimelineCursor(rawTick, Map.of());
        }
        int tick = Math.max(0, rawTick - Math.max(0, definition.delayTicks()));
        if (definition.loop() && definition.durationTicks() > 0) {
            tick = tick % definition.durationTicks();
        }

        List<TimelineKeyframe> frames = definition.keyframes();
        TimelineKeyframe left = frames.get(0);
        TimelineKeyframe right = frames.get(frames.size() - 1);

        for (int i = 0; i < frames.size() - 1; i++) {
            TimelineKeyframe a = frames.get(i);
            TimelineKeyframe b = frames.get(i + 1);
            if (tick >= a.tick() && tick <= b.tick()) {
                left = a;
                right = b;
                break;
            }
        }

        if (left == right || right.tick() == left.tick()) {
            return new TimelineCursor(tick, left.params());
        }

        double alpha = (tick - left.tick()) / (double) (right.tick() - left.tick());
        alpha = Easing.apply(right.easing(), alpha);

        Map<String, Double> params = new HashMap<>(left.params());
        for (Map.Entry<String, Double> entry : right.params().entrySet()) {
            double lv = left.params().getOrDefault(entry.getKey(), entry.getValue());
            params.put(entry.getKey(), lv + ((entry.getValue() - lv) * alpha));
        }

        return new TimelineCursor(tick, Map.copyOf(params));
    }
}
