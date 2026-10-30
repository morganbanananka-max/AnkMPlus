#!/usr/bin/env python3
"""Install verified ThunderHack-derived convenience modules into ONE AnkHack registry.

No nested client JARs, privileged code, or fabricated stub modules.
Keep startup conservative and limit automatic command emission.
"""
from pathlib import Path
from shutil import copy2

root = Path("upstream/src/main/java/meteordevelopment/meteorclient/systems/modules")
files = ["AnkAutoTpAccept.java", "AnkAutoFlyme.java"]
for name in files:
    src = Path("unified/native") / name
    dst = root / "misc" / name
    assert src.is_file(), f"Missing real source: {src}"
    assert not dst.exists(), f"Duplicate implementation: {dst}"
    copy2(src, dst)

registry = root / "Modules.java"
code = registry.read_text()
needle = "        add(new AnkAutoDuel());"
assert code.count(needle) == 1, f"Unique module registration anchor required, found {code.count(needle)}"
insert = needle + "\n        add(new AnkAutoTpAccept());\n        add(new AnkAutoFlyme());"
registry.write_text(code.replace(needle, insert, 1))
assert registry.read_text().count("new AnkAutoTpAccept()") == 1
assert registry.read_text().count("new AnkAutoFlyme()") == 1

notices = Path("upstream/ANKHACK_LICENSE_AND_ATTRIBUTIONS.md")
with notices.open("a", encoding="utf-8") as handle:
    handle.write("""
ThunderHack Reborn 1.21.11 (GPL-3.0) gameplay adaptation:
- AnkAutoTpAccept: username-restricted teleport accept; user config required,
  strict command validation, message-pattern filtering, bounded cooldown.
- AnkAutoFlyme: existing server flight command with user-input guard,
  cooldown and attempt budget; not a server-rule bypass.
Server chat messages may be spoofed; no authentication guarantees are claimed.
""")
print("Registered 2 real ThunderHack-derived convenience modules.")
