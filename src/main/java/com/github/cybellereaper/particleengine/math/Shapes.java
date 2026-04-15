package com.github.cybellereaper.particleengine.math;

import com.github.cybellereaper.particleengine.effect.EmitterDefinition;

import com.github.cybellereaper.particleengine.util.KeyframeValueResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Shapes {
    private Shapes() {}

    public static List<Vec3> point(EmitterDefinition emitter, int tick, Random random) {
        return List.of(new Vec3(0, 0, 0));
    }

    public static List<Vec3> line(EmitterDefinition emitter, int tick, Random random) {
        Vec3 from = vec(emitter.params(), "from", tick, new Vec3(0, 0, 0));
        Vec3 to = vec(emitter.params(), "to", tick, new Vec3(0, 0, 1));
        int points = Math.max(2, emitter.points());
        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) out.add(Vec3.lerp(from, to, i / (double) (points - 1)));
        return out;
    }

    public static List<Vec3> circle(EmitterDefinition emitter, int tick, Random random) {
        double r = num(emitter.params(), "radius", tick, 1.0);
        int points = Math.max(3, emitter.points());
        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2D * i) / points;
            out.add(new Vec3(Math.cos(a) * r, 0, Math.sin(a) * r));
        }
        return out;
    }

    public static List<Vec3> spiral(EmitterDefinition emitter, int tick, Random random) {
        double r = num(emitter.params(), "radius", tick, 1.2);
        double h = num(emitter.params(), "height", tick, 2.0);
        double turns = num(emitter.params(), "turns", tick, 3.0);
        int points = Math.max(6, emitter.points());
        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t = i / (double) (points - 1);
            double a = turns * (Math.PI * 2D) * t;
            out.add(new Vec3(Math.cos(a) * r * t, h * t, Math.sin(a) * r * t));
        }
        return out;
    }

    public static List<Vec3> helix(EmitterDefinition emitter, int tick, Random random) {
        double r = num(emitter.params(), "radius", tick, 1.0);
        double h = num(emitter.params(), "height", tick, 2.0);
        double turns = num(emitter.params(), "turns", tick, 2.0);
        int points = Math.max(8, emitter.points());
        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t = i / (double) (points - 1);
            double a = turns * (Math.PI * 2D) * t + (tick * 0.12D);
            out.add(new Vec3(Math.cos(a) * r, h * (t - 0.5), Math.sin(a) * r));
        }
        return out;
    }

    public static List<Vec3> sphere(EmitterDefinition emitter, int tick, Random random) {
        double r = num(emitter.params(), "radius", tick, 1.0);
        int points = Math.max(8, emitter.points());
        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double u = random.nextDouble();
            double v = random.nextDouble();
            double theta = 2D * Math.PI * u;
            double phi = Math.acos(2D * v - 1D);
            out.add(new Vec3(
                    r * Math.sin(phi) * Math.cos(theta),
                    r * Math.cos(phi),
                    r * Math.sin(phi) * Math.sin(theta)
            ));
        }
        return out;
    }

    public static List<Vec3> box(EmitterDefinition emitter, int tick, Random random) {
        Vec3 size = vec(emitter.params(), "size", tick, new Vec3(1, 1, 1));
        int points = Math.max(8, emitter.points());
        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            out.add(new Vec3(
                    (random.nextDouble() - 0.5) * size.x(),
                    (random.nextDouble() - 0.5) * size.y(),
                    (random.nextDouble() - 0.5) * size.z()
            ));
        }
        return out;
    }

    public static List<Vec3> cone(EmitterDefinition emitter, int tick, Random random) {
        double r = num(emitter.params(), "radius", tick, 1.0);
        double h = num(emitter.params(), "height", tick, 2.0);
        int points = Math.max(6, emitter.points());
        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t = i / (double) (points - 1);
            double a = (Math.PI * 2D * i) / points;
            out.add(new Vec3(Math.cos(a) * r * (1D - t), h * t, Math.sin(a) * r * (1D - t)));
        }
        return out;
    }

    public static List<Vec3> cylinder(EmitterDefinition emitter, int tick, Random random) {
        double r = num(emitter.params(), "radius", tick, 1.0);
        double h = num(emitter.params(), "height", tick, 2.0);
        int points = Math.max(8, emitter.points());
        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2D * i) / points;
            out.add(new Vec3(Math.cos(a) * r, (random.nextDouble() - 0.5) * h, Math.sin(a) * r));
        }
        return out;
    }

    public static List<Vec3> bezier(EmitterDefinition emitter, int tick, Random random) {
        Vec3 p0 = vec(emitter.params(), "p0", tick, new Vec3(0, 0, 0));
        Vec3 p1 = vec(emitter.params(), "p1", tick, new Vec3(0, 1, 0));
        Vec3 p2 = vec(emitter.params(), "p2", tick, new Vec3(0, 1, 1));
        Vec3 p3 = vec(emitter.params(), "p3", tick, new Vec3(0, 0, 2));
        int points = Math.max(4, emitter.points());
        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t = i / (double) (points - 1);
            out.add(cubicBezier(p0, p1, p2, p3, t));
        }
        return out;
    }

    public static List<Vec3> polyline(EmitterDefinition emitter, int tick, Random random) {
        List<Vec3> controlPoints = vecList(emitter.params(), "path", tick);
        if (controlPoints.size() < 2) {
            return line(emitter, tick, random);
        }

        int points = Math.max(2, emitter.points());
        if (points == 2) {
            return List.of(controlPoints.getFirst(), controlPoints.getLast());
        }

        List<Double> cumulativeLengths = cumulativeLengths(controlPoints);
        double totalLength = cumulativeLengths.getLast();
        if (totalLength <= 0D) {
            return List.of(controlPoints.getFirst(), controlPoints.getLast());
        }

        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double normalized = i / (double) (points - 1);
            double targetDistance = normalized * totalLength;
            out.add(sampleAlongPath(controlPoints, cumulativeLengths, targetDistance));
        }
        return out;
    }

    private static Vec3 cubicBezier(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double u = 1D - t;
        return p0.scale(u * u * u)
                .add(p1.scale(3D * u * u * t))
                .add(p2.scale(3D * u * t * t))
                .add(p3.scale(t * t * t));
    }

    private static Vec3 sampleAlongPath(List<Vec3> points, List<Double> cumulativeLengths, double targetDistance) {
        for (int i = 1; i < cumulativeLengths.size(); i++) {
            double segmentEnd = cumulativeLengths.get(i);
            if (targetDistance > segmentEnd) continue;

            double segmentStart = cumulativeLengths.get(i - 1);
            double segmentLength = segmentEnd - segmentStart;
            if (segmentLength <= 0D) return points.get(i);

            double segmentT = (targetDistance - segmentStart) / segmentLength;
            return Vec3.lerp(points.get(i - 1), points.get(i), segmentT);
        }
        return points.getLast();
    }

    private static List<Double> cumulativeLengths(List<Vec3> points) {
        List<Double> cumulativeLengths = new ArrayList<>(points.size());
        cumulativeLengths.add(0D);
        double length = 0D;
        for (int i = 1; i < points.size(); i++) {
            length += distance(points.get(i - 1), points.get(i));
            cumulativeLengths.add(length);
        }
        return cumulativeLengths;
    }

    private static double distance(Vec3 a, Vec3 b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        double dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static Vec3 vec(Map<String, Object> map, String key, int tick, Vec3 fallback) {
        Object raw = KeyframeValueResolver.resolveObject(map, key, tick);
        if (!(raw instanceof List<?> list) || list.size() < 3) return fallback;
        return new Vec3(asDouble(list.get(0), fallback.x()), asDouble(list.get(1), fallback.y()), asDouble(list.get(2), fallback.z()));
    }

    private static List<Vec3> vecList(Map<String, Object> map, String key, int tick) {
        Object raw = KeyframeValueResolver.resolveObject(map, key, tick);
        if (!(raw instanceof List<?> list)) return List.of();

        List<Vec3> points = new ArrayList<>();
        for (Object entry : list) {
            Vec3 point = vecFrom(entry);
            if (point == null) continue;
            points.add(point);
        }
        return points;
    }

    private static Vec3 vecFrom(Object raw) {
        if (raw instanceof List<?> vec && vec.size() >= 3) {
            return new Vec3(asDouble(vec.get(0), 0D), asDouble(vec.get(1), 0D), asDouble(vec.get(2), 0D));
        }
        if (raw instanceof Map<?, ?> point) {
            return new Vec3(
                    asDouble(point.get("x"), 0D),
                    asDouble(point.get("y"), 0D),
                    asDouble(point.get("z"), 0D)
            );
        }
        return null;
    }

    private static double num(Map<String, Object> map, String key, int tick, double fallback) {
        return KeyframeValueResolver.resolveNumber(map, key, tick, fallback);
    }

    private static double asDouble(Object raw, double fallback) {
        if (raw instanceof Number n) return n.doubleValue();
        if (raw instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) { return fallback; }
        }
        return fallback;
    }
}
