package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EquipmentSlot;

/** ThunderHack Reborn's armor durability warning adapted to Meteor's events. GPL-3.0. */
public class AnkDurabilityAlert extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Integer> threshold = sg.add(new IntSetting.Builder()
        .name("threshold-percent").description("Alert when the weakest equipped armor piece reaches this durability.")
        .defaultValue(20).range(1, 99).sliderRange(1, 99).build());
    private final Setting<Integer> repeatSeconds = sg.add(new IntSetting.Builder()
        .name("repeat-seconds").description("Minimum time between warnings; prevents chat spam.")
        .defaultValue(30).range(5, 300).sliderRange(5, 90).build());
    private int ticks, lastAlert;
    public AnkDurabilityAlert() {
        super(Categories.Player, "ank-durability-alert", "Warns before damaged equipped armor breaks (ThunderHack adaptation).");
    }
    @Override public void onActivate() {
        ticks = 0;
        lastAlert = -100000;
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        ticks++;
        if (ticks - lastAlert < repeatSeconds.get() * 20) return;
        int lowest = 101;
        boolean any = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = mc.player.getEquippedStack(slot);
            if (stack.isEmpty() || !stack.isDamageable()) continue;
            any = true;
            int percent = Math.max(0, Math.round(100f * (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage()));
            lowest = Math.min(lowest, percent);
        }
        if (any && lowest <= threshold.get()) {
            warning("Armor durability is only " + lowest + "%! Repair or replace it.");
            lastAlert = ticks;
        }
    }
}
