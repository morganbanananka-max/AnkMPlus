package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Source-derived adaptation of ThunderHack Reborn AutoTpAccept (GPL-3.0).
 * Never accept a request until the user explicitly configures one name.
 * Server-side game messages are not authenticated; users should only enable
 * this on servers they trust. Strict matching and a cooldown limit exposure.
 */
public class AnkAutoTpAccept extends Module {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<String> trustedPlayer = sg.add(new StringSetting.Builder()
        .name("trusted-player").description("Exact username to accept teleport requests from. Blank disables automatic acceptance.")
        .defaultValue("").build());
    private final Setting<String> command = sg.add(new StringSetting.Builder()
        .name("accept-command").description("Server command, without slash, to accept a request.")
        .defaultValue("tpaccept").build());
    private final Setting<Integer> cooldown = sg.add(new IntSetting.Builder()
        .name("cooldown-seconds").description("Prevents repeated requests or chat-command spam.")
        .defaultValue(30).range(10,300).sliderRange(10,120).build());

    private volatile boolean pending;
    private int ticksSinceLast;

    public AnkAutoTpAccept() {
        super(Categories.Misc, "ank-auto-tp-accept",
            "Accept teleport requests only from your explicitly trusted player (ThunderHack adaptation).");
    }

    @Override public void onActivate() {
        pending = false;
        ticksSinceLast = 6000;
    }
    @Override public void onDeactivate() { pending = false; }

    @EventHandler private void onPacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof GameMessageS2CPacket packet)) return;
        String name = trustedPlayer.get().trim();
        if (!USERNAME.matcher(name).matches()) return;
        String msg = packet.content().getString().toLowerCase(Locale.ROOT);
        String lower = name.toLowerCase(Locale.ROOT);
        // Match whole user tokens: do not accept messages about similarly named players.
        if (!Pattern.compile("(?<![a-z0-9_])" + Pattern.quote(lower) + "(?![a-z0-9_])").matcher(msg).find()) return;
        boolean explicitRequest = (msg.contains("teleport") && (msg.contains("request") || msg.contains("förfrågan")))
            || (msg.contains("tpa") && msg.contains("request"))
            || (msg.contains("/tpaccept") && msg.contains("request"));
        if (explicitRequest) pending = true;
    }

    @EventHandler private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null || mc.getNetworkHandler() == null) {
            pending = false; return;
        }
        if (ticksSinceLast < 100000) ticksSinceLast++;
        if (!pending || ticksSinceLast < cooldown.get() * 20) return;
        pending = false;
        String name = trustedPlayer.get().trim();
        String cmd = command.get().trim();
        if (!USERNAME.matcher(name).matches()) return;
        // Only a simple command identifier is allowed; no spaces, arguments or slashes.
        if (!cmd.matches("[A-Za-z][A-Za-z0-9_-]{1,31}")) return;
        mc.getNetworkHandler().sendChatCommand(cmd);
        ticksSinceLast = 0;
    }
}
