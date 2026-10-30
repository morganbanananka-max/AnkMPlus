package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Adaptation of ThunderHack Reborn 1.21.11 ElytraReplace, GPL-3.0.
 * Pick-up swap protects both Elytra items and refuses to run in any container
 * GUI or while dragging another item. It never drops an item.
 */
public class AnkElytraReplace extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Integer> threshold = sg.add(new IntSetting.Builder()
        .name("replace-at-percent").description("Replace worn Elytra when its remaining durability is at or below this percentage.")
        .defaultValue(5).range(1, 80).sliderRange(1, 40).build());
    private final Setting<Boolean> onGround = sg.add(new BoolSetting.Builder()
        .name("ground-only").description("Wait until safely on the ground before swapping Elytra.")
        .defaultValue(true).build());
    private final Setting<Integer> cooldown = sg.add(new IntSetting.Builder()
        .name("cooldown-ticks").description("Cooldown between inventory swaps.")
        .defaultValue(20).range(5, 200).sliderRange(5, 100).build());
    private int delay;
    public AnkElytraReplace() {
        super(Categories.Player, "ank-elytra-replace", "Replaces nearly broken Elytra with a healthier one in your inventory (ThunderHack adaptation).");
    }
    @Override public void onActivate() { delay=0; }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (delay > 0) { delay--; return; }
        if (onGround.get() && !mc.player.isOnGround()) return;
        if (mc.player.currentScreenHandler != mc.player.playerScreenHandler || mc.currentScreen != null) return;
        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) return;
        ItemStack worn = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (!worn.isOf(Items.ELYTRA) || !worn.isDamageable()) return;
        int remaining = worn.getMaxDamage() - worn.getDamage();
        if (remaining * 100 > threshold.get() * worn.getMaxDamage()) return;
        int bestSlot = -1, bestRemaining = remaining;
        for (int i=0;i<36;i++) {
            ItemStack stack=mc.player.getInventory().getStack(i);
            if (!stack.isOf(Items.ELYTRA) || !stack.isDamageable()) continue;
            int available=stack.getMaxDamage()-stack.getDamage();
            if (available > bestRemaining && available * 100 > threshold.get() * stack.getMaxDamage()) {
                bestSlot=i;bestRemaining=available;
            }
        }
        if (bestSlot<0) return;
        int id=SlotUtils.indexToId(bestSlot);
        int chestId=SlotUtils.indexToId(38); // PlayerInventory chest index 38
        if (id<0 || chestId<0 || id==chestId) return;
        int sync=mc.player.playerScreenHandler.syncId;
        mc.interactionManager.clickSlot(sync,id,0,SlotActionType.PICKUP,mc.player);
        mc.interactionManager.clickSlot(sync,chestId,0,SlotActionType.PICKUP,mc.player);
        mc.interactionManager.clickSlot(sync,id,0,SlotActionType.PICKUP,mc.player);
        delay=cooldown.get();
    }
}
