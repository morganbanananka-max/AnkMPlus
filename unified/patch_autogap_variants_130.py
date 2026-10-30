#!/usr/bin/env python3
"""Real Meteor/ThunderHack Reborn AutoGap integration in one module.

Meteor keeps its mainhand inventory slot selection, effect handling,
and aura/Baritone coordination. ThunderOffhand uses offhand apple consumption
at a health threshold. Auto saves only observed apple consumption per world.
This is not a Grim bypass. Source adaptation GPL-3.0.
"""
from pathlib import Path
p=Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules/player/AutoGap.java")
s=p.read_text()
def edit(old,new):
 global s
 n=s.count(old)
 assert n==1,("AutoGap anchor",n,old[:90])
 s=s.replace(old,new,1)
edit("import meteordevelopment.meteorclient.events.entity.player.ItemUseCrosshairTargetEvent;",
"""import meteordevelopment.meteorclient.AnkVariantProfiles;
import meteordevelopment.meteorclient.events.entity.player.ItemUseCrosshairTargetEvent;""")
edit("import meteordevelopment.meteorclient.settings.BoolSetting;",
"""import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;""")
edit("import net.minecraft.registry.entry.RegistryEntry;",
"""import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;""")
edit("""    private final SettingGroup sgHealth = settings.createGroup("Health");""",
"""    private final SettingGroup sgHealth = settings.createGroup("Health");
    private final SettingGroup sgVariants = settings.createGroup("AnkHack implementations");

    public enum Implementation { Auto, Meteor, ThunderOffhand }
    private final Setting<Implementation> implementation = sgVariants.add(new EnumSetting.Builder<Implementation>()
        .name("implementation")
        .description("Meteor inventory/effect logic or ThunderHack's offhand health-trigger logic.")
        .defaultValue(Implementation.Auto).build());
    private final Setting<Integer> thunderHealth = sgVariants.add(new IntSetting.Builder()
        .name("thunder-health").description("Offhand eating health threshold in half-hearts x2.")
        .defaultValue(15).range(1,40).sliderRange(1,30)
        .visible(() -> implementation.get()!=Implementation.Meteor).build());
    private final Setting<Boolean> includeAbsorption = sgVariants.add(new BoolSetting.Builder()
        .name("thunder-include-absorption").description("Include absorption hearts in ThunderHack health threshold.")
        .defaultValue(false).visible(() -> implementation.get()!=Implementation.Meteor).build());

    private Implementation chosen = Implementation.Meteor;
    private String server = "unknown";
    private boolean thunderEating;
    private int previousApples = -1, recentEatingTicks;

    private Implementation current() {
        return implementation.get()==Implementation.Auto ? chosen : implementation.get();
    }

    @Override public String getInfoString() {
        return implementation.get()==Implementation.Auto ? "Auto: "+chosen.name() : current().name();
    }

    @Override public void onActivate() {
        chosen=Implementation.Meteor;
        server=AnkVariantProfiles.serverKey(mc);
        if(implementation.get()==Implementation.Auto) {
            try{chosen=Implementation.valueOf(AnkVariantProfiles.choose(server,"auto-gap"));}
            catch(IllegalArgumentException ignored){}
        }
        thunderEating=false;
        previousApples=-1;
        recentEatingTicks=0;
    }

    private int appleCount() {
        if(mc.player==null)return 0;
        int total=0;
        for(int i=0;i<36;i++){
            ItemStack stack=mc.player.getInventory().getStack(i);
            if(stack.isOf(Items.GOLDEN_APPLE)||stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) total+=stack.getCount();
        }
        ItemStack offhand=mc.player.getOffHandStack();
        if(offhand.isOf(Items.GOLDEN_APPLE)||offhand.isOf(Items.ENCHANTED_GOLDEN_APPLE))
            total+=offhand.getCount();
        return total;
    }

    private void thunderTick() {
        if(eating)stopEating();
        ItemStack offhand=mc.player.getOffHandStack();
        boolean apple=offhand.isOf(Items.GOLDEN_APPLE)||offhand.isOf(Items.ENCHANTED_GOLDEN_APPLE);
        float health=mc.player.getHealth()+(includeAbsorption.get()?mc.player.getAbsorptionAmount():0);
        boolean wants=apple && health<=thunderHealth.get() && mc.currentScreen==null;
        if(!wants){
            if(thunderEating){mc.options.useKey.setPressed(false);thunderEating=false;}
            return;
        }
        if(!mc.player.isUsingItem() && mc.interactionManager!=null)
            mc.interactionManager.interactItem(mc.player,Hand.OFF_HAND);
        mc.options.useKey.setPressed(true);
        thunderEating=true;
        recentEatingTicks=45;
    }

    @EventHandler private void observeConsumption(TickEvent.Post event) {
        if(mc.player==null || mc.world==null)return;
        int count=appleCount();
        if(previousApples>=0 && count<previousApples
            && implementation.get()==Implementation.Auto
            && recentEatingTicks>0) {
            AnkVariantProfiles.stable(server,"auto-gap",chosen.name());
        }
        previousApples=count;
        if(recentEatingTicks>0)recentEatingTicks--;
    }""")
edit("""    public void onDeactivate() {
        if (eating) stopEating();
    }""",
"""    public void onDeactivate() {
        if (eating) stopEating();
        if (thunderEating) {
            mc.options.useKey.setPressed(false);
            thunderEating=false;
        }
    }""")
edit("""    private void onTick(TickEvent.Pre event) {
        if (eating) {""",
"""    private void onTick(TickEvent.Pre event) {
        if(mc.player==null || mc.world==null)return;
        if(current()==Implementation.ThunderOffhand){thunderTick();return;}
        if(thunderEating){mc.options.useKey.setPressed(false);thunderEating=false;}
        if(eating) recentEatingTicks=45;
        if (eating) {""")
edit("""                if (slot != -1) startEating();""",
"""                if (slot != -1) {
                    recentEatingTicks=45;
                    startEating();
                }""")
p.write_text(s)
profile=Path("upstream/src/main/java/meteordevelopment/meteorclient/AnkVariantProfiles.java")
t=profile.read_text()
anchor='case "bow-spam" -> new String[]{"Meteor","Thunder"};'
assert t.count(anchor)==1, "Unknown variant profile schema"
profile.write_text(t.replace(anchor,anchor+'\n            case "auto-gap" -> new String[]{"Meteor","ThunderOffhand"};',1))
with Path("upstream/ANKHACK_LICENSE_AND_ATTRIBUTIONS.md").open("a",encoding="utf-8") as f:
 f.write("""
ThunderHack Reborn 1.21.11 AutoGApple adaptation GPL-3.0:
  Offhand health-trigger apple consumption in Meteor AutoGap as a real
  ThunderOffhand implementation. Meteor strategy retains advanced potion/
  mainhand/aura/Baritone logic. Auto observes apple count drops as limited
  evidence, not a guarantee that the server accepted or optimized effects.
""")
print("Integrated Meteor and ThunderHack AutoGap strategies into a single module.")
