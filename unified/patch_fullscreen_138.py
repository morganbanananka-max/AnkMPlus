"""Use AnkHack's own unified navigation at every Minecraft window size.

Old full-size flow used the unrelated floating Meteor window grid.
Keep three functional panels (search, category list, module list) centered,
without letting the module view grow to a ridiculous 1600px on 1920px screens.
"""
from pathlib import Path
p=Path("upstream/src/main/java/meteordevelopment/meteorclient/gui/screens/ModulesScreen.java")
s=p.read_text(encoding="utf-8")
def change(old,new):
    global s
    if s.count(old)!=1:
        raise RuntimeError(f"AnkHack fullscreen GUI mismatch ({s.count(old)}): {old[:90]}")
    s=s.replace(old,new,1)

change('''    protected class WCategoryController extends WContainer {
        public final List<WWindow> windows''',
'''    protected class WCategoryController extends WContainer {
        // AnkHack has one unified layout regardless of display resolution.
        // This is a method, not a compile-time constant, to retain legacy code
        // as a safe fallback for later migration.
        private boolean useAnkLayout() { return true; }

        public final List<WWindow> windows''')
change('''            if (getWindowHeight() <= theme.scale(560)) {''',
'''            if (useAnkLayout()) {''')
change('''            if (getWindowHeight() <= theme.scale(560)) return;''',
'''            if (useAnkLayout()) return;''')
change('''                    categoryWindow.minWidth = Math.max(180,
                        (getWindowWidth() - theme.scale(355)) / theme.scale(1));''',
'''                    categoryWindow.minWidth = Math.min(760, Math.max(180,
                        (getWindowWidth() - theme.scale(355)) / theme.scale(1)));''')
change('''                        Math.min(theme.scale(440),getWindowHeight()-theme.scale(138)));''',
'''                        Math.min(theme.scale(720),getWindowHeight()-theme.scale(138)));''')
change('''            double x = this.x + pad;
            // Reserve space for AnkHack's global top navigation.''',
'''            double x = this.x + pad;
            if (useAnkLayout()) {
                // Center the search/sidebar/modules trio on widescreen instead
                // of pinning the entire app to the top-left corner.
                double combined=pad;
                for (Cell<?> cell : cells) {
                    if (cell.widget().visible) combined+=cell.widget().width+pad;
                }
                if (combined < getWindowWidth() - 2*pad)
                    x = Math.max(pad,(getWindowWidth()-combined)/2.0);
            }
            // Reserve space for AnkHack's global top navigation.''')
p.write_text(s,encoding="utf-8")
print("AnkHack unified compact layout now spans all screen sizes, centered and capped at 760 module width.")
