package se.ankan.ankmplus.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import se.ankan.ankmplus.AnkMPlusClient;

/**
 * Real Minecraft 1.21.11 inventory and aim tests:
 * a stationary left-click must leave the held item alone,
 * but shield Stunslam chooses Axe on click one, Mace on click two.
 * The game itself is NOT auto-attacked by this test.
 */
@SuppressWarnings("UnstableApiUsage")
public final class ManualClickGameTest implements FabricClientGameTest {
    private static LivingEntity steve(Minecraft mc) {
        if (mc.player == null || mc.level == null) return null;
        for (var e : mc.level.getEntities(mc.player,
                mc.player.getBoundingBox().inflate(8.0),
                entity -> entity instanceof LivingEntity)) {
            if (e.getName().getString().contains("Steve")) return (LivingEntity) e;
        }
        return null;
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            var server = world.getServer();
            server.runCommand("time set midnight");
            server.runCommand("gamemode survival @p");
            server.runCommand("fill -5 65 -5 5 70 6 minecraft:air");
            server.runCommand("fill -5 64 -5 5 64 6 minecraft:stone");
            server.runCommand("tp @p 0 65 0 90 0");
            server.runCommand("item replace entity @p hotbar.0 with minecraft:firework_rocket");
            server.runCommand("item replace entity @p hotbar.1 with minecraft:netherite_axe");
            server.runCommand("item replace entity @p hotbar.2 with minecraft:mace");
            server.runCommand("summon minecraft:zombie 0 65 2 {NoAI:1b,PersistenceRequired:1b,CustomName:'{\"text\":\"Steve\"}'}");
            ctx.waitTicks(10);
            for (int retries = 0; retries < 80
                    && !ctx.computeOnClient(mc -> steve(mc) != null); retries++) ctx.waitTick();
            if (!ctx.computeOnClient(mc -> steve(mc) != null))
                throw new AssertionError("Steve not loaded in manual test");

            // Control: NO input/no auto hit means Steve must retain full health.
            ctx.waitTicks(15);
            float healthBefore = ctx.computeOnClient(mc -> steve(mc).getHealth());
            if (Math.abs(healthBefore - 20f) > 0.01f)
                throw new AssertionError("A manual mod damaged Steve without left click");

            // Ordinary manual click ON THE GROUND must NOT auto-swap to Mace.
            boolean stayedOnRocket = ctx.computeOnClient(mc -> {
                var p = mc.player;
                var target = steve(mc);
                p.getInventory().setSelectedSlot(0);
                p.setYRot(0);
                p.setXRot(0);
                mc.hitResult = new EntityHitResult(target);
                AnkMPlusClient.beforeManualClick(mc);
                return p.getInventory().getSelectedSlot() == 0;
            });
            if (!stayedOnRocket)
                throw new AssertionError("Ground click incorrectly selected Mace");

            // Simulate opponent actively blocking with a shield on the client.
            // This calls the SAME beforeManualClick invoked by the vanilla
            // click mixin, but does not generate any artificial attacks.
            String firstAxe = ctx.computeOnClient(mc -> {
                var p = mc.player;
                var target = steve(mc);
                p.getInventory().setSelectedSlot(0);
                target.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                target.startUsingItem(InteractionHand.OFF_HAND);
                // Zombies are not player AI and do not automatically raise
                // a shield. Simulate the same synchronized "using item" flag
                // an actual blocking player sends to the client.
                try {
                    var field = LivingEntity.class.getDeclaredField("DATA_LIVING_ENTITY_FLAGS");
                    field.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    net.minecraft.network.syncher.EntityDataAccessor<Byte> flags =
                            (net.minecraft.network.syncher.EntityDataAccessor<Byte>) field.get(null);
                    byte value = target.getEntityData().get(flags);
                    target.getEntityData().set(flags, (byte) (value | 1));
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError("Cannot emulate genuine shield-use state", e);
                }
                boolean raised = target.isUsingItem();
                boolean itemShield = target.getUseItem().is(Items.SHIELD);
                boolean lineOfSight = p.hasLineOfSight(target);
                boolean actualAxe = p.getInventory().getItem(1).getItem() instanceof net.minecraft.world.item.AxeItem;
                boolean actualMace = p.getInventory().getItem(2).is(Items.MACE);
                mc.hitResult = new EntityHitResult(target);
                AnkMPlusClient.beforeManualClick(mc);
                return "slot=" + p.getInventory().getSelectedSlot()
                        + " raised=" + raised
                        + " shield=" + itemShield
                        + " LoS=" + lineOfSight
                        + " axe=" + actualAxe
                        + " mace=" + actualMace
                        + " target=" + target.getName().getString();
            });
            if (!firstAxe.startsWith("slot=1 "))
                throw new AssertionError("Stunslam click 1 did not choose Axe: " + firstAxe);

            // Target's shield is now stunned/lowered. Follow-up MUST choose
            // Mace even on flat ground, since this is the shield-combo exception.
            boolean secondMace = ctx.computeOnClient(mc -> {
                var p = mc.player;
                var target = steve(mc);
                target.stopUsingItem();
                mc.hitResult = new EntityHitResult(target);
                AnkMPlusClient.beforeManualClick(mc);
                return p.getInventory().getSelectedSlot() == 2;
            });
            if (!secondMace) throw new AssertionError("Stunslam click 2 did not choose Mace");
            System.out.println("[AnkM+ Manual GameTest] PASS: no auto-hit; no ground Mace; shield Axe then Mace with manual clicks");
        }
    }
}
