"""Merge ThunderHack's timing-based Parkour behavior into Meteor's one Parkour.

No duplicate module, one setting UI. Auto is a conservative session-local
fallback on repeated corrections; it makes no anticheat bypass guarantee.
"""
from pathlib import Path

p=Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules/movement/Parkour.java")
s=p.read_text(encoding="utf-8")
old='import meteordevelopment.meteorclient.events.world.TickEvent;'
new='''import meteordevelopment.meteorclient.AnkVariantProfiles;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import java.util.concurrent.atomic.AtomicInteger;'''
assert s.count(old)==1
s=s.replace(old,new)
old='''    public Parkour() {
        super(Categories.Movement, "parkour", "Automatically jumps at the edges of blocks.");
    }'''
new='''    // Different actual implementations, not aliases for the same behavior.
    public enum Implementation { MeteorEdge, ThunderTimed, Auto }

    private final Setting<Implementation> implementation = sgGeneral.add(new EnumSetting.Builder<Implementation>()
        .name("implementation")
        .description("Meteor edge detection, ThunderHack timed edge jumps, or guarded Auto.")
        .defaultValue(Implementation.Auto).build());

    private final Setting<Integer> thunderDelayMs = sgGeneral.add(new IntSetting.Builder()
        .name("thunder-min-delay-ms")
        .description("Minimum delay between ThunderTimed jumps.")
        .defaultValue(150).range(80,500).sliderRange(80,500)
        .visible(() -> implementation.get() != Implementation.MeteorEdge).build());

    private final AtomicInteger pendingCorrections = new AtomicInteger();
    private String server="unknown";
    private int worldTicks;
    private int correctionWindowTicks;
    private int correctionsInWindow;
    private int conservativeTicks;
    private long lastJumpNanos;

    public Parkour() {
        super(Categories.Movement, "parkour", "Automatically jumps at edges with Meteor/Thunder timing modes.");
    }

    @Override public void onActivate() { reset(); server=AnkVariantProfiles.serverKey(mc); }
    @Override public void onDeactivate() { reset(); }

    private void reset() {
        worldTicks=0;
        correctionWindowTicks=0;
        correctionsInWindow=0;
        conservativeTicks=0;
        lastJumpNanos=0;
        pendingCorrections.set(0);
    }

    @EventHandler
    private void onCorrection(PacketEvent.Receive e) {
        if(e.packet instanceof PlayerPositionLookS2CPacket) pendingCorrections.incrementAndGet();
    }

    private Implementation effective() {
        if(implementation.get()!=Implementation.Auto) return implementation.get();
        return conservativeTicks>0 ? Implementation.ThunderTimed : Implementation.MeteorEdge;
    }

    @Override public String getInfoString() {
        return (implementation.get()==Implementation.Auto ? "Auto: " : "")+effective().name();
    }'''
assert s.count(old)==1
s=s.replace(old,new)
old='''    @EventHandler
    private void onTick(TickEvent.Post event) {
        if(!mc.player.isOnGround() || mc.options.jumpKey.isPressed()) return;

        if(mc.player.isSneaking() || mc.options.sneakKey.isPressed()) return;

        Box box = mc.player.getBoundingBox();
        Box adjustedBox = box.offset(0, -0.5, 0).expand(-edgeDistance.get(), 0, -edgeDistance.get());

        Stream<VoxelShape> blockCollisions = Streams.stream(mc.world.getBlockCollisions(mc.player, adjustedBox));

        if(blockCollisions.findAny().isPresent()) return;

        mc.player.jump();
    }'''
new='''    @EventHandler
    private void onTick(TickEvent.Post event) {
        if(mc.player==null || mc.world==null) {
            reset();server="unknown";return;
        }
        String current=AnkVariantProfiles.serverKey(mc);
        if(!current.equals(server)) { reset();server=current; }
        worldTicks++;
        int corrections=pendingCorrections.getAndSet(0);
        if(worldTicks>100 && implementation.get()==Implementation.Auto) {
            correctionsInWindow+=Math.min(100,corrections);
            if(++correctionWindowTicks>=120) {
                // World corrections can also mean teleportation or lag.
                // Treat them as a temporary caution signal, not an anticheat flag.
                if(correctionsInWindow>=3) conservativeTicks=400;
                correctionsInWindow=0;correctionWindowTicks=0;
            }
            if(conservativeTicks>0)conservativeTicks--;
        }
        if(mc.currentScreen!=null || !mc.player.isOnGround()
            || mc.options.jumpKey.isPressed() || mc.player.isSneaking()
            || mc.options.sneakKey.isPressed() || mc.player.isTouchingWater()
            || mc.player.isInLava() || mc.player.hasVehicle()) return;
        if(Math.abs(mc.player.forwardSpeed)+Math.abs(mc.player.sidewaysSpeed)<0.01f) return;

        Implementation active=effective();
        long now=System.nanoTime();
        if(active==Implementation.ThunderTimed
            && lastJumpNanos>0 && now-lastJumpNanos<thunderDelayMs.get()*1_000_000L) return;

        Box box=mc.player.getBoundingBox();
        double margin=active==Implementation.ThunderTimed
            ? Math.max(0.01,edgeDistance.get()) : edgeDistance.get();
        Box adjustedBox=box.offset(0,-0.5,0).expand(-margin,0,-margin);
        Stream<VoxelShape> collisions=Streams.stream(mc.world.getBlockCollisions(mc.player,adjustedBox));
        if(collisions.findAny().isPresent())return;
        mc.player.jump();
        lastJumpNanos=now;
    }'''
assert s.count(old)==1
s=s.replace(old,new)
p.write_text(s,encoding="utf-8")
print("Parkour merged: MeteorEdge/ThunderTimed/Auto modes in one existing module.")
