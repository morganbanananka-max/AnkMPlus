package se.ankan.ankmplus;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * AnkM+ 9.6 / Fabric 1.21.11.
 * Keeps AUTO-HIT, but NEVER uses a softened hitbox, an aimbot or extended reach.
 * The camera/look ray must truly intersect an entity's bounding box within
 * 3.0 blocks with no block between eye and hit point.
 *
 * Elytra and chestplate are swapped only when a user-aimed approach is close.
 * All inventory actions use the normal client game-mode menu packets.
 */
public final class AnkMPlusClient implements ClientModInitializer {
    private static final double REACH = 3.0;
    private static final int CHEST_SLOT = 6;
    private static AnkMPlusClient INSTANCE;

    private KeyMapping autoKey;
    private KeyMapping swapKey;
    private KeyMapping stunKey;
    private boolean autoHit = true;
    private boolean autoSwap = true;
    private boolean stunSlam = true;

    private LocalPlayer session;
    private LivingEntity approach;
    private boolean approachAirborne;
    private int lastApproachTick;
    private int tick;
    private int lastHitTick = -100;
    private int restoreAfterTick;
    private int lastRestoreWarningTick = -100;
    private int shieldFollowupAt = -1;
    private LivingEntity shieldFollowupTarget;

    private int priorHotbar = -1;
    private int modHotbar = -1;
    private int restoreHotbarAt;

    private enum ArmorMode { NONE, HOTBAR_CHESTPLATE, INVENTORY_CHESTPLATE, EMPTY_SLOT }
    private ArmorMode armorMode = ArmorMode.NONE;
    private int storedMenuSlot = -1;
    private int storedHotbar = -1;
    private ItemStack savedElytra = ItemStack.EMPTY;
    private ItemStack savedChestplate = ItemStack.EMPTY;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("ankmplus", "combat"));
        autoKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.ankmplus.autoattack", InputConstants.Type.KEYSYM,
                InputConstants.KEY_N, category));
        swapKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.ankmplus.swap", InputConstants.Type.KEYSYM,
                InputConstants.KEY_B, category));
        stunKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.ankmplus.stun", InputConstants.Type.KEYSYM,
                InputConstants.KEY_V, category));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            if (INSTANCE != null) INSTANCE.tryAutoHit(Minecraft.getInstance());
        });
        log("client ready | N auto-hit | B auto-swap | V shield combo");
    }

    private static void log(String s) { System.out.println("[AnkM+ 9.6] " + s); }

    private static boolean airborne(LocalPlayer p) {
        return !p.onGround() && (p.isFallFlying() ||
                p.fallDistance > 0.30f || p.getDeltaMovement().y < -0.08);
    }

    private static boolean targetAllowed(LocalPlayer self, LivingEntity other) {
        if (other == self || !other.isAlive() || other.isSpectator()) return false;
        if (other instanceof Player) return true;
        String name = other.getName().getString().toLowerCase(Locale.ROOT);
        return name.contains("steve") || name.contains("dummy") || name.contains("bot");
    }

    /**
     * Strict entity ray. All tests use exact AABB surfaces, zero inflation.
     * When a block is in front of the hit point the attack is refused.
     */
    private LivingEntity aimedTarget(Minecraft mc) {
        LocalPlayer self = mc.player;
        if (self == null || mc.level == null) return null;

        Vec3 eye = self.getEyePosition();
        Vec3 look = self.getLookAngle();
        LivingEntity best = null;
        double nearest = Double.POSITIVE_INFINITY;

        for (var candidate : mc.level.getEntities(self, self.getBoundingBox().inflate(4.0),
                entity -> entity instanceof LivingEntity)) {
            LivingEntity other = (LivingEntity) candidate;
            if (!targetAllowed(self, other)) continue;
            AABB aabb = other.getBoundingBox();
            double distance = ExactHitGeometry.rayAabbDistance(
                    eye.x, eye.y, eye.z, look.x, look.y, look.z,
                    aabb.minX, aabb.minY, aabb.minZ,
                    aabb.maxX, aabb.maxY, aabb.maxZ, REACH);
            if (!Double.isFinite(distance) || distance >= nearest) continue;
            Vec3 hit = eye.add(look.scale(distance));
            var block = mc.level.clip(new ClipContext(
                    eye, hit, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
            if (block.getType() == HitResult.Type.BLOCK &&
                    block.getLocation().distanceToSqr(eye) <= distance * distance + 1.0e-4)
                continue;
            if (!self.hasLineOfSight(other)) continue;
            best = other;
            nearest = distance;
        }
        return best;
    }

    /** Only preparation (never attack) may see players further than 3 blocks. */
    private LivingEntity approachTarget(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return null;
        Vec3 look = p.getLookAngle();
        Vec3 eye = p.getEyePosition();
        double velocity = p.getDeltaMovement().length();
        double distance = Math.max(10, Math.min(28, 10 + velocity * 5));
        LivingEntity best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (var candidate : mc.level.getEntities(p, p.getBoundingBox().inflate(distance),
                e -> e instanceof LivingEntity)) {
            LivingEntity other = (LivingEntity) candidate;
            if (!targetAllowed(p, other)) continue;
            Vec3 towards = other.getBoundingBox().getCenter().subtract(eye);
            double forward = towards.dot(look);
            if (forward <= 0 || forward > distance) continue;
            // Narrow preparation cone. DOES NOT allow attacking off crosshair.
            double side = towards.subtract(look.scale(forward)).length();
            if (side > Math.min(2.0, 0.8 + 0.04 * forward)) continue;
            if (forward < bestDistance) {
                bestDistance = forward;
                best = other;
            }
        }
        return best;
    }

    private static int maceSlot(LocalPlayer p, boolean air) {
        var enchantments = p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var density = enchantments.getOrThrow(Enchantments.DENSITY);
        var breach = enchantments.getOrThrow(Enchantments.BREACH);
        var wind = enchantments.getOrThrow(Enchantments.WIND_BURST);
        int chosen = -1, value = Integer.MIN_VALUE;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = p.getInventory().getItem(slot);
            if (!stack.is(Items.MACE)) continue;
            int d = EnchantmentHelper.getItemEnchantmentLevel(density, stack);
            int b = EnchantmentHelper.getItemEnchantmentLevel(breach, stack);
            int w = EnchantmentHelper.getItemEnchantmentLevel(wind, stack);
            int points = MaceChoice.points(air, d, b, w);
            if (points == Integer.MIN_VALUE) continue;
            if (slot == p.getInventory().getSelectedSlot()) points++;
            if (points > value) { value = points; chosen = slot; }
        }
        return chosen;
    }

    private static int axeSlot(LocalPlayer p) {
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().getItem(i).getItem() instanceof AxeItem) return i;
        }
        return -1;
    }

    private static void select(LocalPlayer p, int slot) {
        if (slot < 0 || slot > 8 || p.getInventory().getSelectedSlot() == slot) return;
        p.getInventory().setSelectedSlot(slot);
        p.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    private void selectForAttack(LocalPlayer p, int slot) {
        int selected = p.getInventory().getSelectedSlot();
        // If the user independently changed slot, do not overwrite their
        // previous choice when it is time to restore.
        if (priorHotbar >= 0 && selected != modHotbar) {
            priorHotbar = -1;
            modHotbar = -1;
        }
        if (priorHotbar < 0 && selected != slot) priorHotbar = selected;
        select(p, slot);
        if (priorHotbar >= 0) {
            modHotbar = slot;
            restoreHotbarAt = tick + 5;
        }
    }

    private void restoreHeldItem(LocalPlayer p) {
        if (priorHotbar >= 0 && p.getInventory().getSelectedSlot() == modHotbar)
            select(p, priorHotbar);
        priorHotbar = -1;
        modHotbar = -1;
    }

    private static boolean inventorySafe(Minecraft mc) {
        LocalPlayer p = mc.player;
        return p != null && mc.gameMode != null && mc.screen == null &&
                p.containerMenu == p.inventoryMenu &&
                p.inventoryMenu.getCarried().isEmpty();
    }

    private static int chestplateRank(ItemStack stack) {
        if (stack.is(Items.NETHERITE_CHESTPLATE)) return 6;
        if (stack.is(Items.DIAMOND_CHESTPLATE)) return 5;
        if (stack.is(Items.IRON_CHESTPLATE)) return 4;
        if (stack.is(Items.CHAINMAIL_CHESTPLATE)) return 3;
        if (stack.is(Items.GOLDEN_CHESTPLATE)) return 2;
        if (stack.is(Items.LEATHER_CHESTPLATE)) return 1;
        return 0;
    }

    private void click(Minecraft mc, int slot, int button, ClickType type) {
        LocalPlayer p = mc.player;
        mc.gameMode.handleInventoryMouseClick(
                p.inventoryMenu.containerId, slot, button, type, p);
    }

    private void executeArmorPlan(Minecraft mc, boolean restoring) {
        ArmorTransaction.Mode type = ArmorTransaction.Mode.valueOf(armorMode.name());
        for (ArmorTransaction.Step op : ArmorTransaction.plan(type, storedMenuSlot, restoring)) {
            click(mc, op.slot(), op.button(),
                    op.kind() == ArmorTransaction.Kind.SWAP ? ClickType.SWAP : ClickType.PICKUP);
        }
    }

    /** No item is dropped. If there is no safe destination, abort swap. */
    private boolean equipChestplateOrRemoveElytra(Minecraft mc) {
        if (armorMode != ArmorMode.NONE) return true;
        if (!inventorySafe(mc)) return false;
        LocalPlayer p = mc.player;
        ItemStack elytra = p.getItemBySlot(EquipmentSlot.CHEST);
        if (!elytra.is(Items.ELYTRA)) return true; // Already wearing chestplate.

        int chestSource = -1, best = 0;
        for (int i = 9; i <= 44; i++) {
            ItemStack stack = p.inventoryMenu.getSlot(i).getItem();
            int rank = chestplateRank(stack);
            if (rank > best) {
                best = rank;
                chestSource = i;
                if (rank == 6) break;
            }
        }

        int empty = -1;
        if (chestSource < 0) {
            for (int i = 9; i <= 44; i++) {
                if (p.inventoryMenu.getSlot(i).getItem().isEmpty()) {
                    empty = i;
                    break;
                }
            }
            if (empty < 0) return false;
        }

        savedElytra = elytra.copy();
        storedMenuSlot = chestSource >= 0 ? chestSource : empty;
        storedHotbar = chestSource >= 36 ? chestSource - 36 : -1;
        savedChestplate = chestSource >= 0
                ? p.inventoryMenu.getSlot(chestSource).getItem().copy()
                : ItemStack.EMPTY;
        armorMode = chestSource < 0 ? ArmorMode.EMPTY_SLOT
                : storedHotbar >= 0 ? ArmorMode.HOTBAR_CHESTPLATE
                : ArmorMode.INVENTORY_CHESTPLATE;

        executeArmorPlan(mc, false);

        log("chest change " + armorMode + " source=" + storedMenuSlot);
        return true;
    }

    private static boolean same(ItemStack x, ItemStack y) {
        return !x.isEmpty() && !y.isEmpty() && ItemStack.isSameItemSameComponents(x, y);
    }

    /** Confirm actual chest contents before forgetting tracked equipment. */
    private void clearArmorState() {
        armorMode = ArmorMode.NONE;
        storedMenuSlot = -1;
        storedHotbar = -1;
        savedElytra = ItemStack.EMPTY;
        savedChestplate = ItemStack.EMPTY;
    }

    private boolean restoreArmor(Minecraft mc) {
        if (armorMode == ArmorMode.NONE) return true;
        if (!inventorySafe(mc)) return false;
        LocalPlayer p = mc.player;
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        if (same(chest, savedElytra)) {
            clearArmorState();
            log("Elytra restored");
            return true;
        }
        if (storedMenuSlot < 9 || storedMenuSlot > 44) return false;
        // Do not move someone else's item if inventory changed during combat.
        ItemStack source = p.inventoryMenu.getSlot(storedMenuSlot).getItem();
        if (!same(source, savedElytra)) return false;

        switch (armorMode) {
            case HOTBAR_CHESTPLATE -> {
                if (!chest.is(savedChestplate.getItem())) return false;
                executeArmorPlan(mc, true);
            }
            case INVENTORY_CHESTPLATE -> {
                if (!chest.is(savedChestplate.getItem())) return false;
                executeArmorPlan(mc, true);
            }
            case EMPTY_SLOT -> {
                if (!chest.isEmpty()) return false;
                executeArmorPlan(mc, true);
            }
            default -> { return false; }
        }
        if (same(p.getItemBySlot(EquipmentSlot.CHEST), savedElytra)) {
            clearArmorState();
            log("Elytra restored and chestplate returned to original slot");
            return true;
        }
        return false;
    }

    /**
     * Precise auto-hit: player provides aim. Every packet is gated by exact
     * entity ray + block clipping and the player's attack cooldown.
     */
    private void tryAutoHit(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (!autoHit || p == null || mc.gameMode == null || mc.screen != null ||
                p.isSpectator() || p.isDeadOrDying() || mc.level == null ||
                tick - lastHitTick < 9 || !p.inventoryMenu.getCarried().isEmpty()) return;

        LivingEntity target = aimedTarget(mc);
        if (target == null) return; // Absolutely NO off-hitbox auto attacks.
        boolean air = !p.onGround() &&
                (airborne(p) || (armorMode != ArmorMode.NONE && approachAirborne));
        int mace = maceSlot(p, air);
        if (mace < 0) return;

        // Elytra armor packets and held-item packet are ordered ahead of attack.
        if (air && p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            if (!equipChestplateOrRemoveElytra(mc)) return;
        }

        // At high speed the shield axe -> mace combo misses; avoid it.
        if (stunSlam && target.isUsingItem() && target.getUseItem().is(Items.SHIELD)
                && !air && FlightDecision.shieldComboSafe(p.getDeltaMovement().length())) {
            int axe = axeSlot(p);
            if (axe >= 0 && shieldFollowupAt < 0) {
                if (p.getAttackStrengthScale(0) < 0.93f) return;
                selectForAttack(p, axe);
                mc.gameMode.attack(p, target);
                p.swing(InteractionHand.MAIN_HAND);
                lastHitTick = tick;
                shieldFollowupTarget = target;
                shieldFollowupAt = tick + 9;
                restoreAfterTick = tick + 12;
                log("shield combo axe packet: " + target.getName().getString());
                return;
            }
        }

        if (shieldFollowupAt >= 0 && shieldFollowupTarget == target &&
                tick < shieldFollowupAt) return;

        if (autoSwap) selectForAttack(p, mace);
        if (p.getInventory().getSelectedSlot() != mace) return;
        if (p.getAttackStrengthScale(0f) < 0.90f) return;

        mc.gameMode.attack(p, target);
        p.swing(InteractionHand.MAIN_HAND);
        lastHitTick = tick;
        shieldFollowupTarget = null;
        shieldFollowupAt = -1;
        restoreAfterTick = tick + 2;
        restoreHotbarAt = tick + 4;
        approach = null;  // Allow armor to be restored even when still close.
        log("Mace attack PACKET (not confirmed hit): " + target.getName().getString());
    }

    /** Never cancel vanilla left-click; select a legitimate Mace before it. */
    public static void beforeManualClick(Minecraft mc) {
        AnkMPlusClient mod = INSTANCE;
        if (mod == null || !mod.autoSwap || mc.player == null || mc.level == null ||
                mc.screen != null || mc.gameMode == null) return;
        LivingEntity hit = mod.aimedTarget(mc);
        if (hit == null) return;
        LocalPlayer p = mc.player;
        boolean air = !p.onGround() &&
                (airborne(p) || (mod.armorMode != ArmorMode.NONE && mod.approachAirborne));
        int mace = maceSlot(p, air);
        if (mace < 0) return;
        if (air && p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA))
            mod.equipChestplateOrRemoveElytra(mc);
        mod.selectForAttack(p, mace);
        mod.lastHitTick = mod.tick; // Prevent double auto-hit during manual attack.
        mod.restoreHotbarAt = mod.tick + 5;
        mod.restoreAfterTick = mod.tick + 3;
    }

    private void tick(Minecraft mc) {
        while (autoKey.consumeClick()) { autoHit = !autoHit; log("auto-hit=" + autoHit); }
        while (swapKey.consumeClick()) { autoSwap = !autoSwap; log("auto-swap=" + autoSwap); }
        while (stunKey.consumeClick()) { stunSlam = !stunSlam; log("stunslam=" + stunSlam); }

        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || p.isDeadOrDying()) {
            session = p;
            approach = null;
            shieldFollowupAt = -1;
            // Tracked items remain intact: do not drop inventory on disconnect.
            priorHotbar = -1;
            modHotbar = -1;
            return;
        }
        if (session != p) {
            session = p;
            approach = null;
            shieldFollowupAt = -1;
            priorHotbar = -1;
            modHotbar = -1;
            // World change invalidates references to prior inventory.
            // Only restore if the actual tracked Elytra can be identified.
            restoreAfterTick = tick;
        }

        tick++;

        if (priorHotbar >= 0 && p.getInventory().getSelectedSlot() != modHotbar) {
            priorHotbar = -1; // User chose their own slot.
            modHotbar = -1;
        } else if (priorHotbar >= 0 && tick >= restoreHotbarAt &&
                tick - lastHitTick >= 2) {
            restoreHeldItem(p);
        }

        // Restore regardless of whether auto-hit is enabled and never
        // overwrite user-modified inventory slots.
        if (armorMode != ArmorMode.NONE && tick >= restoreAfterTick &&
                (approach == null || !autoHit || tick - lastApproachTick > 5 ||
                        tick - lastHitTick >= 2 && tick - lastHitTick < 9)) {
            if (!restoreArmor(mc) && tick - lastRestoreWarningTick >= 60) {
                log("armor restore pending: inventory busy/changed; items not dropped");
                lastRestoreWarningTick = tick;
            }
        }

        if (!autoHit || !autoSwap || mc.gameMode == null || mc.screen != null ||
                p.isSpectator()) {
            approach = null;
            return;
        }

        LivingEntity candidate = approachTarget(mc);
        if (candidate == null) {
            if (approach != null && tick - lastApproachTick > 4) {
                approach = null;
                restoreAfterTick = tick;
            }
            return;
        }
        if (candidate != approach) {
            approach = candidate;
            approachAirborne = airborne(p);
        }
        if (p.onGround()) approachAirborne = false;
        else if (airborne(p)) approachAirborne = true;
        lastApproachTick = tick;

        // Equipping while far away loses velocity and causes failed flybys.
        // At most prepare a chest swap close to target.
        if (approachAirborne && tick - lastHitTick >= 9 &&
                armorMode == ArmorMode.NONE &&
                p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            double closing = candidate.getBoundingBox().getCenter()
                    .subtract(p.getEyePosition()).dot(p.getLookAngle());
            if (closing > 0 &&
                    closing <= FlightDecision.elytraRemoveDistance(
                            p.getDeltaMovement().length())) {
                if (equipChestplateOrRemoveElytra(mc)) restoreAfterTick = tick + 8;
            }
        }

        // Only select a Mace very near the target, preserving fireworks.
        if (p.distanceToSqr(candidate) <= 16.0) {
            int chosen = maceSlot(p, approachAirborne);
            if (chosen >= 0) selectForAttack(p, chosen);
        }
        tryAutoHit(mc);
    }
}
