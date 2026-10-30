"""Insert a runtime marker that is emitted only when AnkHack's actual GUI opens.

This marker is used by CI; a non-black screenshot of vanilla Minecraft alone
must never count as successful verification of AnkHack's module screen.
"""
from pathlib import Path

path = Path("upstream/src/main/java/meteordevelopment/meteorclient/MeteorClient.java")
source = path.read_text(encoding="utf-8")
anchor = "private void onOpenScreen(OpenScreenEvent event) {"
marker = 'LOG.info("ANKHACK_GUI_OPENED");'
if source.count(marker) > 1:
    raise RuntimeError("Duplicate GUI runtime markers")
if marker not in source:
    if source.count(anchor) != 1:
        raise RuntimeError("Unexpected MeteorClient GUI event source layout")
    source = source.replace(anchor,
        anchor + """
        // CI requires proof the actual mod GUI opened, not the vanilla title screen.
        if (event.screen instanceof meteordevelopment.meteorclient.gui.tabs.TabScreen)
            LOG.info("ANKHACK_GUI_OPENED");""", 1)
path.write_text(source, encoding="utf-8")
assert source.count(marker) == 1
print("Added runtime GUI-open confirmation marker.")
