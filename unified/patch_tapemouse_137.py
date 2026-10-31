"""Port the *behavior* of ThunderHack's TapeMouse into existing AutoClicker.

One native module, not duplicate click engines. Existing Press/Hold/Disabled
modes are preserved, with a new monotonic-time and randomized ThunderTape mode.
ThunderHack Reborn GPL-3.0 source provided by the user.
"""
from pathlib import Path

p=Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules/player/AutoClicker.java")
code=p.read_text(encoding="utf-8")
def replace(old,new):
    global code
    count=code.count(old)
    if count!=1:
        raise RuntimeError(f"AutoClicker anchor count {count}: {old[:70]}")
    code=code.replace(old,new,1)

replace("import meteordevelopment.orbit.EventHandler;",
'''import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.EntityHitResult;
import java.util.concurrent.ThreadLocalRandom;''')
replace('''    private int rightClickTimer, leftClickTimer;''',
'''    // ThunderHack's TapeMouse uses elapsed time instead of tick delays,
    // and adds optional jitter to avoid a mechanically fixed interval.
    private final Setting<Integer> tapeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("thunder-tape-delay-ms")
        .description("Time between TapeMouse actions. Never less than 200 ms.")
        .defaultValue(650).range(200,4000).sliderRange(200,4000)
        .visible(() -> leftClickMode.get()==Mode.ThunderTape
            || rightClickMode.get()==Mode.ThunderTape).build());
    private final Setting<Integer> tapeJitter = sgGeneral.add(new IntSetting.Builder()
        .name("thunder-tape-random-ms")
        .description("Additional random wait on each TapeMouse interval.")
        .defaultValue(150).range(0,1500).sliderRange(0,1000)
        .visible(() -> leftClickMode.get()==Mode.ThunderTape
            || rightClickMode.get()==Mode.ThunderTape).build());
    private final Setting<Boolean> tapeEntitiesOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("thunder-tape-entities-only")
        .description("For left TapeMouse: click only when crosshair actually targets an entity.")
        .defaultValue(true).visible(() -> leftClickMode.get()==Mode.ThunderTape).build());

    private long leftTapeNextNanos, rightTapeNextNanos;
    private int rightClickTimer, leftClickTimer;

    private long tapeIntervalNanos() {
        int jitter=tapeJitter.get()==0 ? 0
            : ThreadLocalRandom.current().nextInt(tapeJitter.get()+1);
        return (long)(Math.max(200,tapeDelay.get()) + jitter)*1_000_000L;
    }''')
replace("        rightClickTimer = 0;\n        leftClickTimer = 0;",
        "        rightClickTimer = 0;\n        leftClickTimer = 0;\n        leftTapeNextNanos=0; rightTapeNextNanos=0;")
replace('''        if (!inScreens.get() && mc.currentScreen != null) return;

        switch (leftClickMode.get()) {''',
'''        if(mc.player==null || mc.world==null || mc.interactionManager==null) return;
        if (!inScreens.get() && mc.currentScreen != null) return;
        // TapeMouse must never click inside menus or on disconnected worlds.
        long now=System.nanoTime();

        switch (leftClickMode.get()) {''')
replace('''            case Disabled -> {}
            case Hold -> mc.options.attackKey.setPressed(true);''',
'''            case Disabled -> {}
            case ThunderTape -> {
                if(mc.currentScreen==null && !mc.player.isUsingItem()
                    && !mc.player.isSpectator() && now>=leftTapeNextNanos) {
                    leftTapeNextNanos=now+tapeIntervalNanos();
                    if(!tapeEntitiesOnly.get()
                        || mc.crosshairTarget instanceof EntityHitResult) Utils.leftClick();
                }
            }
            case Hold -> mc.options.attackKey.setPressed(true);''')
# The right-click switch also has Disabled and Hold, matching precisely once
# after the left mode insertion.
replace('''        switch (rightClickMode.get()) {
            case Disabled -> {}
            case Hold -> mc.options.useKey.setPressed(true);''',
'''        switch (rightClickMode.get()) {
            case Disabled -> {}
            case ThunderTape -> {
                if(mc.currentScreen==null && !mc.player.isSpectator()
                    && !mc.player.isUsingItem() && now>=rightTapeNextNanos) {
                    rightTapeNextNanos=now+tapeIntervalNanos();
                    Utils.rightClick();
                }
            }
            case Hold -> mc.options.useKey.setPressed(true);''')
replace('''        Hold,
        Press
    }''',
'''        Hold,
        Press,
        ThunderTape
    }''')
p.write_text(code,encoding="utf-8")
assert code.count("case ThunderTape ->") == 2
print("AutoClicker now offers ThunderTape left/right, bounded random delay, one existing module.")
