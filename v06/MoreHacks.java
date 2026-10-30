package se.ankclient;

import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BlockItem;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;

/** Additional natively integrated mods. Ported concepts from open-source
 * ThunderHack/Wurst modules; no upstream client or event bus is bundled. */
public final class MoreHacks {
    private MoreHacks(){}
    private static boolean playing(MinecraftClient c){
        return c.world!=null&&c.player!=null&&c.interactionManager!=null&&c.currentScreen==null;
    }
    public static void registerAll(ModuleManager r){
        r.register(new Module("scaffold","Scaffold","Place available held blocks below your feet when support exists",Module.Category.WORLD,false){
            private int cooldown;
            @Override public void tick(MinecraftClient c){
                if(!playing(c)||!(c.player.getMainHandStack().getItem() instanceof BlockItem))return;
                if(cooldown-->0)return;cooldown=NativeOptions.integer("action_delay");
                BlockPos target=c.player.getBlockPos().down();
                if(!c.world.getBlockState(target).isAir())return;
                for(Direction dir:Direction.values()){
                    if(dir==Direction.DOWN)continue;
                    BlockPos adjacent=target.offset(dir);
                    BlockState state=c.world.getBlockState(adjacent);
                    if(state.isAir()||state.getCollisionShape(c.world,adjacent).isEmpty())continue;
                    Direction face=dir.getOpposite();
                    Vec3d hit=Vec3d.ofCenter(adjacent).add(face.getOffsetX()*0.5,face.getOffsetY()*0.5,face.getOffsetZ()*0.5);
                    if(c.interactionManager.interactBlock(c.player,Hand.MAIN_HAND,
                        new BlockHitResult(hit,face,adjacent,false)).isAccepted()){
                        c.player.swingHand(Hand.MAIN_HAND);
                        return;
                    }
                }
            }
        });
        r.register(new Module("player_glow","PlayerESP Glow","Outline other loaded players using client glow",Module.Category.RENDER,false){
            final Set<UUID> modified=new HashSet<>();
            @Override public void tick(MinecraftClient c){
                if(!playing(c))return;
                for(PlayerEntity p:c.world.getPlayers()){
                    if(p==c.player)continue;
                    if(!p.isGlowing()){
                        p.setGlowing(true);
                        modified.add(p.getUuid());
                    }
                }
            }
            @Override public void onDisable(MinecraftClient c){
                if(c!=null&&c.world!=null){
                    for(PlayerEntity p:c.world.getPlayers())
                        if(modified.contains(p.getUuid()))p.setGlowing(false);
                }
                modified.clear();
            }
        });
        r.register(new Module("auto_replenish","AutoReplenish","Replace nearly empty hotbar stacks with matching inventory stacks",Module.Category.AUTOMATION,false){
            private int cooldown;
            @Override public void tick(MinecraftClient c){
                if(!playing(c)||c.player.isCreative()||c.player.currentScreenHandler!=c.player.playerScreenHandler)return;
                if(cooldown-->0)return;cooldown=24;
                int hot=c.player.getInventory().getSelectedSlot();
                ItemStack active=c.player.getInventory().getStack(hot);
                if(active.isEmpty()||active.isDamageable()||active.getCount()>8)return;
                for(int slot=9;slot<36;slot++){
                    ItemStack other=c.player.getInventory().getStack(slot);
                    if(!other.isEmpty() && ItemStack.areItemsAndComponentsEqual(active,other)
                        &&other.getCount()>active.getCount()){
                        c.interactionManager.clickSlot(c.player.playerScreenHandler.syncId,slot,hot,SlotActionType.SWAP,c.player);
                        return;
                    }
                }
            }
        });
        r.register(new Module("auto_pearl","EmergencyPearl","Throw one ender pearl on a long dangerous fall",Module.Category.MOVEMENT,false){
            private int cooldown;
            @Override public void tick(MinecraftClient c){
                if(!playing(c)||!c.player.isAlive())return;
                if(cooldown-->0)return;
                if(c.player.fallDistance<15f||c.player.getVelocity().y>=-0.4)return;
                int slot=-1;
                for(int i=0;i<9;i++)if(c.player.getInventory().getStack(i).isOf(Items.ENDER_PEARL)){slot=i;break;}
                if(slot<0)return;
                int old=c.player.getInventory().getSelectedSlot();
                c.player.getInventory().setSelectedSlot(slot);
                if(c.interactionManager.interactItem(c.player,Hand.MAIN_HAND).isAccepted())
                    c.player.swingHand(Hand.MAIN_HAND);
                c.player.getInventory().setSelectedSlot(old);
                cooldown=130;
            }
        });
        r.register(new Module("offhand_gapple","OffhandGapple","Fill an empty offhand with golden apples",Module.Category.COMBAT,false){
            private int delay;
            @Override public void tick(MinecraftClient c){
                if(!playing(c)||c.player.isCreative()||!c.player.getOffHandStack().isEmpty()
                    ||c.player.currentScreenHandler!=c.player.playerScreenHandler)return;
                if(delay-->0)return;delay=25;
                for(int i=0;i<36;i++){
                    if(!c.player.getInventory().getStack(i).isOf(Items.GOLDEN_APPLE)
                        &&!c.player.getInventory().getStack(i).isOf(Items.ENCHANTED_GOLDEN_APPLE))continue;
                    c.interactionManager.clickSlot(c.player.playerScreenHandler.syncId,i<9?i+36:i,40,
                        SlotActionType.SWAP,c.player);
                    return;
                }
            }
        });
        r.register(new Module("elytra_rocket","ElytraRocket","Use a firework rocket when gliding and sneaking",Module.Category.MOVEMENT,false){
            private int delay;
            @Override public void tick(MinecraftClient c){
                if(!playing(c)||!c.player.isGliding()||!c.options.sneakKey.isPressed())return;
                if(delay-->0)return;delay=30;
                int selected=c.player.getInventory().getSelectedSlot();
                int rocket=-1;
                for(int i=0;i<9;i++)if(c.player.getInventory().getStack(i).isOf(Items.FIREWORK_ROCKET)){rocket=i;break;}
                if(rocket<0)return;
                c.player.getInventory().setSelectedSlot(rocket);
                if(c.interactionManager.interactItem(c.player,Hand.MAIN_HAND).isAccepted())
                    c.player.swingHand(Hand.MAIN_HAND);
                c.player.getInventory().setSelectedSlot(selected);
            }
        });
    }
}
