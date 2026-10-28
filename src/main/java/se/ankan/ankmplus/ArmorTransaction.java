package se.ankan.ankmplus;

import java.util.List;

/**
 * Explicit reversible transactions for the player inventory menu:
 * armor chest = 6, main inventory = 9..35, hotbar = 36..44.
 * This plan is what the real game uses, and it can be simulated in tests.
 */
public final class ArmorTransaction {
    private ArmorTransaction() {}

    public enum Mode { NONE, HOTBAR_CHESTPLATE, INVENTORY_CHESTPLATE, EMPTY_SLOT }
    public enum Kind { PICKUP, SWAP }
    public record Step(int slot, int button, Kind kind) {}

    public static List<Step> plan(Mode mode, int source, boolean restore) {
        if (mode == Mode.NONE) return List.of();
        if (source < 9 || source > 44) throw new IllegalArgumentException("invalid slot: " + source);
        Step chest = new Step(6, 0, Kind.PICKUP);
        Step sourcePick = new Step(source, 0, Kind.PICKUP);
        if (mode == Mode.HOTBAR_CHESTPLATE) {
            if (source < 36) throw new IllegalArgumentException("hotbar armor must be in hotbar");
            return List.of(new Step(6, source - 36, Kind.SWAP));
        }
        if (mode == Mode.INVENTORY_CHESTPLATE) {
            if (source > 35) throw new IllegalArgumentException("main armor must be in main inventory");
            return restore
                    ? List.of(sourcePick, chest, sourcePick)
                    : List.of(chest, sourcePick, chest);
        }
        return restore
                ? List.of(sourcePick, chest)
                : List.of(chest, sourcePick);
    }
}
