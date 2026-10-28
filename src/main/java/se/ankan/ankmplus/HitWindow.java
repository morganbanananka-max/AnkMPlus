package se.ankan.ankmplus;

/** Reach and forward-facing aim tolerance. Never extends vanilla attack distance. */
public final class HitWindow {
    private HitWindow() {}

    public static boolean allowed(double nearestCollisionBoxDistance, double lookAlignment,
                                  boolean airborne) {
        if (!Double.isFinite(nearestCollisionBoxDistance) ||
                !Double.isFinite(lookAlignment)) return false;
        if (nearestCollisionBoxDistance < 0 ||
                nearestCollisionBoxDistance > 3.0) return false;
        // Aerial target can be slightly off the crosshair during a flyby.
        // Ground attacks remain much more conservative.
        return lookAlignment >= (airborne ? 0.48 : 0.80);
    }

    public static double score(double distance, double alignment) {
        return distance + (1.0 - alignment) * 2.0;
    }
}
