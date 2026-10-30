package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adapted from ThunderHack Reborn 1.21.11 AntiCrash (GPL-3.0).
 * Only reject clearly implausible/nonfinite client-bound packet values,
 * not normal server teleports, explosions or Grim corrections.
 */
public class AnkAntiCrash extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Boolean> warnings = sg.add(new BoolSetting.Builder()
        .name("warnings").description("Show occasional notifications when malformed packets are blocked.")
        .defaultValue(true).build());
    private final AtomicInteger blocked = new AtomicInteger();
    private int ticksSinceLastWarning = 200;

    public AnkAntiCrash() {
        super(Categories.Misc, "ank-anti-crash",
            "Reject obviously invalid explosion, particle and teleport coordinates (ThunderHack adaptation).");
    }
    @Override public void onActivate() { blocked.set(0);ticksSinceLastWarning=200; }
    @Override public String getInfoString() { return "Blocked: " + blocked.get(); }

    private static boolean invalid(double x) {
        return !Double.isFinite(x) || Math.abs(x)>100_000_000;
    }
    private static boolean invalid(Vec3d p) {
        return p==null || invalid(p.x) || invalid(p.y) || invalid(p.z);
    }

    @EventHandler private void onReceive(PacketEvent.Receive event) {
        boolean reject = false;
        if (event.packet instanceof ExplosionS2CPacket packet) {
            reject = invalid(packet.center()) || invalid(packet.radius());
        } else if (event.packet instanceof ParticleS2CPacket packet) {
            reject = invalid(packet.getX()) || invalid(packet.getY()) || invalid(packet.getZ())
                || invalid(packet.getSpeed()) || invalid(packet.getOffsetX())
                || invalid(packet.getOffsetY()) || invalid(packet.getOffsetZ());
        } else if (event.packet instanceof PlayerPositionLookS2CPacket packet) {
            var change = packet.change();
            reject = invalid(change.position()) || invalid(change.yaw()) || invalid(change.pitch());
        }
        if (reject) {
            event.cancel();
            blocked.incrementAndGet();
        }
    }

    @EventHandler private void onTick(TickEvent.Post event) {
        if (mc.player==null || mc.world==null) return;
        if(ticksSinceLastWarning<200)ticksSinceLastWarning++;
        if(blocked.get()>0 && warnings.get() && ticksSinceLastWarning>=200) {
            warning("Rejected malformed server packet values. Total blocked: %s",blocked.get());
            ticksSinceLastWarning=0;
        }
    }
}
