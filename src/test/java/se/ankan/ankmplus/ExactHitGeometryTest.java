package se.ankan.ankmplus;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExactHitGeometryTest {
    private double ray(double y, double z, double range) {
        return ExactHitGeometry.rayAabbDistance(
                0, y, z, 1, 0, 0,
                2, 0, -0.3, 2.6, 1.8, 0.3, range);
    }

    @Test void onlyHitRealBoundingBox() {
        assertEquals(2.0, ray(1, 0, 3), 0.00001);
        assertEquals(Double.POSITIVE_INFINITY, ray(2.1, 0, 3));
        assertEquals(Double.POSITIVE_INFINITY, ray(1, 0.31, 3));
        assertEquals(Double.POSITIVE_INFINITY, ray(1, -0.31, 3));
    }

    @Test void neverExtendVanillaReach() {
        assertEquals(Double.POSITIVE_INFINITY, ray(1, 0, 1.99));
        assertEquals(2.0, ray(1, 0, 2), 0.00001);
        assertEquals(Double.POSITIVE_INFINITY,
                ExactHitGeometry.rayAabbDistance(
                        0, 1, 0, 1, 0, 0,
                        3.01, 0, -0.5, 3.5, 2, 0.5, 3));
    }

    @Test void behindAndParallelAreMisses() {
        assertEquals(Double.POSITIVE_INFINITY,
                ExactHitGeometry.rayAabbDistance(
                        0, 1, 0, -1, 0, 0,
                        2, 0, -0.5, 2.6, 2, 0.5, 3));
        assertEquals(Double.POSITIVE_INFINITY,
                ExactHitGeometry.rayAabbDistance(
                        0, 1, 0, 0, 1, 0,
                        2, 0, -0.5, 2.6, 2, 0.5, 3));
    }

    @Test void insideBodyCanReturnZero() {
        assertEquals(0, ExactHitGeometry.rayAabbDistance(
                2.2, 1, 0, 1, 0, 0,
                2, 0, -0.3, 2.6, 1.8, 0.3, 3), 0.0001);
    }

    @Test void invalidNumbersNeverAttack() {
        assertEquals(Double.POSITIVE_INFINITY,
                ExactHitGeometry.rayAabbDistance(
                        0, 1, 0, Double.NaN, 0, 0,
                        2, 0, -0.5, 2.6, 2, 0.5, 3));
        assertEquals(Double.POSITIVE_INFINITY,
                ExactHitGeometry.rayAabbDistance(
                        0, 1, 0, 1, 0, 0,
                        2, 0, -0.5, 2.6, 2, 0.5, -1));
    }
}
