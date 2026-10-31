/*
 * AnkHack native vehicle speed, independently adapted from the GPL-3.0
 * ThunderHack Reborn EntitySpeed behavior.
 *
 * Bounded motion on the controlled vehicle, no client packet spoofing.
 * Does not claim to defeat any server's movement checks.
 */
package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.AnkVariantProfiles;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;
import java.util.concurrent.atomic.AtomicInteger;

public final class AnkVehicleSpeed extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Double> speed = sg.add(new DoubleSetting.Builder()
        .name("horizontal-speed")
        .description("Desired speed of the controlled vehicle, blocks per tick.")
        .defaultValue(0.30).range(0.10, 0.80).sliderRange(0.10,0.80).build());
    private final Setting<Boolean> protectChunks = sg.add(new BoolSetting.Builder()
        .name("avoid-unloaded-chunks")
        .description("Do not accelerate into chunks not yet loaded.")
        .defaultValue(true).build());
    private final Setting<Boolean> pauseOnCorrections = sg.add(new BoolSetting.Builder()
        .name("pause-after-server-corrections")
        .description("Pause acceleration temporarily after repeated server position corrections.")
        .defaultValue(true).build());

    private final AtomicInteger pendingCorrections = new AtomicInteger();
    private String previousServer = "unknown";
    private int worldTicks;
    private int correctionWindowTicks;
    private int correctionsInWindow;
    private int pauseTicks;

    public AnkVehicleSpeed() {
        super(Categories.Movement, "ank-vehicle-speed",
            "Capped speed for controlled rides; stops at unloaded chunks and pauses if rejected.");
    }

    @Override public void onActivate() {
        reset();
        previousServer = AnkVariantProfiles.serverKey(mc);
    }
    @Override public void onDeactivate() { reset(); }

    private void reset() {
        pendingCorrections.set(0);
        worldTicks=0;
        correctionWindowTicks=0;
        correctionsInWindow=0;
        pauseTicks=0;
    }

    @EventHandler
    private void onCorrection(PacketEvent.Receive e) {
        if (e.packet instanceof PlayerPositionLookS2CPacket) pendingCorrections.incrementAndGet();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player==null || mc.world==null) {
            reset(); previousServer="unknown"; return;
        }
        String currentServer=AnkVariantProfiles.serverKey(mc);
        if (!currentServer.equals(previousServer)) {
            reset(); previousServer=currentServer;
        }
        worldTicks++;
        int newCorrections=pendingCorrections.getAndSet(0);
        if (worldTicks>100) {
            correctionsInWindow+=Math.min(100,newCorrections);
            if (++correctionWindowTicks>=200) {
                if (pauseOnCorrections.get() && correctionsInWindow>=3 && pauseTicks==0) {
                    pauseTicks=200;
                    warning("Vehicle acceleration paused for 10 seconds after repeated server corrections.");
                }
                correctionWindowTicks=0;
                correctionsInWindow=0;
            }
        }
        if (pauseTicks>0) { pauseTicks--; return; }
        if (mc.currentScreen!=null || mc.player.isSpectator()
            || Modules.get().isActive(Flight.class)
            || Modules.get().isActive(BoatFly.class)) return;

        Entity vehicle=mc.player.getControllingVehicle();
        if (vehicle==null || mc.player.isSneaking() || vehicle.isRemoved()) return;
        if (vehicle.getY() < mc.world.getBottomY()+2) return;

        float forward=mc.player.forwardSpeed;
        float sideways=mc.player.sidewaysSpeed;
        double magnitude=Math.hypot(forward,sideways);
        if (magnitude<0.05) return;
        double yaw=Math.toRadians(mc.player.getYaw());
        double vx=(-Math.sin(yaw)*forward-Math.cos(yaw)*sideways)/magnitude;
        double vz=(Math.cos(yaw)*forward-Math.sin(yaw)*sideways)/magnitude;
        double desiredSpeed=speed.get();
        double nextX=vehicle.getX()+vx*desiredSpeed;
        double nextZ=vehicle.getZ()+vz*desiredSpeed;

        if (protectChunks.get()) {
            int cx=((int)Math.floor(nextX))>>4;
            int cz=((int)Math.floor(nextZ))>>4;
            if (!mc.world.getChunkManager().isChunkLoaded(cx,cz)) return;
        }
        Vec3d motion=vehicle.getVelocity();
        // Do not replace vertical motion or cancel normal vehicle travel.
        vehicle.setVelocity(vx*desiredSpeed,motion.y,vz*desiredSpeed);
    }

    @Override public String getInfoString() {
        return pauseTicks>0 ? "Paused" : String.format(java.util.Locale.ROOT,"%.2f",speed.get());
    }
}
