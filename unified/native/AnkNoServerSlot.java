/*
 * Native port of ThunderHack Reborn NoServerSlot, GPL-3.0.
 * Server-selected hotbar corrections can break other mods. Opt-in only, with
 * a maximum number of rejected corrections before this module disables.
 */
package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;

public final class AnkNoServerSlot extends Module {
    private final SettingGroup sg=settings.getDefaultGroup();
    private final Setting<Integer> limit=sg.add(new IntSetting.Builder()
        .name("max-corrections-per-second")
        .description("Stop this hack instead of repeatedly fighting a server.")
        .defaultValue(3).range(1,12).sliderRange(1,12).build());
    private int corrections, tick;
    public AnkNoServerSlot() {
        super(Categories.Player, "ank-no-server-slot",
            "ThunderHack-derived protection against forced server hotbar selections.");
    }
    @Override public void onActivate() { corrections=0;tick=0; }
    @EventHandler
    private void tick(TickEvent.Pre event) {
        if (++tick>=20){corrections=0;tick=0;}
    }
    @EventHandler
    private void receive(PacketEvent.Receive event) {
        if(!(event.packet instanceof UpdateSelectedSlotS2CPacket) || mc.player==null
          || mc.getNetworkHandler()==null) return;
        if(++corrections > limit.get()) {
            warning("NoServerSlot: repeated corrections detected; disabling to avoid a packet loop.");
            disable();return;
        }
        event.cancel();
        mc.getNetworkHandler().sendPacket(
            new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot()));
    }
}
