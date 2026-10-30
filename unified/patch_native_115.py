"""Register independent native AnkHack modules in the single existing registry.

Run after other native patches against the pinned Meteor source checkout.
Idempotent and fail-closed: unexpected upstream layouts stop the build.
"""
from pathlib import Path

root = Path("upstream/src/main/java/meteordevelopment/meteorclient")
modules = root / "systems/modules"
sources = {
    "combat/AnkTriggerBot.java": "unified/native/AnkTriggerBot.java",
    "misc/AnkServerDiagnostics.java": "unified/native/AnkServerDiagnostics.java",
}
for relative, original in sources.items():
    destination = modules / relative
    destination.parent.mkdir(parents=True, exist_ok=True)
    text = Path(original).read_text(encoding="utf-8")
    assert "package meteordevelopment.meteorclient.systems.modules." in text
    destination.write_text(text, encoding="utf-8")

registry = modules / "Modules.java"
text = registry.read_text(encoding="utf-8")
updates = [
    ("add(new KillAura());", "add(new AnkTriggerBot());"),
    ("add(new PacketLogger());", "add(new AnkServerDiagnostics());"),
]
for anchor, addition in updates:
    if text.count(addition) > 1:
        raise RuntimeError("Duplicate registry entry: " + addition)
    if addition not in text:
        if text.count(anchor) != 1:
            raise RuntimeError("Expected unique registry anchor: " + anchor)
        text = text.replace(anchor, addition + "\n        " + anchor, 1)
registry.write_text(text, encoding="utf-8")
for _, addition in updates:
    assert text.count(addition) == 1
print("Two native modules integrated into one existing registry (not nested clients).")
