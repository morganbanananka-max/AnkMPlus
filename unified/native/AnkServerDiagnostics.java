/*
 * AnkHack independent diagnostics module. GPL-3.0.
 * Passive statistics: does not alter, cancel or replay any network packet.
 */
package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.AnkVariantProfiles;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class AnkServerDiagnostics extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Boolean> alertCorrections = sg.add(new BoolSetting.Builder()
        .name("correction-alerts")
        .description("Warn when the server repeatedly corrects your position.")
        .defaultValue(true).build());
    private final Setting<Integer> alertThreshold = sg.add(new IntSetting.Builder()
        .name("corrections-per-minute")
        .description("Correction threshold for an alert.")
        .defaultValue(4).min(2).sliderMax(20)
        .visible(alertCorrections::get).build());

    private final ConcurrentLinkedDeque<Long> correctionTimes = new ConcurrentLinkedDeque<>();
    private String server = "unknown";
    private long lastAlert;
    private int ticks;

    public AnkServerDiagnostics() {
        super(Categories.Misc, "ank-server-diagnostics",
            "Display actual ping and position-correction rate. Passive, read-only.");
    }
    @Override public void onActivate() {
        correctionTimes.clear();
        server = AnkVariantProfiles.serverKey(mc);
        lastAlert = 0;
        ticks = 0;
    }
    @Override public void onDeactivate() { correctionTimes.clear(); }

    @EventHandler
    private void onReceive(PacketEvent.Receive e) {
        if (e.packet instanceof PlayerPositionLookS2CPacket) {
            correctionTimes.addLast(System.currentTimeMillis());
            while (correctionTimes.size() > 200) correctionTimes.pollFirst();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post e) {
        if (mc.player == null || mc.world == null) return;
        if (++ticks % 20 != 0) return;
        String current = AnkVariantProfiles.serverKey(mc);
        if (!current.equals(server)) {
            server = current;
            correctionTimes.clear();
            lastAlert = 0;
        }
        long now = System.currentTimeMillis();
        Long first;
        while ((first = correctionTimes.peekFirst()) != null && now - first > 60_000L)
            correctionTimes.pollFirst();

        if (alertCorrections.get() && correctionTimes.size() >= alertThreshold.get()
            && now - lastAlert > 60_000L) {
            lastAlert = now;
            warning("Server position corrections: %d in the last minute. Check movement settings.",
                correctionTimes.size());
        }
    }

    @Override
    public String getInfoString() {
        if (mc.player == null || mc.getNetworkHandler() == null) return "Offline";
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        String latency = entry != null ? Integer.toString(entry.getLatency()) + " ms" : "? ms";
        return latency + " | corrections: " + correctionTimes.size() + "/min";
    }
}
