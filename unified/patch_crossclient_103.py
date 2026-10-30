#!/usr/bin/env python3
"""Port source ideas from three separate clients into the existing AnkHack modules.

This script deliberately extends the one original modules (not a fake second mod or
nested jars). All adapted code is GPL-3.0 with upstream notices retained.
"""
from pathlib import Path
BASE=Path("upstream/src/main/java/meteordevelopment/meteorclient")
def patch(rel,old,new,expected=1):
    p=BASE/rel
    s=p.read_text()
    actual=s.count(old)
    if actual!=expected: raise RuntimeError(f"Expected {expected} matches, got {actual}: {rel}:{old[:110]!r}")
    p.write_text(s.replace(old,new))
# LiquidBounce "Sprint" (nextgen) -> two extra native movement behaviors.
# Original https://github.com/CCBlueX/LiquidBounce
# Copyright (c) 2015-2026 CCBlueX, GPL-3.0.
SPRINT="systems/modules/movement/Sprint.java"
patch(SPRINT,"""    public enum Mode {
        Strict,
        Rage
    }""","""    // Native variants: Meteor Strict/Rage and adapted LiquidBounce sprint logic.
    public enum Mode {
        Strict,
        Rage,
        LiquidLegit,
        LiquidOmni
    }""")
patch(SPRINT,"""    public boolean shouldSprint() {
        if (mc.currentScreen != null && !Modules.get().get(GUIMove.class).sprint.get()) return false;""",
"""    public boolean shouldSprint() {
        if (mc.currentScreen != null && !Modules.get().get(GUIMove.class).sprint.get()) return false;
        if (mode.get() == Mode.LiquidLegit) {
            return isActive() && mc.player.forwardSpeed > 0.8f
                && !mc.player.isSneaking() && !mc.player.hasBlindnessEffect()
                && mc.player.getHungerManager().canSprint()
                && (!mc.player.horizontalCollision || mc.player.collidedSoftly);
        }
        if (mode.get() == Mode.LiquidOmni) {
            // LiquidBounce-inspired omnidirectional sprint request.
            // No protocol exploit or forced yaw changes; server may ignore this.
            return isActive() && !mc.player.isSneaking()
                && mc.player.getHungerManager().canSprint()
                && (Math.abs(mc.player.forwardSpeed)+Math.abs(mc.player.sidewaysSpeed)) > 0.01f;
        }""")

# Wurst AutoFish bite detection -> an option in Meteor AutoFish, not second AutoFish.
# https://github.com/Wurst-Imperium/Wurst7 GPL-3.0.
FISH="systems/modules/player/AutoFish.java"
patch(FISH,"import meteordevelopment.meteorclient.events.world.TickEvent;",
"""import meteordevelopment.meteorclient.AnkVariantProfiles;
import meteordevelopment.meteorclient.events.world.TickEvent;""")
patch(FISH,"import meteordevelopment.meteorclient.settings.BoolSetting;",
"""import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;""")
patch(FISH,"import net.minecraft.item.ItemStack;",
"""import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;""")
patch(FISH,"""    private final SettingGroup sgGeneral = settings.getDefaultGroup();""",
"""    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // The two implementations share rod selection, delays, config and GUI.
    // WurstMotion adapts Wurst's entity/bobber bite detection concept.
    public enum BiteMode { Auto, MeteorEntity, WurstMotion }
    private final Setting<BiteMode> biteMode = sgGeneral.add(new EnumSetting.Builder<BiteMode>()
        .name("bite-implementation")
        .description("Meteor entity metadata, Wurst-inspired bobber motion, or per-world Auto.")
        .defaultValue(BiteMode.Auto).build());
    private BiteMode activeBiteMode=BiteMode.MeteorEntity;
    private String activeServer="unknown";
    private int tickCounter=0, lastAttempt=0, lostBites=0;
    private int beforeFish=0, verifyTicks=0;
    private boolean biteForRetrieval=false;

    private int fishCount() {
        if (mc.player==null) return 0;
        int sum=0;
        for (int i=0;i<mc.player.getInventory().size();i++) {
            var stack=mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.COD)||stack.isOf(Items.SALMON)
                ||stack.isOf(Items.TROPICAL_FISH)||stack.isOf(Items.PUFFERFISH))
                sum+=stack.getCount();
        }
        return sum;
    }
    private BiteMode selectedBiteMode() {
        return biteMode.get()==BiteMode.Auto ? activeBiteMode : biteMode.get();
    }
    private void onBiteAttempt() {
        if (biteMode.get()!=BiteMode.Auto || verifyTicks>0) return;
        beforeFish=fishCount();
        verifyTicks=32;
        lastAttempt=tickCounter;
        biteForRetrieval=true;
    }
    private void verifyCatch() {
        if (verifyTicks<=0 || mc.player==null) return;
        verifyTicks--;
        if (fishCount()>beforeFish) {
            AnkVariantProfiles.stable(activeServer,"auto-fish",activeBiteMode.name());
            verifyTicks=0;lostBites=0;biteForRetrieval=false;
        } else if (verifyTicks==0) {
            // Fish are not every possible loot. Count two empty attempts before
            // switching; this is weak evidence, not proof of a failed module.
            lostBites++;
            biteForRetrieval=false;
            if (lostBites >= 3 && biteMode.get()==BiteMode.Auto) {
                AnkVariantProfiles.correction(activeServer,"auto-fish",activeBiteMode.name());
                String candidate=AnkVariantProfiles.nextUntested(activeServer,"auto-fish",activeBiteMode.name());
                try { activeBiteMode=BiteMode.valueOf(candidate); }
                catch (IllegalArgumentException ignored) {}
                lostBites=0;
            }
        }
    }
    @Override
    public String getInfoString() { return selectedBiteMode().name(); }""")
patch(FISH,"""    public void onActivate() {
        castDelayLeft = 0.0;""",
"""    public void onActivate() {
        activeServer=AnkVariantProfiles.serverKey(mc);
        activeBiteMode=BiteMode.MeteorEntity;
        if (biteMode.get()==BiteMode.Auto) {
            try {activeBiteMode=BiteMode.valueOf(AnkVariantProfiles.choose(activeServer,"auto-fish"));}
            catch(IllegalArgumentException ignored){}
        }
        tickCounter=0;verifyTicks=0;lostBites=0;biteForRetrieval=false;
        castDelayLeft = 0.0;""")
patch(FISH,"""    private void onTick(TickEvent.Pre event) {
        int bestRodSlot = findBestRod();""",
"""    private void onTick(TickEvent.Pre event) {
        tickCounter++;
        verifyCatch();
        int bestRodSlot = findBestRod();""")
patch(FISH,"""            if (((FishingBobberEntityAccessor) mc.player.fishHook).meteor$hasCaughtFish()) {
                catchDelayLeft = randomizeDelay(catchDelay.get(), catchDelayVariance.get());
                wasHooked = true;
            }""",
"""            boolean bite = selectedBiteMode()==BiteMode.MeteorEntity
                ? ((FishingBobberEntityAccessor) mc.player.fishHook).meteor$hasCaughtFish()
                : mc.player.fishHook.getVelocity().y < -0.12
                    && mc.player.fishHook.isTouchingWater();
            if (bite) {
                catchDelayLeft = randomizeDelay(catchDelay.get(), catchDelayVariance.get());
                wasHooked = true;
            }""")
patch(FISH,"""        useRod();
    }

    private void useRod() {""",
"""        onBiteAttempt();
        useRod();
    }

    private void useRod() {""")

# ThunderHack-Recode AntiAFK: add an alternate timed mode to the actual single AntiAFK.
# https://github.com/Pan4ur/ThunderHack-Recode GPL-3.0.
AFK="systems/modules/player/AntiAFK.java"
patch(AFK,"""    private final SettingGroup sgActions = settings.createGroup("Actions");""",
"""    private final SettingGroup sgActions = settings.createGroup("Actions");
    // Adapted from ThunderHack Recode AntiAFK's idle-gating and rotation policy.
    public enum Implementation { Meteor, ThunderTimed }
    private final Setting<Implementation> implementation=sgActions.add(new EnumSetting.Builder<Implementation>()
        .name("implementation").description("Meteor actions or ThunderHack-style idle actions.")
        .defaultValue(Implementation.Meteor).build());
    private int idleTicks, thunderTicks;
    private double previousX=Double.NaN, previousZ=Double.NaN;
    private float previousYaw;
    private final Setting<Integer> idleSeconds=sgActions.add(new IntSetting.Builder()
        .name("thunder-idle-seconds")
        .description("Wait this many seconds with no manual movement before acting.")
        .defaultValue(10).min(2).sliderMax(90)
        .visible(() -> implementation.get()==Implementation.ThunderTimed).build());""")
patch(AFK,"""    public void onActivate() {
        if (sendMessages.get() && messages.get().isEmpty()) {""",
"""    public void onActivate() {
        idleTicks=0;thunderTicks=0;previousX=Double.NaN;previousZ=Double.NaN;
        if (mc.player!=null) previousYaw=mc.player.getYaw();
        if (sendMessages.get() && messages.get().isEmpty()) {""")
patch(AFK,"""    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;""",
"""    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;
        if (implementation.get()==Implementation.ThunderTimed) {
            if (mc.currentScreen!=null) return;
            boolean userInput=mc.options.forwardKey.isPressed()
                ||mc.options.backKey.isPressed()||mc.options.leftKey.isPressed()
                ||mc.options.rightKey.isPressed()||mc.options.jumpKey.isPressed();
            if (!Double.isNaN(previousX)) {
                userInput |= Math.abs(mc.player.getX()-previousX)>0.04
                    || Math.abs(mc.player.getZ()-previousZ)>0.04
                    || Math.abs(mc.player.getYaw()-previousYaw)>0.1;
            }
            previousX=mc.player.getX();previousZ=mc.player.getZ();
            previousYaw=mc.player.getYaw();
            if (userInput) idleTicks=0;
            else idleTicks++;
            if (idleTicks < idleSeconds.get()*20) return;
            // Only an occasional rotation/jump, never unsolicited chat commands.
            thunderTicks++;
            if (thunderTicks%100==0) {
                mc.player.setYaw(mc.player.getYaw()+5);
                previousYaw=mc.player.getYaw();
            }
            if (thunderTicks%240==0 && mc.player.isOnGround()) mc.player.jump();
            return;
        }""")
# Document provenance in the actual corresponding source.
notice=Path("upstream/ANKHACK_LICENSE_AND_ATTRIBUTIONS.md")
with notice.open("a") as out:
    out.write("""
Further native integrations:
- Wurst AutoFish: Wurst-origin bobber motion bite detection adaptation (GPL-3.0).
- LiquidBounce Sprint: legitimately moving / omnidirectional client sprint logic
  adapted to the existing Sprint module (GPL-3.0).
- ThunderHack Recode AntiAFK: idle-gating and periodic actions adapted
  to existing AntiAFK module (GPL-3.0).
These are not complete ports of the original client features or their GUIs.
""")
print("Integrated LiquidBounce Sprint, Wurst AutoFish and ThunderHack AntiAFK ideas in native original modules.")
