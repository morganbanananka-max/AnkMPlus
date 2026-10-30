/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * GPL-3.0; adapted from Wurst 7 GlideHack into AnkHack's unified system.
 */
package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

public final class AnkGlide extends Module {
    private final SettingGroup group=settings.getDefaultGroup();
    private final Setting<Double> maxFall=group.add(new DoubleSetting.Builder()
       .name("max-fall-speed").description("Limit negative Y velocity when gliding.")
       .defaultValue(0.125).min(0.005).sliderMax(0.25).build());
    private final Setting<Boolean> pauseSneak=group.add(new BoolSetting.Builder()
       .name("pause-when-sneaking").description("Let the player fall normally while sneaking.")
       .defaultValue(true).build());
    private final Setting<Double> minHeight=group.add(new DoubleSetting.Builder()
       .name("min-ground-clearance").description("Do not slow falling close to the ground.")
       .defaultValue(0).min(0).sliderMax(2).build());
    public AnkGlide() {
        super(Categories.Movement,"glide","Wurst-derived controlled descent; does not grant server flight permission.");
    }
    @EventHandler
    private void tick(TickEvent.Pre event) {
        if(mc.player == null || mc.world == null || mc.player.isOnGround()
           || mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isClimbing()
           || mc.player.isGliding() || (pauseSneak.get() && mc.player.isSneaking())
           || Modules.get().isActive(Flight.class) || Modules.get().isActive(Freecam.class))
           return;
        Vec3d v=mc.player.getVelocity();
        if(v.y >= -maxFall.get()) return;
        if(minHeight.get() > 0) {
            var box=mc.player.getBoundingBox();
            var under=box.offset(0,-minHeight.get(),0);
            if(!mc.world.isSpaceEmpty(mc.player,under)) return;
        }
        mc.player.setVelocity(v.x, -maxFall.get(), v.z);
    }
}
