package se.ankan.ankmplus;

/** Ground favors Breach, aerial favors Density; no off-hand or inventory hacking. */
public final class MaceChoice {
    private MaceChoice() {}
    public static int points(boolean airborne, int density, int breach, int windBurst) {
        return airborne
                ? 1000 * density + 50 * windBurst + breach
                : 1000 * breach + 10 * windBurst + density;
    }
}
