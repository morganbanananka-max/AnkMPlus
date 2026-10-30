#!/usr/bin/env python3
"""Ensure AnkHack's actual ClickGUI remains usable in Minecraft's small 854x480
client window, instead of clipping the second row of category windows.
No visual mockups: modifies live Meteor/AnkHack widget code.
"""
from pathlib import Path
p=Path("upstream/src/main/java/meteordevelopment/meteorclient/gui/screens/ModulesScreen.java")
s=p.read_text()

def edit(before,after):
    global s
    count=s.count(before)
    if count!=1:raise RuntimeError("Responsive GUI patch expected one anchor, got "+str(count)+": "+repr(before[:90]))
    s=s.replace(before,after,1)

edit("""        // Help
        WVerticalList help = add(theme.verticalList()).pad(4).bottom().widget();
        help.add(theme.label("Left click - Toggle module"));
        help.add(theme.label("Right click - Open module settings"));""",
"""        // On smaller Minecraft windows this help text covers the second row
        // of category windows, so reserve the space for actual controls.
        if (getWindowHeight() > theme.scale(560)) {
            WVerticalList help = add(theme.verticalList()).pad(4).bottom().widget();
            help.add(theme.label("Left click - Toggle module"));
            help.add(theme.label("Right click - Open module settings"));
        }""")
edit("""        w.view.hasScrollBar = false;
        w.view.spacing = 0;

        for (Module module : moduleList) {""",
"""        w.view.hasScrollBar = false;
        w.view.spacing = 0;
        // The old full-height categories made second-row windows disappear
        // below the bottom of the screen. Keep their *contents* scrollable.
        // A half-height view permits a practical two-row layout at 854x480.
        w.view.maxHeight = Math.min(w.view.maxHeight,
            Math.max(theme.scale(120), (getWindowHeight() - theme.scale(130)) / 2));

        for (Module module : moduleList) {""")
p.write_text(s)
print("AnkHack responsive category scroll views and low-resolution help layout installed.")
