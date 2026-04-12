package com.github.cybellereaper.particleengine.timeline;

import com.github.cybellereaper.particleengine.effect.TimelineDefinition;
import com.github.cybellereaper.particleengine.effect.TimelineKeyframe;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimelineEngineTest {
    private final TimelineEngine engine = new TimelineEngine();

    @Test
    void interpolatesBetweenKeyframes() {
        TimelineDefinition definition = new TimelineDefinition(
                20,
                false,
                0,
                List.of(
                        new TimelineKeyframe(0, "linear", Map.of("scale", 0.0)),
                        new TimelineKeyframe(10, "linear", Map.of("scale", 1.0))
                )
        );

        TimelineCursor cursor = engine.resolve(definition, 5);
        assertEquals(0.5, cursor.parameters().get("scale"), 0.0001);
    }

    @Test
    void appliesLooping() {
        TimelineDefinition definition = new TimelineDefinition(
                10,
                true,
                0,
                List.of(
                        new TimelineKeyframe(0, "linear", Map.of("scale", 0.0)),
                        new TimelineKeyframe(5, "linear", Map.of("scale", 1.0))
                )
        );

        TimelineCursor cursor = engine.resolve(definition, 12);
        assertEquals(0.4, cursor.parameters().get("scale"), 0.0001);
    }
}
