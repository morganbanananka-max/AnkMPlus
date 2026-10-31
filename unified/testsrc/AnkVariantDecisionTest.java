/*
 * Reproducible offline behavioral checks for AnkHack's pure Java auto selector.
 * Run: javac -d /tmp/ankhack-check unified/native/AnkVariantDecision.java
 *          unified/testsrc/AnkVariantDecisionTest.java
 *      java -cp /tmp/ankhack-check meteordevelopment.meteorclient.AnkVariantDecisionTest
 * These simulate observations; they do NOT prove in-game client functionality.
 */
package meteordevelopment.meteorclient;

import java.util.*;

public final class AnkVariantDecisionTest {
    private static int assertions;

    private static void eq(String expected, String actual) {
        assertions++;
        if (!Objects.equals(expected, actual))
            throw new AssertionError("Expected '"+expected+"', got '"+actual+"'");
    }

    private static AnkVariantDecision.Evidence ev(int good, int corrections, int samples, int speed) {
        return new AnkVariantDecision.Evidence(good,corrections,samples,speed);
    }

    public static void main(String[] args) {
        String[] flight={"Abilities","Velocity","WurstMotion"};
        eq("",AnkVariantDecision.choose(new String[0],Map.of()));
        eq("Abilities",AnkVariantDecision.choose(flight,Map.of()));
        eq("Abilities",AnkVariantDecision.choose(flight,Map.of("Velocity",ev(1,0,0,0))));
        eq("Velocity",AnkVariantDecision.choose(flight,Map.of("Velocity",ev(4,0,0,0))));
        eq("Abilities",AnkVariantDecision.choose(flight,Map.of("Velocity",ev(4,3,0,0))));
        eq("WurstMotion",AnkVariantDecision.choose(flight,
            Map.of("Velocity",ev(8,6,0,0), "WurstMotion",ev(6,0,0,0))));
        eq("Abilities",AnkVariantDecision.choose(flight,
            Map.of("Abilities",ev(60,0,0,0),"Velocity",ev(3,0,0,0))));
        eq("Abilities",AnkVariantDecision.choose(flight,
            Map.of("UnrelatedClient",ev(10_000,0,10,100_000))));
        eq("Abilities",AnkVariantDecision.choose(flight,
            Map.of("Velocity",ev(-5,-2,-10,-1))));
        eq("Strict",AnkVariantDecision.choose(new String[]{"Strict","LiquidLegit"},Map.of()));
        eq("LiquidLegit",AnkVariantDecision.choose(new String[]{"Strict","LiquidLegit"},
            Map.of("LiquidLegit",ev(7,0,0,0))));
        eq("Strict",AnkVariantDecision.choose(new String[]{"Strict","LiquidLegit"},
            Map.of("LiquidLegit",ev(7,4,0,0))));
        eq("",AnkVariantDecision.choose(null,Map.of()));
        eq("Fallback",AnkVariantDecision.choose(new String[]{"Fallback"},Map.of()));

        // Independent profile observations cannot leak between servers.
        Map<String,Map<String,AnkVariantDecision.Evidence>> servers=new HashMap<>();
        servers.put("server-a",Map.of("Velocity",ev(8,0,0,0)));
        servers.put("server-b",Map.of("Velocity",ev(8,6,0,0)));
        eq("Velocity",AnkVariantDecision.choose(flight,servers.get("server-a")));
        eq("Abilities",AnkVariantDecision.choose(flight,servers.get("server-b")));

        // Fuzz 20k reproducible synthetic evidence scenarios; decision is
        // deterministic, always yields a registered variant, and has no I/O.
        Random rng=new Random(0xA11CAFE);
        for(int i=0;i<20_000;i++) {
            Map<String,AnkVariantDecision.Evidence> random=new HashMap<>();
            for(String v:flight) {
                if(rng.nextBoolean())random.put(v,
                    ev(rng.nextInt(50),rng.nextInt(12),rng.nextInt(30),rng.nextInt(1000)));
            }
            String chosen=AnkVariantDecision.choose(flight,random);
            if(!List.of(flight).contains(chosen))
                throw new AssertionError("Unregistered variant: "+chosen);
            eq(chosen,AnkVariantDecision.choose(flight,random));
            if(random.getOrDefault("Velocity",ev(0,0,0,0)).successes()<3
                && random.getOrDefault("WurstMotion",ev(0,0,0,0)).successes()<3)
                eq("Abilities",chosen);
        }
        System.out.println("PASS AnkVariantDecisionTest: "+assertions+
            " deterministic checks, 20,000 simulated sessions, zero Minecraft dependencies.");
    }
}
