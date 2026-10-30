package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

/**
 * Minimal ThunderHack Reborn NoServerRotate adaptation (GPL-3.0).
 * Lets vanilla handle the position correction then restores the local camera
 * orientation on the next game tick. No teleport packets are cancelled.
 */
public class AnkNoServerRotate extends Module {
    private volatile boolean pending;
    private volatile float yaw, pitch;
    public AnkNoServerRotate() {
        super(Categories.Player,"ank-no-server-rotate","Keeps the local camera angle during server position corrections (ThunderHack adaptation).");
    }
    @Override public void onActivate() { pending=false; }
    @EventHandler private void onPacket(PacketEvent.Receive event) {
        if (mc.player == null || !(event.packet instanceof PlayerPositionLookS2CPacket)) return;
        yaw=mc.player.getYaw();pitch=mc.player.getPitch();pending=true;
    }
    @EventHandler private void onTick(TickEvent.Post event) {
        if (!pending || mc.player==null) return;
        pending=false;
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }
}
