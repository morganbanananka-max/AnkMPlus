package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

/**
 * ThunderHack Reborn 1.21.11 AutoFlyme-inspired server command helper (GPL-3.0).
 * Requests an existing, server-defined flight permission. This does NOT bypass
 * server flight rules. Unlike the upstream spam/retry modes it never changes
 * client tick speed and has a strict per-activation attempt budget.
 */
public class AnkAutoFlyme extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<String> command = sg.add(new StringSetting.Builder()
        .name("flight-command").description("Existing server flight command, without slash; default is flyme.")
        .defaultValue("flyme").build());
    private final Setting<Boolean> requireJump = sg.add(new BoolSetting.Builder()
        .name("require-jump-held").description("Only request flight while you hold the jump key.")
        .defaultValue(true).build());
    private final Setting<Integer> retrySeconds = sg.add(new IntSetting.Builder()
        .name("retry-seconds").description("Minimum seconds between permission requests.")
        .defaultValue(30).range(10,300).sliderRange(10,90).build());
    private final Setting<Integer> maxAttempts = sg.add(new IntSetting.Builder()
        .name("max-attempts").description("Maximum command requests per manual activation.")
        .defaultValue(2).range(1,5).sliderRange(1,5).build());
    private int ticks;
    private int attempts;

    public AnkAutoFlyme() {
        super(Categories.Misc, "ank-auto-flyme",
            "Request a server's existing /flyme permission with bounded retries (ThunderHack adaptation).");
    }
    @Override public void onActivate() {
        ticks = 6000;
        attempts = 0;
    }
    @Override public void onDeactivate() {
        ticks = 0;
        attempts = 0;
    }
    @Override public String getInfoString() { return attempts + "/" + maxAttempts.get(); }

    @EventHandler private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;
        if (ticks < 100000) ticks++;
        if (attempts >= maxAttempts.get()) return;
        if (mc.player.getAbilities().allowFlying || mc.player.getAbilities().flying) return;
        if (mc.currentScreen != null || mc.player.isSpectator()) return;
        if (requireJump.get() && !mc.options.jumpKey.isPressed()) return;
        if (ticks < retrySeconds.get() * 20) return;
        String cmd = command.get().trim();
        if (!cmd.matches("[A-Za-z][A-Za-z0-9_-]{1,31}")) return;
        mc.getNetworkHandler().sendChatCommand(cmd);
        ticks = 0;
        attempts++;
    }
}
