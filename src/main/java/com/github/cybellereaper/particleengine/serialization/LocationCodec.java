package com.github.cybellereaper.particleengine.serialization;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public final class LocationCodec {
    private LocationCodec() {}

    public static String encode(Location location) {
        return "%s,%.4f,%.4f,%.4f,%.2f,%.2f".formatted(
                location.getWorld() != null ? location.getWorld().getName() : "world",
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public static Location decode(String raw) {
        String[] parts = raw.split(",");
        if (parts.length < 6) {
            throw new IllegalArgumentException("Invalid location format.");
        }
        return new Location(
                Bukkit.getWorld(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]),
                Float.parseFloat(parts[5])
        );
    }
}
