#!/usr/bin/env python3
"""Merge ThunderHack Reborn's actual AutoTool selection heuristic as a second
native strategy of the original Meteor AutoTool.

The two paths share module lifecycle, GUI, settings, hotbar switching and
blacklist. Auto records observed success when blocks actually break; it does
not claim anticheat compatibility based on guesses.
"""
from pathlib import Path
p=Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules/player/AutoTool.java")
s=p.read_text()
def edit(a,b,count=1):
 global s
 if s.count(a)!=count:raise RuntimeError("AutoTool anchor count "+str(s.count(a))+" for "+a[:120])
 s=s.replace(a,b,count)
edit("import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;",
"""import meteordevelopment.meteorclient.AnkVariantProfiles;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;""")
edit("import net.minecraft.registry.tag.ItemTags;",
"""import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.BlockPos;""")
edit("    private final SettingGroup sgWhitelist = settings.createGroup(\"Whitelist\");",
"""    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");
    private final SettingGroup sgAnk = settings.createGroup("AnkHack variants");

    public enum Implementation { Auto, MeteorScore, ThunderSpeed }
    private final Setting<Implementation> implementation=sgAnk.add(new EnumSetting.Builder<Implementation>()
        .name("implementation")
        .description("Meteor comprehensive tool scoring or ThunderHack simple speed+enchant scoring.")
        .defaultValue(Implementation.Auto).build());
    private final Setting<Boolean> thunderSaveTool=sgAnk.add(new BoolSetting.Builder()
        .name("thunder-save-tool")
        .description("ThunderHack-style protection of tools with 10 or fewer durability.")
        .defaultValue(true).visible(() -> implementation.get()!=Implementation.MeteorScore).build());
    private Implementation active=Implementation.MeteorScore;
    private String server="unknown";
    private BlockPos trackingBlock;
    private int trackingTicks, failedTrial;
    private Implementation selected(){
        return implementation.get()==Implementation.Auto?active:implementation.get();
    }
    @Override public String getInfoString(){return selected().name();}
    @Override public void onActivate(){
        server=AnkVariantProfiles.serverKey(mc);
        active=Implementation.MeteorScore;
        if(implementation.get()==Implementation.Auto)try {
            active=Implementation.valueOf(AnkVariantProfiles.choose(server,"auto-tool"));
        }catch(IllegalArgumentException ignored){}
        trackingBlock=null;trackingTicks=0;failedTrial=0;
    }
    private void sampleToolSuccess() {
        if (trackingBlock==null || mc.world==null || mc.player==null) return;
        if (mc.world.getBlockState(trackingBlock).isAir()) {
            AnkVariantProfiles.stable(server,"auto-tool",active.name());
            trackingBlock=null;failedTrial=0;return;
        }
        if (++trackingTicks<100)return;
        // If the player released mining early this is not a tool failure.
        if(mc.options.attackKey.isPressed()){
            AnkVariantProfiles.correction(server,"auto-tool",active.name());
            if(++failedTrial>=2 && implementation.get()==Implementation.Auto){
                String candidate=AnkVariantProfiles.nextUntested(server,"auto-tool",active.name());
                try {active=Implementation.valueOf(candidate);}
                catch(IllegalArgumentException ignored){active=Implementation.MeteorScore;}
                failedTrial=0;
            }
        }
        trackingBlock=null;
    }
    private double thunderScore(ItemStack stack, BlockState state) {
        if (!isTool(stack) || stack.isEmpty())return -1;
        if(thunderSaveTool.get() && stack.isDamageable()
            && stack.getMaxDamage()-stack.getDamage()<=10)return -1;
        if(!stack.isSuitableFor(state))return -1;
        if(state.getBlock()==Blocks.ENDER_CHEST && silkTouchForEnderChest.get()
            && !Utils.hasEnchantments(stack,Enchantments.SILK_TOUCH))return -1;
        return stack.getMiningSpeedMultiplier(state)
            + Utils.getEnchantmentLevel(stack,Enchantments.EFFICIENCY);
    }""")
edit("""    private void onTick(TickEvent.Post event) {
        if (Modules.get().isActive(InfinityMiner.class)) return;""",
"""    private void onTick(TickEvent.Post event) {
        sampleToolSuccess();
        if (Modules.get().isActive(InfinityMiner.class)) return;""")
edit("""        // Check if we should switch to a better tool
        ItemStack currentStack = mc.player.getMainHandStack();""",
"""        // Check if we should switch to a better tool
        ItemStack currentStack = mc.player.getMainHandStack();
        if(implementation.get()==Implementation.Auto && trackingBlock==null){
            trackingBlock=event.blockPos.toImmutable();
            trackingTicks=0;
        }""")
edit("""            double score = getScore(itemStack, blockState, silkTouchForEnderChest.get(), fortuneForOresCrops.get(), prefer.get(), itemStack2 -> !shouldStopUsing(itemStack2));""",
"""            double score = selected()==Implementation.ThunderSpeed
                ? thunderScore(itemStack,blockState)
                : getScore(itemStack, blockState, silkTouchForEnderChest.get(),
                    fortuneForOresCrops.get(), prefer.get(), itemStack2 -> !shouldStopUsing(itemStack2));""")
edit("""        if ((bestSlot != -1 && (bestScore > getScore(currentStack, blockState, silkTouchForEnderChest.get(), fortuneForOresCrops.get(), prefer.get(), itemStack -> !shouldStopUsing(itemStack))) || shouldStopUsing(currentStack) || !isTool(currentStack))) {""",
"""        double currentScore = selected()==Implementation.ThunderSpeed
            ? thunderScore(currentStack,blockState)
            : getScore(currentStack,blockState,silkTouchForEnderChest.get(),
                fortuneForOresCrops.get(),prefer.get(), itemStack -> !shouldStopUsing(itemStack));
        if (bestSlot != -1 && (bestScore > currentScore || shouldStopUsing(currentStack) || !isTool(currentStack))) {""")
p.write_text(s)
q=Path("upstream/src/main/java/meteordevelopment/meteorclient/AnkVariantProfiles.java")
s=q.read_text()
needle='case "auto-eat" -> new String[]{"MeteorSmart","ThunderFast"};'
if s.count(needle)!=1:raise RuntimeError("Missing variant family: "+needle)
q.write_text(s.replace(needle,needle+'\n            case "auto-tool" -> new String[]{"MeteorScore","ThunderSpeed"};'))
with Path("upstream/ANKHACK_LICENSE_AND_ATTRIBUTIONS.md").open("a") as f:
 f.write("\nAutoTool: original Meteor scoring retained, ThunderHack Reborn 1.21.11 speed+Efficiency scoring ported under the same module. Empirical Auto feedback is heuristic and does not prove superiority; GPL-3.0 attribution retained.\n")
print("AutoTool includes Auto, MeteorScore and ThunderSpeed; no duplicate module entries.")
