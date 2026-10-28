package se.ankan.ankmplus;

/**
 * Deterministic slab raycast against an EXACT uninflated hitbox.
 * Pure Java and independently unit-testable.
 */
public final class ExactHitGeometry {
    private ExactHitGeometry() {}

    public static double rayAabbDistance(
            double ox, double oy, double oz, double dx, double dy, double dz,
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ, double reach) {
        if (!(Double.isFinite(ox) && Double.isFinite(oy) && Double.isFinite(oz)
                && Double.isFinite(dx) && Double.isFinite(dy) && Double.isFinite(dz)
                && Double.isFinite(reach)) || reach < 0) return Double.POSITIVE_INFINITY;
        double[] origin = {ox, oy, oz};
        double[] dir = {dx, dy, dz};
        double[] min = {minX, minY, minZ};
        double[] max = {maxX, maxY, maxZ};
        double entry = 0.0;
        double exit = reach;
        for (int i = 0; i < 3; i++) {
            if (!Double.isFinite(min[i]) || !Double.isFinite(max[i])
                    || min[i] > max[i]) return Double.POSITIVE_INFINITY;
            if (Math.abs(dir[i]) < 1.0e-12) {
                if (origin[i] < min[i] || origin[i] > max[i])
                    return Double.POSITIVE_INFINITY;
                continue;
            }
            double a = (min[i] - origin[i]) / dir[i];
            double b = (max[i] - origin[i]) / dir[i];
            if (a > b) {
                double temp = a;
                a = b;
                b = temp;
            }
            entry = Math.max(entry, a);
            exit = Math.min(exit, b);
            if (exit < entry) return Double.POSITIVE_INFINITY;
        }
        return entry <= reach ? entry : Double.POSITIVE_INFINITY;
    }
}
