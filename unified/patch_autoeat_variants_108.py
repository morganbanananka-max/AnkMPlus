#!/usr/bin/env python3
"""Native AutoEat strategy integration: Meteor vs user-supplied ThunderHack Reborn 1.21.11.
One module, one UI/config; conserve Meteor's well-tested eat/pause/restore machinery.
"""
from pathlib import Path
p=Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules/player/AutoEat.java")
s=p.read_text()
def edit(old,new,n=1):
 global s
 count=s.count(old)
 if count!=n: raise RuntimeError(f"AutoEat anchor mismatch {count}/{n}: {old[:110]}")
 s=s.replace(old,new,n)
edit("import meteordevelopment.meteorclient.events.entity.player.ItemUseCrosshairTargetEvent;",
"""import meteordevelopment.meteorclient.AnkVariantProfiles;
import meteordevelopment.meteorclient.events.entity.player.ItemUseCrosshairTargetEvent;""")
edit("    private final SettingGroup sgThreshold = settings.createGroup(\"Threshold\");",
"""    private final SettingGroup sgThreshold = settings.createGroup("Threshold");
    private final SettingGroup sgVariants = settings.createGroup("AnkHack implementation");

    // Ported from ThunderHack-Reborn's real AutoEat.java 1.21.11 source (GPL-3.0).
    // ThunderFast selects the first permitted hotbar item at a hunger-only threshold.
    // MeteorSmart keeps Meteor's prioritised food + health/hunger engine intact.
    public enum Implementation { Auto, MeteorSmart, ThunderFast }
    private final Setting<Implementation> implementation=sgVariants.add(
        new EnumSetting.Builder<Implementation>()
        .name("implementation")
        .description("MeteorSmart, ThunderHack Reborn's hunger-first strategy, or learned Auto.")
        .defaultValue(Implementation.Auto).build());
    private final Setting<Integer> thunderHunger=sgVariants.add(new IntSetting.Builder()
        .name("thunder-hunger")
        .description("ThunderHack's hunger-only food trigger (0..19).")
        .defaultValue(8).range(0,19).sliderRange(0,19)
        .visible(() -> implementation.get()!=Implementation.MeteorSmart).build());
    private Implementation activeStrategy=Implementation.MeteorSmart;
    private String serverKey="unknown";
    private int previousFoodLevel=-1, trialTicks=0, trialFoodLevel=-1;
    private Implementation selectedStrategy(){
        return implementation.get()==Implementation.Auto ? activeStrategy : implementation.get();
    }
    @Override public String getInfoString(){return selectedStrategy().name();}
    @Override public void onActivate() {
        serverKey=AnkVariantProfiles.serverKey(mc);
        activeStrategy=Implementation.MeteorSmart;
        if (implementation.get()==Implementation.Auto) try {
            activeStrategy=Implementation.valueOf(AnkVariantProfiles.choose(serverKey,"auto-eat"));
        }catch(IllegalArgumentException ignored){}
        previousFoodLevel=-1;trialTicks=0;trialFoodLevel=-1;
    }
    private void checkEvidence() {
        if(mc.player==null || implementation.get()!=Implementation.Auto)return;
        int current=mc.player.getHungerManager().getFoodLevel();
        if(trialTicks>0){
            --trialTicks;
            if(current>trialFoodLevel){
                AnkVariantProfiles.stable(serverKey,"auto-eat",activeStrategy.name());
                trialTicks=0;
            }else if(trialTicks==0 && trialFoodLevel<20){
                AnkVariantProfiles.correction(serverKey,"auto-eat",activeStrategy.name());
                String next=AnkVariantProfiles.nextUntested(serverKey,"auto-eat",activeStrategy.name());
                try {activeStrategy=Implementation.valueOf(next);}
                catch(IllegalArgumentException ignored){activeStrategy=Implementation.MeteorSmart;}
            }
        }
        previousFoodLevel=current;
    }
    private void noteEatingTrial() {
        if(implementation.get()!=Implementation.Auto || trialTicks>0 || mc.player==null)return;
        trialFoodLevel=mc.player.getHungerManager().getFoodLevel();
        trialTicks=100; // 5 seconds; enough for consuming most ordinary foods.
    }""")
edit("""    public void onDeactivate() {
        if (eating) stopEating();
    }""",
"""    public void onDeactivate() {
        if (eating) stopEating();
        trialTicks=0;
    }""")
edit("""    private void onTick(TickEvent.Pre event) {
        // Don't eat if AutoGap is already eating""",
"""    private void onTick(TickEvent.Pre event) {
        checkEvidence();
        // Don't eat if AutoGap is already eating""")
edit("""        if (shouldEat()) {
            startEating();
        }""",
"""        if (shouldEat()) {
            noteEatingTrial();
            startEating();
        }""")
edit("""    public boolean shouldEat() {
        boolean healthLow = mc.player.getHealth() <= healthThreshold.get();""",
"""    public boolean shouldEat() {
        if(selectedStrategy()==Implementation.ThunderFast) {
            if (mc.player.getHungerManager().getFoodLevel()>thunderHunger.get())return false;
            slot=findSlot();
            if(slot==-1)return false;
            var food=mc.player.getInventory().getStack(slot).get(DataComponentTypes.FOOD);
            return food!=null && (mc.player.getHungerManager().isNotFull() || food.canAlwaysEat());
        }
        boolean healthLow = mc.player.getHealth() <= healthThreshold.get();""")
edit("""    private int findSlot() {
        // prefer offhand""",
"""    private int findSlot() {
        if(selectedStrategy()==Implementation.ThunderFast) {
            // ThunderHack's original: first hotbar food satisfying user exclusions.
            // Meteor's blacklist remains shared so the user cannot accidentally eat
            // a totem/resource food that they configured as forbidden.
            for(int hotbar=0;hotbar<9;hotbar++) {
                ItemStack stack=mc.player.getInventory().getStack(hotbar);
                if(stack.get(DataComponentTypes.FOOD)!=null
                    && !blacklist.get().contains(stack.getItem()))return hotbar;
            }
            Item off=mc.player.getOffHandStack().getItem();
            if(off.getComponents().get(DataComponentTypes.FOOD)!=null &&
                !blacklist.get().contains(off))return SlotUtils.OFFHAND;
            return -1;
        }
        // prefer offhand""")
p.write_text(s)
q=Path("upstream/src/main/java/meteordevelopment/meteorclient/AnkVariantProfiles.java")
s=q.read_text()
needle='case "replenish" -> new String[]{"MeteorMerge","ThunderSwap"};'
if s.count(needle)!=1:raise RuntimeError("profile engine anchor missing")
q.write_text(s.replace(needle,needle+'\n            case "auto-eat" -> new String[]{"MeteorSmart","ThunderFast"};'))
notice=Path("upstream/ANKHACK_LICENSE_AND_ATTRIBUTIONS.md")
with notice.open("a") as f:
 f.write("\nAutoEat: Meteor's shared eat/slot/auras framework preserved; native hunger-only ThunderFast algorithm adapted from user-supplied ThunderHack Reborn 1.21.11 AutoEat.java, GPL-3.0. Local Auto feedback is heuristic, not quality proof.\n")
print("Integrated AutoEat: Auto/MeteorSmart/ThunderFast via same event bus and module UI.")
