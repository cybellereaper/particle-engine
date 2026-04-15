package com.github.cybellereaper.particleengine.anchor;

import com.github.cybellereaper.particleengine.effect.AnchorPoint;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class AnchorLocationCalculatorTest {
    private final AnchorLocationCalculator calculator = new AnchorLocationCalculator();

    @Test
    void calculateHeadOffsetsByEntityHeight() {
        Location base = new Location(null, 10, 64, 10);

        Location result = calculator.calculate(base, new Vector(0, 0, 1), 1.8D, AnchorPoint.HEAD);

        assertEquals(10D, result.getX(), 1e-6);
        assertEquals(65.8D, result.getY(), 1e-6);
        assertEquals(10D, result.getZ(), 1e-6);
        assertNotSame(base, result);
    }

    @Test
    void calculateBackOffsetsBehindAndUpward() {
        Location base = new Location(null, 4, 70, 8);

        Location result = calculator.calculate(base, new Vector(0, 0, 1), 2.0D, AnchorPoint.BACK);

        assertEquals(4D, result.getX(), 1e-6);
        assertEquals(71.1D, result.getY(), 1e-6);
        assertEquals(7.65D, result.getZ(), 1e-6);
    }

    @Test
    void calculateFeetReturnsCloneUnchanged() {
        Location base = new Location(null, -2, 12, 0.5);

        Location result = calculator.calculate(base, new Vector(1, 0, 0), 1.8D, AnchorPoint.FEET);

        assertEquals(base, result);
        assertNotSame(base, result);
    }

    @Test
    void calculateBackHandlesZeroLengthDirection() {
        Location base = new Location(null, 1, 2, 3);

        Location result = calculator.calculate(base, new Vector(0, 0, 0), 1.0D, AnchorPoint.BACK);

        assertEquals(1D, result.getX(), 1e-6);
        assertEquals(2.6D, result.getY(), 1e-6);
        assertEquals(3D, result.getZ(), 1e-6);
    }
}
