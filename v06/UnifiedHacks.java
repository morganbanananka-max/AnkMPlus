package se.ankclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * Native gameplay modules. Adapted ideas: ThunderHack Reborn's ChestStealer,
 * AutoFish and AntiAFK (GPL-3.0), implemented against AnkHack's one registry.
 * No other client's mod initializer, event bus, GUI or internal runtime.
 */
public final class UnifiedHacks {
    private UnifiedHacks(){}
    private static boolean ready(MinecraftClient c){
        return c.player!=null && c.world!=null && c.interactionManager!=null && c.currentScreen==null;
    }
    public static void registerAll(ModuleManager r){
        r.register(new Module("chest_stealer","ChestStealer","Take items from an open chest, one stack each interval",Module.Category.AUTOMATION,false){
            int wait;
            @Override public void tick(MinecraftClient c){
                if(c.player==null || c.interactionManager==null || !(c.currentScreen instanceof GenericContainerScreen)
                    || !(c.player.currentScreenHandler instanceof GenericContainerScreenHandler chest))return;
                if(wait-- > 0)return;wait=NativeOptions.integer("action_delay");
                int slots=chest.getRows()*9;
                for(int i=0;i<slots;i++){
                    if(chest.getSlot(i).hasStack()){
                        c.interactionManager.clickSlot(chest.syncId,i,0,SlotActionType.QUICK_MOVE,c.player);
                        return;
                    }
                }
            }
        });
        r.register(new Module("killaura","KillAura","Attack nearby hostile mobs only, at weapon cooldown",Module.Category.COMBAT,false){
            @Override public void tick(MinecraftClient c){
                if(!ready(c) || c.player.getAttackCooldownProgress(0)<0.97f)return;
                HostileEntity best=null;double dist=12.25;
                for(Entity e:c.world.getEntities()){
                    if(!(e instanceof HostileEntity h) || !h.isAlive() || !c.player.canSee(h))continue;
                    double d=c.player.squaredDistanceTo(h);
                    if(d<dist){dist=d;best=h;}
                }
                if(best!=null){c.interactionManager.attackEntity(c.player,best);c.player.swingHand(Hand.MAIN_HAND);}
            }
        });
        r.register(new Module("pvp_aura","PvPAura","Attack nearby players when active; respects normal cooldown",Module.Category.COMBAT,false){
            @Override public void tick(MinecraftClient c){
                if(!ready(c)||c.player.getAttackCooldownProgress(0)<0.98f)return;
                PlayerEntity best=null;double dmin=12.25;
                for(PlayerEntity p:c.world.getPlayers()){
                    if(p==c.player || !p.isAlive() || p.isSpectator() || !c.player.canSee(p))continue;
                    double d=c.player.squaredDistanceTo(p);
                    if(d<dmin){dmin=d;best=p;}
                }
                if(best!=null){c.interactionManager.attackEntity(c.player,best);c.player.swingHand(Hand.MAIN_HAND);}
            }
        });
        r.register(new Module("crystal_aura","CrystalAura","Break visible end crystals within melee range",Module.Category.COMBAT,false){
            @Override public void tick(MinecraftClient c){
                if(!ready(c)||c.player.getAttackCooldownProgress(0)<0.97f)return;
                EndCrystalEntity target=null;double best=12.25;
                for(Entity e:c.world.getEntities()){
                    if(!(e instanceof EndCrystalEntity crystal)||!crystal.isAlive()||!c.player.canSee(crystal))continue;
                    double d=c.player.squaredDistanceTo(crystal);
                    if(d<best){target=crystal;best=d;}
                }
                if(target!=null){c.interactionManager.attackEntity(c.player,target);c.player.swingHand(Hand.MAIN_HAND);}
            }
        });
        r.register(new Module("aim_assist","AimAssist","Gently move aim toward nearby player when attack is held",Module.Category.COMBAT,false){
            @Override public void tick(MinecraftClient c){
                if(!ready(c)||!c.options.attackKey.isPressed())return;
                PlayerEntity target=null;double bestAngle=15.0;
                Vec3d from=c.player.getEyePos();
                for(PlayerEntity p:c.world.getPlayers()){
                    if(p==c.player||!p.isAlive()||p.isSpectator()||!c.player.canSee(p)||c.player.squaredDistanceTo(p)>25)continue;
                    Vec3d d=p.getEyePos().subtract(from);
                    double yaw=Math.toDegrees(Math.atan2(d.z,d.x))-90;
                    double diff=Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees((float)(yaw-c.player.getYaw())));
                    if(diff<bestAngle){bestAngle=diff;target=p;}
                }
                if(target!=null){
                    Vec3d d=target.getEyePos().subtract(from);
                    float yaw=(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90);
                    float pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z)));
                    c.player.setYaw(c.player.getYaw()+net.minecraft.util.math.MathHelper.wrapDegrees(yaw-c.player.getYaw())*0.18f);
                    c.player.setPitch(c.player.getPitch()+(pitch-c.player.getPitch())*0.15f);
                }
            }
        });
        r.register(new Module("anti_afk","AntiAFK","Make a periodic hand swing while idle",Module.Category.UTILITY,false){
            private int ticks;
            @Override public void tick(MinecraftClient c){
                if(!ready(c))return;
                if(++ticks>=500){ticks=0;c.player.swingHand(Hand.MAIN_HAND);}
            }
        });
        r.register(new Module("auto_fish","AutoFish","Cast and retrieve the fishing rod after bobber dips",Module.Category.AUTOMATION,false){
            private int cooldown=0,air=0;
            @Override public void tick(MinecraftClient c){
                if(!ready(c)||!c.player.getMainHandStack().isOf(Items.FISHING_ROD))return;
                if(cooldown>0){cooldown--;return;}
                FishingBobberEntity bobber=c.player.fishHook;
                if(bobber==null){
                    if(c.interactionManager.interactItem(c.player,Hand.MAIN_HAND).isAccepted())c.player.swingHand(Hand.MAIN_HAND);
                    cooldown=25;air=0;
                }else if(bobber.isTouchingWater() && bobber.getVelocity().y < -0.15){
                    if(++air>=2){
                        if(c.interactionManager.interactItem(c.player,Hand.MAIN_HAND).isAccepted())c.player.swingHand(Hand.MAIN_HAND);
                        cooldown=28;air=0;
                    }
                }else air=0;
            }
        });
    }
}
