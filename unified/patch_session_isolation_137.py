"""Reset Auto profiles when switching worlds or servers with modules active.

Observed apple counts, arrow consumption, sprint distances and preferred
implementations must not cross world boundaries. No extra packets or ticks.
"""
from pathlib import Path

base=Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules")
def one(path,old,new):
    p=base/path
    data=p.read_text(encoding="utf-8")
    if data.count(old)!=1:
        raise RuntimeError(f"Session isolation missing exact anchor in {path}: {data.count(old)} {old[:70]}")
    p.write_text(data.replace(old,new,1),encoding="utf-8")

one("movement/Sprint.java",
'''    private void onTickMovement(TickEvent.Post event) {
        if (unsprintInWater.get() && mc.player.isTouchingWater()) return;''',
'''    private void onTickMovement(TickEvent.Post event) {
        if(mc.player==null || mc.world==null) return;
        String connected=AnkVariantProfiles.serverKey(mc);
        if(!connected.equals(profileServer)) {
            profileServer=connected;
            chosen=Mode.Strict;
            if(mode.get()==Mode.Auto) {
                try {chosen=Mode.valueOf(AnkVariantProfiles.choose(profileServer,"sprint"));}
                catch(IllegalArgumentException ignored){}
            }
            validTicks=0;trialCount=0;correctionCount=0;motionSum=0;
            previousX=Double.NaN;previousZ=Double.NaN;
        }
        if (unsprintInWater.get() && mc.player.isTouchingWater()) return;''')

one("player/AutoGap.java",
'''    private void onTick(TickEvent.Pre event) {
        if(mc.player==null || mc.world==null)return;
        if(current()==Implementation.ThunderOffhand){thunderTick();return;}''',
'''    private void onTick(TickEvent.Pre event) {
        if(mc.player==null || mc.world==null)return;
        String connected=AnkVariantProfiles.serverKey(mc);
        if(!connected.equals(server)) {
            if(thunderEating)mc.options.useKey.setPressed(false);
            if(eating)stopEating();
            thunderEating=false;previousApples=-1;recentEatingTicks=0;
            server=connected;chosen=Implementation.Meteor;
            if(implementation.get()==Implementation.Auto) {
                try{chosen=Implementation.valueOf(AnkVariantProfiles.choose(server,"auto-gap"));}
                catch(IllegalArgumentException ignored){}
            }
        }
        if(current()==Implementation.ThunderOffhand){thunderTick();return;}''')

one("combat/BowSpam.java",
'''    private void onTick(TickEvent.Pre event) {
        if(mc.player==null || mc.world==null || mc.interactionManager==null)return;
        if(current()==Implementation.Thunder) {''',
'''    private void onTick(TickEvent.Pre event) {
        if(mc.player==null || mc.world==null || mc.interactionManager==null)return;
        String connected=AnkVariantProfiles.serverKey(mc);
        if(!connected.equals(server)) {
            setPressed(false);
            server=connected;active=Implementation.Meteor;
            pendingAmmo=-1;pendingTicks=0;
            wasBow=false;wasHoldingRightClick=false;
            if(implementation.get()==Implementation.Auto) {
                try{active=Implementation.valueOf(AnkVariantProfiles.choose(server,"bow-spam"));}
                catch(IllegalArgumentException ignored){}
            }
        }
        if(current()==Implementation.Thunder) {''')
print("Auto Sprint, AutoGap and BowSpam now reset session state across server/world transitions.")
