"""AnkHack 1.0.36 independently checkable native integration contract.

This catches regressions, duplicates and unsafe defaults. Java compilation
and actual Minecraft GUI startup are separate hard CI gates, not inferred here.
"""
from pathlib import Path
import unittest

ROOT=Path("upstream/src/main/java/meteordevelopment/meteorclient")

def file(relative):
    return (ROOT/relative).read_text(encoding="utf-8")

class AnkHack136Tests(unittest.TestCase):
    def test_unique_registered_antilegitmiss(self):
        registry=file("systems/modules/Modules.java")
        self.assertEqual(1,registry.count("add(new AnkAntiLegitMiss());"))
        self.assertEqual(1,registry.count("add(new AnkTriggerBot());"))
        self.assertIn("extends Module",file("systems/modules/combat/AnkAntiLegitMiss.java"))

    def test_no_sticky_cancel_after_missed_attack(self):
        code=file("events/entity/player/DoAttackEvent.java")
        self.assertIn("INSTANCE.setCancelled(false);",code)
        self.assertLess(code.index("INSTANCE.setCancelled(false);"),code.index("return INSTANCE;"))
        code=file("systems/modules/combat/AnkAntiLegitMiss.java")
        self.assertIn("event.setCancelled(true)",code)
        self.assertIn("HitResult.Type.MISS",code)
        self.assertIn(".name(\"block-hits\")",code)
        self.assertIn(".defaultValue(false)",code)
        self.assertNotIn("sendPacket(",code)

    def test_tps_strategy_real_algorithm(self):
        timer=file("systems/modules/world/Timer.java")
        self.assertEqual(1,timer.count("enum Strategy { Fixed, ThunderServerSync }"))
        self.assertIn("TickRate.INSTANCE.getTickRate()",timer)
        self.assertIn("TickRate.INSTANCE.getTimeSinceLastTick() >= 3.0f",timer)
        self.assertIn("mc.isInSingleplayer()",timer)
        self.assertIn("Math.max(minServerMultiplier.get(), Math.min(1.0, estimated / 20.0))",timer)
        self.assertIn("if (override != OFF) return override;",timer)
        self.assertIn(".defaultValue(Strategy.Fixed)",timer)

    def test_one_noslow_with_real_thunder_motion(self):
        reg=file("systems/modules/Modules.java")
        self.assertEqual(1,reg.count("add(new NoSlow());"))
        s=file("systems/modules/movement/NoSlow.java")
        self.assertIn("ThunderMotion,",s)
        self.assertIn("mc.player.setVelocity(vx*target,current.y,vz*target)",s)
        self.assertIn(".range(0.02,0.20)",s)
        self.assertIn("Modules.get().get(Timer.class).setOverride(Timer.OFF)",s)
        self.assertIn("if (mc.player==null || mc.world==null) return;",s)
        self.assertNotIn("sendPacket(",s)

    def test_no_engine_duplication(self):
        for f in ["systems/modules/combat/AnkAntiLegitMiss.java",
                  "systems/modules/world/Timer.java",
                  "systems/modules/movement/NoSlow.java"]:
            code=file(f)
            self.assertNotIn("ProcessBuilder",code)
            self.assertNotIn("Runtime.getRuntime()",code)
            self.assertNotIn("ClassLoader",code)

if __name__=="__main__":
    unittest.main()
