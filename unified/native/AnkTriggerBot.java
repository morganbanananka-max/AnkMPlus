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

/**
 * Source-derived, independently adapted ThunderHack Reborn TriggerBot.
 * Uses vanilla client crosshair hit-testing and normal attack cooldown.
 * No aim snapping, reach extension, spoofed ground packets, or fake hits.
 */
public class AnkTriggerBot extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Boolean> playersOnly = sg.add(new BoolSetting.Builder()
        .name("players-only").description("Only attack players, not neutral or friendly mobs.")
        .defaultValue(true).build());
    private final Setting<Boolean> requireAttack = sg.add(new BoolSetting.Builder()
        .name("require-attack-held").description("Only click while the attack key is pressed.")
        .defaultValue(false).build());
    private final Setting<Boolean> pauseUsing = sg.add(new BoolSetting.Builder()
        .name("pause-while-using-item").description("Avoid attacks while eating, shielding or drawing a bow.")
        .defaultValue(true).build());
    private final Setting<Double> minCooldown = sg.add(new DoubleSetting.Builder()
        .name("min-attack-charge").description("Minimum vanilla attack cooldown to trigger a hit.")
        .defaultValue(0.94).range(0.5,1).sliderRange(0.5,1).build());
    private final Setting<Double> range = sg.add(new DoubleSetting.Builder()
        .name("range").description("Never attack beyond this distance or outside the normal crosshair hit result.")
        .defaultValue(3.0).range(1,3).sliderRange(1,3).build());
    private final Setting<Integer> delay = sg.add(new IntSetting.Builder()
        .name("minimum-delay-ticks").description("Minimum delay between automatic attacks.")
        .defaultValue(9).range(1,30).sliderRange(1,20).build());

    private int ticks;

    public AnkTriggerBot() {
        super(Categories.Combat, "ank-trigger-bot",
            "Crosshair-triggered attacks using normal vanilla reach and cooldown (ThunderHack adaptation).");
    }

    @Override public void onActivate() { ticks=0; }
    @Override public void onDeactivate() { ticks=0; }

    @EventHandler private void onTick(TickEvent.Post event) {
        if(mc.player==null || mc.world==null || mc.interactionManager==null)return;
        if(mc.currentScreen!=null || mc.player.isSpectator() || !mc.player.isAlive())return;
        if(pauseUsing.get() && mc.player.isUsingItem())return;
        if(requireAttack.get() && !mc.options.attackKey.isPressed())return;
        if(ticks>0){ ticks--; return; }
        if(mc.player.getAttackCooldownProgress(0.0f)<minCooldown.get())return;
        if(!(mc.crosshairTarget instanceof EntityHitResult result))return;
        if(!(result.getEntity() instanceof LivingEntity target) || !target.isAlive())return;
        if(playersOnly.get() && !(target instanceof PlayerEntity))return;
        if(target instanceof PlayerEntity player && Friends.get().isFriend(player))return;
        if(mc.player.squaredDistanceTo(target)>range.get()*range.get())return;
        if(!mc.player.canSee(target))return;
        mc.interactionManager.attackEntity(mc.player,target);
        mc.player.swingHand(Hand.MAIN_HAND);
        ticks=delay.get();
    }
}
