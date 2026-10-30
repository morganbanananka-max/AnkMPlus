/*
 * AnkHack native manual-weapon timing based on ThunderHack Reborn
 * PerfectDelay (GPL-3.0). Does not generate additional clicks or shots.
 */
package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Hand;

public final class AnkPerfectRelease extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Boolean> bow = sg.add(new BoolSetting.Builder()
        .name("bows").description("Automatically release manually drawn bows.")
        .defaultValue(true).build());
    private final Setting<Boolean> trident = sg.add(new BoolSetting.Builder()
        .name("tridents").description("Automatically release manually charged tridents.")
        .defaultValue(true).build());
    private final Setting<Integer> bowTicks = sg.add(new IntSetting.Builder()
        .name("bow-ticks").description("Bow charge duration (20 ticks is the normal full draw).")
        .defaultValue(20).range(4,40).sliderRange(4,30).visible(bow::get).build());
    private final Setting<Integer> tridentTicks = sg.add(new IntSetting.Builder()
        .name("trident-ticks").description("Trident charge duration (10 ticks is normal minimum).")
        .defaultValue(10).range(10,40).sliderRange(10,30).visible(trident::get).build());

    private int cooldown;
    public AnkPerfectRelease() {
        super(Categories.Combat, "ank-perfect-release",
            "Finish a manually held bow or trident charge at the chosen time. No packet spam.");
    }
    @Override public void onActivate() { cooldown=0; }
    @Override public void onDeactivate() { cooldown=0; }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (cooldown>0) cooldown--;
        if (cooldown>0 || mc.player==null || mc.world==null
            || mc.currentScreen!=null || mc.interactionManager==null
            || !mc.options.useKey.isPressed() || !mc.player.isUsingItem()
            || Modules.get().isActive(BowSpam.class)) return;

        Hand hand=mc.player.getActiveHand();
        ItemStack item=mc.player.getStackInHand(hand);
        int time=mc.player.getItemUseTime();
        if ((bow.get() && item.getItem() instanceof BowItem && time>=bowTicks.get())
            || (trident.get() && item.getItem() instanceof TridentItem && time>=tridentTicks.get())) {
            mc.interactionManager.stopUsingItem(mc.player);
            cooldown=4;
        }
    }
}
