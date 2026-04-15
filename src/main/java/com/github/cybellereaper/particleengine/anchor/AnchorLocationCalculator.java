package com.github.cybellereaper.particleengine.anchor;

import com.github.cybellereaper.particleengine.effect.AnchorPoint;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class AnchorLocationCalculator {
    private static final double DEFAULT_BACK_OFFSET = 0.35D;

    public Location calculate(Location base, Vector forwardDirection, double entityHeight, AnchorPoint point) {
        return switch (point) {
            case HEAD -> base.clone().add(0D, entityHeight, 0D);
            case BACK -> behind(base, forwardDirection, entityHeight);
            case FEET -> base.clone();
        };
    }

    private Location behind(Location base, Vector forwardDirection, double entityHeight) {
        Location at = base.clone();
        Vector direction = forwardDirection.clone();
        if (direction.lengthSquared() > 0D) {
            direction.normalize().multiply(-DEFAULT_BACK_OFFSET);
            at.add(direction);
        }
        double verticalOffset = Math.min(entityHeight * 0.6D, 1.1D);
        return at.add(0D, verticalOffset, 0D);
    }
}
