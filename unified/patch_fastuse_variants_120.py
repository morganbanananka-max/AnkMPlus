#!/usr/bin/env python3
"""Integrate ThunderHack Reborn 1.21.11 FastUse into Meteor FastUse.
One real module, two distinct item filters and cooldown implementations.
Auto selects saved per-world/server evidence; success heuristics are not
claims that the client's speed beats server-side protections. GPL-3.0.
"""
from pathlib import Path
p=Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules/player/FastUse.java")
s=p.read_text()

def edit(old,new):
    global s
    assert s.count(old)==1,(old,s.count(old))
    s=s.replace(old,new,1)

edit("import meteordevelopment.meteorclient.settings.*;",
"""import meteordevelopment.meteorclient.AnkVariantProfiles;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;""")

edit("    private final SettingGroup sgGeneral = settings.getDefaultGroup();",
"""    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // Meteor filters arbitrary item lists. ThunderHack has explicit block/
    // crystal/experience-bottle filters and its own cooldown.
    public enum Implementation { Auto, Meteor, Thunder }
    private final Setting<Implementation> implementation = sgGeneral.add(new EnumSetting.Builder<Implementation>()
        .name("implementation")
        .description("Meteor item-list logic or ThunderHack's item-category logic; Auto uses per-server observations.")
        .defaultValue(Implementation.Auto).build());

    private final Setting<Integer> thunderDelay = sgGeneral.add(new IntSetting.Builder()
        .name("thunder-delay").description("Cooldown in ticks for Thunder implementation.")
        .defaultValue(0).range(0,5).sliderRange(0,5)
        .visible(() -> implementation.get()!=Implementation.Meteor).build());

    private final Setting<Boolean> thunderAll = sgGeneral.add(new BoolSetting.Builder()
        .name("thunder-all").description("Thunder implementation affects all items.")
        .defaultValue(true).visible(() -> implementation.get()!=Implementation.Meteor).build());
    private final Setting<Boolean> thunderBlocks = sgGeneral.add(new BoolSetting.Builder()
        .name("thunder-blocks").description("Thunder implementation affects blocks.")
        .defaultValue(false).visible(() -> implementation.get()!=Implementation.Meteor && !thunderAll.get()).build());
    private final Setting<Boolean> thunderCrystals = sgGeneral.add(new BoolSetting.Builder()
        .name("thunder-crystals").description("Thunder implementation affects end crystals.")
        .defaultValue(false).visible(() -> implementation.get()!=Implementation.Meteor && !thunderAll.get()).build());
    private final Setting<Boolean> thunderXp = sgGeneral.add(new BoolSetting.Builder()
        .name("thunder-experience-bottles").description("Thunder implementation affects experience bottles.")
        .defaultValue(false).visible(() -> implementation.get()!=Implementation.Meteor && !thunderAll.get()).build());

    private Implementation chosen=Implementation.Meteor;
    private String server="unknown";
    private int priorCount=-1, unchangedTicks=0, rejectedTrials=0, switches=0;
    private Item priorItem=null;

    private Implementation current() {
        return implementation.get()==Implementation.Auto ? chosen : implementation.get();
    }
    @Override public String getInfoString() {
        return implementation.get()==Implementation.Auto ? "Auto: "+chosen.name() : current().name();
    }
    @Override public void onActivate() {
        server=AnkVariantProfiles.serverKey(mc);
        chosen=Implementation.Meteor;
        if (implementation.get()==Implementation.Auto) {
            try {chosen=Implementation.valueOf(AnkVariantProfiles.choose(server,"fast-use"));}
            catch (IllegalArgumentException ignored) {}
        }
        priorCount=-1;priorItem=null;unchangedTicks=0;rejectedTrials=0;switches=0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (implementation.get()!=Implementation.Auto || mc.player==null || mc.world==null) return;
        ItemStack stack=mc.player.getMainHandStack();
        boolean assess=(stack.getItem() instanceof BlockItem || stack.isOf(Items.EXPERIENCE_BOTTLE))
            && !mc.player.getAbilities().creativeMode && mc.options.useKey.isPressed();
        if (!assess || priorItem!=stack.getItem()) {
            priorItem=stack.getItem();priorCount=stack.getCount();unchangedTicks=0;return;
        }
        if (stack.getCount()<priorCount) {
            AnkVariantProfiles.stable(server,"fast-use",chosen.name());
            unchangedTicks=0;rejectedTrials=0;
        } else if (++unchangedTicks>=100) {
            // No observed consumption while use was held for ~5 seconds.
            // Terrain/collision may explain it; require two such observations
            // before switching and only permit one switch per session.
            unchangedTicks=0;rejectedTrials++;
            if (rejectedTrials>=2 && switches<1) {
                AnkVariantProfiles.correction(server,"fast-use",chosen.name());
                String next=AnkVariantProfiles.nextUntested(server,"fast-use",chosen.name());
                try {chosen=Implementation.valueOf(next);}catch(IllegalArgumentException ignored){}
                rejectedTrials=0;switches++;
            }
        }
        priorCount=stack.getCount();
    }""")

edit("""    public int getItemUseCooldown(ItemStack itemStack) {
        if (mode.get() == Mode.All || shouldWorkSome(itemStack)) {
            return cooldown.get();
        }
        return 4; //default cooldown
    }""",
"""    public int getItemUseCooldown(ItemStack itemStack) {
        if (current()==Implementation.Thunder) {
            Item item=itemStack.getItem();
            if (thunderAll.get() || (thunderBlocks.get() && item instanceof BlockItem)
                || (thunderCrystals.get() && item==Items.END_CRYSTAL)
                || (thunderXp.get() && item==Items.EXPERIENCE_BOTTLE))
                return thunderDelay.get();
            return 4;
        }
        if (mode.get() == Mode.All || shouldWorkSome(itemStack)) return cooldown.get();
        return 4;
    }""")
p.write_text(s)

profiles=Path("upstream/src/main/java/meteordevelopment/meteorclient/AnkVariantProfiles.java")
r=profiles.read_text()
anchor='case "sprint" -> new String[]{"Strict","Rage","LiquidLegit","LiquidOmni"};'
assert r.count(anchor)==1
r=r.replace(anchor,anchor+'\n            case "fast-use" -> new String[]{"Meteor","Thunder"};')
profiles.write_text(r)

with Path("upstream/ANKHACK_LICENSE_AND_ATTRIBUTIONS.md").open("a") as f:
    f.write("""\nThunderHack Reborn 1.21.11 FastUse GPL-3.0: independent item-category
    and cooldown implementation within the one Meteor FastUse module.
    Auto selection per-server uses weak block/XP inventory-change evidence,
    which cannot prove a variant bypasses server protections.\n""")
print("Integrated Meteor/Thunder FastUse variants and conservative server-profile Auto.")
