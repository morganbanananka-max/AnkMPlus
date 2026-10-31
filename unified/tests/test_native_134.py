"""AnkHack 1.0.34 source-level integration/regression checks.

This test suite never pretends static inspection is equivalent to Minecraft
gameplay testing. Actual Java compilation and runtime are separate CI gates.
"""
from pathlib import Path
import unittest

ROOT=Path("upstream/src/main/java/meteordevelopment/meteorclient")
MODULES=ROOT/"systems/modules"
REG=(MODULES/"Modules.java")

class Ank134Test(unittest.TestCase):
    def test_one_vehicle_module_registered(self):
        s=REG.read_text(encoding="utf-8")
        self.assertEqual(s.count("add(new AnkVehicleSpeed());"),1)
        self.assertEqual(s.count("add(new AnkWaterSpeed());"),1)
        self.assertEqual(s.count("add(new Parkour());"),1)
        self.assertEqual(s.count("add(new AnkPerfectRelease());"),1)

    def test_vehicle_bounds_and_session_reset(self):
        s=(MODULES/"movement/AnkVehicleSpeed.java").read_text(encoding="utf-8")
        for needed in ["extends Module","isChunkLoaded(cx,cz)","getControllingVehicle()",
                       "Math.hypot(forward,sideways)","speed.get()",
                       "previousServer","PlayerPositionLookS2CPacket",
                       "if (pauseTicks>0)", "Modules.get().isActive(AnkBoatFly.class)"]:
            with self.subTest(needle=needed):self.assertIn(needed,s)
        self.assertNotIn("sendPacket(",s)
        self.assertNotIn("0.80).sliderRange(1.0",s)

    def test_parkour_uses_one_module_with_distinct_modes(self):
        s=(MODULES/"movement/Parkour.java").read_text(encoding="utf-8")
        for needed in ["enum Implementation { MeteorEdge, ThunderTimed, Auto }",
                       "active==Implementation.ThunderTimed",
                       "thunderDelayMs.get()*1_000_000L",
                       "AnkVariantProfiles.serverKey(mc)",
                       "conservativeTicks>0",
                       "getBlockCollisions(mc.player,adjustedBox)"]:
            with self.subTest(needle=needed):self.assertIn(needed,s)

    def test_crossbow_no_packet_spam_and_bowspam_conflict(self):
        s=(MODULES/"combat/AnkPerfectRelease.java").read_text(encoding="utf-8")
        for needed in ["CrossbowItem.isCharged(item)","crossbowTicks.get()",
                       "Modules.get().isActive(BowSpam.class)","getItemUseTime()",
                       "stopUsingItem(mc.player)"]:
            with self.subTest(needle=needed):self.assertIn(needed,s)
        self.assertNotIn("sendPacket(",s)

    def test_no_client_jar_mod_mergers(self):
        for f in [MODULES/"movement/AnkVehicleSpeed.java",MODULES/"movement/Parkour.java",
                  MODULES/"combat/AnkPerfectRelease.java"]:
            s=f.read_text(encoding="utf-8")
            self.assertNotIn("ProcessBuilder",s)
            self.assertNotIn("Runtime.getRuntime()",s)
            self.assertNotIn("ClassLoader",s)

if __name__=="__main__":unittest.main()
