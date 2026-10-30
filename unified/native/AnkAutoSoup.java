/*
 * Native AnkHack port of AutoSoup from the user-supplied ThunderHack Reborn
 * Minecraft 1.21.11 source archive, GPL-3.0; original copyright preserved.
 * Uses one AnkHack module, settings system, input/event bus and shared GUI.
 */
package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

public final class AnkAutoSoup extends Module {
    private final SettingGroup general=settings.getDefaultGroup();
    private final Setting<Double> triggerHealth=general.add(new DoubleSetting.Builder()
        .name("health-threshold").description("Use mushroom stew at or below this health.")
        .defaultValue(7).min(1).sliderMax(20).build());
    private final Setting<Integer> cooldown=general.add(new IntSetting.Builder()
        .name("cooldown-ticks").description("Avoid repeated packet spam if items fail to consume.")
        .defaultValue(12).min(3).sliderMax(60).build());
    private int ticks;
    public AnkAutoSoup(){
        super(Categories.Player,"auto-soup","ThunderHack Reborn-derived emergency mushroom stew consumption.");
    }
    @Override public void onActivate(){ticks=0;}
    @EventHandler
    private void onTick(TickEvent.Pre event){
        if(mc.player==null||mc.world==null||mc.interactionManager==null
            ||mc.currentScreen!=null||mc.player.isSpectator()||mc.player.isDead())return;
        if(ticks>0){ticks--;return;}
        if(mc.player.getHealth()>triggerHealth.get())return;
        int slot=-1;
        for(int i=0;i<9;i++){
            if(mc.player.getInventory().getStack(i).isOf(Items.MUSHROOM_STEW)){slot=i;break;}
        }
        if(slot<0)return;
        int previous=mc.player.getInventory().getSelectedSlot();
        try{
            if(previous!=slot){
                mc.player.getInventory().setSelectedSlot(slot);
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            }
            mc.interactionManager.interactItem(mc.player,Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            ticks=cooldown.get();
        }finally{
            if(previous!=slot){
                mc.player.getInventory().setSelectedSlot(previous);
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(previous));
            }
        }
    }
}
