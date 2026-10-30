/*
 * Original AnkHack gameplay module, based on the user's previous AutoMaceSelect rules.
 * Requires deliberate attack input, sufficient falling distance, a valid nearby target
 * and weapon cooldown. No automatic attack or out-of-reach hits.
 */
package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;

public final class AnkSmartMace extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Double> fallDistance = sg.add(new DoubleSetting.Builder()
        .name("minimum-fall")
        .description("Minimum fall distance before a mace can be selected.")
        .defaultValue(2.5).min(1.5).sliderMax(10).build());
    private int prior=-1;
    private int hold=0;
    public AnkSmartMace() {
        super(Categories.Combat,"ank-smart-mace",
            "Switch to a mace only during manual, aimed, valid falling attacks; then restore.");
    }
    @EventHandler
    private void tick(TickEvent.Pre event) {
        if (mc.player==null || mc.world==null) {prior=-1;hold=0;return;}
        if (hold>0) {
            --hold;
            if (hold==0 && prior>=0)mc.player.getInventory().setSelectedSlot(prior);
            if (hold==0) prior=-1;
            return;
        }
        if (!mc.options.attackKey.isPressed() || mc.currentScreen!=null
            || mc.player.fallDistance<fallDistance.get()
            || mc.player.getVelocity().y>=-0.1
            || mc.player.getAttackCooldownProgress(0)<0.93f
            || !(mc.crosshairTarget instanceof EntityHitResult result)
            || !(result.getEntity() instanceof LivingEntity enemy)
            || !enemy.isAlive()
            || mc.player.squaredDistanceTo(enemy)>9.0) return;
        int mace=-1;
        for (int i=0;i<9;i++) {
            ItemStack s=mc.player.getInventory().getStack(i);
            if(s.isOf(Items.MACE)) {mace=i;break;}
        }
        if(mace<0 || mace==mc.player.getInventory().getSelectedSlot())return;
        prior=mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(mace);
        hold=6;
    }
    @Override public void onDeactivate() {
        if(mc.player!=null && prior>=0)mc.player.getInventory().setSelectedSlot(prior);
        prior=-1;hold=0;
    }
}
