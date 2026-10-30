#!/usr/bin/env python3
"""Install three adapted ThunderHack Reborn 1.21.11 features with working logic.
No extra hack-client JAR, one shared module registry and settings interface.
"""
from pathlib import Path
from shutil import copy2

root=Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules")
targets={
    "AnkAutoDuel.java":"misc",
    "AnkElytraReplace.java":"player",
    "AnkNoServerRotate.java":"player",
}
for name,area in targets.items():
    source=Path("unified/native") / name
    target=root / area / name
    assert source.is_file() and not target.exists(),name
    copy2(source,target)
reg=root/"Modules.java"
s=reg.read_text()
anchors={
    "        add(new AutoReconnect());":"        add(new AutoReconnect());\n        add(new AnkAutoDuel());",
    "        add(new AutoRespawn());":"        add(new AutoRespawn());\n        add(new AnkElytraReplace());\n        add(new AnkNoServerRotate());",
}
for old,new in anchors.items():
    assert s.count(old)==1,(old,s.count(old))
    s=s.replace(old,new,1)
reg.write_text(s)
with Path("upstream/ANKHACK_LICENSE_AND_ATTRIBUTIONS.md").open("a") as f:
    f.write("""
Additional ThunderHack Reborn 1.21.11 GPL-3.0 adaptations:
- AnkAutoDuel: explicit username-matched duel requests, accept mode only
- AnkElytraReplace: safer vanilla inventory swaps with configurable thresholds
- AnkNoServerRotate: conservative local camera restoration on next tick
The source is GPL-3.0 compatible, game behavior requires real-world testing.
""")
print("Registered three new source-derived gameplay modules.")
