/*
 * AnkHack native implementation based on the behavior of ThunderHack
 * Reborn's GPL-3.0 NoJumpDelay. Only affects the local jumping timer.
 */
package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.LivingEntityAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;

public final class AnkNoJumpDelay extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Boolean> onlyWhileJumping = sg.add(new BoolSetting.Builder()
        .name("only-while-jumping")
        .description("Do not change the local cooldown when Jump is not held.")
        .defaultValue(true).build());

    public AnkNoJumpDelay() {
        super(Categories.Movement, "ank-no-jump-delay",
            "Clears the local jump cooldown while grounded. Servers may still limit jumping.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null
            || !mc.player.isOnGround() || mc.player.isSpectator()
            || mc.player.hasVehicle() || mc.player.isClimbing()
            || mc.player.isTouchingWater() || mc.player.isInLava()
            || Modules.get().isActive(Jesus.class)) return;
        if (onlyWhileJumping.get() && !mc.options.jumpKey.isPressed()) return;
        ((LivingEntityAccessor) mc.player).meteor$setJumpCooldown(0);
    }
}
