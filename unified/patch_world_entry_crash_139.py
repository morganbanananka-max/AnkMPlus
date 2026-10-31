"""Crash resilience for restored modules during world entry.

Fix a concrete NULL PLAYER vulnerability in Flight: the mod previously
processed early PlayerAbilities packets and Flight activation before the
client player was available, throwing NullPointerException.

Also defer saved-module activation until the Minecraft world/player/network
interaction manager have all initialized. Isolate individual onActivate
exceptions so one broken saved module cannot take down the entire client.
This does not claim to address unidentified crashes from third-party mods.
"""
from pathlib import Path

ROOT=Path("upstream/src/main/java/meteordevelopment/meteorclient")
def once(rel,old,new):
    p=ROOT/rel
    s=p.read_text(encoding="utf-8")
    count=s.count(old)
    if count!=1:raise RuntimeError(f"{rel} expected exactly one anchor, found {count}: {old[:90]}")
    p.write_text(s.replace(old,new,1),encoding="utf-8")

flight="systems/modules/movement/Flight.java"
once(flight,
'''    public void onActivate() {
        resetVariantSession();
        if (selectedMode() == Mode.Abilities && !mc.player.isSpectator()) {''',
'''    public void onActivate() {
        resetVariantSession();
        // A saved active Flight can be restored before the world player exists.
        if (mc.player == null || mc.world == null) return;
        if (selectedMode() == Mode.Abilities && !mc.player.isSpectator()) {''')
once(flight,
'''    public void onDeactivate() {
        if (selectedMode() == Mode.Abilities && !mc.player.isSpectator()) {''',
'''    public void onDeactivate() {
        if (mc.player == null || mc.world == null) return;
        if (selectedMode() == Mode.Abilities && !mc.player.isSpectator()) {''')
once(flight,
'''    private void onPreTick(TickEvent.Pre event) {
        float currentYaw = mc.player.getYaw();''',
'''    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        float currentYaw = mc.player.getYaw();''')
once(flight,
'''    private void onPostTick(TickEvent.Post event) {
        if (mc.player != null && mc.world != null && mode.get() == Mode.Auto) {''',
'''    private void onPostTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (mode.get() == Mode.Auto) {''')
once(flight,
'''    private void onSendPacket(PacketEvent.Send event) {
        if (mode.get() == Mode.Auto && !mc.isInSingleplayer()) return;''',
'''    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;
        if (mode.get() == Mode.Auto && !mc.isInSingleplayer()) return;''')
once(flight,
'''    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket''',
'''    private void onReceivePacket(PacketEvent.Receive event) {
        // The server can send PlayerAbilitiesS2CPacket before the local
        // player is attached. Never cancel those initial packets.
        if (mc.player == null || mc.world == null) return;
        if (event.packet instanceof PlayerPositionLookS2CPacket''')
once(flight,
'''    private void abilitiesOff() {
        mc.player.getAbilities().flying = false;''',
'''    private void abilitiesOff() {
        if (mc.player == null || mc.world == null) return;
        mc.player.getAbilities().flying = false;''')
once(flight,
'''        if (!isActive() || selectedMode() != Mode.Velocity) return -1;''',
'''        if (mc.player == null || mc.world == null || !isActive()
            || selectedMode() != Mode.Velocity) return -1;''')

# One broken saved module should not brick an entire player's mod profile.
once("systems/modules/Module.java",
'''    public void disable() {
        if (isActive()) toggle();
    }''',
'''    public void disable() {
        if (isActive()) toggle();
    }

    /**
     * Crash recovery only: force a failing module inactive without running
     * its potentially crashing onDeactivate() again. The full exception is
     * logged by Modules. Other modules remain unaffected.
     */
    public final void deactivateAfterActivationFailure() {
        if (!active) return;
        try { MeteorClient.EVENT_BUS.unsubscribe(this); }
        catch (RuntimeException ex) {
            MeteorClient.LOG.warn("Could not unsubscribe failing module {}",name,ex);
        }
        active=false;
        Modules.get().removeActive(this);
    }''')

modules="systems/modules/Modules.java"
once(modules,
'''import meteordevelopment.meteorclient.events.game.GameLeftEvent;''',
'''import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;''')

once(modules,
'''    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        synchronized (active) {
            for (Module module : getAll()) {
                if (module.isActive() && !module.runInMainMenu) {
                    MeteorClient.EVENT_BUS.subscribe(module);
                    module.onActivate();
                }
            }
        }
    }''',
'''    // Joining can fire while Minecraft still constructs its ClientPlayer.
    // Delay saved modules until the first fully initialized world tick.
    private boolean pendingWorldActivation;

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        pendingWorldActivation=true;
    }

    @EventHandler
    private void activateSavedModulesAfterWorldReady(TickEvent.Post event) {
        if (!pendingWorldActivation) return;
        if (mc.player == null || mc.world == null || mc.interactionManager == null
            || mc.getNetworkHandler() == null) return;

        pendingWorldActivation=false;
        MeteorClient.LOG.info("[AnkHack] World-ready: activating saved modules safely");
        synchronized (active) {
            for (Module module : getAll()) {
                if (!module.isActive() || module.runInMainMenu) continue;
                try {
                    MeteorClient.EVENT_BUS.subscribe(module);
                    module.onActivate();
                } catch (RuntimeException | LinkageError ex) {
                    MeteorClient.LOG.error(
                        "[AnkHack] Saved module {} failed on world entry; disabling it to protect the game",
                        module.name,ex);
                    module.deactivateAfterActivationFailure();
                }
            }
        }
        MeteorClient.LOG.info("[AnkHack] World-ready activation complete");
    }''')
once(modules,
'''    private void onGameLeft(GameLeftEvent event) {
        synchronized (active) {
            for (Module module : getAll()) {
                if (module.isActive() && !module.runInMainMenu) {
                    MeteorClient.EVENT_BUS.unsubscribe(module);
                    module.onDeactivate();
                }
            }
        }
    }''',
'''    private void onGameLeft(GameLeftEvent event) {
        pendingWorldActivation=false;
        synchronized (active) {
            for (Module module : getAll()) {
                if (!module.isActive() || module.runInMainMenu) continue;
                try {
                    MeteorClient.EVENT_BUS.unsubscribe(module);
                    module.onDeactivate();
                } catch (RuntimeException | LinkageError ex) {
                    MeteorClient.LOG.error(
                        "[AnkHack] Module {} failed during world unload; disabling safely",
                        module.name,ex);
                    module.deactivateAfterActivationFailure();
                }
            }
        }
    }''')

print("AnkHack crash protection: Flight null-player packet/tick guards, deferred world-ready activation, failing module isolation.")
