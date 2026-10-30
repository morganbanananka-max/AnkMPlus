"""Brand AnkHack's actual compact GUI, not just the Fabric metadata.

Applies only after patch_gui_compact_127.py and fails if the expected
Minecraft 1.21.11 widget layout changed.
"""
from pathlib import Path

path=Path("upstream/src/main/java/meteordevelopment/meteorclient/gui/screens/ModulesScreen.java")
source=path.read_text(encoding="utf-8")
old='WWindow picker = theme.window("Categories");'
new='WWindow picker = theme.window("AnkHack");'
if source.count(old)!=1:
    raise RuntimeError("Compact GUI picker anchor missing or duplicated")
source=source.replace(old,new,1)
old='picker.id = "ank-category-picker";'
new='''picker.id = "ank-category-picker";
                // A gold item distinguishes AnkHack's own navigation visually.
                picker.beforeHeaderInit = header -> header.add(
                    theme.item(Items.GOLD_INGOT.getDefaultStack())).pad(2);'''
if source.count(old)!=1:
    raise RuntimeError("Compact GUI widget ID anchor missing or duplicated")
source=source.replace(old,new,1)
path.write_text(source,encoding="utf-8")
print("AnkHack compact category picker branded; preserved category navigation.")
