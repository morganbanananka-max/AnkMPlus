"""Correct AnkHack identity without hiding upstream GPL contributors."""
from pathlib import Path
import json

path = Path("upstream/src/main/resources/fabric.mod.json")
meta = json.loads(path.read_text(encoding="utf-8"))
if meta.get("id") != "ankhack":
    raise RuntimeError("Only patch unified AnkHack client metadata")
meta["name"] = "AnkHack"
meta["description"] = (
    "One Fabric client with original AnkHack features, GPL-compatible integrations "
    "and a standalone LGPL Baritone library. Based on Meteor Client; upstream "
    "credits and licenses are preserved."
)
custom = meta.setdefault("custom", {})
custom["meteor-client:color"] = "255,201,55"
modmenu = custom.get("modmenu")
if isinstance(modmenu, dict):
    links = modmenu.get("links")
    if isinstance(links, dict):
        # Meteor's Discord is not the official support channel for AnkHack.
        links.pop("modmenu.discord", None)
        if not links:
            modmenu.pop("links", None)
    if not modmenu:
        custom.pop("modmenu", None)
path.write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
assert meta.get("license") == "GPL-3.0"
assert custom["meteor-client:color"] == "255,201,55"
assert meta["name"] == "AnkHack"
print("Corrected client branding; preserved original author credits.")
