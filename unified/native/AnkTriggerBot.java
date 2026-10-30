/*
 * AnkHack independent implementation. GPL-3.0.
 * Aimed, reach-limited, cooldown-respecting TriggerBot.
 */
package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

public final class AnkTriggerBot extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Boolean> playersOnly = sg.add(new BoolSetting.Builder()
        .name("players-only").description("Do not attack mobs.")
        .defaultValue(true).build());
    private final Setting<Boolean> ignoreFriends = sg.add(new BoolSetting.Builder()
        .name("ignore-friends").description("Never attack friends.")
        .defaultValue(true).build());
    private final Setting<Boolean> requireClick = sg.add(new BoolSetting.Builder()
        .name("require-click").description("Require holding attack as well as aiming.")
        .defaultValue(false).build());
    private final Setting<Double> cooldown = sg.add(new DoubleSetting.Builder()
        .name("attack-cooldown").description("Minimum actual weapon cooldown.")
        .defaultValue(0.95).min(0.5).max(1.0).sliderRange(0.5,1.0).build());
    private final Setting<Integer> minTicks = sg.add(new IntSetting.Builder()
        .name("min-interval-ticks").description("Minimum ticks between attacks.")
        .defaultValue(10).min(1).sliderMax(40).build());

    private int sinceAttack = 0;
    public AnkTriggerBot() {
        super(Categories.Combat, "ank-trigger-bot",
            "Attacks only your actual crosshair target within normal reach and weapon cooldown.");
    }
    @Override public void onActivate() { sinceAttack = 0; }
    @Override public void onDeactivate() { sinceAttack = 0; }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (sinceAttack < 1000) sinceAttack++;
        if (mc.player == null || mc.world == null || mc.interactionManager == null ||
            mc.currentScreen != null || mc.player.isSpectator() || mc.player.isUsingItem() ||
            sinceAttack < minTicks.get() ||
            (requireClick.get() && !mc.options.attackKey.isPressed()) ||
            mc.player.getAttackCooldownProgress(0.0f) < cooldown.get() ||
            !(mc.crosshairTarget instanceof EntityHitResult hit) ||
            !(hit.getEntity() instanceof LivingEntity target) ||
            !target.isAlive() || target.isSpectator() ||
            mc.player.squaredDistanceTo(target) > 9.0d) return;

        if (playersOnly.get() && !(target instanceof PlayerEntity)) return;
        if (target instanceof PlayerEntity player) {
            if (player == mc.player || player.isSpectator() ||
                player.getAbilities().creativeMode ||
                (ignoreFriends.get() && Friends.get().isFriend(player))) return;
        }
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        sinceAttack = 0;
    }
}
