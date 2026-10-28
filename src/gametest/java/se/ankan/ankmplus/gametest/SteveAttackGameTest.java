package se.ankan.ankmplus.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * REAL integrated-server test.
 * The player never presses attack, so damage on Steve has to come
 * from AnkM+ auto-hit. The same test verifies aiming away and a block
 * between the player and Steve prevent all attacks.
 */
@SuppressWarnings("UnstableApiUsage")
public final class SteveAttackGameTest implements FabricClientGameTest {
    private static float health(Minecraft mc) {
        if (mc.level == null || mc.player == null) return -1;
        for (var entity : mc.level.getEntities(mc.player,
                mc.player.getBoundingBox().inflate(8.0),
                other -> other instanceof LivingEntity)) {
            if (entity.getName().getString().equals("Steve"))
                return ((LivingEntity) entity).getHealth();
        }
        return -1;
    }

    private static void assertHealthSame(float before, float after, String message) {
        if (before < 1 || Math.abs(before - after) > 0.01f)
            throw new AssertionError(message + " (" + before + " -> " + after + ")");
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getClientLevel().waitForChunksRender();
            var server = world.getServer();

            // Reset the flat arena and park the player facing away from target.
            server.runCommand("time set midnight"); // Prevent zombie burning in daylight.
            server.runCommand("gamemode survival @p");
            server.runCommand("fill -5 65 -5 5 70 6 minecraft:air");
            server.runCommand("fill -5 64 -5 5 64 6 minecraft:stone");
            server.runCommand("tp @p 0 65 0 90 0");
            server.runCommand("give @p minecraft:mace");
            server.runCommand("enchant @p minecraft:breach 1");
            ctx.getInput().lookAt(90, 0);
            server.runCommand("summon minecraft:zombie 0 65 2 {NoAI:1b,PersistenceRequired:1b,CustomName:'{\"text\":\"Steve\"}'}");
            ctx.waitTicks(8);
            float initial = ctx.computeOnClient(SteveAttackGameTest::health);
            if (initial < 1)
                throw new AssertionError("Steve training zombie did not spawn.");

            // CASE 1: Auto-hit must not attack when the player looks away.
            ctx.waitTicks(15);
            float away = ctx.computeOnClient(SteveAttackGameTest::health);
            assertHealthSame(initial, away, "CHEAT: auto-hit struck an off-crosshair target");

            // CASE 2: Looking at Steve through a wall is not a valid attack.
            server.runCommand("setblock 0 66 1 minecraft:glass");
            ctx.getInput().lookAt(0, 0);
            ctx.waitTicks(15);
            float blocked = ctx.computeOnClient(SteveAttackGameTest::health);
            assertHealthSame(initial, blocked, "CHEAT: auto-hit struck through glass");

            // CASE 3: The exact same auto-hit must now DAMAGE Steve in real Minecraft.
            server.runCommand("setblock 0 66 1 minecraft:air");
            ctx.waitTicks(18);
            float hit = ctx.computeOnClient(SteveAttackGameTest::health);
            if (!(hit >= 0 && hit < initial)) {
                throw new AssertionError("AUTO-HIT FAILED: Steve still at HP " + hit
                        + " (expected lower than " + initial + ")");
            }

            System.out.println("[AnkM+ GameTest] PASS: no off-target hits; no wall hits; Steve damaged "
                    + initial + " -> " + hit);
        }
    }
}
