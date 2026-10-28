package se.ankan.ankmplus;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.EntityHitResult;

import java.util.UUID;

/**
 * AnkM+ manual restoration of the 8.2 feature set for Fabric 1.21.11.
 *
 * This mod NEVER sends attack packets. Minecraft's original left-click
 * performs every hit. It merely selects the best hotbar weapon immediately
 * before the player's own click. It does not move the camera, manipulate
 * armor/Elytra, search nearby players, or render a HUD.
 *
 * B: enable/disable manual-click Mace swap.
 * V: enable/disable shield Stunslam, requiring TWO user clicks.
 */
public final class AnkMPlusClient implements ClientModInitializer {
    private static AnkMPlusClient instance;
    private KeyMapping maceSwapKey;
    private KeyMapping stunSlamKey;
    private boolean maceSwap = true;
    private boolean stunSlam = true;

    private LocalPlayer currentPlayer;
    private int ticks;
    private int previousSlot = -1;
    private int selectedByMod = -1;
    private int restoreSlotAt;
    private UUID shieldTarget;
    private int shieldFirstClickAt = -1000;

    @Override
    public void onInitializeClient() {
        instance = this;
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("ankmplus", "combat"));
        maceSwapKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.ankmplus.swap", InputConstants.Type.KEYSYM,
                InputConstants.KEY_B, category));
        stunSlamKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.ankmplus.stun", InputConstants.Type.KEYSYM,
                InputConstants.KEY_V, category));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        System.out.println("[AnkM+ manual 8.2] loaded: B=Mace on click, V=shield two-click combo");
    }

    private void tick(Minecraft mc) {
        while (maceSwapKey.consumeClick()) maceSwap = !maceSwap;
        while (stunSlamKey.consumeClick()) {
            stunSlam = !stunSlam;
            if (!stunSlam) shieldTarget = null;
        }

        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || !p.isAlive()) {
            currentPlayer = null;
            previousSlot = -1;
            selectedByMod = -1;
            shieldTarget = null;
            return;
        }
        if (p != currentPlayer) {
            currentPlayer = p;
            previousSlot = -1;
            selectedByMod = -1;
            shieldTarget = null;
        }
        ticks++;
        if (shieldTarget != null && ticks - shieldFirstClickAt > 100) {
            shieldTarget = null;
        }
        if (previousSlot >= 0) {
            if (p.getInventory().getSelectedSlot() != selectedByMod) {
                // The user manually changed hotbar selection; respect it.
                previousSlot = -1;
                selectedByMod = -1;
            } else if (ticks >= restoreSlotAt) {
                setSlot(p, previousSlot);
                previousSlot = -1;
                selectedByMod = -1;
            }
        }
    }

    private static void setSlot(LocalPlayer p, int slot) {
        if (slot < 0 || slot >= 9 || p.getInventory().getSelectedSlot() == slot) return;
        p.getInventory().setSelectedSlot(slot);
        p.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    private void selectForManualClick(LocalPlayer p, int slot) {
        int current = p.getInventory().getSelectedSlot();
        if (previousSlot >= 0 && current != selectedByMod) {
            previousSlot = -1;
            selectedByMod = -1;
        }
        if (previousSlot < 0 && current != slot) previousSlot = current;
        setSlot(p, slot);
        if (previousSlot >= 0) {
            selectedByMod = slot;
            restoreSlotAt = ticks + 6;
        }
    }

    private static boolean falling(LocalPlayer p) {
        return !p.onGround() && (p.fallDistance > 0.30f ||
                p.isFallFlying() || p.getDeltaMovement().y < -0.08);
    }

    private static int maceSlot(LocalPlayer p) {
        boolean air = falling(p);
        var registry = p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var density = registry.getOrThrow(Enchantments.DENSITY);
        var breach = registry.getOrThrow(Enchantments.BREACH);
        var burst = registry.getOrThrow(Enchantments.WIND_BURST);
        int bestSlot = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (!stack.is(Items.MACE)) continue;
            int d = EnchantmentHelper.getItemEnchantmentLevel(density, stack);
            int b = EnchantmentHelper.getItemEnchantmentLevel(breach, stack);
            int w = EnchantmentHelper.getItemEnchantmentLevel(burst, stack);
            int score = ManualCombatDecision.maceScore(air, d, b, w);
            if (p.getInventory().getSelectedSlot() == i) score++;
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private static int axeSlot(LocalPlayer p) {
        int held = p.getInventory().getSelectedSlot();
        if (p.getInventory().getItem(held).getItem() instanceof AxeItem) return held;
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().getItem(i).getItem() instanceof AxeItem) return i;
        }
        return -1;
    }

    /**
     * Invoked by the original Minecraft.startAttack call, only after the user
     * physically presses attack. No tick/frame callback calls this method.
     */
    public static void beforeManualClick(Minecraft mc) {
        AnkMPlusClient self = instance;
        if (self == null || mc.player == null || mc.level == null ||
                mc.gameMode == null || mc.screen != null || mc.player.isSpectator()) return;
        LocalPlayer p = mc.player;

        if (!(mc.hitResult instanceof EntityHitResult entityHit) ||
                !(entityHit.getEntity() instanceof LivingEntity target) ||
                !target.isAlive() || !p.hasLineOfSight(target)) return;

        int mace = maceSlot(p);
        int axe = axeSlot(p);
        UUID id = target.getUUID();
        boolean secondClick = self.shieldTarget != null &&
                id.equals(self.shieldTarget) &&
                self.ticks - self.shieldFirstClickAt <= 100;
        boolean shieldUp = target.isUsingItem() && target.getUseItem().is(Items.SHIELD);

        ManualCombatDecision.Action action = ManualCombatDecision.action(
                self.maceSwap, self.stunSlam, shieldUp,
                secondClick, axe >= 0, mace >= 0);

        if (action == ManualCombatDecision.Action.AXE) {
            self.selectForManualClick(p, axe);
            self.shieldTarget = id;
            self.shieldFirstClickAt = self.ticks;
        } else if (action == ManualCombatDecision.Action.MACE) {
            self.selectForManualClick(p, mace);
            if (secondClick) self.shieldTarget = null;
        } else if (self.shieldTarget != null && !secondClick) {
            self.shieldTarget = null;
        }
        // NO gameMode.attack, NO swing(), NO automatic chestplate or Elytra swap.
        // The vanilla Minecraft attack proceeds after this callback.
    }
}
