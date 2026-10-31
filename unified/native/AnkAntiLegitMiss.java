/*
 * AnkHack implementation inspired by the GPL-3.0 ThunderHack
 * Reborn AntiLegitMiss behavior. No reach changes or network spoofing.
 */
package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.entity.player.DoAttackEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public final class AnkAntiLegitMiss extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Boolean> blockHits = sg.add(new BoolSetting.Builder()
        .name("block-hits")
        .description("Also suppress attack input when looking at blocks. Disables normal block breaking while active.")
        .defaultValue(false).build());
    private final Setting<Boolean> nonLiving = sg.add(new BoolSetting.Builder()
        .name("non-living-entities")
        .description("Suppress attacks against non-living entities such as armor stands.")
        .defaultValue(false).build());
    private final Setting<Boolean> onlyWithWeapon = sg.add(new BoolSetting.Builder()
        .name("only-with-weapon")
        .description("Ignore the module when the main hand is empty.")
        .defaultValue(true).build());

    public AnkAntiLegitMiss() {
        super(Categories.Combat, "ank-anti-legit-miss",
            "Avoid accidental missed attacks while keeping normal mining available by default.");
    }

    @EventHandler
    private void onAttack(DoAttackEvent event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        if (onlyWithWeapon.get() && mc.player.getMainHandStack().isEmpty()) return;
        HitResult ray = mc.crosshairTarget;
        if (ray == null || ray.getType() == HitResult.Type.MISS) {
            event.setCancelled(true);
        } else if (ray.getType() == HitResult.Type.BLOCK && blockHits.get()) {
            event.setCancelled(true);
        } else if (ray instanceof EntityHitResult hit && nonLiving.get()
                   && !(hit.getEntity() instanceof LivingEntity)) {
            event.setCancelled(true);
        }
    }
}
