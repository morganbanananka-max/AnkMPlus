"""Fail-closed regression gate for AnkHack 1.0.37's shared adaptive engine.

Pure Java behavioral test covers 20k simulated sessions and is separately
executed via javac in CI. These checks verify that the real client uses it.
"""
from pathlib import Path
import unittest

ROOT=Path("upstream/src/main/java/meteordevelopment/meteorclient")

class Adaptive137Tests(unittest.TestCase):
    def test_engine_is_real_and_wired(self):
        profile=(ROOT/"AnkVariantProfiles.java").read_text()
        engine=(ROOT/"AnkVariantDecision.java").read_text()
        self.assertIn("return AnkVariantDecision.choose(available,evidence);",profile)
        self.assertIn("new AnkVariantDecision.Evidence(",profile)
        self.assertIn("public record Evidence(",engine)
        self.assertIn("if (sample == null || !sample.credible()) continue;",engine)
        self.assertIn("return successes() >= 3;",engine)
        self.assertIn("Math.log1p(observations)",engine)
        self.assertIn("12.0 * Math.min(1000, value.corrections())",engine)

    def test_families_are_explicit_no_fake_defaults(self):
        profile=(ROOT/"AnkVariantProfiles.java").read_text()
        for family in ("flight","sprint","auto-fish","auto-gap","bow-spam"):
            self.assertIn('case "'+family+'"',profile)
        self.assertIn("default -> new String[0];",profile)
        self.assertIn('if(server==null || "unknown".equals(server))return available[0];',profile)

    def test_real_thunder_tape_is_a_mode_not_an_extra_client(self):
        registry=(ROOT/"systems/modules/Modules.java").read_text()
        self.assertEqual(1,registry.count("add(new AutoClicker());"))
        code=(ROOT/"systems/modules/player/AutoClicker.java").read_text()
        self.assertEqual(2,code.count("case ThunderTape ->"))
        self.assertIn("ThreadLocalRandom.current().nextInt(tapeJitter.get()+1)",code)
        self.assertIn("System.nanoTime()",code)
        self.assertIn("Math.max(200,tapeDelay.get())",code)
        self.assertIn("mc.crosshairTarget instanceof EntityHitResult",code)
        self.assertIn("mc.currentScreen==null",code)
        self.assertIn("Utils.leftClick()",code)
        self.assertIn("Utils.rightClick()",code)
        self.assertNotIn("sendPacket(",code)
        self.assertNotIn("ProcessBuilder",code)
        self.assertNotIn("ClassLoader",code)

if __name__=="__main__":unittest.main()
