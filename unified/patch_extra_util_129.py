#!/usr/bin/env python3
"""Install two real ThunderHack Reborn 1.21.11 utility features.

No external network access; no embedded second-client JAR.
AntiCrash blocks clearly malformed packets rather than server corrections.
VisualRange observes nearby players without retaining names on disk.
"""
from pathlib import Path
from shutil import copy2

base=Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules")
for name in ("AnkAntiCrash.java","AnkVisualRange.java"):
    src=Path("unified/native")/name
    dst=base/"misc"/name
    assert src.is_file(),src
    assert not dst.exists(),dst
    copy2(src,dst)

registry=base/"Modules.java"
data=registry.read_text()
marker="        add(new AnkServerDiagnostics());"
assert data.count(marker)==1,(marker,data.count(marker))
data=data.replace(marker,marker+"\n        add(new AnkAntiCrash());\n        add(new AnkVisualRange());",1)
registry.write_text(data)

with Path("upstream/ANKHACK_LICENSE_AND_ATTRIBUTIONS.md").open("a",encoding="utf-8") as f:
    f.write("""
Additional ThunderHack Reborn 1.21.11 GPL-3.0 source-derived modules:
- AnkAntiCrash rejects nonfinite/absurd explosion, particle and teleport data;
  does not confuse ordinary Grim position corrections with malicious packets.
- AnkVisualRange scans nearby players with bounded, configurable notices;
  does not store player identities persistently.
These are source-adapted, not guaranteed multiplayer gameplay tested.
""")
print("Registered source-backed AntiCrash and VisualRange.")
