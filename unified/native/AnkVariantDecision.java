/*
 * AnkHack original implementation.
 * Evidence-based, conservative recommendation for duplicate hack implementations.
 * Standalone Java 21 source: unit-testable without Minecraft or Fabric.
 */
package meteordevelopment.meteorclient;

import java.util.Map;

public final class AnkVariantDecision {
    private AnkVariantDecision() {}

    public record Evidence(int stable, int corrections, int motionSamples, int motionSum) {
        public Evidence {
            stable = Math.max(0, stable);
            corrections = Math.max(0, corrections);
            motionSamples = Math.max(0, motionSamples);
            motionSum = Math.max(0, motionSum);
        }

        public int successes() {
            return Math.min(100_000, stable + motionSamples);
        }

        public boolean credible() {
            // A single lucky jump, shot or lag spike is not enough evidence
            // to change another server's settings.
            return successes() >= 3;
        }

        public int meanMotion() {
            return motionSamples == 0 ? 0 : motionSum / motionSamples;
        }
    }

    public static String choose(String[] names, Map<String, Evidence> observations) {
        if (names == null || names.length == 0 || names[0] == null) return "";
        final String fallback = names[0];
        if (observations == null || observations.isEmpty()) return fallback;

        Evidence baseline = observations.getOrDefault(fallback, new Evidence(0,0,0,0));
        double baselineScore = quality(baseline);
        String winner = fallback;
        double best = baselineScore;

        // Never use a fallback from a different family.
        // Switching to an alternative requires repeated independent observations
        // and a clear improvement beyond small/random fluctuations.
        for (int i = 1; i < names.length; i++) {
            String name = names[i];
            if (name == null || name.isBlank() || name.equals(fallback)) continue;
            Evidence sample = observations.get(name);
            if (sample == null || !sample.credible()) continue;
            double score = quality(sample);
            double margin = Math.max(2.0, Math.abs(baselineScore) * 0.12);
            if (score > best + margin) {
                winner = name;
                best = score;
            }
        }
        return winner;
    }

    static double quality(Evidence value) {
        // Corrected movement is strong negative evidence. Successful samples
        // have diminishing returns, avoiding long-running server bias.
        int observations = Math.min(1000, value.successes());
        double positive = 6.0 * Math.log1p(observations);
        double negative = 12.0 * Math.min(1000, value.corrections());
        double speed = value.motionSamples() >= 3
            ? Math.min(6.0, value.meanMotion() / 50.0)
            : 0.0;
        return positive + speed - negative;
    }
}
