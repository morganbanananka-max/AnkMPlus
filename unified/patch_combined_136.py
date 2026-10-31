"""AnkHack native combat guard and two real ThunderHack-style variant ports.

Runs after 1.0.35 patches against the pinned Meteor 1.21.11 source.
Fail closed if upstream changes; do not silently create fake variants.
"""
from pathlib import Path
import shutil

root=Path("upstream/src/main/java/meteordevelopment/meteorclient")
def replace_once(file,old,new):
    path=root/file
    data=path.read_text(encoding="utf-8")
    if data.count(old)!=1:
        raise RuntimeError(f"{file}: expected 1 anchor {old[:60]!r}, got {data.count(old)}")
    path.write_text(data.replace(old,new,1),encoding="utf-8")

# This is essential: DoAttackEvent uses a static instance, so cancellation
# must reset on each invocation. Otherwise one missed hit poisons all attacks.
replace_once("events/entity/player/DoAttackEvent.java",
'''    public static DoAttackEvent get() {
        return INSTANCE;
    }''',
'''    public static DoAttackEvent get() {
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }''')

combat=root/"systems/modules/combat"
src=Path("unified/native/AnkAntiLegitMiss.java")
if "public final class AnkAntiLegitMiss extends Module" not in src.read_text():
    raise RuntimeError("Missing native combat implementation")
shutil.copy2(src,combat/"AnkAntiLegitMiss.java")
replace_once("systems/modules/Modules.java",
"        add(new AnkTriggerBot());",
"        add(new AnkTriggerBot());\n        add(new AnkAntiLegitMiss());")

# ThunderHack Reborn TpsSync behavior integrated into existing Meteor Timer.
# The estimate is NOT guaranteed server TPS and stale data is ignored.
timer="systems/modules/world/Timer.java"
replace_once(timer,
"import meteordevelopment.meteorclient.settings.DoubleSetting;",
'''import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.utils.world.TickRate;''')
replace_once(timer,
'''    public static final double OFF = 1;''',
'''    public enum Strategy { Fixed, ThunderServerSync }

    private final Setting<Strategy> strategy = sgGeneral.add(new EnumSetting.Builder<Strategy>()
        .name("strategy")
        .description("Fixed client speed, or a conservative server-time synchronization strategy.")
        .defaultValue(Strategy.Fixed).build());

    private final Setting<Double> minServerMultiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("sync-minimum")
        .description("Minimum multiplayer synchronization multiplier; no speedup above 1.")
        .defaultValue(0.5).range(0.2,1.0).sliderRange(0.2,1.0)
        .visible(() -> strategy.get()==Strategy.ThunderServerSync).build());

    public static final double OFF = 1;''')
replace_once(timer,
'''    public double getMultiplier() {
        return override != OFF ? override : (isActive() ? multiplier.get() : OFF);
    }''',
'''    public double getMultiplier() {
        if (override != OFF) return override;
        if (!isActive()) return OFF;
        if (strategy.get() != Strategy.ThunderServerSync) return multiplier.get();
        // Never alter local singleplayer ticks or act on missing/stale packets.
        if (mc.player == null || mc.world == null || mc.isInSingleplayer()
            || TickRate.INSTANCE.getTimeSinceLastTick() >= 3.0f) return OFF;
        float estimated = TickRate.INSTANCE.getTickRate();
        if (!Float.isFinite(estimated) || estimated < 1.0f) return OFF;
        return Math.max(minServerMultiplier.get(), Math.min(1.0, estimated / 20.0));
    }''')

# AntiWeb: add a genuinely different bounded motion algorithm to NoSlow,
# rather than a second conflicting web module or fake dropdown variant.
noslow="systems/modules/movement/NoSlow.java"
replace_once(noslow,
'''    private boolean resetTimer;''',
'''    private final Setting<Double> thunderMotionSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("web-thunder-motion-speed")
        .description("Max horizontal velocity in a cobweb for ThunderMotion. Server may correct it.")
        .defaultValue(0.10).range(0.02,0.20).sliderRange(0.02,0.20)
        .visible(() -> web.get()==WebMode.ThunderMotion).build());

    private boolean resetTimer;''')
replace_once(noslow,
'''    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (web.get() == WebMode.Timer) {''',
'''    @Override
    public void onDeactivate() {
        // Do not leave a temporary timer speed enabled after disabling NoSlow.
        if (web.get()==WebMode.Timer && !resetTimer)
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
        resetTimer=true;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player==null || mc.world==null) return;
        if (web.get()==WebMode.ThunderMotion
            && mc.world.getBlockState(mc.player.getBlockPos()).isOf(Blocks.COBWEB)
            && !mc.player.hasVehicle() && !mc.player.isSpectator()) {
            double f=mc.player.forwardSpeed, s=mc.player.sidewaysSpeed;
            double length=Math.hypot(f,s);
            if (length>0.01) {
                double yaw=Math.toRadians(mc.player.getYaw());
                double vx=(-Math.sin(yaw)*f-Math.cos(yaw)*s)/length;
                double vz=(Math.cos(yaw)*f-Math.sin(yaw)*s)/length;
                var current=mc.player.getVelocity();
                double target=thunderMotionSpeed.get();
                mc.player.setVelocity(vx*target,current.y,vz*target);
            }
        }
        if (web.get() == WebMode.Timer) {''')
replace_once(noslow,
'''    public enum WebMode {
        Vanilla,
        Timer,
        None
    }''',
'''    public enum WebMode {
        Vanilla,
        Timer,
        ThunderMotion,
        None
    }''')

print("AnkHack 1.0.36: AntiLegitMiss, cancellable reset, Timer TPS strategy, NoSlow ThunderMotion, one registry.")
