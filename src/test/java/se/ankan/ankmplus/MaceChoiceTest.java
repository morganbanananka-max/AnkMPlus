package se.ankan.ankmplus;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MaceChoiceTest {
    @Test void densityWinsAirborne() {
        assertTrue(MaceChoice.points(true, 4, 0, 0) >
                   MaceChoice.points(true, 0, 4, 3));
    }
    @Test void breachWinsOnGround() {
        assertTrue(MaceChoice.points(false, 0, 4, 0) >
                   MaceChoice.points(false, 0, 3, 3));
    }
    @Test void useAvailableMaceEvenWithoutBreachOnGround() {
        assertTrue(MaceChoice.points(false, 5, 0, 3) >= 0);
        assertTrue(MaceChoice.points(false, 0, 1, 0) > MaceChoice.points(false, 5, 0, 3));
        assertEquals(0, MaceChoice.points(false, 0, 0, 0));
    }
}
