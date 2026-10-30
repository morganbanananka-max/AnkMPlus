/*
 * Wurst 7 JetpackHack behavior adapted into AnkHack's single native event bus.
 * Copyright Wurst-Imperium and contributors (GPL-3.0).
 * Not an anti-cheat bypass guarantee.
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
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

public final class AnkJetpack extends Module {
    private final SettingGroup general=settings.getDefaultGroup();
    private final Setting<Double> vertical=general.add(new DoubleSetting.Builder()
        .name("up-speed").description("Vertical jump impulse when Jump is held.")
        .defaultValue(0.42).range(0.1,0.85).sliderRange(0.1,0.85).build());
    private final Setting<Boolean> requireJump=general.add(new BoolSetting.Builder()
        .name("require-jump-key").description("Do not ascend unless Jump is held.")
        .defaultValue(true).build());
    private int corrections;
    public AnkJetpack(){
        super(Categories.Movement,"ank-jetpack","Wurst-derived repeated midair jump impulse.");
    }
    @Override public void onActivate(){corrections=0;}
    @EventHandler private void tick(TickEvent.Pre event){
        if(mc.player==null||mc.world==null||mc.currentScreen!=null
            ||mc.player.hasVehicle()||mc.player.isGliding()
            ||Modules.get().isActive(Flight.class))return;
        if(requireJump.get()&&!mc.options.jumpKey.isPressed())return;
        Vec3d motion=mc.player.getVelocity();
        // Original Wurst Jetpack calls jumpFromGround repeatedly. Preserve the
        // upward jump impulse, but do not overwrite horizontal player input.
        mc.player.setVelocity(motion.x,vertical.get(),motion.z);
    }
}
