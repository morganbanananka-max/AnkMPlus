package se.ankan.ankmplus.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import se.ankan.ankmplus.AnkMPlusClient;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
            if (entity.getName().getString().contains("Steve"))
                return ((LivingEntity) entity).getHealth();
        }
        return -1;
    }

    private static void assertHealthSame(float before, float after, String message) {
        if (before < 1 || Math.abs(before - after) > 0.01f)
            throw new AssertionError(message + " (" + before + " -> " + after + ")");
    }

    /** Invoke the same inventory transaction used during an actual Elytra hit. */
    private static boolean invokeEquipment(Minecraft mc, String name, boolean disableAuto) {
        try {
            Class<?> cls = AnkMPlusClient.class;
            Field instanceField = cls.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            Object instance = instanceField.get(null);
            if (instance == null) throw new AssertionError("AnkM+ not initialized");
            if (disableAuto) {
                Field auto = cls.getDeclaredField("autoHit");
                auto.setAccessible(true);
                auto.setBoolean(instance, false);
                Field restoreAt = cls.getDeclaredField("restoreAfterTick");
                restoreAt.setAccessible(true);
                restoreAt.setInt(instance, Integer.MAX_VALUE);
            }
            Method method = cls.getDeclaredMethod(name, Minecraft.class);
            method.setAccessible(true);
            return (boolean) method.invoke(instance, mc);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed real inventory interaction: " + name, e);
        }
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            var server = world.getServer();

            // Reset the flat arena and park the player facing away from target.
            server.runCommand("time set midnight"); // Prevent zombie burning in daylight.
            server.runCommand("gamemode survival @p");
            server.runCommand("fill -5 65 -5 5 70 6 minecraft:air");
            server.runCommand("fill -5 64 -5 5 64 6 minecraft:stone");
            server.runCommand("tp @p 0 65 0 90 0");
            server.runCommand("give @p minecraft:mace");
            server.runCommand("enchant @p minecraft:breach 1");
            ctx.runOnClient(mc -> {
                mc.player.setYRot(90);
                mc.player.setXRot(0);
            });
            server.runCommand("summon minecraft:zombie 0 65 2 {NoAI:1b,PersistenceRequired:1b,CustomName:'{\"text\":\"Steve\"}'}");
            ctx.waitTicks(8);
            float initial = ctx.computeOnClient(SteveAttackGameTest::health);
            // Integrated server chunk/entity replication can take longer on CI.
            // Do not mislabel slow loading as an AnkM+ attack failure.
            for (int retry = 0; retry < 60 && initial < 1; retry++) {
                ctx.waitTick();
                initial = ctx.computeOnClient(SteveAttackGameTest::health);
            }
            if (initial < 1) {
                String debug = ctx.computeOnClient(mc -> {
                    if (mc.player == null || mc.level == null) return "no client world/player";
                    StringBuilder nearby = new StringBuilder("player=" + mc.player.position());
                    for (var entity : mc.level.getEntities(mc.player,
                            mc.player.getBoundingBox().inflate(16.0),
                            other -> other instanceof LivingEntity)) {
                        nearby.append(" / ").append(entity.getType()).append(":")
                                .append(entity.getName().getString())
                                .append("@").append(entity.position());
                    }
                    return nearby.toString();
                });
                throw new AssertionError("Steve not visible to client: " + debug);
            }

            // CASE 1: Auto-hit must not attack when the player looks away.
            ctx.waitTicks(15);
            float away = ctx.computeOnClient(SteveAttackGameTest::health);
            assertHealthSame(initial, away, "CHEAT: auto-hit struck an off-crosshair target");

            // CASE 2: Looking at Steve through a wall is not a valid attack.
            server.runCommand("setblock 0 66 1 minecraft:glass");
            ctx.runOnClient(mc -> {
                mc.player.setYRot(0);
                mc.player.setXRot(0);
            });
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

            // Exercise the REAL player inventory-click packets rather than
            // only checking a simulated transaction plan.
            server.runCommand("kill @e[type=minecraft:zombie]");
            server.runCommand("clear @p");
            server.runCommand("item replace entity @p armor.chest with minecraft:elytra");
            server.runCommand("item replace entity @p hotbar.1 with minecraft:netherite_chestplate");
            ctx.waitTicks(5);

            boolean startedCorrectly = ctx.computeOnClient(mc ->
                    mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
                    && mc.player.getInventory().getItem(1).is(Items.NETHERITE_CHESTPLATE));
            if (!startedCorrectly) throw new AssertionError("Minecraft did not equip test Elytra/chestplate");

            boolean swapIssued = ctx.computeOnClient(mc ->
                    invokeEquipment(mc, "equipChestplateOrRemoveElytra", true));
            if (!swapIssued) throw new AssertionError("Elytra/chestplate swap refused");
            ctx.waitTicks(4);
            boolean exchanged = ctx.computeOnClient(mc ->
                    mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE)
                    && mc.player.getInventory().getItem(1).is(Items.ELYTRA));
            if (!exchanged) throw new AssertionError("Real inventory swap did not equip chestplate");

            boolean restoreIssued = ctx.computeOnClient(mc ->
                    invokeEquipment(mc, "restoreArmor", false));
            if (!restoreIssued) throw new AssertionError("Real Elytra restore refused");
            ctx.waitTicks(4);
            boolean restored = ctx.computeOnClient(mc ->
                    mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
                    && mc.player.getInventory().getItem(1).is(Items.NETHERITE_CHESTPLATE));
            if (!restored)
                throw new AssertionError("Real inventory restore misplaced Elytra/chestplate");
            System.out.println("[AnkM+ GameTest] PASS: real Elytra/chestplate swap and original hotbar restoration");

            // CASE 5: Missed/aborted approach must not leave the chestplate on forever.
            // Keep a Steve dummy IN the preparation cone, but remove the Mace so
            // it is impossible to score a hit or clear approach via attacking.
            ctx.waitTicks(12); // Let the previous swap cooldown expire.
            server.runCommand("summon minecraft:zombie 0 65 2 {NoAI:1b,PersistenceRequired:1b,CustomName:'{\"text\":\"Steve\"}'}");
            ctx.runOnClient(mc -> { mc.player.setYRot(0); mc.player.setXRot(0); });
            ctx.waitTicks(5);
            boolean startedSecond = ctx.computeOnClient(mc ->
                    invokeEquipment(mc, "equipChestplateOrRemoveElytra", true));
            if (!startedSecond) throw new AssertionError("Aborted-pass swap refused");
            ctx.computeOnClient(mc -> {
                try {
                    Class<?> cls = AnkMPlusClient.class;
                    Field instanceField = cls.getDeclaredField("INSTANCE");
                    instanceField.setAccessible(true);
                    Object mod = instanceField.get(null);
                    Field auto = cls.getDeclaredField("autoHit");
                    Field start = cls.getDeclaredField("restoreAfterTick");
                    Field tickField = cls.getDeclaredField("tick");
                    auto.setAccessible(true);
                    start.setAccessible(true);
                    tickField.setAccessible(true);
                    auto.setBoolean(mod, true);
                    start.setInt(mod, tickField.getInt(mod) + 1);
                    return true;
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError("Cannot set up aborted approach check", e);
                }
            });
            ctx.waitTicks(16);
            boolean abortedRestored = ctx.computeOnClient(mc ->
                    mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
                    && mc.player.getInventory().getItem(1).is(Items.NETHERITE_CHESTPLATE));
            if (!abortedRestored)
                throw new AssertionError("Missed attack left chestplate worn or Elytra in wrong slot");
            System.out.println("[AnkM+ GameTest] PASS: missed-attack Elytra recovery");
        }
    }
}
