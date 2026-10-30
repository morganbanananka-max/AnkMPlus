#!/usr/bin/env python3
"""Patch the actual unified Flight module to expose multiple native source variants.

Requires unified/patch_unified.py to run first. This is not a jar-in-jar bundle.
"""
from pathlib import Path
root=Path("upstream/src/main/java/meteordevelopment/meteorclient")
flight=root/"systems/modules/movement/Flight.java"
s=flight.read_text()
def replace(a,b,expected=1):
    global s
    count=s.count(a)
    if count!=expected: raise RuntimeError(f"Flight anchor count {count}, expected {expected}: {a[:100]!r}")
    s=s.replace(a,b)
replace("import meteordevelopment.meteorclient.events.packets.PacketEvent;",
"""import meteordevelopment.meteorclient.AnkVariantProfiles;
import meteordevelopment.meteorclient.events.packets.PacketEvent;""")
replace("import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;",
"""import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;""")
replace("    private int delayLeft = delay.get();",
"""    // One Flight module, multiple implementations selected in its settings.
    // Auto is evidence-driven: a correction is negative evidence, a stable
    // flight interval is limited positive evidence. Default is not a benchmark.
    private Mode activeVariant = Mode.Abilities;
    private String activeServer = "unknown";
    private int flightTicks;
    private int correctionsSinceSwitch;
    private int variantSwitches;
    private int lastVariantSwitchTick;
    private int firstCorrectionTick;

    private final Setting<Double> wurstHorizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("wurst-horizontal-speed")
        .description("Blocks per tick, Wurst-style motion variant.")
        .defaultValue(0.35).min(0.05).sliderMax(2)
        .visible(() -> mode.get() == Mode.WurstMotion || mode.get() == Mode.Auto)
        .build());

    private final Setting<Double> wurstVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("wurst-vertical-speed")
        .description("Vertical speed for Wurst-style flight.")
        .defaultValue(0.3).min(0.05).sliderMax(2)
        .visible(() -> mode.get() == Mode.WurstMotion || mode.get() == Mode.Auto)
        .build());

    private Mode selectedMode() { return mode.get() == Mode.Auto ? activeVariant : mode.get(); }

    private void resetVariantSession() {
        activeServer = AnkVariantProfiles.serverKey(mc);
        activeVariant = Mode.Abilities;
        if (mode.get() == Mode.Auto) {
            try { activeVariant = Mode.valueOf(AnkVariantProfiles.choose(activeServer, "flight")); }
            catch (IllegalArgumentException ignored) {}
        }
        flightTicks = 0;
        correctionsSinceSwitch = 0;
        variantSwitches = 0;
        lastVariantSwitchTick = 0;
        firstCorrectionTick = 0;
        AnkVariantProfiles.resetSession(activeServer, "flight");
    }

    private void flightFeedback() {
        if (mode.get() != Mode.Auto || !mc.player.isAlive()) return;
        flightTicks++;
        // Observe only continued flight, not simply standing still.
        if (flightTicks > 80 && flightTicks % 200 == 0 && !mc.player.isOnGround()) {
            AnkVariantProfiles.stable(activeServer, "flight", activeVariant.name());
        }
        if (correctionsSinceSwitch < 2 || flightTicks - lastVariantSwitchTick < 70) return;
        if (variantSwitches >= 2) {
            this.warning("Auto Flight: repeated server corrections. Disabling Flight to prevent a loop.");
            this.disable();
            return;
        }
        Mode next;
        try { next = Mode.valueOf(AnkVariantProfiles.nextUntested(activeServer,"flight",activeVariant.name())); }
        catch (IllegalArgumentException ex) { next = Mode.Abilities; }
        if (next == activeVariant) {
            this.warning("Auto Flight: no better untested implementation; disabling Flight.");
            this.disable();
            return;
        }
        if (activeVariant == Mode.Abilities && !mc.player.isSpectator()) abilitiesOff();
        activeVariant = next;
        correctionsSinceSwitch = 0;
        lastVariantSwitchTick = flightTicks;
        variantSwitches++;
        this.info("Flight corrected by server. Testing %s (not guaranteed to work).", next.name());
    }

    private int delayLeft = delay.get();""")
replace("""    public Flight() {
        super(Categories.Movement, "flight", "FLYYYY! No Fall is recommended with this module.");
    }""",
"""    public Flight() {
        super(Categories.Movement, "flight", "One Flight module with Meteor and Wurst movement variants and per-server learning.");
    }

    @Override
    public String getInfoString() {
        if (mode.get() == Mode.Auto) return "Auto: " + activeVariant.name() + " / score " +
            AnkVariantProfiles.score(activeServer,"flight",activeVariant.name());
        return selectedMode().name();
    }""")
replace("        if (mode.get() == Mode.Abilities && !mc.player.isSpectator()) {",
"""        resetVariantSession();
        if (selectedMode() == Mode.Abilities && !mc.player.isSpectator()) {""",1)
# The second occurrence is onDeactivate, and we must NOT reset the session there.
replace("        if (mode.get() == Mode.Abilities && !mc.player.isSpectator()) {",
"""        if (selectedMode() == Mode.Abilities && !mc.player.isSpectator()) {""",1)
replace("    private void onPostTick(TickEvent.Post event) {",
"""    private void onPostTick(TickEvent.Post event) {
        if (mc.player != null && mc.world != null) flightFeedback();""")
replace("        switch (mode.get()) {",
"""        switch (selectedMode()) {""")
replace("""            case Abilities -> {
                if (mc.player.isSpectator()) return;""",
"""            case WurstMotion -> {
                // Integrated adaptation of Wurst FlightHack's motion control.
                // Wurst-Imperium GPL-3.0, adapted to a single AnkHack Flight module.
                mc.player.getAbilities().flying = false;
                double forward = (mc.options.forwardKey.isPressed() ? 1 : 0)
                    - (mc.options.backKey.isPressed() ? 1 : 0);
                double strafe = (mc.options.leftKey.isPressed() ? 1 : 0)
                    - (mc.options.rightKey.isPressed() ? 1 : 0);
                double len = Math.hypot(forward, strafe);
                if (len > 0) { forward /= len; strafe /= len; }
                double rad = Math.toRadians(mc.player.getYaw());
                double vx = (-Math.sin(rad) * forward - Math.cos(rad) * strafe) * wurstHorizontal.get();
                double vz = ( Math.cos(rad) * forward - Math.sin(rad) * strafe) * wurstHorizontal.get();
                double vy = 0;
                if (mc.options.jumpKey.isPressed()) vy += wurstVertical.get();
                if (mc.options.sneakKey.isPressed()) vy -= wurstVertical.get();
                mc.player.setVelocity(vx, vy, vz);
            }
            case Auto -> { /* resolved by selectedMode() */ }
            case Abilities -> {
                if (mc.player.isSpectator()) return;""")
replace("        if (!(event.packet instanceof PlayerAbilitiesS2CPacket packet) || mode.get() != Mode.Abilities) return;",
"""        if (event.packet instanceof PlayerPositionLookS2CPacket
            && mode.get() == Mode.Auto && flightTicks > 30) {
            if (firstCorrectionTick == 0 || flightTicks - firstCorrectionTick > 140) {
                firstCorrectionTick = flightTicks;
                correctionsSinceSwitch = 0;
            }
            correctionsSinceSwitch++;
            AnkVariantProfiles.correction(activeServer, "flight", activeVariant.name());
        }
        if (!(event.packet instanceof PlayerAbilitiesS2CPacket packet) || selectedMode() != Mode.Abilities) return;""")
replace("mode.get() == Mode.Abilities", "selectedMode() == Mode.Abilities",1)
# Fix the original remaining appearances while keeping mode settings predicates intact.
s=s.replace("if (mode.get() == Mode.Abilities)", "if (selectedMode() == Mode.Abilities)")
s=s.replace("if (mode.get() != Mode.Velocity)", "if (selectedMode() != Mode.Velocity)")
s=s.replace("mode.get() != Mode.Velocity", "selectedMode() != Mode.Velocity")
replace("""    public enum Mode {
        Abilities,
        Velocity
    }""",
"""    public enum Mode {
        Auto,
        Abilities,
        Velocity,
        WurstMotion
    }""")
# Default to Auto, but don't replace user-provided persisted settings (settings system handles that).
replace("""        .defaultValue(Mode.Abilities)""","""        .defaultValue(Mode.Auto)""")
flight.write_text(s)

profiles=root/"AnkVariantProfiles.java"
profiles.write_text(Path("unified/native/AnkVariantProfiles.java").read_text())
assert "getInfoString()" in s and "WurstMotion" in s and "case Auto" in s
print("One Flight module now supports Auto/Meteor Abilities/Meteor Velocity/WurstMotion variants.")
