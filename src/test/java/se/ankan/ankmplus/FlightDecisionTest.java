package se.ankan.ankmplus;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightDecisionTest {
    @Test void elytraNotRemovedThirtyBlocksAway() {
        assertEquals(3.2, FlightDecision.elytraRemoveDistance(0), 0.001);
        assertEquals(3.4, FlightDecision.elytraRemoveDistance(0.5), 0.001);
        assertEquals(6.1, FlightDecision.elytraRemoveDistance(2.0), 0.001);
        assertEquals(16.0, FlightDecision.elytraRemoveDistance(9.0), 0.001);
        assertEquals(16.0, FlightDecision.elytraRemoveDistance(20.0), 0.001);\n        assertEquals(3.2, FlightDecision.elytraRemoveDistance(Double.POSITIVE_INFINITY), 0.001);
    }

    @Test void preserveTargetDuringFastFlybyButNotForever() {
        assertTrue(FlightDecision.keepTarget(5, 14, 25.0, true));
        assertTrue(FlightDecision.keepTarget(14, 38, 400, true));
        assertFalse(FlightDecision.keepTarget(15, 14, 25.0, true));
        assertFalse(FlightDecision.keepTarget(5, 39, 25.0, true));
        assertFalse(FlightDecision.keepTarget(5, 14, 401, true));
        assertFalse(FlightDecision.keepTarget(5, 14, 25, false));
    }

    @Test void avoidCooldownComboDuringFastApproach() {
        assertTrue(FlightDecision.shieldComboSafe(0.1));
        assertFalse(FlightDecision.shieldComboSafe(0.7));
        assertFalse(FlightDecision.shieldComboSafe(3.0));
    }
}
