package com.github.cybellereaper.particleengine.math;

public record Vec3(double x, double y, double z) {
    public Vec3 add(Vec3 other) { return new Vec3(x + other.x, y + other.y, z + other.z); }
    public Vec3 scale(double factor) { return new Vec3(x * factor, y * factor, z * factor); }

    public static Vec3 lerp(Vec3 a, Vec3 b, double t) {
        return new Vec3(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t,
                a.z + (b.z - a.z) * t
        );
    }
}
