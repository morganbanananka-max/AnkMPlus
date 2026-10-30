/*
 * AnkHack integration of ThunderHack Reborn 1.21.11 ToolSaver concept.
 * GPL-3.0. Original source supplied by the user; attribution preserved.
 */
package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;

public final class AnkToolSaver extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Integer> keepPercent = sg.add(new IntSetting.Builder()
        .name("save-at-percent")
        .description("Select a non-tool before the selected tool's remaining durability falls below this percentage.")
        .defaultValue(10).range(1,50).sliderRange(1,50).build());
    private int ticks;
    public AnkToolSaver() {
        super(Categories.Player, "ank-tool-saver", "ThunderHack-derived hotbar protection for low-durability tools.");
    }
    @Override public void onActivate() { ticks=0; }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.currentScreen != null || mc.world == null) return;
        if (++ticks < 5) return;
        ticks=0;
        ItemStack tool=mc.player.getMainHandStack();
        if (!tool.isDamageable() || tool.getMaxDamage() <= 0 ||
            !(tool.isIn(ItemTags.PICKAXES) || tool.isIn(ItemTags.AXES)
                || tool.isIn(ItemTags.SHOVELS) || tool.isIn(ItemTags.HOES))) return;
        int remaining=tool.getMaxDamage()-tool.getDamage();
        if (remaining*100 > keepPercent.get()*tool.getMaxDamage()) return;
        int old=mc.player.getInventory().getSelectedSlot();
        for (int i=0;i<9;i++) {
            if(i==old) continue;
            ItemStack candidate=mc.player.getInventory().getStack(i);
            if (candidate.isEmpty() || !candidate.isDamageable()) {
                mc.player.getInventory().setSelectedSlot(i);
                warning("ToolSaver: switched away from a tool with %s durability remaining.", remaining);
                return;
            }
        }
        // Do not repeatedly spam chat when no safe slot is available.
    }
}
