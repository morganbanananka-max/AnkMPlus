package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Adaptation of ThunderHack Reborn 1.21.11 AntiAttack, GPL-3.0.
 * Uses Meteor's cancellable attack event instead of ThunderHack's packet accessor.
 */
public class AnkAntiAttack extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Boolean> protectFriends = sg.add(new BoolSetting.Builder()
        .name("protect-friends").description("Prevent accidental attacks on marked friends.")
        .defaultValue(true).build());
    private final Setting<Boolean> protectVillagers = sg.add(new BoolSetting.Builder()
        .name("protect-villagers").description("Prevent attacking villagers by accident.")
        .defaultValue(false).build());
    private final Setting<Boolean> protectPiglins = sg.add(new BoolSetting.Builder()
        .name("protect-zombified-piglins").description("Avoid making zombified piglins hostile.")
        .defaultValue(false).build());
    public AnkAntiAttack() {
        super(Categories.Player, "ank-anti-attack", "Prevents hitting selected friendly or neutral entities (ThunderHack adaptation).");
    }
    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (event.entity == null || mc.player == null || mc.world == null) return;
        if (protectFriends.get() && event.entity instanceof PlayerEntity player && Friends.get().isFriend(player)) {
            event.cancel(); return;
        }
        if (protectVillagers.get() && event.entity instanceof VillagerEntity) {
            event.cancel(); return;
        }
        if (protectPiglins.get() && event.entity instanceof ZombifiedPiglinEntity) event.cancel();
    }
}
