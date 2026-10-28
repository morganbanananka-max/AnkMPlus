package se.ankan.ankmplus;

/**
 * Determines which hotbar item a PHYSICAL left-click uses.
 * No auto-attacks, aiming, Elytra swapping or camera manipulation.
 */
public final class ManualCombatDecision {
    private ManualCombatDecision() {}

    public enum Action { NORMAL, AXE, MACE }

    /**
     * A real Java Edition Mace smash requires more than 1.5 blocks of fall,
     * the attacker airborne, and no active Elytra glide.
     * Horizontal speed is deliberately ignored: fast smashes are valid.
     */
    public static boolean realSmash(boolean onGround, boolean gliding,
                                    double fallDistance) {
        return !onGround && !gliding && Double.isFinite(fallDistance)
                && fallDistance > 1.5;
    }

    public static Action action(boolean maceSwap, boolean stunSlam,
                                boolean shieldUp, boolean pendingSecondClick,
                                boolean validSmash, boolean hasAxe,
                                boolean hasMace) {
        // One actual click with Axe to disable the shield, a SECOND actual
        // click with the Mace to follow up. Never issue an attack packet.
        if (stunSlam && pendingSecondClick && hasMace) return Action.MACE;
        if (stunSlam && shieldUp && hasAxe && hasMace) return Action.AXE;
        if (maceSwap && validSmash && hasMace) return Action.MACE;
        return Action.NORMAL;
    }

    public static int maceScore(boolean falling, int density, int breach, int windBurst) {
        return falling
                ? density * 1000 + windBurst * 40 + breach
                : breach * 1000 + windBurst * 10 + density;
    }
}
