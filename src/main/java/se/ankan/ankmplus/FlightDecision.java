package se.ankan.ankmplus;

/**
 * Pure combat timing rules shared by AnkM+ and its automated tests.
 * Values are expressed in blocks and client ticks.
 */
public final class FlightDecision {
    private FlightDecision() {}

    /** Keep Elytra equipped for most of the approach, but budget travel time for the swap. */
    public static double elytraRemoveDistance(double speedPerTick) {
        if (!Double.isFinite(speedPerTick) || speedPerTick < 0) speedPerTick = 0;
        return Math.max(3.2, Math.min(6.5, 2.5 + speedPerTick * 1.15));
    }

    /** Maintain a short-lived target even if the camera or flight path shifts for a moment. */
    public static boolean keepTarget(int lostTicks, int ageTicks,
                                     double distanceSquared, boolean airborne) {
        return airborne && lostTicks <= 14 && ageTicks <= 38 &&
                distanceSquared <= 400 && distanceSquared >= 0;
    }

    /** Two-hit axe combos are unreliable when passing a target quickly. */
    public static boolean shieldComboSafe(double speedPerTick) {
        return Double.isFinite(speedPerTick) && speedPerTick < 0.18;
    }
}
