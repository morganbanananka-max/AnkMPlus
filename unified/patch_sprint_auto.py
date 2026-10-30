#!/usr/bin/env python3
"""Empirical Sprint Auto selector using existing AnkHack server-specific scores.
Not a claim that a server will permit every sprint or movement technique.
"""
from pathlib import Path
p=Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules/movement/Sprint.java")
s=p.read_text()
def edit(old,new,expected=1):
 global s
 actual=s.count(old)
 if actual!=expected: raise RuntimeError(f"Sprint patch expected {expected}, found {actual}: {old[:90]}")
 s=s.replace(old,new)
edit("import meteordevelopment.meteorclient.events.packets.PacketEvent;",
"""import meteordevelopment.meteorclient.AnkVariantProfiles;
import meteordevelopment.meteorclient.events.packets.PacketEvent;""")
edit("import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;",
"""import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;""")
edit("    public enum Mode {\n        Strict,",
"""    public enum Mode {
        Auto,
        Strict,""")
edit('        .defaultValue(Mode.Strict)','        .defaultValue(Mode.Auto)')
edit("""    public Sprint() {
        super(Categories.Movement, "sprint", "Automatically sprints.");
    }""",
"""    public Sprint() {
        super(Categories.Movement, "sprint",
            "Meteor/LiquidBounce sprint variants with local server-based Auto selection.");
    }

    private Mode chosen=Mode.Strict;
    private String profileServer="unknown";
    private int trialCount=0;
    private int validTicks=0;
    private int correctionCount=0;
    private double motionSum=0,previousX=Double.NaN,previousZ=Double.NaN;

    private Mode chosenMode(){return mode.get()==Mode.Auto?chosen:mode.get();}

    @Override
    public String getInfoString(){
        return mode.get()==Mode.Auto ? "Auto: " + chosen.name() : chosenMode().name();
    }

    @Override
    public void onActivate() {
        profileServer=AnkVariantProfiles.serverKey(mc);
        chosen=Mode.Strict;
        try {chosen=Mode.valueOf(AnkVariantProfiles.choose(profileServer,"sprint"));}
        catch(IllegalArgumentException ignored){}
        validTicks=0;trialCount=0;motionSum=0;correctionCount=0;
        previousX=Double.NaN;previousZ=Double.NaN;
    }

    private void observeSprint() {
        if(mode.get()!=Mode.Auto||mc.player==null||mc.world==null) return;
        double xx=mc.player.getX(),zz=mc.player.getZ();
        if(!Double.isNaN(previousX)) {
            double dx=xx-previousX,dz=zz-previousZ;
            double speed=Math.sqrt(dx*dx+dz*dz);
            // Sample only comparable player-controlled runs on solid ground.
            if(mc.currentScreen==null && mc.player.isOnGround()
                && !mc.player.isTouchingWater() && !mc.player.hasVehicle()
                && mc.player.forwardSpeed>0.8f
                && speed>0.025 && speed<0.8) {
                motionSum+=speed;
                validTicks++;
            }
        }
        previousX=xx;previousZ=zz;
        if(validTicks<130) return;
        if(profileServer.equals("unknown")){validTicks=0;motionSum=0;return;}
        double mean=motionSum/Math.max(1,validTicks);
        AnkVariantProfiles.measuredSpeed(profileServer,"sprint",chosen.name(),mean);
        validTicks=0;motionSum=0;
        if(trialCount++>=3) {
            try {chosen=Mode.valueOf(AnkVariantProfiles.choose(profileServer,"sprint"));}
            catch(IllegalArgumentException ignored){chosen=Mode.Strict;}
            return;
        }
        String next=AnkVariantProfiles.nextUntested(profileServer,"sprint",chosen.name());
        if(next.equals(chosen.name()))return;
        try {chosen=Mode.valueOf(next);}
        catch(IllegalArgumentException ignored){chosen=Mode.Strict;}
        this.info("Sprint Auto testing %s; previous measured %.3f blocks/tick.",chosen.name(),mean);
    }

    @EventHandler
    private void onSetback(PacketEvent.Receive event){
        if(mode.get()!=Mode.Auto || mc.player==null) return;
        if(!(event.packet instanceof PlayerPositionLookS2CPacket))return;
        if(validTicks<25)return;
        AnkVariantProfiles.correction(profileServer,"sprint",chosen.name());
        correctionCount++;
        if(correctionCount>=2){
            correctionCount=0;
            chosen=Mode.Strict;
            validTicks=0;motionSum=0;
            this.warning("Sprint Auto observed corrections; using safe Strict variant.");
        }
    }""")
edit("""        mc.player.setSprinting(shouldSprint());
    }""",
"""        mc.player.setSprinting(shouldSprint());
        observeSprint();
    }""")
# All comparisons inside shouldSprint and rageSprint should respect the resolved Auto variant.
split=s.index("    public boolean shouldSprint() {")
prefix=s[:split]
tail=s[split:].replace("mode.get() == Mode.", "chosenMode() == Mode.")
s=prefix+tail
assert "return isActive()" in s and "LiquidOmni" in s and "Auto" in s
p.write_text(s)
print("Native Sprint Auto selector now empirically tests available Meteor and LiquidBounce behaviors.")
