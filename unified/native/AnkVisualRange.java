package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.AnkVariantProfiles;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

import java.util.*;

/**
 * Source-derived adaptation of ThunderHack Reborn VisualRange (GPL-3.0).
 * Tracks player visibility and nearby distance without external network calls
 * or storing any player's identity on disk.
 */
public class AnkVisualRange extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Integer> radius = sg.add(new IntSetting.Builder()
        .name("radius").description("Observe players inside this distance in blocks.")
        .defaultValue(72).range(8,128).sliderRange(16,128).build());
    private final Setting<Boolean> announceEnter = sg.add(new BoolSetting.Builder()
        .name("announce-enter").description("Tell you when another player enters the radius.")
        .defaultValue(true).build());
    private final Setting<Boolean> announceLeave = sg.add(new BoolSetting.Builder()
        .name("announce-leave").description("Tell you when another player leaves the radius.")
        .defaultValue(false).build());
    private final Setting<Boolean> includeFriends = sg.add(new BoolSetting.Builder()
        .name("include-friends").description("Also announce friends.")
        .defaultValue(true).build());

    private final Map<UUID,String> nearby = new HashMap<>();
    private String worldKey="unknown";
    private boolean initialized;
    private int ticks;

    public AnkVisualRange() {
        super(Categories.Misc, "ank-visual-range",
            "Announces nearby players entering or leaving your range (ThunderHack adaptation).");
    }

    @Override public void onActivate() {
        nearby.clear();
        initialized=false;
        ticks=0;
        worldKey=AnkVariantProfiles.serverKey(mc);
    }
    @Override public void onDeactivate() {
        nearby.clear();initialized=false;
    }
    @Override public String getInfoString() { return Integer.toString(nearby.size()); }

    @EventHandler private void onTick(TickEvent.Post event) {
        if(mc.player==null || mc.world==null) {
            nearby.clear();initialized=false;worldKey="unknown";return;
        }
        String currentWorld=AnkVariantProfiles.serverKey(mc);
        if(!worldKey.equals(currentWorld)) {
            nearby.clear();initialized=false;worldKey=currentWorld;
        }
        if(++ticks<20)return;
        ticks=0;
        Map<UUID,String> now=new HashMap<>();
        double range2=(double)radius.get()*radius.get();
        for(PlayerEntity player:mc.world.getPlayers()) {
            if(player==mc.player || !player.isAlive() || player.isSpectator())continue;
            if(mc.player.squaredDistanceTo(player)>range2)continue;
            if(!includeFriends.get() && Friends.get().isFriend(player))continue;
            now.put(player.getUuid(),player.getName().getString());
        }
        // Initial scan is a baseline, not a flood of join notifications.
        if(!initialized) {
            nearby.clear();nearby.putAll(now);initialized=true;return;
        }
        int announcements=0;
        for(Map.Entry<UUID,String> entry:now.entrySet()) {
            if(!nearby.containsKey(entry.getKey()) && announceEnter.get() && announcements<3) {
                info("Player in range: %s",entry.getValue());
                announcements++;
            }
        }
        for(Map.Entry<UUID,String> entry:nearby.entrySet()) {
            if(!now.containsKey(entry.getKey()) && announceLeave.get() && announcements<3) {
                info("Player left range: %s",entry.getValue());
                announcements++;
            }
        }
        nearby.clear();nearby.putAll(now);
    }
}
