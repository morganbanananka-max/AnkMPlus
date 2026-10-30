/*
 * Original AnkHack adaptation of LiquidBounce ModuleAutoRod (GPL-3.0).
 * Copyright (c) 2015-2026 CCBlueX for the original concept/source.
 * This port is explicitly LIMITED: it requires manual aim, rather than
 * falsely claiming to replicate LiquidBounce's trajectory/rotation engine.
 */
package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

public final class AnkCombatRod extends Module {
    private final SettingGroup group=settings.getDefaultGroup();
    private final Setting<Double> range=group.add(new DoubleSetting.Builder()
        .name("rod-range").description("Maximum aimed target distance in blocks.")
        .defaultValue(5.0).min(2).sliderMax(8).build());
    private final Setting<Integer> cooldown=group.add(new IntSetting.Builder()
        .name("cooldown-ticks").description("Ticks between rod throws.")
        .defaultValue(15).min(5).sliderMax(80).build());
    private int left;
    public AnkCombatRod() {
        super(Categories.Combat,"ank-combat-rod","LiquidBounce-inspired rod throw on a manually aimed combat target.");
    }
    @Override public void onActivate(){left=0;}
    @EventHandler
    private void tick(TickEvent.Pre event) {
        if(mc.player==null || mc.world==null || mc.interactionManager==null
            || mc.currentScreen!=null || !mc.player.getMainHandStack().isOf(Items.FISHING_ROD))
            return;
        if(left>0){left--;return;}
        if(mc.player.fishHook!=null)return;
        if(!(mc.crosshairTarget instanceof EntityHitResult result)
           || !(result.getEntity() instanceof LivingEntity living)
           || living==mc.player || !living.isAlive()
           || mc.player.squaredDistanceTo(living)>range.get()*range.get())
           return;
        mc.interactionManager.interactItem(mc.player,Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
        left=cooldown.get();
    }
}
