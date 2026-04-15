package com.github.cybellereaper.particleengine.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnchorPointTest {
    @Test
    void fromSelectorRecognizesSupportedSelectors() {
        assertEquals(AnchorPoint.HEAD, AnchorPoint.fromSelector("head"));
        assertEquals(AnchorPoint.BACK, AnchorPoint.fromSelector("BACK"));
        assertEquals(AnchorPoint.FEET, AnchorPoint.fromSelector(" feet "));
    }

    @Test
    void fromSelectorFallsBackToFeetWhenUnknown() {
        assertEquals(AnchorPoint.FEET, AnchorPoint.fromSelector("self"));
        assertEquals(AnchorPoint.FEET, AnchorPoint.fromSelector(""));
        assertEquals(AnchorPoint.FEET, AnchorPoint.fromSelector(null));
    }
}
