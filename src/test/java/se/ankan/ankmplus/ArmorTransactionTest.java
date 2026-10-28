package se.ankan.ankmplus;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ArmorTransactionTest {
    private static final class Inventory {
        String[] items = new String[45];
        String cursor = null;
        Inventory(int source, String sourceItem) {
            items[6] = "Elytra";
            items[source] = sourceItem;
        }
        void apply(List<ArmorTransaction.Step> steps) {
            for (ArmorTransaction.Step step : steps) {
                if (step.kind() == ArmorTransaction.Kind.SWAP) {
                    int slot = 36 + step.button();
                    String held = items[step.slot()];
                    items[step.slot()] = items[slot];
                    items[slot] = held;
                } else {
                    String held = items[step.slot()];
                    items[step.slot()] = cursor;
                    cursor = held;
                }
            }
            assertNull(cursor, "No item can remain on the cursor after transaction");
        }
    }

    private void reversible(int source, String armor,
                            ArmorTransaction.Mode mode) {
        Inventory inventory = new Inventory(source, armor);
        inventory.apply(ArmorTransaction.plan(mode, source, false));
        assertEquals("Elytra", inventory.items[source], "Elytra must be in original source");
        assertEquals(armor, inventory.items[6], "Correct chestplate must be equipped");
        inventory.apply(ArmorTransaction.plan(mode, source, true));
        assertEquals("Elytra", inventory.items[6], "Elytra must return to chest slot");
        assertEquals(armor, inventory.items[source], "Chestplate must return to original slot");
    }

    @Test void eachHotbarSlotIsReversible() {
        for (int i = 36; i <= 44; i++) {
            reversible(i, "Netherite Chestplate", ArmorTransaction.Mode.HOTBAR_CHESTPLATE);
        }
    }

    @Test void eachMainInventorySlotIsReversible() {
        for (int i = 9; i <= 35; i++) {
            reversible(i, "Diamond Chestplate", ArmorTransaction.Mode.INVENTORY_CHESTPLATE);
        }
    }

    @Test void emptySlotCanSafelyHoldElytra() {
        for (int i = 9; i <= 44; i++) {
            reversible(i, null, ArmorTransaction.Mode.EMPTY_SLOT);
        }
    }

    @Test void invalidMenuLocationsFailBeforeTouchingInventory() {
        assertThrows(IllegalArgumentException.class, () ->
                ArmorTransaction.plan(ArmorTransaction.Mode.HOTBAR_CHESTPLATE, 20, false));
        assertThrows(IllegalArgumentException.class, () ->
                ArmorTransaction.plan(ArmorTransaction.Mode.INVENTORY_CHESTPLATE, 40, true));
        assertThrows(IllegalArgumentException.class, () ->
                ArmorTransaction.plan(ArmorTransaction.Mode.EMPTY_SLOT, 6, false));
    }
}
