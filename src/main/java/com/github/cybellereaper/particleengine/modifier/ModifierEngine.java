package com.github.cybellereaper.particleengine.modifier;

import com.github.cybellereaper.particleengine.effect.ModifierDefinition;
import com.github.cybellereaper.particleengine.math.Vec3;
import com.github.cybellereaper.particleengine.util.KeyframeValueResolver;

import java.util.List;
import java.util.Random;

/**
 * Applies modifier transformations to a sampled emitter point. Each modifier
 * type listed below interprets the {@link ModifierDefinition#params() params}
 * map (numeric values support keyframed resolution via {@link KeyframeValueResolver}).
 *
 * Supported modifier types:
 * <ul>
 *   <li>{@code SPIN_YAW}   - rotate around Y axis. params: {@code speed} (deg/tick)</li>
 *   <li>{@code SPIN_PITCH} - rotate around X axis. params: {@code speed}</li>
 *   <li>{@code SPIN_ROLL}  - rotate around Z axis. params: {@code speed}</li>
 *   <li>{@code WAVE_X}     - sinusoidal X offset. params: {@code amplitude}, {@code frequency}</li>
 *   <li>{@code WAVE_Y}     - sinusoidal Y offset. params: {@code amplitude}, {@code frequency}</li>
 *   <li>{@code WAVE_Z}     - sinusoidal Z offset. params: {@code amplitude}, {@code frequency}</li>
 *   <li>{@code OFFSET}     - constant translation. params: {@code x},{@code y},{@code z}</li>
 *   <li>{@code GRAVITY}    - downward acceleration. params: {@code strength}</li>
 *   <li>{@code SCALE}      - multiplicative scale. params: {@code factor} (or {@code x},{@code y},{@code z})</li>
 *   <li>{@code PULSE}      - radial breathing scale around origin. params: {@code amplitude}, {@code frequency}</li>
 *   <li>{@code JITTER}     - random offset within +/- amplitude. params: {@code amplitude}</li>
 *   <li>{@code NOISE}      - smooth time-dependent pseudo-noise offset. params: {@code amplitude}, {@code frequency}</li>
 * </ul>
 */
public final class ModifierEngine {
    private final Random jitterRandom;

    public ModifierEngine() {
        this(new Random(0xC0FFEE));
    }

    public ModifierEngine(Random jitterRandom) {
        this.jitterRandom = jitterRandom;
    }

    public Vec3 apply(List<ModifierDefinition> modifiers, Vec3 point, int tick) {
        Vec3 current = point;
        for (ModifierDefinition modifier : modifiers) {
            current = switch (modifier.type().toUpperCase()) {
                case "SPIN_YAW" -> spinYaw(current, tick, num(modifier, "speed", tick, 1.0));
                case "SPIN_PITCH" -> spinPitch(current, tick, num(modifier, "speed", tick, 1.0));
                case "SPIN_ROLL" -> spinRoll(current, tick, num(modifier, "speed", tick, 1.0));
                case "WAVE_X" -> waveX(current, tick, num(modifier, "amplitude", tick, 0.2), num(modifier, "frequency", tick, 0.15));
                case "WAVE_Y" -> waveY(current, tick, num(modifier, "amplitude", tick, 0.2), num(modifier, "frequency", tick, 0.15));
                case "WAVE_Z" -> waveZ(current, tick, num(modifier, "amplitude", tick, 0.2), num(modifier, "frequency", tick, 0.15));
                case "OFFSET" -> offset(current, num(modifier, "x", tick, 0.0), num(modifier, "y", tick, 0.0), num(modifier, "z", tick, 0.0));
                case "GRAVITY" -> gravity(current, tick, num(modifier, "strength", tick, 0.04));
                case "SCALE" -> scale(current,
                        num(modifier, "x", tick, num(modifier, "factor", tick, 1.0)),
                        num(modifier, "y", tick, num(modifier, "factor", tick, 1.0)),
                        num(modifier, "z", tick, num(modifier, "factor", tick, 1.0)));
                case "PULSE" -> pulse(current, tick, num(modifier, "amplitude", tick, 0.2), num(modifier, "frequency", tick, 0.15));
                case "JITTER" -> jitter(current, num(modifier, "amplitude", tick, 0.1));
                case "NOISE" -> noise(current, tick, num(modifier, "amplitude", tick, 0.2), num(modifier, "frequency", tick, 0.1));
                default -> current;
            };
        }
        return current;
    }

    private double num(ModifierDefinition modifier, String key, int tick, double fallback) {
        return KeyframeValueResolver.resolveNumber(modifier.params(), key, tick, fallback);
    }

    private Vec3 spinYaw(Vec3 point, int tick, double speed) {
        double angle = Math.toRadians(tick * speed);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3((point.x() * cos) - (point.z() * sin), point.y(), (point.x() * sin) + (point.z() * cos));
    }

    private Vec3 spinPitch(Vec3 point, int tick, double speed) {
        double angle = Math.toRadians(tick * speed);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(point.x(), (point.y() * cos) - (point.z() * sin), (point.y() * sin) + (point.z() * cos));
    }

    private Vec3 spinRoll(Vec3 point, int tick, double speed) {
        double angle = Math.toRadians(tick * speed);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3((point.x() * cos) - (point.y() * sin), (point.x() * sin) + (point.y() * cos), point.z());
    }

    private Vec3 waveX(Vec3 point, int tick, double amplitude, double frequency) {
        return new Vec3(point.x() + (Math.sin(tick * frequency) * amplitude), point.y(), point.z());
    }

    private Vec3 waveY(Vec3 point, int tick, double amplitude, double frequency) {
        return new Vec3(point.x(), point.y() + (Math.sin(tick * frequency) * amplitude), point.z());
    }

    private Vec3 waveZ(Vec3 point, int tick, double amplitude, double frequency) {
        return new Vec3(point.x(), point.y(), point.z() + (Math.sin(tick * frequency) * amplitude));
    }

    private Vec3 offset(Vec3 point, double dx, double dy, double dz) {
        return new Vec3(point.x() + dx, point.y() + dy, point.z() + dz);
    }

    private Vec3 gravity(Vec3 point, int tick, double strength) {
        return new Vec3(point.x(), point.y() - (strength * tick), point.z());
    }

    private Vec3 scale(Vec3 point, double sx, double sy, double sz) {
        return new Vec3(point.x() * sx, point.y() * sy, point.z() * sz);
    }

    private Vec3 pulse(Vec3 point, int tick, double amplitude, double frequency) {
        double factor = 1D + (Math.sin(tick * frequency) * amplitude);
        return new Vec3(point.x() * factor, point.y() * factor, point.z() * factor);
    }

    private Vec3 jitter(Vec3 point, double amplitude) {
        if (amplitude <= 0D) return point;
        double dx = (jitterRandom.nextDouble() - 0.5D) * 2D * amplitude;
        double dy = (jitterRandom.nextDouble() - 0.5D) * 2D * amplitude;
        double dz = (jitterRandom.nextDouble() - 0.5D) * 2D * amplitude;
        return new Vec3(point.x() + dx, point.y() + dy, point.z() + dz);
    }

    private Vec3 noise(Vec3 point, int tick, double amplitude, double frequency) {
        if (amplitude <= 0D) return point;
        double tx = tick * frequency;
        double dx = Math.sin(tx) + Math.sin(tx * 2.13D) * 0.5D;
        double dy = Math.sin(tx * 1.71D + 1.3D) + Math.sin(tx * 3.07D) * 0.5D;
        double dz = Math.sin(tx * 0.93D + 2.6D) + Math.sin(tx * 2.41D) * 0.5D;
        double scale = amplitude / 1.5D;
        return new Vec3(point.x() + dx * scale, point.y() + dy * scale, point.z() + dz * scale);
    }
}
