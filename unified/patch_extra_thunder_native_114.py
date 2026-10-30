#!/usr/bin/env python3
"""Port three genuine gameplay behaviors from user-provided ThunderHack Reborn
1.21.11 to AnkHack's ONE Meteor event bus and module registry. GPL-3.0.
No separate hack client is loaded; each class has real runtime behavior.
"""
from pathlib import Path
import shutil
base=Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules")
files={
    "AnkAntiAttack.java":"player",
    "AnkDurabilityAlert.java":"player",
    "AnkElytraRecast.java":"movement",
}
for filename,part in files.items():
    source=Path("unified/native")/filename
    target=base/part/filename
    assert source.is_file(),filename
    assert not target.exists(),filename
    shutil.copy2(source,target)

reg=base/"Modules.java"
s=reg.read_text()
edits={
    "        add(new AutoTool());":
        "        add(new AutoTool());\n        add(new AnkAntiAttack());\n        add(new AnkDurabilityAlert());",
    "        add(new ElytraFly());":
        "        add(new ElytraFly());\n        add(new AnkElytraRecast());"
}
for old,new in edits.items():
    assert s.count(old)==1,(old,s.count(old))
    s=s.replace(old,new,1)
reg.write_text(s)

notice=Path("upstream/ANKHACK_LICENSE_AND_ATTRIBUTIONS.md")
with notice.open("a") as f:
    f.write("""
Additional ThunderHack Reborn 1.21.11 GPL-3.0 adaptations:
- AnkAntiAttack blocks selected accidental attacks using Meteor attack event.
- AnkDurabilityAlert warns when equipped armor approaches breaking.
- AnkElytraRecast sends ordinary fall-flying command while descending.
These are carefully limited source-based reimplementations, not full ports of
all ThunderHack settings or exploit modes. Gameplay tests still required.
""")
print("Registered 3 additional ThunderHack-derived AnkHack gameplay modules")
