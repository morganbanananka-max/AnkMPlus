package se.ankan.ankmplus;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static se.ankan.ankmplus.ManualCombatDecision.Action.*;

class ManualCombatDecisionTest {
    @Test void noMaceWhenStandingOrOnlyJumping() {
        assertFalse(ManualCombatDecision.realSmash(true, false, 10.0));
        assertFalse(ManualCombatDecision.realSmash(false, false, 0.0));
        assertFalse(ManualCombatDecision.realSmash(false, false, 0.5));
        assertFalse(ManualCombatDecision.realSmash(false, false, 1.5));
        assertFalse(ManualCombatDecision.realSmash(false, true, 50.0));
        assertFalse(ManualCombatDecision.realSmash(false, false, Double.NaN));
        assertEquals(NORMAL, ManualCombatDecision.action(true, true,
                false, false, false, true, true));
    }

    @Test void smashAfterRealFallOnly() {
        assertTrue(ManualCombatDecision.realSmash(false, false, 1.51));
        assertTrue(ManualCombatDecision.realSmash(false, false, 25.0));
        assertEquals(MACE, ManualCombatDecision.action(true, true,
                false, false, true, false, true));
        assertEquals(NORMAL, ManualCombatDecision.action(false, true,
                false, false, true, false, true));
    }

    @Test void shieldStunslamWorksOnGroundAndInAirWithTwoClicks() {
        // First click: actively shielding opponent -> Axe, even on ground.
        assertEquals(AXE, ManualCombatDecision.action(true, true,
                true, false, false, true, true));
        // Second click: same opponent -> Mace even though we aren't falling.
        assertEquals(MACE, ManualCombatDecision.action(true, true,
                false, true, false, true, true));
        assertEquals(MACE, ManualCombatDecision.action(false, true,
                false, true, false, true, true));
        // Stun selection always outranks ordinary smash selection.
        assertEquals(AXE, ManualCombatDecision.action(true, true,
                true, false, true, true, true));
    }

    @Test void shieldNeverForcesAxeWithoutStunslamOrRequiredItems() {
        assertEquals(NORMAL, ManualCombatDecision.action(true, false,
                true, false, false, true, true));
        assertEquals(NORMAL, ManualCombatDecision.action(false, true,
                true, false, false, true, false));
        assertEquals(NORMAL, ManualCombatDecision.action(false, true,
                true, false, false, false, true));
        assertEquals(NORMAL, ManualCombatDecision.action(false, false,
                false, false, false, true, true));
    }

    @Test void fastMovementDoesNotBlockRealSmash() {
        // Eligibility deliberately has no horizontal-speed parameter.
        assertTrue(ManualCombatDecision.realSmash(false, false, 15.0));
        assertEquals(MACE, ManualCombatDecision.action(true, true,
                false, false, true, true, true));
    }

    @Test void densitySmashAndBreachFallbackScores() {
        assertTrue(ManualCombatDecision.maceScore(true, 5, 0, 0) >
                ManualCombatDecision.maceScore(true, 0, 5, 3));
        assertTrue(ManualCombatDecision.maceScore(false, 0, 5, 0) >
                ManualCombatDecision.maceScore(false, 5, 0, 3));
    }
}
