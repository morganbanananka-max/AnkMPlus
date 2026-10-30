#!/usr/bin/env python3
"""Improve the ACTUAL AnkHack in-game Meteor-derived module GUI, not an image mockup.
Search-first position, description search, and responsive nonoverlapping category layout.
"""
from pathlib import Path
p=Path("upstream/src/main/java/meteordevelopment/meteorclient/gui/screens/ModulesScreen.java")
s=p.read_text()
def edit(a,b,n=1):
 global s
 actual=s.count(a)
 if actual!=n:raise RuntimeError(f"GUI anchor count {actual}/{n} for {a[:70]!r}")
 s=s.replace(a,b,n)
edit("import java.util.ArrayList;",
"""import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;""")
edit("""            List<Pair<Module, String>> modules = Modules.get().searchTitles(text);""",
"""            List<Pair<Module, String>> modules = new ArrayList<>(Modules.get().searchTitles(text));
            // Include description matches, not just display names. Use a case-folded
            // string to make searching 'glide', 'sprint', 'food', etc practical.
            String needle = text.strip().toLowerCase(Locale.ROOT);
            Set<Module> already = new HashSet<>();
            for (Pair<Module,String> p : modules) already.add(p.getLeft());
            for (Module module : Modules.get().getAll()) {
                if (already.contains(module) || Config.get().hiddenModules.get().contains(module)) continue;
                if (module.description != null &&
                    module.description.toLowerCase(Locale.ROOT).contains(needle)) {
                    modules.add(new Pair<>(module,module.name));
                    already.add(module);
                }
            }""")
edit("""            for (Category category : Modules.loopCategories()) {""",
"""            // Search first: immediately accessible at the top-left, never hidden
            // after six or more windows on a narrow Minecraft client.
            windows.add(createSearch(this));
            for (Category category : Modules.loopCategories()) {""")
edit("""            windows.add(createSearch(this));

            refresh();""",
"""            refresh();""")
edit("""            double h = theme.scale(40);

            double x = this.x + pad;
            double y = this.y;""",
"""            double x = this.x + pad;
            double y = this.y;
            double rowHeight = 0;""")
# Measure before checking screen bounds to prevent clipping on small windows.
edit("""                if (x + cell.width > windowWidth) {
                    x = x + pad;
                    y += h;
                }""",
"""                cell.width = cell.widget().width;
                cell.height = cell.widget().height;
                if (x + cell.width > windowWidth && x > this.x + pad) {
                    x = this.x + pad;
                    y += rowHeight + pad;
                    rowHeight = 0;
                }""")
edit("""                x += cell.width + pad;
            }""",
"""                x += cell.width + pad;
                rowHeight = Math.max(rowHeight, cell.height);
            }""")
p.write_text(s)

theme=Path("upstream/src/main/java/meteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme.java")
s=theme.read_text()
s=s.replace(".defaultValue(AlignmentX.Center)",".defaultValue(AlignmentX.Left)",1)
s=s.replace('name("category-icons")\n        .description("Adds item icons to module categories.")\n        .defaultValue(false)',
            'name("category-icons")\n        .description("Adds item icons to module categories.")\n        .defaultValue(true)')
theme.write_text(s)
print("Updated real AnkHack module GUI with search-first layout, description search, category wrap and left-aligned modules.")
