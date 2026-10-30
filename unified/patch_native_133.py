"""Register native AnkHack gameplay features in the one existing client registry.

Requires the preceding 1.0.32 patches. Reentrant and fail-closed.
All features use Meteor's Fabric module lifecycle and shared event bus.
"""
from pathlib import Path
import shutil

java=Path("upstream/src/main/java/meteordevelopment/meteorclient")
root=java/"systems/modules"
sources={
    "movement/AnkNoJumpDelay.java":("AutoJump", "AnkNoJumpDelay"),
    "combat/AnkPerfectRelease.java":("BowSpam", "AnkPerfectRelease"),
    "movement/AnkWaterSpeed.java":("Jesus", "AnkWaterSpeed"),
}
registry=root/"Modules.java"
existing=registry.read_text(encoding="utf-8")
changed=existing
for rel,(original,feature) in sources.items():
    anchor=f"        add(new {original}());"
    addition=f"        add(new {feature}());"
    if changed.count(anchor)!=1:
        raise RuntimeError(f"Expected one registry anchor {original}, found {changed.count(anchor)}")
    if changed.count(addition)>1:
        raise RuntimeError(f"Duplicate native registration {feature}")
    if addition not in changed:
        changed=changed.replace(anchor,anchor+"\n"+addition,1)

# Do not write a partially patched registry when an anchor is missing.
for rel,(original,feature) in sources.items():
    src=Path("unified/native")/(feature+".java")
    text=src.read_text(encoding="utf-8")
    pkg="meteordevelopment.meteorclient.systems.modules."+rel.split("/")[0]
    if ("package "+pkg+";") not in text or ("class "+feature+" ") not in text:
        raise RuntimeError("Native module source did not pass package/class validation: "+feature)
    dst=root/rel
    dst.parent.mkdir(parents=True,exist_ok=True)
    shutil.copy2(src,dst)
registry.write_text(changed,encoding="utf-8")
print("Registered 3 native AnkHack modules, keeping existing registry and variants.")
