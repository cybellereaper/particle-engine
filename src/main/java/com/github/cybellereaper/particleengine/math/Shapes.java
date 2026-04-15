package com.github.cybellereaper.particleengine.math;

import com.github.cybellereaper.particleengine.effect.EmitterDefinition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Shapes {
    private Shapes() {}

    public static List<Vec3> point(EmitterDefinition emitter, int tick, Random random) {
        return List.of(new Vec3(0, 0, 0));
    }

    public static List<Vec3> line(EmitterDefinition emitter, int tick, Random random) {
        Vec3 from = vec(emitter.params(), "from", new Vec3(0, 0, 0), tick);
        Vec3 to = vec(emitter.params(), "to", new Vec3(0, 0, 1), tick);
        int points = Math.max(2, emitter.points());
        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) out.add(Vec3.lerp(from, to, i / (double) (points - 1)));
        return out;
    }

    public static List<Vec3> circle(EmitterDefinition emitter, int tick, Random random) {
        double r = num(emitter.params(), "radius", 1.0, tick);
        int points = Math.max(3, emitter.points());
        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2D * i) / points;
            out.add(new Vec3(Math.cos(a) * r, 0, Math.sin(a) * r));
        }
        return out;
    }

    public static List<Vec3> spiral(EmitterDefinition emitter, int tick, Random random) {
        double r = num(emitter.params(), "radius", 1.2, tick);
        double h = num(emitter.params(), "height", 2.0, tick);
        double turns = num(emitter.params(), "turns", 3.0, tick);
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
        double r = num(emitter.params(), "radius", 1.0, tick);
        double h = num(emitter.params(), "height", 2.0, tick);
        double turns = num(emitter.params(), "turns", 2.0, tick);
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
        double r = num(emitter.params(), "radius", 1.0, tick);
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
        Vec3 size = vec(emitter.params(), "size", new Vec3(1, 1, 1), tick);
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
        double r = num(emitter.params(), "radius", 1.0, tick);
        double h = num(emitter.params(), "height", 2.0, tick);
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
        double r = num(emitter.params(), "radius", 1.0, tick);
        double h = num(emitter.params(), "height", 2.0, tick);
        int points = Math.max(8, emitter.points());
        List<Vec3> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2D * i) / points;
            out.add(new Vec3(Math.cos(a) * r, (random.nextDouble() - 0.5) * h, Math.sin(a) * r));
        }
        return out;
    }

    public static List<Vec3> bezier(EmitterDefinition emitter, int tick, Random random) {
        Vec3 p0 = vec(emitter.params(), "p0", new Vec3(0, 0, 0), tick);
        Vec3 p1 = vec(emitter.params(), "p1", new Vec3(0, 1, 0), tick);
        Vec3 p2 = vec(emitter.params(), "p2", new Vec3(0, 1, 1), tick);
        Vec3 p3 = vec(emitter.params(), "p3", new Vec3(0, 0, 2), tick);
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

    private static Vec3 vec(Map<String, Object> map, String key, Vec3 fallback, int tick) {
        Object raw = resolveTickValue(map.get(key), tick);
        if (!(raw instanceof List<?> list) || list.size() < 3) return fallback;
        return new Vec3(asDouble(list.get(0), fallback.x()), asDouble(list.get(1), fallback.y()), asDouble(list.get(2), fallback.z()));
    }

    private static List<Vec3> vecList(Map<String, Object> map, String key, int tick) {
        Object raw = resolveTickValue(map.get(key), tick);
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

    private static double num(Map<String, Object> map, String key, double fallback, int tick) {
        return asDouble(resolveTickValue(map.get(key), tick), fallback);
    }

    private static Object resolveTickValue(Object raw, int tick) {
        if (!(raw instanceof Map<?, ?> candidate)) {
            return raw;
        }
        Object frameObject = candidate.get("keyframes");
        if (!(frameObject instanceof List<?> frameList) || frameList.isEmpty()) {
            return raw;
        }

        List<Map<?, ?>> keyframes = new ArrayList<>();
        for (Object frame : frameList) {
            if (frame instanceof Map<?, ?> map && map.containsKey("tick") && map.containsKey("value")) {
                keyframes.add(map);
            }
        }
        if (keyframes.isEmpty()) {
            return raw;
        }

        keyframes.sort(Comparator.comparingInt(frame -> asInt(frame.get("tick"), 0)));
        Object firstValue = keyframes.getFirst().get("value");
        if (tick <= asInt(keyframes.getFirst().get("tick"), 0)) {
            return firstValue;
        }

        for (int i = 1; i < keyframes.size(); i++) {
            Map<?, ?> previous = keyframes.get(i - 1);
            Map<?, ?> current = keyframes.get(i);
            int fromTick = asInt(previous.get("tick"), 0);
            int toTick = asInt(current.get("tick"), fromTick);
            if (tick > toTick) {
                continue;
            }
            Object fromValue = previous.get("value");
            Object toValue = current.get("value");
            if (toTick <= fromTick) {
                return toValue;
            }
            double progress = (tick - fromTick) / (double) (toTick - fromTick);
            return interpolateValue(fromValue, toValue, progress);
        }
        return keyframes.getLast().get("value");
    }

    private static Object interpolateValue(Object from, Object to, double progress) {
        if (from instanceof Number || to instanceof Number) {
            return interpolateNumber(from, to, progress);
        }
        Vec3 fromVec = vecFrom(from);
        Vec3 toVec = vecFrom(to);
        if (fromVec != null && toVec != null) {
            Vec3 interpolated = Vec3.lerp(fromVec, toVec, progress);
            return List.of(interpolated.x(), interpolated.y(), interpolated.z());
        }

        List<Vec3> fromPoints = vecListFrom(from);
        List<Vec3> toPoints = vecListFrom(to);
        if (!fromPoints.isEmpty() && fromPoints.size() == toPoints.size()) {
            List<List<Double>> interpolated = new ArrayList<>(fromPoints.size());
            for (int i = 0; i < fromPoints.size(); i++) {
                Vec3 point = Vec3.lerp(fromPoints.get(i), toPoints.get(i), progress);
                interpolated.add(List.of(point.x(), point.y(), point.z()));
            }
            return interpolated;
        }
        return progress < 1D ? from : to;
    }

    private static double interpolateNumber(Object from, Object to, double progress) {
        double fromValue = asDouble(from, 0D);
        double toValue = asDouble(to, fromValue);
        return fromValue + ((toValue - fromValue) * progress);
    }

    private static List<Vec3> vecListFrom(Object raw) {
        if (!(raw instanceof List<?> list)) return List.of();
        List<Vec3> points = new ArrayList<>();
        for (Object entry : list) {
            Vec3 point = vecFrom(entry);
            if (point != null) {
                points.add(point);
            }
        }
        return points;
    }

    private static int asInt(Object raw, int fallback) {
        if (raw instanceof Number number) return number.intValue();
        if (raw instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) { return fallback; }
        }
        return fallback;
    }

    private static double asDouble(Object raw, double fallback) {
        if (raw instanceof Number n) return n.doubleValue();
        if (raw instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) { return fallback; }
        }
        return fallback;
    }
}
