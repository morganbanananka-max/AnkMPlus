/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 * GPL-3.0; adapted to AnkHack's existing Meteor event and settings framework.
 * Source: Wurst 7 AutoSwimHack.
 */
package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public final class AnkAutoSwim extends Module {
    public AnkAutoSwim() {
        super(Categories.Movement, "auto-swim", "Wurst-derived automatic water sprinting while swimming forward.");
    }
    @EventHandler
    private void tick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.player.horizontalCollision
            || mc.player.isSneaking() || mc.player.getAbilities().flying
            || !mc.player.isTouchingWater() || mc.player.forwardSpeed <= 0.0f)
            return;
        if (mc.player.isSwimming() || mc.player.isSubmergedInWater())
            mc.player.setSprinting(true);
    }
}
