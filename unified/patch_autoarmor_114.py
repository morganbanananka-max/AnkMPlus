#!/usr/bin/env python3
"""Integrate ThunderHack Reborn's armor-priority scoring as a genuine second
strategy inside Meteor's single AutoArmor module (both GPL-3.0).
Each strategy keeps the existing safety checks, inventory API and UI.
Auto verifies actual inventory changes; it does not pretend to benchmark armor quality.
"""
from pathlib import Path

p=Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules/combat/AutoArmor.java")
s=p.read_text()
def edit(before,after):
    global s
    n=s.count(before)
    if n!=1:raise RuntimeError(f"AutoArmor patch anchor count {n}: {before[:105]}")
    s=s.replace(before,after,1)

edit("import meteordevelopment.meteorclient.events.world.TickEvent;",
"""import meteordevelopment.meteorclient.AnkVariantProfiles;
import meteordevelopment.meteorclient.events.world.TickEvent;""")
edit("    private final SettingGroup sgGeneral = settings.getDefaultGroup();",
"""    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum Implementation { Auto, MeteorScore, ThunderPvP }

    private final Setting<Implementation> implementation = sgGeneral.add(
        new EnumSetting.Builder<Implementation>()
            .name("implementation")
            .description("Meteor enchantment balancing or ThunderHack's armor-first PvP weighting, with per-server fallback.")
            .defaultValue(Implementation.Auto).build());

    private Implementation activeImplementation = Implementation.MeteorScore;
    private String armorServer = "unknown";
    private int pendingArmorSlot = -1, pendingTicks = 0, failures = 0;
    private ItemStack previousArmor = ItemStack.EMPTY;

    private Implementation chosen() {
        return implementation.get() == Implementation.Auto ? activeImplementation : implementation.get();
    }

    @Override
    public String getInfoString() {
        return implementation.get() == Implementation.Auto ? "Auto: " + activeImplementation.name() : chosen().name();
    }

    private static EquipmentSlot slotFor(int slot) {
        return switch(slot) {
            case 0 -> EquipmentSlot.FEET;
            case 1 -> EquipmentSlot.LEGS;
            case 2 -> EquipmentSlot.CHEST;
            default -> EquipmentSlot.HEAD;
        };
    }

    private void observeArmorChange() {
        if (pendingArmorSlot < 0 || mc.player == null) return;
        if (pendingTicks-- > 0) return;
        ItemStack now = mc.player.getEquippedStack(slotFor(pendingArmorSlot));
        boolean changed = !ItemStack.areItemsAndComponentsEqual(previousArmor, now)
            || previousArmor.getCount() != now.getCount();
        if (implementation.get() == Implementation.Auto && !armorServer.equals("unknown")) {
            if (changed) {
                AnkVariantProfiles.stable(armorServer, "armor", activeImplementation.name());
                failures = 0;
            } else {
                AnkVariantProfiles.correction(armorServer, "armor", activeImplementation.name());
                if (++failures >= 2) {
                    String next = AnkVariantProfiles.nextUntested(armorServer, "armor", activeImplementation.name());
                    try { activeImplementation = Implementation.valueOf(next); }
                    catch (IllegalArgumentException ignored) { activeImplementation = Implementation.MeteorScore; }
                    failures = 0;
                }
            }
        }
        pendingArmorSlot = -1;
        previousArmor = ItemStack.EMPTY;
    }""")
edit("""    public void onActivate() {
        timer = 0;
    }""",
"""    public void onActivate() {
        timer = 0;
        armorServer = AnkVariantProfiles.serverKey(mc);
        activeImplementation = Implementation.MeteorScore;
        if (implementation.get() == Implementation.Auto) {
            try { activeImplementation = Implementation.valueOf(AnkVariantProfiles.choose(armorServer,"armor")); }
            catch (IllegalArgumentException ignored) {}
        }
        pendingArmorSlot = -1;
        failures = 0;
    }

    @Override
    public void onDeactivate() {
        pendingArmorSlot = -1;
        previousArmor = ItemStack.EMPTY;
    }""")
edit("""    private void onPreTick(TickEvent.Pre event) {
        // Wait for timer (delay)""",
"""    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (pendingArmorSlot >= 0) {
            observeArmorChange();
            if (pendingArmorSlot >= 0) return;
        }
        // Wait for timer (delay)""")
edit("        score += 3 * Utils.getEnchantmentLevel(enchantments, protection);",
"""        // ThunderHack heavily prioritizes real armor points, while Meteor
        // emphasizes preferred enchantments. Both are real scoring modes.
        score += (chosen() == Implementation.ThunderPvP ? 2 : 3)
            * Utils.getEnchantmentLevel(enchantments, protection);""")
edit("""                    score += (int) switch (modifier.modifier().operation()) {""",
"""                    score += (chosen() == Implementation.ThunderPvP ? 10 : 1) * (int) switch (modifier.modifier().operation()) {""")
edit("""    private void swap(int from, int armorSlotId) {
        InvUtils.move().from(from).toArmor(armorSlotId);""",
"""    private void swap(int from, int armorSlotId) {
        if (implementation.get() == Implementation.Auto && pendingArmorSlot < 0) {
            pendingArmorSlot = armorSlotId;
            previousArmor = mc.player.getEquippedStack(slotFor(armorSlotId)).copy();
            pendingTicks = 3;
        }
        InvUtils.move().from(from).toArmor(armorSlotId);""")
p.write_text(s)

profiles=Path("upstream/src/main/java/meteordevelopment/meteorclient/AnkVariantProfiles.java")
a=profiles.read_text()
target='case "sprint" -> new String[]{"Strict","Rage","LiquidLegit","LiquidOmni"};'
if a.count(target)!=1:raise RuntimeError("Armor variant-family anchor missing")
a=a.replace(target,target+'\n            case "armor" -> new String[]{"MeteorScore","ThunderPvP"};')
profiles.write_text(a)

notice=Path("upstream/ANKHACK_LICENSE_AND_ATTRIBUTIONS.md")
with notice.open("a") as out:
    out.write("\nAutoArmor: ThunderHack Reborn 1.21.11 PvP armor-priority scoring integrated into Meteor AutoArmor as a selectable strategy (GPL-3.0). Auto only scores whether inventory swaps complete, not actual protection or anti-cheat bypass quality. Not gameplay verified.\n")
print("AutoArmor now has one module with Auto/MeteorScore/ThunderPvP variants and equip-change feedback.")
