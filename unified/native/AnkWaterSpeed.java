/*
 * AnkHack native bounded WaterSpeed behavior, adapted from the
 * ThunderHack Reborn GPL-3.0 concept. No extra network packets.
 */
package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

public final class AnkWaterSpeed extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Double> multiplier=sg.add(new DoubleSetting.Builder()
        .name("multiplier").description("Multiply water movement, without changing vertical speed.")
        .defaultValue(1.10).range(1.01,1.50).sliderRange(1.01,1.50).build());
    private final Setting<Double> maxSpeed=sg.add(new DoubleSetting.Builder()
        .name("maximum-horizontal-speed")
        .description("Hard cap for horizontal water motion in blocks per tick.")
        .defaultValue(0.18).range(0.05,0.45).sliderRange(0.05,0.45).build());

    public AnkWaterSpeed() {
        super(Categories.Movement, "ank-water-speed",
            "Small capped boost to horizontal swimming speed; movement may be corrected by servers.");
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player==null || mc.world==null || mc.currentScreen!=null
            || !mc.player.isTouchingWater() || mc.player.hasVehicle()
            || mc.player.isGliding() || mc.player.isSpectator()
            || Modules.get().isActive(Flight.class)
            || Modules.get().isActive(Jesus.class)) return;
        if (Math.abs(mc.player.forwardSpeed)+Math.abs(mc.player.sidewaysSpeed)<0.01f) return;
        Vec3d v=mc.player.getVelocity();
        double speed=Math.hypot(v.x,v.z);
        if (speed<0.002 || speed>=maxSpeed.get()) return;
        double adjusted=Math.min(speed*multiplier.get(),maxSpeed.get());
        double ratio=adjusted/speed;
        mc.player.setVelocity(v.x*ratio,v.y,v.z*ratio);
    }
}
