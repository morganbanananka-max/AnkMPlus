package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;

/**
 * GPL-3.0 adaptation of ThunderHack Reborn 1.21.11 AutoBuff's potion selection.
 * Uses vanilla interactItem while temporarily looking down.
 * No spoofed movement, spam packets or aura dependence.
 */
public class AnkAutoBuff extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Boolean> strength = sg.add(new BoolSetting.Builder()
        .name("strength").description("Use a strength splash potion if the effect is missing.")
        .defaultValue(true).build());
    private final Setting<Boolean> speed = sg.add(new BoolSetting.Builder()
        .name("speed").description("Use a speed splash potion if the effect is missing.")
        .defaultValue(true).build());
    private final Setting<Boolean> fire = sg.add(new BoolSetting.Builder()
        .name("fire-resistance").description("Use fire-resistance splash potion if the effect is missing.")
        .defaultValue(true).build());
    private final Setting<Boolean> heal = sg.add(new BoolSetting.Builder()
        .name("instant-healing").description("Use a healing splash potion when below the health threshold.")
        .defaultValue(true).build());
    private final Setting<Integer> healHealth = sg.add(new IntSetting.Builder()
        .name("healing-health").description("Trigger instant health potion below this health (half-hearts x2).")
        .defaultValue(8).range(1, 20).sliderRange(1, 20).visible(heal::get).build());
    private final Setting<Integer> cooldown = sg.add(new IntSetting.Builder()
        .name("cooldown-ticks").description("Minimum delay between throwing potions.")
        .defaultValue(45).range(20, 200).sliderRange(20, 100).build());
    private int delay;

    public AnkAutoBuff() {
        super(Categories.Combat, "ank-auto-buff", "Splash yourself with needed buff/healing potions from the hotbar (ThunderHack adaptation).");
    }

    @Override public void onActivate() { delay = 0; }

    private boolean hasEffect(ItemStack stack, RegistryEntry<StatusEffect> effect) {
        if (!(stack.getItem() instanceof SplashPotionItem)) return false;
        PotionContentsComponent potion=stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
        for (StatusEffectInstance active:potion.getEffects()) {
            if (active.getEffectType().equals(effect)) return true;
        }
        return false;
    }

    private int find(RegistryEntry<StatusEffect> effect) {
        for (int i=0;i<9;i++) {
            if (hasEffect(mc.player.getInventory().getStack(i),effect)) return i;
        }
        return -1;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (delay > 0) { delay--; return; }
        if (mc.currentScreen != null || !mc.player.isOnGround() || mc.player.isUsingItem()) return;
        int slot=-1;
        if (heal.get() && mc.player.getHealth() + mc.player.getAbsorptionAmount() < healHealth.get())
            slot=find(StatusEffects.INSTANT_HEALTH);
        if (slot<0 && strength.get() && !mc.player.hasStatusEffect(StatusEffects.STRENGTH))
            slot=find(StatusEffects.STRENGTH);
        if (slot<0 && speed.get() && !mc.player.hasStatusEffect(StatusEffects.SPEED))
            slot=find(StatusEffects.SPEED);
        if (slot<0 && fire.get() && !mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE))
            slot=find(StatusEffects.FIRE_RESISTANCE);
        if (slot<0) return;
        int original=mc.player.getInventory().getSelectedSlot();
        float oldPitch=mc.player.getPitch();
        if (!InvUtils.swap(slot,false)) return;
        try {
            mc.player.setPitch(90f);
            mc.interactionManager.interactItem(mc.player,Hand.MAIN_HAND);
        } finally {
            mc.player.setPitch(oldPitch);
            InvUtils.swap(original,false);
        }
        delay=cooldown.get();
    }
}
