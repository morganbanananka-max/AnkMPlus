package se.ankan.ankmplus;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static se.ankan.ankmplus.ManualCombatDecision.Action.*;

class ManualCombatDecisionTest {
    @Test void ordinaryClickDoesNothingWithoutModdedFeatures() {
        assertEquals(NORMAL, ManualCombatDecision.action(false, false, true, false, true, true));
        assertEquals(NORMAL, ManualCombatDecision.action(true, true, false, false, false, false));
    }

    @Test void shieldComboRequiresTwoPhysicalClicks() {
        assertEquals(AXE, ManualCombatDecision.action(true, true, true, false, true, true));
        assertEquals(MACE, ManualCombatDecision.action(true, true, false, true, true, true));
        assertEquals(MACE, ManualCombatDecision.action(false, true, false, true, true, true));
    }

    @Test void shieldNeverForcesAxeWhenStunslamOffOrWithoutMace() {
        assertEquals(MACE, ManualCombatDecision.action(true, false, true, false, true, true));
        assertEquals(NORMAL, ManualCombatDecision.action(false, true, true, false, true, false));
        assertEquals(NORMAL, ManualCombatDecision.action(false, true, true, false, false, true));
    }

    @Test void fallingPrefersDensityAndGroundPrefersBreach() {
        assertTrue(ManualCombatDecision.maceScore(true, 5, 0, 0) >
                   ManualCombatDecision.maceScore(true, 0, 5, 3));
        assertTrue(ManualCombatDecision.maceScore(false, 0, 5, 0) >
                   ManualCombatDecision.maceScore(false, 5, 0, 3));
        assertTrue(ManualCombatDecision.maceScore(false, 0, 0, 0) >= 0);
    }
}
