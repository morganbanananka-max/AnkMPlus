package se.ankan.ankmplus.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.ankan.ankmplus.AnkMPlusClient;

@Mixin(Minecraft.class)
public abstract class MinecraftAttackMixin {
    // The original Minecraft left-click is never cancelled or delayed.
    @Inject(method = "startAttack", at = @At("HEAD"))
    private void ankmplus$beforeAttack(CallbackInfoReturnable<Boolean> cir) {
        AnkMPlusClient.beforeManualClick((Minecraft) (Object) this);
    }
}
