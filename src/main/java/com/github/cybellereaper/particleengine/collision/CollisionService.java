package com.github.cybellereaper.particleengine.collision;

import org.bukkit.Location;

public final class CollisionService {
    public boolean canRenderAt(Location location) {
        return location.getBlock().isPassable();
    }
}
