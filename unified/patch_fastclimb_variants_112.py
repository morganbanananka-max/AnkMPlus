#!/usr/bin/env python3
"""Add genuine Wurst 7 FastLadder behavior as another mode of Meteor FastClimb.
Keep existing Meteor timer and velocity methods intact and native.
"""
from pathlib import Path
base=Path("upstream/src/main/java/meteordevelopment/meteorclient")
p=base/"systems/modules/movement/FastClimb.java"
s=p.read_text()
def edit(a,b):
 global s
 if s.count(a)!=1:raise RuntimeError(f"FastClimb anchor not unique: {a[:75]}")
 s=s.replace(a,b,1)
edit("import meteordevelopment.meteorclient.events.world.TickEvent;",
"""import meteordevelopment.meteorclient.AnkVariantProfiles;
import meteordevelopment.meteorclient.events.world.TickEvent;""")
edit("import meteordevelopment.meteorclient.settings.DoubleSetting;",
"""import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;""")
edit("    private boolean resetTimer;",
"""    public enum Implementation { Auto, MeteorModern, WurstClassic }
    private final Setting<Implementation> implementation=sgGeneral.add(
        new EnumSetting.Builder<Implementation>()
        .name("implementation")
        .description("Meteor allows modern climb situations; Wurst requires a wall collision.")
        .defaultValue(Implementation.Auto).build());
    private Implementation active=Implementation.MeteorModern;
    private String server="unknown";
    private double startY;
    private int sampleTicks;
    private int samples;
    private boolean resetTimer;
    private Implementation selected(){
        return implementation.get()==Implementation.Auto?active:implementation.get();
    }
    @Override public String getInfoString(){
        return selected().name();
    }
    private void observe(){
        if(mc.player==null||implementation.get()!=Implementation.Auto)return;
        if(!climbing()){sampleTicks=0;return;}
        if(sampleTicks++==0)startY=mc.player.getY();
        if(sampleTicks<35)return;
        double rise=mc.player.getY()-startY;
        if(rise>0.5)AnkVariantProfiles.stable(server,"fast-climb",active.name());
        else AnkVariantProfiles.correction(server,"fast-climb",active.name());
        sampleTicks=0;
        if(++samples>=3){
            try {active=Implementation.valueOf(AnkVariantProfiles.choose(server,"fast-climb"));}
            catch(IllegalArgumentException ignored){active=Implementation.MeteorModern;}
        } else {
            String alternate=AnkVariantProfiles.nextUntested(server,"fast-climb",active.name());
            try {active=Implementation.valueOf(alternate);}
            catch(IllegalArgumentException ignored){active=Implementation.MeteorModern;}
        }
    }
    private boolean shouldClimb(){
        if(selected()==Implementation.WurstClassic){
            // Direct adaptation of Wurst 7 FastLadderHack: horizontalCollision
            // AND climbable AND player explicitly pressing a movement key.
            return mc.player.horizontalCollision && mc.player.isClimbing()
                && (Math.abs(mc.player.forwardSpeed)>1.0E-5f
                || Math.abs(mc.player.sidewaysSpeed)>1.0E-5f);
        }
        return climbing();
    }""")
edit("""    public void onActivate() {
        resetTimer = false;
    }""",
"""    public void onActivate() {
        resetTimer = false;
        server=AnkVariantProfiles.serverKey(mc);
        active=Implementation.MeteorModern;
        sampleTicks=0;samples=0;
        if(implementation.get()==Implementation.Auto)try {
            active=Implementation.valueOf(AnkVariantProfiles.choose(server,"fast-climb"));
        }catch(IllegalArgumentException ignored){}
    }""")
edit("""    private void onPreTick(TickEvent.Pre event) {
        if (timerMode.get()) {""",
"""    private void onPreTick(TickEvent.Pre event) {
        if(mc.player==null||mc.world==null)return;
        observe();
        if (timerMode.get()) {""")
edit("            if (climbing()) {","            if (shouldClimb()) {")
edit("""        if (!timerMode.get() && climbing()) {""",
"""        if (!timerMode.get() && shouldClimb()) {""")
p.write_text(s)
q=base/"AnkVariantProfiles.java"
s=q.read_text()
a='case "auto-tool" -> new String[]{"MeteorScore","ThunderSpeed"};'
if s.count(a)!=1:raise RuntimeError("profiles anchor")
q.write_text(s.replace(a,a+'\n            case "fast-climb" -> new String[]{"MeteorModern","WurstClassic"};'))
notice=Path("upstream/ANKHACK_LICENSE_AND_ATTRIBUTIONS.md")
with notice.open("a") as f:
 f.write("\nFastClimb: MeteorModern mode retained and Wurst FastLadder motion conditions ported into the same module (Wurst-Imperium GPL-3.0). Auto tests climbing outcomes per server, not guaranteed quality.\n")
print("FastClimb modes: Auto, MeteorModern, WurstClassic.")
