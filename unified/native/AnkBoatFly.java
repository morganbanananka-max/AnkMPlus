/*
 * AnkHack adaptation of Wurst 7 BoatFlyHack
 * Wurst source copyright (c) 2014-2026 Wurst-Imperium and contributors
 * GPL-3.0; original: https://github.com/Wurst-Imperium/Wurst7
 */
package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class AnkBoatFly extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Boolean> forwardControl = sg.add(new BoolSetting.Builder()
        .name("forward-control")
        .description("Override the forward boat speed.")
        .defaultValue(false).build());
    private final Setting<Double> forwardSpeed = sg.add(new DoubleSetting.Builder()
        .name("forward-speed").description("Boat speed while holding forward.")
        .defaultValue(1.0).min(0.05).sliderMax(5.0)
        .visible(forwardControl::get).build());
    private final Setting<Double> upwardSpeed = sg.add(new DoubleSetting.Builder()
        .name("upward-speed").description("Upward speed when jump is pressed.")
        .defaultValue(0.3).min(0).sliderMax(5.0).build());

    public AnkBoatFly() {
        super(Categories.Movement, "ank-boat-fly", "Control vertical boat motion and forward speed.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || !mc.player.hasVehicle()) return;
        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null) return;
        Vec3d old = vehicle.getVelocity();
        double dx = old.x, dy = 0, dz = old.z;
        if (mc.options.jumpKey.isPressed()) dy = upwardSpeed.get();
        else if (mc.options.sprintKey.isPressed()) dy = old.y;
        if (mc.options.forwardKey.isPressed() && forwardControl.get()) {
            double radians = Math.toRadians(vehicle.getYaw());
            dx = Math.sin(-radians) * forwardSpeed.get();
            dz = Math.cos(radians) * forwardSpeed.get();
        }
        vehicle.setVelocity(dx, dy, dz);
    }
}
