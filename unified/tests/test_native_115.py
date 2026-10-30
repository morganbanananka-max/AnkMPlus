"""Offline regression checks for AnkHack native integration.

These tests require only Python's standard library. They test patch wiring,
not a Minecraft runtime or gameplay behavior.
"""
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

REPO = Path(__file__).resolve().parents[2]
PATCH = REPO / "unified" / "patch_native_115.py"
MODULES = {
    "AnkTriggerBot": "combat/AnkTriggerBot.java",
    "AnkServerDiagnostics": "misc/AnkServerDiagnostics.java",
}
REGISTRY = Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules/Modules.java")


class NativeIntegrationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.work = Path(self.temp.name)
        folder = self.work / "unified" / "native"
        folder.mkdir(parents=True)
        shutil.copy2(PATCH, self.work / "unified" / "patch_native_115.py")
        for module, relpath in MODULES.items():
            shutil.copy2(REPO / "unified" / "native" / (module + ".java"),
                         folder / (module + ".java"))
        (self.work / REGISTRY).parent.mkdir(parents=True)

    def execute(self):
        return subprocess.run(
            [sys.executable, "unified/patch_native_115.py"],
            cwd=self.work, text=True, capture_output=True,
        )

    def test_registers_modules_once_in_one_registry(self):
        (self.work / REGISTRY).write_text(
            "public class Modules {\n"
            "  void initCombat() { add(new KillAura()); }\n"
            "  void initMisc() { add(new PacketLogger()); }\n"
            "}\n", encoding="utf-8",
        )
        first = self.execute()
        self.assertEqual(0, first.returncode, first.stderr)
        second = self.execute()
        self.assertEqual(0, second.returncode, second.stderr)
        registry = (self.work / REGISTRY).read_text(encoding="utf-8")
        for name, relative_path in MODULES.items():
            self.assertEqual(1, registry.count("add(new " + name + "());"))
            code = (self.work / REGISTRY.parent / relative_path).read_text()
            self.assertIn("class " + name, code)
        self.assertEqual(1, registry.count("add(new KillAura());"))
        self.assertEqual(1, registry.count("add(new PacketLogger());"))

    def test_fails_closed_when_registry_anchor_changes(self):
        original = "public class Modules { void initCombat(){ add(new KillAura()); } }"
        (self.work / REGISTRY).write_text(original, encoding="utf-8")
        result = self.execute()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Expected unique registry anchor", result.stderr)
        self.assertEqual(original, (self.work / REGISTRY).read_text())

    def test_no_client_jar_bundles_or_network_code(self):
        for module in MODULES:
            code = (REPO / "unified" / "native" / (module + ".java")).read_text()
            self.assertIn("public final class " + module, code)
            self.assertIn("@EventHandler", code)
            self.assertNotIn("Runtime.getRuntime()", code)
            self.assertNotIn("ProcessBuilder", code)
        diagnostics = (REPO / "unified/native/AnkServerDiagnostics.java").read_text()
        self.assertNotIn("setCancelled(", diagnostics)
        self.assertIn("PlayerPositionLookS2CPacket", diagnostics)
        trigger = (REPO / "unified/native/AnkTriggerBot.java").read_text()
        self.assertIn("squaredDistanceTo(target) > 9.0d", trigger)
        self.assertIn("getAttackCooldownProgress(", trigger)
        self.assertIn("Friends.get().isFriend(player)", trigger)


if __name__ == "__main__":
    unittest.main()
