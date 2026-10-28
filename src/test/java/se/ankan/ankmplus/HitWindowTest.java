package se.ankan.ankmplus;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HitWindowTest {
    @Test void fastFlybyAllowsSmallMissButOnlyInVanillaRange() {
        assertTrue(HitWindow.allowed(2.8, 0.53, true));
        assertTrue(HitWindow.allowed(3.0, 0.95, true));
        assertFalse(HitWindow.allowed(3.01, 1.0, true));
        assertFalse(HitWindow.allowed(5.0, 1.0, true));
    }

    @Test void dontHitBehindOrAcrossGround() {
        assertFalse(HitWindow.allowed(2.0, -0.10, true));
        assertFalse(HitWindow.allowed(2.0, 0.20, true));
        assertFalse(HitWindow.allowed(2.0, 0.55, false));
        assertTrue(HitWindow.allowed(2.0, 0.90, false));
    }

    @Test void rejectInvalidRangesAndPickCloserAlignedEnemy() {
        assertFalse(HitWindow.allowed(Double.NaN, 1.0, true));
        assertFalse(HitWindow.allowed(Double.POSITIVE_INFINITY, 1.0, true));
        assertFalse(HitWindow.allowed(-1.0, 1.0, true));
        assertTrue(HitWindow.score(1.5, 0.85) < HitWindow.score(2.9, 0.52));
    }
}
