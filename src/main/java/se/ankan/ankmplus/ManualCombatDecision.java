package se.ankan.ankmplus;

/** Pure manual-click decisions: these methods never create attacks. */
public final class ManualCombatDecision {
    private ManualCombatDecision() {}

    public enum Action { NORMAL, AXE, MACE }

    public static Action action(boolean maceSwap, boolean stunSlam,
                                boolean shieldUp, boolean pendingSecondClick,
                                boolean hasAxe, boolean hasMace) {
        if (stunSlam && pendingSecondClick && hasMace) return Action.MACE;
        if (stunSlam && shieldUp && hasAxe && hasMace) return Action.AXE;
        if (maceSwap && hasMace) return Action.MACE;
        return Action.NORMAL;
    }

    public static int maceScore(boolean falling, int density, int breach, int windBurst) {
        return falling
                ? density * 1000 + windBurst * 40 + breach
                : breach * 1000 + windBurst * 10 + density;
    }
}
