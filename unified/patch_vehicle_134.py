"""Register vehicle speed once, preserving existing native and Meteor modules."""
from pathlib import Path
import shutil

root=Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules")
registry=root/"Modules.java"
content=registry.read_text(encoding="utf-8")
anchor="        add(new AnkWaterSpeed());"
addition="        add(new AnkVehicleSpeed());"
if content.count(anchor)!=1:raise RuntimeError("Expected native water speed anchor missing")
if content.count(addition)>1:raise RuntimeError("Duplicate vehicle module")
if addition not in content:
    content=content.replace(anchor,anchor+"\n"+addition,1)
source=Path("unified/native/AnkVehicleSpeed.java")
code=source.read_text(encoding="utf-8")
assert "package meteordevelopment.meteorclient.systems.modules.movement;" in code
assert "class AnkVehicleSpeed extends Module" in code
target=root/"movement/AnkVehicleSpeed.java"
shutil.copy2(source,target)
registry.write_text(content,encoding="utf-8")
assert content.count(addition)==1
print("Registered independent vehicle speed once.")
