#!/usr/bin/env python3
"""Native ThunderHack Reborn BowSpam behavior within Meteor's existing BowSpam.

One shared module, real Meteor and ThunderHk variants, manual selection and
per-world observational Auto. GPL-3.0 source adaptation. Does not claim
anti-cheat bypass or server-side confirmation; Arrow consumption is weak evidence.
"""
from pathlib import Path

path=Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules/combat/BowSpam.java")
s=path.read_text()
def edit(old,new):
    global s
    n=s.count(old)
    assert n==1, (old[:100],n)
    s=s.replace(old,new,1)

edit("import meteordevelopment.meteorclient.events.world.TickEvent;",
"""import meteordevelopment.meteorclient.AnkVariantProfiles;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;""")

edit("    private final SettingGroup sgCrossbows = settings.createGroup(\"Crossbows\");",
"""    private final SettingGroup sgCrossbows = settings.createGroup("Crossbows");
    private final SettingGroup sgVariants = settings.createGroup("AnkHack variants");

    public enum Implementation { Auto, Meteor, Thunder }

    private final Setting<Implementation> implementation = sgVariants.add(new EnumSetting.Builder<Implementation>()
        .name("implementation").description("Meteor auto-charging or ThunderHack's manual-use release and restart.")
        .defaultValue(Implementation.Auto).build());
    private final Setting<Integer> thunderDelay = sgVariants.add(new IntSetting.Builder()
        .name("thunder-delay").description("Bow charge time for the ThunderHack implementation.")
        .defaultValue(3).range(1,20).sliderRange(1,20)
        .visible(() -> implementation.get()!=Implementation.Meteor).build());

    private Implementation active=Implementation.Meteor;
    private String server="unknown";
    private int pendingAmmo=-1, pendingTicks;
    private Implementation pendingVariant=Implementation.Meteor;

    private Implementation current() {
        return implementation.get()==Implementation.Auto ? active : implementation.get();
    }
    @Override public String getInfoString() {
        return implementation.get()==Implementation.Auto ? "Auto: "+active.name() : current().name();
    }
    private int arrows() {
        if (mc.player==null)return 0;
        int total=0;
        for(int i=0;i<36;i++){
            ItemStack stack=mc.player.getInventory().getStack(i);
            if(stack.getItem() instanceof ArrowItem)total+=stack.getCount();
        }
        return total;
    }
    private void observeRelease() {
        if (mc.player==null || mc.player.getAbilities().creativeMode)return;
        pendingAmmo=arrows();pendingTicks=18;pendingVariant=current();
    }
    @EventHandler private void observeArrowConsumption(TickEvent.Post event) {
        if(pendingTicks<=0 || mc.player==null)return;
        pendingTicks--;
        if(arrows()<pendingAmmo) {
            if (implementation.get()==Implementation.Auto)
                AnkVariantProfiles.stable(server,"bow-spam",pendingVariant.name());
            pendingTicks=0;
        }
    }""")

edit("""    public void onActivate() {
        wasBow = false;
        wasHoldingRightClick = false;
    }""",
"""    public void onActivate() {
        wasBow = false;
        wasHoldingRightClick = false;
        pendingTicks=0; pendingAmmo=-1;
        server=AnkVariantProfiles.serverKey(mc);
        active=Implementation.Meteor;
        if(implementation.get()==Implementation.Auto) {
            try {active=Implementation.valueOf(AnkVariantProfiles.choose(server,"bow-spam"));}
            catch(IllegalArgumentException ignored) {}
        }
    }""")

edit("""    private void onTick(TickEvent.Pre event) {
        FindItemResult crossbow""",
"""    private void onTick(TickEvent.Pre event) {
        if(mc.player==null || mc.world==null || mc.interactionManager==null)return;
        if(current()==Implementation.Thunder) {
            // ThunderHack's bow-specific charge/release/restart cycle.
            // It requires an already active, manually held bow; unlike
            // Meteor's variant it does not synthesize a held-use key.
            if(!mc.player.isUsingItem())return;
            Hand hand=mc.player.getActiveHand();
            if(!mc.player.getStackInHand(hand).isOf(Items.BOW))return;
            if(mc.player.getItemUseTime() >= thunderDelay.get()) {
                observeRelease();
                mc.interactionManager.stopUsingItem(mc.player);
                mc.interactionManager.interactItem(mc.player,hand);
            }
            return;
        }
        FindItemResult crossbow""")
edit("""            if (mc.player.getItemUseTime() >= charge.get()) {
                mc.interactionManager.stopUsingItem(mc.player);""",
"""            if (mc.player.getItemUseTime() >= charge.get()) {
                observeRelease();
                mc.interactionManager.stopUsingItem(mc.player);""")
path.write_text(s)
profiles=Path("upstream/src/main/java/meteordevelopment/meteorclient/AnkVariantProfiles.java")
p=profiles.read_text()
anchor='case "fast-use" -> new String[]{"Meteor","Thunder"};'
assert p.count(anchor)==1, "Unknown variant schema; do not register a fake alternative"
p=p.replace(anchor,anchor+'\n            case "bow-spam" -> new String[]{"Meteor","Thunder"};',1)
profiles.write_text(p)
with Path("upstream/ANKHACK_LICENSE_AND_ATTRIBUTIONS.md").open("a",encoding="utf-8") as f:
    f.write("""
ThunderHack Reborn 1.21.11 BowSpam GPL-3.0: manual bow-use charge/release/
restart behavior integrated into the original Meteor BowSpam module. The
Auto profile uses only observed survival-arrow consumption as weak evidence,
not server-proof effectiveness. Infinity bows cannot provide this signal.
""")
print("Integrated Meteor and ThunderHack BowSpam in one registered module.")
