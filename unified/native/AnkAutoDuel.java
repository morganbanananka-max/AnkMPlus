package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import java.util.Locale;

/**
 * ThunderHack Reborn 1.21.11 AutoDuel Accept-mode adaptation (GPL-3.0).
 * Only accepts explicit requests from the player name configured by the user;
 * no unsolicited commands on enable, no unbounded chat spam.
 */
public class AnkAutoDuel extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<String> nickname = sg.add(new StringSetting.Builder()
        .name("player").description("Only accept /duel requests from this exact Minecraft username.")
        .defaultValue("").build());
    private final Setting<Integer> cooldown = sg.add(new IntSetting.Builder()
        .name("cooldown-seconds").description("Minimum time between duel acceptance commands.")
        .defaultValue(30).range(5, 300).sliderRange(5, 90).build());
    private volatile boolean pending;
    private int ticksSinceLastCommand = 6000;

    public AnkAutoDuel() {
        super(Categories.Misc, "ank-auto-duel", "Accept duel requests from one trusted player (ThunderHack adaptation).");
    }
    @Override public void onActivate() {
        pending = false;
        ticksSinceLastCommand = 6000;
    }
    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof GameMessageS2CPacket packet)) return;
        String name = nickname.get().trim();
        if (!name.matches("[A-Za-z0-9_]{3,16}")) return;
        String text = packet.content().getString().toLowerCase(Locale.ROOT);
        if (text.contains("duel request received from " + name.toLowerCase(Locale.ROOT))) pending = true;
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.getNetworkHandler() == null || mc.world == null) {
            pending = false; return;
        }
        if (ticksSinceLastCommand < 100000) ticksSinceLastCommand++;
        if (!pending || ticksSinceLastCommand < cooldown.get() * 20) return;
        pending = false;
        String name = nickname.get().trim();
        if (!name.matches("[A-Za-z0-9_]{3,16}")) return;
        mc.getNetworkHandler().sendChatCommand("duel accept " + name);
        ticksSinceLastCommand = 0;
    }
}
