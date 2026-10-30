#!/usr/bin/env python3
"""Register a functional vanilla-hit-result TriggerBot in AnkHack, not a stub."""
from pathlib import Path
from shutil import copy2

root=Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules")
src=Path("unified/native/AnkTriggerBot.java")
dest=root/"combat/AnkTriggerBot.java"
assert src.is_file() and not dest.exists()
copy2(src,dest)
reg=root/"Modules.java"
s=reg.read_text()
anchor="        add(new AnkAutoBuff());"
assert s.count(anchor)==1,(anchor,s.count(anchor))
reg.write_text(s.replace(anchor,anchor+"\n        add(new AnkTriggerBot());",1))
with Path("upstream/ANKHACK_LICENSE_AND_ATTRIBUTIONS.md").open("a",encoding="utf-8") as f:
    f.write("""
ThunderHack Reborn 1.21.11 TriggerBot source-derived adaptation (GPL-3.0):
  AnkTriggerBot requires the actual vanilla EntityHitResult and attack
  cooldown; does not extend reach or spoof attack/movement packets.
  No claim of undetectability under Grim or another anti-cheat.
""")
print("AnkTriggerBot registered with real attack logic.")
