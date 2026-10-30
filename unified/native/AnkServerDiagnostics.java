package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.AnkVariantProfiles;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AnkHack server correction diagnostics, original implementation.
 *
 * PlayerPositionLookS2CPacket is a correction/teleport packet, NOT proof of
 * a Grim flag. Join, respawn, dimension transfer and teleport can emit it.
 * Collect observations after the grace period and on the client tick thread.
 *
 * Does not attempt to spoof or evade the server's anticheat.
 */
public class AnkServerDiagnostics extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<Boolean> warnings = sg.add(new BoolSetting.Builder()
        .name("warnings")
        .description("Warn when many server position corrections arrive in a short period.")
        .defaultValue(true).build());

    private final Setting<Integer> threshold = sg.add(new IntSetting.Builder()
        .name("warning-threshold")
        .description("How many position corrections in six seconds should trigger a warning.")
        .defaultValue(4).range(2,20).sliderRange(2,10).build());

    private final Setting<Boolean> stopFlight = sg.add(new BoolSetting.Builder()
        .name("stop-flight-after-repeated-corrections")
        .description("Disable Flight rather than repeatedly moving against server corrections.")
        .defaultValue(true).build());

    // Adapted from the user-supplied ThunderHack LagNotifier concept.
    // World-time packets are useful indicators, NOT a reliable proof of server TPS.
    private final Setting<Boolean> timeUpdateWarnings = sg.add(new BoolSetting.Builder()
        .name("world-time-stall-warnings")
        .description("Warn if a multiplayer server stops sending world-time updates.")
        .defaultValue(false).build());

    private final Setting<Integer> stallSeconds = sg.add(new IntSetting.Builder()
        .name("stall-seconds")
        .description("Seconds without a world-time update before warning (plugin filters can cause this).")
        .defaultValue(8).range(5,30).sliderRange(5,30)
        .visible(timeUpdateWarnings::get).build());

    private final AtomicLong lastTimeUpdateNanos = new AtomicLong();
    private long lastStallAlertNanos;

    private final AtomicInteger pending = new AtomicInteger();
    private String lastServer = "unknown";
    private int worldTicks;
    private int totalCorrections;
    private int correctionsInWindow;
    private int windowTicks;
    private int warnCooldown;

    public AnkServerDiagnostics() {
        super(Categories.Misc, "ank-server-diagnostics",
            "Shows server position corrections and optionally stops repeatedly rejected Flight. Corrections are not necessarily anticheat flags.");
    }

    @Override public void onActivate() {
        clear();
        lastServer = AnkVariantProfiles.serverKey(mc);
    }
    @Override public void onDeactivate() { clear(); }

    private void clear() {
        pending.set(0);
        lastTimeUpdateNanos.set(0L);
        lastStallAlertNanos = 0L;
        worldTicks = 0;
        totalCorrections = 0;
        correctionsInWindow = 0;
        windowTicks = 0;
        warnCooldown = 0;
    }

    @Override public String getInfoString() {
        if(mc.player==null || mc.getNetworkHandler()==null) return "Offline";
        PlayerListEntry entry=mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        String ping=entry==null ? "?" : Integer.toString(entry.getLatency());
        return ping+"ms | "+totalCorrections+" corrections";
    }

    @EventHandler private void onReceive(PacketEvent.Receive event) {
        if(event.packet instanceof PlayerPositionLookS2CPacket) pending.incrementAndGet();
        if(event.packet instanceof WorldTimeUpdateS2CPacket) {
            // Packet listener may not run on the client tick thread.
            lastTimeUpdateNanos.set(System.nanoTime());
        }
    }

    @EventHandler private void onTick(TickEvent.Post event) {
        if(mc.player==null || mc.world==null || mc.getNetworkHandler()==null){
            clear();lastServer="unknown";return;
        }
        String key=AnkVariantProfiles.serverKey(mc);
        if(!key.equals(lastServer)) {
            clear();lastServer=key;
        }
        worldTicks++;
        int count=pending.getAndSet(0);
        // Joining a world, respawning, and teleporting are not movement failures.
        if(worldTicks<100) return;
        if(warnCooldown>0) warnCooldown--;
        if(timeUpdateWarnings.get() && !mc.isInSingleplayer() && worldTicks>=160) {
            long now=System.nanoTime();
            long last=lastTimeUpdateNanos.get();
            long stall=stallSeconds.get()*1_000_000_000L;
            if(last>0 && now-last>stall
                && (lastStallAlertNanos==0 || now-lastStallAlertNanos>30_000_000_000L)) {
                lastStallAlertNanos=now;
                warning("No world-time updates for %s seconds; this may be network lag or a server plugin filtering packets.",stallSeconds.get());
            }
        }
        windowTicks++;
        totalCorrections=Math.min(999999,totalCorrections+Math.max(count,0));
        correctionsInWindow+=Math.max(count,0);
        if(windowTicks<120)return;
        if(correctionsInWindow>=threshold.get()){
            Flight flight=Modules.get().get(Flight.class);
            boolean flightWasActive=flight!=null && flight.isActive();
            if(warnings.get() && warnCooldown==0) {
                warning("Observed %s server position corrections within 6 seconds. This can be teleportation, lag or an anticheat response.",correctionsInWindow);
                warnCooldown=240;
            }
            if(stopFlight.get() && flightWasActive){
                flight.disable();
                warning("Flight was disabled after repeated server corrections; the server has not confirmed these movements.");
            }
        }
        correctionsInWindow=0;
        windowTicks=0;
    }
}
