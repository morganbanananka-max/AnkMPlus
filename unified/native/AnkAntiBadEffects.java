/*
 * ThunderHack Reborn AntiBadEffects adaptation, GPL-3.0.
 * Client-side effect removal only. Servers can reapply effects at any time.
 */
package meteordevelopment.meteorclient.systems.modules.player;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;

public final class AnkAntiBadEffects extends Module {
    private final SettingGroup general=settings.getDefaultGroup();
    private final Setting<Boolean> blindness=general.add(new BoolSetting.Builder()
        .name("blindness").description("Hide the client-side Blindness effect.").defaultValue(true).build());
    private final Setting<Boolean> nausea=general.add(new BoolSetting.Builder()
        .name("nausea").description("Hide the client-side Nausea effect.").defaultValue(true).build());
    private final Setting<Boolean> fatigue=general.add(new BoolSetting.Builder()
        .name("mining-fatigue").description("Hide the client-side Mining Fatigue effect.").defaultValue(true).build());
    private final Setting<Boolean> slow=general.add(new BoolSetting.Builder()
        .name("slowness").description("Hide the client-side Slowness effect.").defaultValue(false).build());
    private final Setting<Boolean> levitation=general.add(new BoolSetting.Builder()
        .name("levitation").description("Hide client-side Levitation.").defaultValue(false).build());
    public AnkAntiBadEffects(){super(Categories.Player,"ank-anti-bad-effects","ThunderHack-derived selected negative status effect filtering.");}
    @EventHandler private void tick(TickEvent.Pre event){
        if(mc.player==null||mc.world==null)return;
        if(blindness.get()&&mc.player.hasStatusEffect(StatusEffects.BLINDNESS))mc.player.removeStatusEffect(StatusEffects.BLINDNESS);
        if(nausea.get()&&mc.player.hasStatusEffect(StatusEffects.NAUSEA))mc.player.removeStatusEffect(StatusEffects.NAUSEA);
        if(fatigue.get()&&mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE))mc.player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
        if(slow.get()&&mc.player.hasStatusEffect(StatusEffects.SLOWNESS))mc.player.removeStatusEffect(StatusEffects.SLOWNESS);
        if(levitation.get()&&mc.player.hasStatusEffect(StatusEffects.LEVITATION))mc.player.removeStatusEffect(StatusEffects.LEVITATION);
    }
}
