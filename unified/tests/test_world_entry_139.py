"""Regression gate for AnkHack's world-entry crash resilience.

This confirms the exact codepaths that used to dereference the player before
world load and validates saved module recovery. Does not claim to reproduce
the user's unprovided crash stack trace.
"""
from pathlib import Path
import unittest

root=Path("upstream/src/main/java/meteordevelopment/meteorclient")
def java(path):
    return (root/path).read_text(encoding="utf-8")

class WorldEntry139Tests(unittest.TestCase):
    def test_flight_protects_join_packet_before_player_exists(self):
        s=java("systems/modules/movement/Flight.java")
        self.assertIn('if (mc.player == null || mc.world == null) return;',s)
        p=s.index("private void onReceivePacket(PacketEvent.Receive event)")
        cancel=s.index("event.cancel(); // Cancel packet",p)
        self.assertLess(s.index("if (mc.player == null || mc.world == null) return;",p),cancel)
        self.assertIn("PlayerAbilitiesS2CPacket",s[p:cancel])
        self.assertIn("if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;",s)
        self.assertIn("if (mc.player == null || mc.world == null || mc.interactionManager == null) return;",s)
        self.assertIn("if (mc.player == null || mc.world == null) return;\n        mc.player.getAbilities().flying = false;",s)

    def test_no_saved_mod_runs_until_real_world_is_ready(self):
        s=java("systems/modules/Modules.java")
        self.assertIn("private boolean pendingWorldActivation;",s)
        self.assertIn("pendingWorldActivation=true;",s)
        self.assertIn("activateSavedModulesAfterWorldReady(TickEvent.Post event)",s)
        self.assertIn("mc.player == null || mc.world == null || mc.interactionManager == null",s)
        self.assertIn("|| mc.getNetworkHandler() == null) return;",s)
        start=s.index("activateSavedModulesAfterWorldReady(TickEvent.Post event)")
        self.assertLess(s.index("pendingWorldActivation=false;",start),s.index("module.onActivate();",start))
        self.assertIn('Saved module {} failed on world entry; disabling it',s)
        self.assertIn("module.deactivateAfterActivationFailure();",s)
        self.assertIn("private void onGameLeft(GameLeftEvent event)",s)

    def test_failed_module_is_quarantined_without_calling_on_deactivate_again(self):
        s=java("systems/modules/Module.java")
        start=s.index("public final void deactivateAfterActivationFailure()")
        body=s[start:start+480]
        self.assertIn("MeteorClient.EVENT_BUS.unsubscribe(this);",body)
        self.assertIn("active=false;",body)
        self.assertIn("Modules.get().removeActive(this);",body)
        self.assertNotIn("onDeactivate();",body)

    def test_gui_remains_pinned_to_approved_branch(self):
        p=java("gui/screens/ModulesScreen.java")
        self.assertIn("new AnkCategoryNavButton(",p)
        self.assertIn("new AnkMenuBackdrop()",p)
        self.assertIn("private boolean useAnkLayout() { return true; }",p)

if __name__=="__main__": unittest.main()
