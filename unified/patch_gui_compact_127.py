#!/usr/bin/env python3
"""Use a functional category picker rather than clipped floating windows at 854x480.

The prior responsive patch had scrolling *inside* each category but still
placed multiple rows of category windows below the screen. This installs a
compact view with persistent search, a scrolling list of category buttons,
and ONE visible category. For normal resolutions retain the multi-window UI.
"""
from pathlib import Path

p=Path("upstream/src/main/java/meteordevelopment/meteorclient/gui/screens/ModulesScreen.java")
s=p.read_text()

def edit(old,new):
    global s
    count=s.count(old)
    assert count==1, f"Compact GUI anchor count {count}: {old[:100]!r}"
    s=s.replace(old,new,1)

edit("import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;",
     "import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;\nimport meteordevelopment.meteorclient.gui.widgets.pressable.WButton;")

edit("""            windows.add(createSearch(this));
            for (Category category : Modules.loopCategories()) {""",
"""            windows.add(createSearch(this));

            // Compact layout at Minecraft's 854x480 default window:
            // search + scrollable category picker + ONE visible category.
            // The earlier two-row floating layout made later categories
            // unreachable below the bottom of the screen.
            if (getWindowHeight() <= theme.scale(560)) {
                WWindow picker = theme.window("Categories");
                picker.id = "ank-category-picker";
                picker.padding = 0;
                picker.spacing = 0;
                add(picker);
                windows.add(picker);
                picker.view.maxHeight = Math.min(theme.scale(300), getWindowHeight() - theme.scale(120));
                picker.view.scrollOnlyWhenMouseOver = true;

                List<WWindow> compactWindows = new ArrayList<>();
                for (Category category : Modules.loopCategories()) {
                    List<Module> categoryModules = new ArrayList<>();
                    for (Module module : Modules.get().getGroup(category)) {
                        if (!Config.get().hiddenModules.get().contains(module)) categoryModules.add(module);
                    }
                    if (categoryModules.isEmpty()) continue;
                    WWindow categoryWindow = createCategory(this, category, categoryModules);
                    categoryWindow.view.maxHeight = Math.min(theme.scale(300), getWindowHeight() - theme.scale(120));
                    categoryWindow.visible = compactWindows.isEmpty();
                    compactWindows.add(categoryWindow);
                    windows.add(categoryWindow);

                    WButton button = picker.add(theme.button(category.name)).expandX().widget();
                    button.action = () -> {
                        for (WWindow window : compactWindows) window.visible = false;
                        categoryWindow.visible = true;
                        invalidate();
                    };
                }
                return;
            }
            for (Category category : Modules.loopCategories()) {""")

edit("""        protected void refresh() {
            if (favorites == null) {""",
"""        protected void refresh() {
            // Search covers favorites in compact mode without adding another
            // off-screen window. Full-size windows retain existing favorites.
            if (getWindowHeight() <= theme.scale(560)) return;
            if (favorites == null) {""")

edit("""            double y = this.y;
            double rowHeight = 0;""",
"""            // Reserve space for AnkHack's global top navigation. The old
            // controller started at y=0 and rendered over the tabs.
            double y = this.y + theme.scale(38);
            double rowHeight = 0;""")
edit("""            for (Cell<?> cell : cells) {
                double windowWidth = getWindowWidth();""",
"""            for (Cell<?> cell : cells) {
                if (!cell.widget().visible) continue;
                double windowWidth = getWindowWidth();""")
p.write_text(s)
print("Compact 854x480 GUI with scrollable category selector and visible-window layout.")
