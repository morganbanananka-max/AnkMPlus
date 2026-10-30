#!/usr/bin/env python3
"""Merge ThunderHack's HotbarReplenish into Meteor AutoReplenish as one module.
Using true source logic, not two side-by-side JARs or duplicate GUI entries.
"""
from pathlib import Path
p=Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules/player/AutoReplenish.java")
s=p.read_text()
def patch(a,b,expected=1):
 global s
 actual=s.count(a)
 if actual != expected:raise RuntimeError(f"AutoReplenish anchor found {actual} times, expected {expected}: {a[:100]}")
 s=s.replace(a,b,expected)
patch("import meteordevelopment.meteorclient.events.world.TickEvent;",
"""import meteordevelopment.meteorclient.AnkVariantProfiles;
import meteordevelopment.meteorclient.events.world.TickEvent;""")
patch("import net.minecraft.item.Items;",
"""import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;""")
patch("    private final SettingGroup sgGeneral = settings.getDefaultGroup();",
"""    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // Adapted from ThunderHack Reborn 1.21.11 HotbarReplenish (GPL-3.0).
    // Both native inventory strategies use exactly one module and GUI.
    public enum Strategy { Auto, MeteorMerge, ThunderSwap }
    private final Setting<Strategy> strategy = sgGeneral.add(new EnumSetting.Builder<Strategy>()
        .name("implementation")
        .description("Meteor merge or ThunderHack-style instant inventory swap, with per-server Auto feedback.")
        .defaultValue(Strategy.Auto).build());

    private final Setting<Integer> thunder64Threshold = sgGeneral.add(new IntSetting.Builder()
        .name("thunder-64-threshold")
        .description("ThunderSwap refills normal stacks at this item count.")
        .defaultValue(16).range(1,63).sliderRange(1,63)
        .visible(() -> strategy.get()!=Strategy.MeteorMerge).build());

    private final Setting<Integer> thunder16Threshold = sgGeneral.add(new IntSetting.Builder()
        .name("thunder-16-threshold")
        .description("ThunderSwap refills pearl/16-stack items at this count.")
        .defaultValue(4).range(1,15).sliderRange(1,15)
        .visible(() -> strategy.get()!=Strategy.MeteorMerge).build());

    private Strategy chosen = Strategy.MeteorMerge;
    private String serverKey = "unknown";
    private int pendingSlot = -1, pendingCount = -1, pendingTicks;
    private Item pendingItem;
    private int failures;
    private Strategy currentStrategy() {
        return strategy.get()==Strategy.Auto ? chosen : strategy.get();
    }
    @Override
    public String getInfoString() {
        return currentStrategy().name();
    }
    private void trackAttempt(int slot, ItemStack prior) {
        if (strategy.get()!=Strategy.Auto || pendingSlot>=0) return;
        pendingSlot=slot;
        pendingCount=prior.getCount();
        pendingItem=prior.getItem();
        pendingTicks=3;
    }
    private void verifyAttempt() {
        if (pendingSlot<0 || mc.player==null) return;
        if (pendingTicks-->0) return;
        ItemStack after=mc.player.getInventory().getStack(pendingSlot);
        if (after.getItem()!=pendingItem || after.getCount()>pendingCount) {
            AnkVariantProfiles.stable(serverKey,"replenish",chosen.name());
            failures=0;
        } else {
            AnkVariantProfiles.correction(serverKey,"replenish",chosen.name());
            failures++;
            if (failures >=3 && strategy.get()==Strategy.Auto) {
                try {
                    chosen=Strategy.valueOf(AnkVariantProfiles.nextUntested(
                        serverKey,"replenish",chosen.name()));
                }catch(IllegalArgumentException ignored){ chosen=Strategy.MeteorMerge; }
                failures=0;
            }
        }
        pendingSlot=-1;
        pendingItem=null;
    }
    private boolean thunderSwapOnce() {
        if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) return false;
        for (int hotbar=0;hotbar<9;hotbar++) {
            ItemStack stack=mc.player.getInventory().getStack(hotbar);
            if(stack.isEmpty() || !stack.isStackable() || excludedItems.get().contains(stack.getItem()))
                continue;
            int threshold=stack.getMaxCount()<=16 ? thunder16Threshold.get() : thunder64Threshold.get();
            if(stack.getCount()>threshold)continue;
            int largest=-1,largestCount=stack.getCount();
            for(int source=9;source<36;source++) {
                ItemStack other=mc.player.getInventory().getStack(source);
                if(!other.isEmpty() && other.getCount()>largestCount
                    && ItemStack.areItemsAndComponentsEqual(stack,other)) {
                    largest=source;largestCount=other.getCount();
                }
            }
            if(largest<0)continue;
            trackAttempt(hotbar,stack);
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,
                largest,hotbar,SlotActionType.SWAP,mc.player);
            return true;
        }
        return false;
    }""")
patch("""    public void onActivate() {
        fillItems();""",
"""    public void onActivate() {
        serverKey=AnkVariantProfiles.serverKey(mc);
        chosen=Strategy.MeteorMerge;
        if (strategy.get()==Strategy.Auto) {
            try { chosen=Strategy.valueOf(AnkVariantProfiles.choose(serverKey,"replenish")); }
            catch(IllegalArgumentException ignored) {}
        }
        pendingSlot=-1;pendingItem=null;pendingTicks=0;failures=0;
        fillItems();""")
patch("""    private void onTick(TickEvent.Pre event) {
        if (mc.currentScreen == null && prevHadOpenScreen) {""",
"""    private void onTick(TickEvent.Pre event) {
        if(mc.player==null || mc.interactionManager==null)return;
        verifyAttempt();
        if (mc.currentScreen == null && prevHadOpenScreen) {""")
patch("""        // Hotbar
        for (int i = 0; i < 9; i++) {""",
"""        if (currentStrategy()==Strategy.ThunderSwap) {
            thunderSwapOnce();
            fillItems();
            tickDelayLeft=tickDelay.get();
            return;
        }

        // Hotbar
        for (int i = 0; i < 9; i++) {""")
patch("        InvUtils.move().from(fromSlot).to(slot);",
"""        // Record genuine before/after inventory changes, not claimed success.
        if(slot>=0 && slot<9)trackAttempt(slot,stack);
        InvUtils.move().from(fromSlot).to(slot);""")
p.write_text(s)
profiles=Path("upstream/src/main/java/meteordevelopment/meteorclient/AnkVariantProfiles.java")
r=profiles.read_text()
a='case "sprint" -> new String[]{"Strict","Rage","LiquidLegit","LiquidOmni"};'
if r.count(a)!=1:raise RuntimeError("Variant family anchor missing")
r=r.replace(a,a+'\n            case "replenish" -> new String[]{"MeteorMerge","ThunderSwap"};')
profiles.write_text(r)
notice=Path("upstream/ANKHACK_LICENSE_AND_ATTRIBUTIONS.md")
with notice.open("a") as out:
 out.write("\nThunderHack Reborn 1.21.11 HotbarReplenish: ThunderSwap strategy integrated directly into Meteor AutoReplenish (GPL-3.0); per-server outcome evidence is heuristic and not in-game validated.\n")
print("AutoReplenish has Auto, MeteorMerge and ThunderSwap strategies with local server profiles.")
