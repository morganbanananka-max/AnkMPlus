/*
 * AnkHack adaptation of ThunderHack Reborn AutoLeave.
 * Copyright retained to ThunderHack Reborn contributors; GPL-3.0.
 * Original: https://github.com/Pan4ur/ThunderHack-Recode
 */
package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class AnkAutoLeave extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Boolean> lowHealth = sg.add(new BoolSetting.Builder()
        .name("low-health").description("Disconnect below configured health.")
        .defaultValue(false).build());
    private final Setting<Double> hp = sg.add(new DoubleSetting.Builder()
        .name("health-threshold").description("HP for emergency disconnect.")
        .defaultValue(8).min(1).sliderMax(20).visible(lowHealth::get).build());
    private final Setting<Boolean> nearbyPlayer = sg.add(new BoolSetting.Builder()
        .name("nearby-player").description("Disconnect if another player appears.")
        .defaultValue(false).build());
    private final Setting<Integer> range = sg.add(new IntSetting.Builder()
        .name("player-range").description("Detection distance in blocks.")
        .defaultValue(32).min(4).sliderMax(128).visible(nearbyPlayer::get).build());
    private final Setting<Boolean> once = sg.add(new BoolSetting.Builder()
        .name("auto-disable").description("Turn the module off before disconnecting.")
        .defaultValue(true).build());
    private long lastAction;

    public AnkAutoLeave() {
        super(Categories.Misc, "ank-auto-leave", "Leave on danger conditions. Off by default.");
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.isInSingleplayer()) return;
        if (System.currentTimeMillis()-lastAction<2000) return;
        String reason = null;
        if (lowHealth.get() && mc.player.getHealth() <= hp.get()) reason = "low health";
        if (reason == null && nearbyPlayer.get()) {
            double dist = (double)range.get()*range.get();
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || !player.isAlive() || player.isSpectator()) continue;
                if (player.squaredDistanceTo(mc.player) <= dist) {
                    reason = "another player nearby";
                    break;
                }
            }
        }
        if (reason == null) return;
        lastAction = System.currentTimeMillis();
        if (once.get()) toggle();
        if (mc.getNetworkHandler()!=null)
            mc.getNetworkHandler().getConnection().disconnect(Text.literal("AnkHack AutoLeave: "+reason));
    }
}
