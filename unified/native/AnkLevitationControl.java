/*
 * ThunderHack Reborn LevitationControl adaptation, GPL-3.0.
 * Shares the original AnkHack movement event loop; no secondary client engine.
 */
package meteordevelopment.meteorclient.systems.modules.movement;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

public final class AnkLevitationControl extends Module {
    private final SettingGroup general=settings.getDefaultGroup();
    private final Setting<Double> up=general.add(new DoubleSetting.Builder()
        .name("up-speed").description("Ascend speed while holding Jump under Levitation.")
        .defaultValue(0.28).min(0.01).sliderMax(1.5).build());
    private final Setting<Double> down=general.add(new DoubleSetting.Builder()
        .name("down-speed").description("Descend speed while sneaking under Levitation.")
        .defaultValue(0.28).min(0.01).sliderMax(1.5).build());
    public AnkLevitationControl() {
        super(Categories.Movement,"ank-levitation-control","ThunderHack-derived hover/up/down controls for Levitation.");
    }
    @EventHandler private void tick(TickEvent.Pre event) {
        if(mc.player==null||mc.world==null
            ||!mc.player.hasStatusEffect(StatusEffects.LEVITATION)
            ||Modules.get().isActive(Flight.class))return;
        Vec3d old=mc.player.getVelocity();
        double y=0;
        if(mc.options.jumpKey.isPressed())y=up.get();
        else if(mc.options.sneakKey.isPressed())y=-down.get();
        mc.player.setVelocity(old.x,y,old.z);
    }
}
