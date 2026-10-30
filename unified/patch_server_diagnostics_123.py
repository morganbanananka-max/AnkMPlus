#!/usr/bin/env python3
"""Add Grim/other-anticheat observation and safe Flight defaults.

Nothing in here guarantees bypasses. Diagnostic packet observations are not
proof of Grim detections: server teleports and lag cause the same packet.
"""
from pathlib import Path
from shutil import copy2

base = Path("upstream/src/main/java/meteordevelopment/meteorclient")
modules = base / "systems/modules"
source = Path("unified/native/AnkServerDiagnostics.java")
target = modules / "misc/AnkServerDiagnostics.java"
assert source.is_file() and not target.exists()
copy2(source, target)

registry = modules / "Modules.java"
text = registry.read_text()
anchor = "        add(new AnkAutoFlyme());"
assert text.count(anchor) == 1, f"module registration anchor: {text.count(anchor)}"
registry.write_text(text.replace(anchor, anchor + "\n        add(new AnkServerDiagnostics());", 1))

flight = modules / "movement/Flight.java"
text = flight.read_text()
old = """        flightTicks = 0;
        correctionsSinceSwitch = 0;"""
new = """        // The Abilities implementation gives flying only on the client.
        // On multiplayer, do not label it a server-accepted mode without
        // permission from the server. Do not claim the alternative bypasses
        // Grim; it may be corrected as well and Auto will eventually stop.
        if (mode.get() == Mode.Auto && mc.player != null
            && !mc.isInSingleplayer()
            && !mc.player.getAbilities().allowFlying
            && !mc.player.isSpectator()
            && activeVariant == Mode.Abilities) {
            activeVariant = Mode.Velocity;
        }
        flightTicks = 0;
        correctionsSinceSwitch = 0;"""
assert text.count(old)==1, f"Flight selection anchor: {text.count(old)}"
text = text.replace(old, new, 1)
old = ".defaultValue(AntiKickMode.Packet)"
assert text.count(old)==1, f"AntiKick default anchor: {text.count(old)}"
text = text.replace(old, ".defaultValue(AntiKickMode.None)", 1)
# If Flight stays enabled while the player changes worlds or servers,
# discard the prior session's active variant and select for the new context.
# This is especially important after switching from Creative singleplayer to
# a multiplayer server with restrictive movement checks.
old_world = """    private void onPostTick(TickEvent.Post event) {
        if (mc.player != null && mc.world != null) flightFeedback();"""
new_world = """    private void onPostTick(TickEvent.Post event) {
        if (mc.player != null && mc.world != null && mode.get() == Mode.Auto) {
            String connectedWorld = AnkVariantProfiles.serverKey(mc);
            if (!connectedWorld.equals(activeServer)) {
                if (activeVariant == Mode.Abilities && !mc.player.isSpectator()) abilitiesOff();
                resetVariantSession();
            }
        }
        if (mc.player != null && mc.world != null) flightFeedback();"""
assert text.count(old_world) == 1, "Cannot patch Flight Auto world-switch hook"
text = text.replace(old_world, new_world, 1)
flight.write_text(text)

notices = Path("upstream/ANKHACK_LICENSE_AND_ATTRIBUTIONS.md")
with notices.open("a", encoding="utf-8") as file:
    file.write("""
- AnkServerDiagnostics: original AnkHack module observing server
  position corrections and optionally disabling repeatedly rejected Flight;
  these corrections are NOT proof of a Grim check violation.
- Flight Auto: on unprivileged multiplayer, avoids starting the client-only
  Abilities variant; removes packet-rewriting antikick default. An anticheat
  may reject all remaining flight implementations. No bypass is claimed.
""")
print("Installed server correction diagnostics and conservative Flight defaults.")
