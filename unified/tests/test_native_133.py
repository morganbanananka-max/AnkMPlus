"""AnkHack 1.0.33 release checks: source coverage and one shared registry.

Static checks are deliberately NOT called in-game functional tests.
"""
import pathlib
import unittest

BASE=pathlib.Path("upstream/src/main/java/meteordevelopment/meteorclient")
NATIVE=BASE/"systems/modules"
REGISTRY=NATIVE/"Modules.java"
CASES={
    "AnkNoJumpDelay":("movement","AutoJump"),
    "AnkPerfectRelease":("combat","BowSpam"),
    "AnkWaterSpeed":("movement","Jesus"),
}

class Native133Tests(unittest.TestCase):
    def test_only_one_instance_registered(self):
        content=REGISTRY.read_text(encoding="utf-8")
        for mod,(family,_) in CASES.items():
            with self.subTest(mod=mod):
                self.assertEqual(content.count("add(new "+mod+"());"),1)
                p=NATIVE/family/(mod+".java")
                self.assertTrue(p.is_file())
                self.assertIn("class "+mod,p.read_text())
    def test_all_original_modules_still_registered(self):
        content=REGISTRY.read_text(encoding="utf-8")
        for _,original in CASES.values():
            self.assertEqual(content.count("add(new "+original+"());"),1)
        for important in ["AnkTriggerBot","AnkServerDiagnostics","AnkAutoBuff","AnkVisualRange"]:
            self.assertEqual(content.count("add(new "+important+"());"),1)

    def test_no_extra_hack_client_engine_or_packets(self):
        for mod,(family,_) in CASES.items():
            text=(NATIVE/family/(mod+".java")).read_text()
            with self.subTest(mod=mod):
                self.assertIn("extends Module",text)
                self.assertIn("@EventHandler",text)
                self.assertNotIn("sendPacket(",text)
                self.assertNotIn("Runtime.getRuntime()",text)
                self.assertNotIn("ProcessBuilder",text)

    def test_safety_invariants(self):
        jump=(NATIVE/"movement/AnkNoJumpDelay.java").read_text()
        self.assertIn("meteor$setJumpCooldown(0)",jump)
        self.assertIn("mc.player.isOnGround()",jump)
        self.assertIn("Modules.get().isActive(Jesus.class)",jump)
        water=(NATIVE/"movement/AnkWaterSpeed.java").read_text()
        self.assertIn("Math.min(speed*multiplier.get(),maxSpeed.get())",water)
        self.assertIn("mc.player.setVelocity(v.x*ratio,v.y,v.z*ratio)",water)
        release=(NATIVE/"combat/AnkPerfectRelease.java").read_text()
        self.assertIn("Modules.get().isActive(BowSpam.class)",release)
        self.assertIn("mc.options.useKey.isPressed()",release)
        self.assertIn("mc.interactionManager.stopUsingItem(mc.player)",release)

if __name__=="__main__":
    unittest.main()
