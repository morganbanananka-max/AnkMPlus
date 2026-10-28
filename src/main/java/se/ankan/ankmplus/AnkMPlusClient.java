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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * AnkM+ 9.4 for Minecraft Java 1.21.11 / Fabric.
 * The mod never moves the camera, cancels normal attacks, displays an overlay,
 * or attacks a hidden target. Preparation searches along the user's OWN aim.
 * The final attack requires a physical ray intersection inside ordinary reach.
 */
public final class AnkMPlusClient implements ClientModInitializer {
    private static final int CHEST_ARMOR_MENU_SLOT = 6;
    private static final double MAX_REACH = 3.0;
    private static AnkMPlusClient INSTANCE;

    private KeyMapping swapKey;
    private KeyMapping stunKey;
    private KeyMapping automaticKey;
    private boolean autoSwap = true;
    private boolean stunSlam = true;
    private boolean autoAttack = true;

    private LocalPlayer session;
    private LivingEntity preparedTarget;
    private boolean preparedInAir;
    private int acquiredAt;
    private int lastAimedAt;
    private int tick;
    private int cooldownTicks;
    private int rearmTicks;
    private int axeCooldownUntil;
    private boolean axeFirst;

    private int previousSlot = -1;
    private int selectedByMod = -1;
    private int returnSlotAt;

    private ItemStack originalElytra = ItemStack.EMPTY;
    private int elytraInventorySlot = -1;
    private int restoreAfterTick;

    public AnkMPlusClient() {
        INSTANCE = this;
    }

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("ankmplus", "combat"));
        swapKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.ankmplus.swap", InputConstants.Type.KEYSYM,
                InputConstants.KEY_B, category));
        stunKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.ankmplus.stun", InputConstants.Type.KEYSYM,
                InputConstants.KEY_V, category));
        automaticKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.ankmplus.autoattack", InputConstants.Type.KEYSYM,
                InputConstants.KEY_N, category));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            if (INSTANCE != null) INSTANCE.attemptAttack(Minecraft.getInstance());
        });
        log("client ready, B=Mace V=Stunslam N=Autoattack");
    }

    private static void log(String text) {
        System.out.println("[AnkM+ 9.4] " + text);
    }

    private static boolean falling(LocalPlayer p) {
        return !p.onGround() &&
                (p.isFallFlying() || p.fallDistance > 0.30f ||
                        p.getDeltaMovement().y < -0.08);
    }

    private static int preferredMace(LocalPlayer p, boolean airborne) {
        var ench = p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var breach = ench.getOrThrow(Enchantments.BREACH);
        var density = ench.getOrThrow(Enchantments.DENSITY);
        var burst = ench.getOrThrow(Enchantments.WIND_BURST);
        int winner = -1;
        int highest = Integer.MIN_VALUE;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack item = p.getInventory().getItem(slot);
            if (!item.is(Items.MACE)) continue;
            int b = EnchantmentHelper.getItemEnchantmentLevel(breach, item);
            int d = EnchantmentHelper.getItemEnchantmentLevel(density, item);
            int w = EnchantmentHelper.getItemEnchantmentLevel(burst, item);
            if (!airborne && b == 0) continue;
            int points = airborne ? d * 1000 + w * 80 + b * 10 : b * 1000 + w * 10;
            if (slot == p.getInventory().getSelectedSlot()) points += 1;
            if (points > highest) {
                highest = points;
                winner = slot;
            }
        }
        return winner;
    }

    private static int axeSlot(LocalPlayer p) {
        int selected = p.getInventory().getSelectedSlot();
        if (p.getInventory().getItem(selected).getItem() instanceof AxeItem) return selected;
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().getItem(i).getItem() instanceof AxeItem) return i;
        }
        return -1;
    }

    private static void select(LocalPlayer p, int slot) {
        if (slot < 0 || slot >= 9 || p.getInventory().getSelectedSlot() == slot) return;
        p.getInventory().setSelectedSlot(slot);
        p.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    private void clearHotbar() {
        previousSlot = -1;
        selectedByMod = -1;
        returnSlotAt = 0;
    }

    private void selectTemporarily(LocalPlayer p, int slot) {
        if (slot < 0) return;
        int current = p.getInventory().getSelectedSlot();
        if (previousSlot >= 0 && current != selectedByMod) clearHotbar();
        if (previousSlot < 0 && current != slot) previousSlot = current;
        select(p, slot);
        if (previousSlot >= 0) {
            selectedByMod = slot;
            returnSlotAt = tick + 10;
        }
    }

    private void restoreHotbar(LocalPlayer p) {
        if (previousSlot >= 0 && p.getInventory().getSelectedSlot() == selectedByMod)
            select(p, previousSlot);
        clearHotbar();
    }

    /** Finds PvP targets and named practice dummies, not just Player entities. */
    private LivingEntity playerInApproachCone(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return null;
        Vec3 eye = p.getEyePosition();
        Vec3 dir = p.getLookAngle();
        double speedPerTick = p.getDeltaMovement().length();
        // Preparation only: detect early, but KEEP the Elytra on until close.
        double distance = Math.max(12.0, Math.min(32.0, 12.0 + 7.0 * speedPerTick));
        LivingEntity result = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (var entity : mc.level.getEntities(p, p.getBoundingBox().inflate(distance),
                candidate -> candidate instanceof LivingEntity)) {
            LivingEntity other = (LivingEntity) entity;
            if (!validTarget(p, other)) continue;
            Vec3 relative = other.getBoundingBox().getCenter().subtract(eye);
            double ahead = relative.dot(dir);
            if (ahead <= 0 || ahead >= distance) continue;
            double lateralSquared = Math.max(0, relative.lengthSqr() - ahead * ahead);
            double permitted = 1.25 + Math.min(2.25, ahead * 0.08);
            if (lateralSquared > permitted * permitted) continue;
            if (ahead < nearest) {
                nearest = ahead;
                result = other;
            }
        }
        // A close, directly targeted player must be usable even if fast flight
        // caused the wide approach cone to miss a tick entirely.
        if (mc.hitResult instanceof EntityHitResult hit &&
                hit.getEntity() instanceof LivingEntity direct && validTarget(p, direct)) {
            return direct;
        }
        return result;
    }

    private static boolean validTarget(LocalPlayer p, LivingEntity other) {
        if (other == p || !other.isAlive() || other.isSpectator()) return false;
        // Client practice dummies frequently render as Steve but are not Player instances.
        if (other instanceof Player) return true;
        String name = other.getName().getString().toLowerCase(java.util.Locale.ROOT);
        return name.contains("steve") || name.contains("dummy") || name.contains("bot");
    }

    private static double rayDistance(LocalPlayer p, LivingEntity entity) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(MAX_REACH));
        AABB bounds = entity.getBoundingBox().inflate(0.05);
        Optional<Vec3> point = bounds.clip(eye, end);
        return point.map(eye::distanceTo).orElse(Double.POSITIVE_INFINITY);
    }

    private static boolean playerShielding(LivingEntity entity) {
        return entity.isUsingItem() && entity.getUseItem().is(Items.SHIELD);
    }

    private static boolean matchingItem(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && ItemStack.isSameItemSameComponents(a, b);
    }

    private static boolean inventoryReady(Minecraft mc) {
        LocalPlayer p = mc.player;
        return p != null && mc.gameMode != null && mc.screen == null &&
                p.containerMenu == p.inventoryMenu &&
                p.inventoryMenu.getCarried().isEmpty();
    }

    /**
     * Chest armor slot 6 -> a known, empty inventory-menu slot 9..35.
     * Nothing is dropped. Original Elytra identity is remembered.
     */
    private boolean removeElytra(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (elytraInventorySlot >= 0) return true;
        if (!inventoryReady(mc) || p == null ||
                !p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return false;
        int empty = -1;
        for (int slot = 9; slot <= 35; slot++) {
            if (p.inventoryMenu.getSlot(slot).getItem().isEmpty()) {
                empty = slot;
                break;
            }
        }
        if (empty < 0) return false;
        originalElytra = p.getItemBySlot(EquipmentSlot.CHEST).copy();
        elytraInventorySlot = empty;
        int id = p.inventoryMenu.containerId;
        mc.gameMode.handleInventoryMouseClick(id, CHEST_ARMOR_MENU_SLOT, 0, ClickType.PICKUP, p);
        mc.gameMode.handleInventoryMouseClick(id, empty, 0, ClickType.PICKUP, p);
        log("elytra temporarily stored in menu slot " + empty);
        return true;
    }

    private int locateElytra(LocalPlayer p) {
        if (elytraInventorySlot >= 9 && elytraInventorySlot <= 35 &&
                matchingItem(p.inventoryMenu.getSlot(elytraInventorySlot).getItem(), originalElytra))
            return elytraInventorySlot;
        for (int i = 9; i <= 44; i++) {
            if (matchingItem(p.inventoryMenu.getSlot(i).getItem(), originalElytra)) return i;
        }
        return -1;
    }

    /** Restore SAME Elytra to equipment chest slot, never to an arbitrary slot. */
    private boolean restoreElytra(Minecraft mc) {
        if (elytraInventorySlot < 0) return true;
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null || mc.screen != null ||
                p.containerMenu != p.inventoryMenu) return false;
        if (matchingItem(p.getItemBySlot(EquipmentSlot.CHEST), originalElytra)) {
            elytraInventorySlot = -1;
            originalElytra = ItemStack.EMPTY;
            log("elytra re-equipped");
            return true;
        }
        if (!p.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) return false;
        int id = p.inventoryMenu.containerId;
        if (!p.inventoryMenu.getCarried().isEmpty()) {
            if (matchingItem(p.inventoryMenu.getCarried(), originalElytra))
                mc.gameMode.handleInventoryMouseClick(id, CHEST_ARMOR_MENU_SLOT, 0, ClickType.PICKUP, p);
            return false;
        }
        int source = locateElytra(p);
        if (source < 0) return false;
        mc.gameMode.handleInventoryMouseClick(id, source, 0, ClickType.PICKUP, p);
        mc.gameMode.handleInventoryMouseClick(id, CHEST_ARMOR_MENU_SLOT, 0, ClickType.PICKUP, p);
        return matchingItem(p.getItemBySlot(EquipmentSlot.CHEST), originalElytra);
    }

    private void cancelApproach(String reason) {
        if (preparedTarget != null)
            log("approach finished: " + reason + " (" + (tick - acquiredAt) + " ticks)");
        preparedTarget = null;
        preparedInAir = false;
        axeFirst = false;
        restoreAfterTick = tick + 1;
        rearmTicks = Math.max(rearmTicks, 6);
    }

    /** Attack directly when a target reaches the crosshair, even between client ticks. */
    private void attemptAttack(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null ||
                mc.screen != null || p.isSpectator() || !autoAttack ||
                cooldownTicks > 0) return;

        // The old version only attacked a target first discovered on the 20-Hz
        // client tick. At fast Elytra speed this often meant no target at all.
        LivingEntity target = preparedTarget;
        if (mc.hitResult instanceof EntityHitResult hit &&
                hit.getEntity() instanceof LivingEntity direct && validTarget(p, direct)) {
            if (target != direct) {
                target = direct;
                preparedTarget = direct;
                preparedInAir = falling(p);
                acquiredAt = tick;
                lastAimedAt = tick;
                axeFirst = false;
            }
        }
        if (target == null || !validTarget(p, target)) return;
        if (rayDistance(p, target) > MAX_REACH || !p.hasLineOfSight(target)) return;
        if (rearmTicks > 0 && tick - lastAimedAt > 2) return;

        // The same-frame emergency path is essential for a high-speed flyby.
        // Packet order: inventory swap, selected hotbar slot, attack.
        if (p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA) &&
                (preparedInAir || p.isFallFlying())) {
            if (!removeElytra(mc)) return;
        }
        if (p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA) && preparedInAir)
            return;
        if (p.getAttackStrengthScale(0f) < (preparedInAir ? 0.80f : 0.92f)) return;

        int mace = preferredMace(p, preparedInAir);
        if (mace < 0) return;

        if (axeFirst) {
            if (tick < axeCooldownUntil) return;
            selectTemporarily(p, mace);
            axeFirst = false;
        } else if (stunSlam && playerShielding(target) &&
                axeSlot(p) >= 0 && FlightDecision.shieldComboSafe(p.getDeltaMovement().length())) {
            selectTemporarily(p, axeSlot(p));
            mc.gameMode.attack(p, target);
            p.swing(InteractionHand.MAIN_HAND);
            axeFirst = true;
            axeCooldownUntil = tick + 8;
            cooldownTicks = 1;
            log("shield axe strike");
            return;
        } else {
            if (autoSwap) selectTemporarily(p, mace);
            else if (p.getInventory().getSelectedSlot() != mace) return;
        }
        if (p.getInventory().getSelectedSlot() != mace) return;
        mc.gameMode.attack(p, target);
        p.swing(InteractionHand.MAIN_HAND);
        cooldownTicks = 12;
        log("mace attack packet sent to " + target.getName().getString());
        cancelApproach("attack sent");
    }

    /** Manual clicks are never cancelled; this only picks a Mace beforehand. */
    public static void beforeManualClick(Minecraft mc) {
        AnkMPlusClient self = INSTANCE;
        if (self == null || !self.autoSwap) return;
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null ||
                mc.screen != null || p.isSpectator()) return;
        if (!(mc.hitResult instanceof EntityHitResult hit) ||
                !(hit.getEntity() instanceof LivingEntity)) return;
        int mace = preferredMace(p, falling(p) || (self.preparedInAir && self.elytraInventorySlot >= 0));
        if (mace >= 0) self.selectTemporarily(p, mace);
    }

    private void tick(Minecraft mc) {
        while (swapKey.consumeClick()) autoSwap = !autoSwap;
        while (stunKey.consumeClick()) stunSlam = !stunSlam;
        while (automaticKey.consumeClick()) autoAttack = !autoAttack;

        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || p.isDeadOrDying()) {
            session = p;
            preparedTarget = null;
            preparedInAir = false;
            axeFirst = false;
            clearHotbar();
            return; // Remember saved Elytra so a later rejoin can try to recover it.
        }
        if (session != p) {
            session = p;
            preparedTarget = null;
            preparedInAir = false;
            axeFirst = false;
            clearHotbar();
            restoreAfterTick = tick;
        }
        tick++;
        if (cooldownTicks > 0) cooldownTicks--;
        if (rearmTicks > 0) rearmTicks--;

        if (previousSlot >= 0) {
            if (p.getInventory().getSelectedSlot() != selectedByMod) clearHotbar();
            else if (preparedTarget == null && tick >= returnSlotAt) restoreHotbar(p);
        }

        if (elytraInventorySlot >= 0 && preparedTarget == null &&
                tick >= restoreAfterTick) {
            if (!restoreElytra(mc)) restoreAfterTick = tick + 4;
        }

        if (mc.gameMode == null || mc.screen != null || p.isSpectator() ||
                !autoAttack || rearmTicks > 0) {
            if (preparedTarget != null) cancelApproach("inactive");
            return;
        }

        LivingEntity candidate = playerInApproachCone(mc);
        if (candidate == null) {
            // Retain the target briefly while approaching rapidly.
            if (preparedTarget != null &&
                    preparedTarget.isAlive() &&
                    FlightDecision.keepTarget(tick - lastAimedAt,
                            tick - acquiredAt,
                            p.distanceToSqr(preparedTarget), !p.onGround())) {
                attemptAttack(mc);
                return;
            }
            if (preparedTarget != null) cancelApproach("lost target");
            return;
        }

        if (preparedTarget != null && preparedTarget != candidate &&
                elytraInventorySlot >= 0) {
            cancelApproach("different target");
            return;
        }
        if (preparedTarget != candidate) {
            preparedTarget = candidate;
            preparedInAir = falling(p);
            acquiredAt = tick;
            axeFirst = false;
            log("preparing " + candidate.getName().getString() +
                    ", airborne=" + preparedInAir);
        }
        lastAimedAt = tick;
        // If the player takes off while already approaching, keep the same
        // target but upgrade the plan to an airborne smash.
        if (falling(p)) preparedInAir = true;

        int mace = preferredMace(p, preparedInAir);
        if (mace < 0) {
            cancelApproach("missing suitable Mace");
            return;
        }

        if (elytraInventorySlot >= 0 &&
                p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            // Pending inventory packet: never send a flying Mace attack yet.
            return;
        }

        if (preparedInAir && p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            // Removing Elytra 20-30 blocks out kills gliding momentum and was
            // the main reason previous builds never reached the opponent.
            // Allow up to ~3 ticks for the inventory transaction at high speed.
            double closing = candidate.getBoundingBox().getCenter()
                    .subtract(p.getEyePosition()).dot(p.getLookAngle());
            double speed = p.getDeltaMovement().length();
            double switchDistance = FlightDecision.elytraRemoveDistance(speed);
            if (closing <= switchDistance) {
                if (!removeElytra(mc)) {
                    // A busy inventory should not disable ordinary attacks.
                    return;
                }
            }
        }
        if (autoSwap && !(axeFirst && tick < axeCooldownUntil))
            selectTemporarily(p, mace);

        attemptAttack(mc);
    }
}
