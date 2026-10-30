#!/usr/bin/env python3
"""Integrate three actual source adaptations into the one native module registry.
Run after patch_unified.py, patch_flight_variants.py,
patch_crossclient_103.py and patch_sprint_auto.py.
"""
from pathlib import Path
import shutil
base=Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules")
registry=base/"Modules.java"
sources={
 "AnkAutoSwim.java":base/"movement/AnkAutoSwim.java",
 "AnkGlide.java":base/"movement/AnkGlide.java",
 "AnkCombatRod.java":base/"combat/AnkCombatRod.java",
 "AnkAutoSoup.java":base/"player/AnkAutoSoup.java",
 "AnkToolSaver.java":base/"player/AnkToolSaver.java",
 "AnkNoServerSlot.java":base/"player/AnkNoServerSlot.java",
 "AnkViewLock.java":base/"player/AnkViewLock.java",
 "AnkAntiBadEffects.java":base/"player/AnkAntiBadEffects.java",
 "AnkLevitationControl.java":base/"movement/AnkLevitationControl.java",
 "AnkJetpack.java":base/"movement/AnkJetpack.java",
}
for name,target in sources.items():
    origin=Path("unified/native")/name
    if not origin.is_file():raise RuntimeError("Missing native module source: "+name)
    shutil.copy2(origin,target)

s=registry.read_text()
for old,new in [
 ("        add(new AnkSmartMace());","        add(new AnkSmartMace());\n        add(new AnkCombatRod());"),
 ("        add(new AnkBoatFly());","        add(new AnkBoatFly());\n        add(new AnkAutoSwim());\n        add(new AnkGlide());"),
 ("        add(new AutoFish());","        add(new AutoFish());\n        add(new AnkAutoSoup());\n        add(new AnkToolSaver());\n        add(new AnkNoServerSlot());\n        add(new AnkViewLock());\n        add(new AnkAntiBadEffects());"),
 ("        add(new AnkGlide());","        add(new AnkGlide());\n        add(new AnkLevitationControl());\n        add(new AnkJetpack());"),
]:
    assert s.count(old)==1,(old,s.count(old))
    s=s.replace(old,new,1)
registry.write_text(s)
notice=Path("upstream/ANKHACK_LICENSE_AND_ATTRIBUTIONS.md")
with notice.open("a") as out:
    out.write("""
Additional native adaptations (GPL-3.0):
- AutoSoup, ToolSaver, NoServerSlot, ViewLock, AntiBadEffects, LevitationControl from the user-supplied ThunderHack Reborn 1.21.11 archive.
- Wurst AutoSwim, Glide and Jetpack behaviors, copyright Wurst-Imperium/contributors.
- LiquidBounce AutoRod concept, copyright CCBlueX.
- Unlike LiquidBounce AutoRod's original implementation, AnkCombatRod
  currently requires the player to aim manually. It is not a full port.
All run under AnkHack's existing module registry, settings and event bus.
""")
print("Installed native AnkAutoSwim, AnkGlide, AnkCombatRod in one module registry")
