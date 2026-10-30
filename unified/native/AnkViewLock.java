/*
 * Copyright of original ViewLock behavior belongs to ThunderHack Reborn contributors.
 * GPL-3.0; adapted from user-provided 1.21.11 sources to the one AnkHack module framework.
 */
package meteordevelopment.meteorclient.systems.modules.player;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public final class AnkViewLock extends Module {
    private final SettingGroup general=settings.getDefaultGroup();
    private final Setting<Boolean> lockCurrent=general.add(new BoolSetting.Builder()
        .name("lock-current").description("Use your current view when enabled.")
        .defaultValue(true).build());
    private final Setting<Boolean> yaw=general.add(new BoolSetting.Builder()
        .name("lock-yaw").description("Lock horizontal view angle.")
        .defaultValue(true).build());
    private final Setting<Boolean> pitch=general.add(new BoolSetting.Builder()
        .name("lock-pitch").description("Lock vertical view angle.")
        .defaultValue(true).build());
    private final Setting<Double> yawValue=general.add(new DoubleSetting.Builder()
        .name("yaw-value").description("Manual yaw when lock-current is off.")
        .defaultValue(0).range(-180,180).sliderRange(-180,180).build());
    private final Setting<Double> pitchValue=general.add(new DoubleSetting.Builder()
        .name("pitch-value").description("Manual pitch when lock-current is off.")
        .defaultValue(0).range(-90,90).sliderRange(-90,90).build());
    private float storedYaw,storedPitch;
    public AnkViewLock(){super(Categories.Player,"ank-view-lock","ThunderHack Reborn-derived manual or current camera view lock.");}
    @Override public void onActivate(){
        storedYaw=mc.player!=null?mc.player.getYaw():0;
        storedPitch=mc.player!=null?mc.player.getPitch():0;
    }
    @EventHandler private void tick(TickEvent.Post event){
        if(mc.player==null||mc.currentScreen!=null)return;
        if(yaw.get())mc.player.setYaw(lockCurrent.get()?storedYaw:yawValue.get().floatValue());
        if(pitch.get())mc.player.setPitch(lockCurrent.get()?storedPitch:pitchValue.get().floatValue());
    }
}
