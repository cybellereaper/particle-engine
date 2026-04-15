package com.github.cybellereaper.particleengine.modifier;

import com.github.cybellereaper.particleengine.effect.ModifierDefinition;
import com.github.cybellereaper.particleengine.math.Vec3;
import com.github.cybellereaper.particleengine.util.KeyframeValueResolver;

import java.util.List;

public final class ModifierEngine {
    public Vec3 apply(List<ModifierDefinition> modifiers, Vec3 point, int tick) {
        Vec3 current = point;
        for (ModifierDefinition modifier : modifiers) {
            current = switch (modifier.type().toUpperCase()) {
                case "SPIN_YAW" -> spinYaw(current, tick, KeyframeValueResolver.resolveNumber(modifier.params(), "speed", tick, 1.0));
                case "WAVE_Y" -> waveY(current, tick, KeyframeValueResolver.resolveNumber(modifier.params(), "amplitude", tick, 0.2), KeyframeValueResolver.resolveNumber(modifier.params(), "frequency", tick, 0.15));
                default -> current;
            };
        }
        return current;
    }

    private Vec3 spinYaw(Vec3 point, int tick, double speed) {
        double angle = Math.toRadians(tick * speed);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3((point.x() * cos) - (point.z() * sin), point.y(), (point.x() * sin) + (point.z() * cos));
    }

    private Vec3 waveY(Vec3 point, int tick, double amplitude, double frequency) {
        return new Vec3(point.x(), point.y() + (Math.sin(tick * frequency) * amplitude), point.z());
    }

}
