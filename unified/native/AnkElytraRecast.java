package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

/**
 * Conservative ElytraRecast implementation, adapted from ThunderHack Reborn (GPL-3.0).
 * Sends normal vanilla start-fall-flying command only while descending with Elytra.
 * Does not claim to reproduce ThunderHack's pitch or exploit-specific modes.
 */
public class AnkElytraRecast extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Boolean> requireJump = sg.add(new BoolSetting.Builder()
        .name("require-jump-held").description("Only retry glide when the jump key is held.")
        .defaultValue(true).build());
    private final Setting<Integer> interval = sg.add(new IntSetting.Builder()
        .name("retry-ticks").description("Cooldown between glide requests.")
        .defaultValue(12).range(4, 40).sliderRange(4, 40).build());
    private int ticks;
    public AnkElytraRecast() {
        super(Categories.Movement, "ank-elytra-recast", "Retries starting Elytra flight while falling (ThunderHack adaptation).");
    }
    @Override public void onActivate() { ticks = 0; }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;
        if (ticks > 0) ticks--;
        if (ticks > 0) return;
        if (requireJump.get() && !mc.options.jumpKey.isPressed()) return;
        if (mc.player.isOnGround() || mc.player.isGliding() || mc.player.isTouchingWater()) return;
        if (!mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) return;
        if (mc.player.fallDistance < 1.1f || mc.player.getVelocity().y >= -0.05) return;
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        ticks = interval.get();
    }
}
