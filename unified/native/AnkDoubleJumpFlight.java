/*
 * AnkHack native Creative-style double-tap flight control.
 * This is one input handler for the existing Flight module; no extra flight engine.
 */
package meteordevelopment.meteorclient;

import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.world.ClientWorld;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class AnkDoubleJumpFlight {
    private long lastTapAt;
    private ClientWorld lastWorld;

    @EventHandler
    private void onKey(KeyEvent event) {
        if (mc == null || mc.player == null || mc.world == null || mc.currentScreen != null
            || mc.getOverlay() != null || event.action != KeyAction.Press) {
            lastTapAt = 0;
            return;
        }

        // Honor remapped Jump key rather than assuming Space.
        if (event.key() != Input.getKey(mc.options.jumpKey)) return;

        // Creative and spectator already implement double-tap flight in vanilla.
        if (mc.player.getAbilities().creativeMode || mc.player.isSpectator()
            || mc.player.hasVehicle() || mc.player.isGliding()
            || mc.player.isTouchingWater() || mc.player.isSleeping()) {
            lastTapAt = 0;
            return;
        }

        if (mc.world != lastWorld) {
            lastWorld = mc.world;
            lastTapAt = 0;
        }

        Flight flight = Modules.get().get(Flight.class);
        if (flight == null || !flight.ankhackDoubleJumpEnabled()) {
            lastTapAt = 0;
            return;
        }

        long now = System.nanoTime();
        long elapsedMs = lastTapAt == 0 ? Long.MAX_VALUE : (now - lastTapAt) / 1_000_000L;
        lastTapAt = now;

        // Matches vanilla's two separate jump presses. No auto-trigger on held Jump.
        if (elapsedMs >= 40 && elapsedMs <= flight.ankhackDoubleJumpWindowMs()) {
            lastTapAt = 0;
            flight.toggle();
            flight.sendToggledMsg();
        }
    }
}
