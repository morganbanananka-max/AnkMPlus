"""Rebuild the *actual* AnkHack compact GUI, not a screenshot or skin.

Works after 1.0.37 patches. Preserves the approved black/amber palette,
but fixes the cluttered floating windows, invisible selected category,
clipped module list and opaque empty lower area.
"""
from pathlib import Path
from shutil import copy2

base=Path("upstream/src/main/java/meteordevelopment/meteorclient")
screen=base/"gui/screens/ModulesScreen.java"
source=screen.read_text(encoding="utf-8")
def change(old,new):
    global source
    if source.count(old)!=1:
        raise RuntimeError("Unexpected ModulesScreen anchor (%d): %s" % (source.count(old),old[:90]))
    source=source.replace(old,new,1)

change("import net.minecraft.item.Items;",
'''import net.minecraft.item.Items;
import net.minecraft.client.gui.DrawContext;''')

change('''    @Override
    protected void init() {
        super.init();
        controller.refresh();
    }''',
'''    // One native AnkHack viewport behind the functional Meteor-derived
    // controls. Uses the normal Minecraft GUI renderer and scales with window.
    @Override
    protected void onRenderBefore(DrawContext context, float delta) {
        if (client == null) return;
        int width=client.getWindow().getScaledWidth();
        int height=client.getWindow().getScaledHeight();
        if (width<320 || height<230) return;
        context.fill(0,0,width,height,0xC307090D);
        context.fill(8,31,width-8,height-8,0xEA0D1118);
        context.fill(8,31,width-8,33,0xFFFFC63C);
        context.fill(8,33,10,height-8,0xFF58431E);
        context.fill(8,height-27,width-8,height-8,0xFF171A20);
        context.fill(8,height-27,width-8,height-26,0xFF46391D);
        context.drawText(client.textRenderer,"ANKHACK",20,43,0xFFFFCE4C,false);
        context.drawText(client.textRenderer,"MODULE CONTROL",99,43,0xFFC1BBAE,false);
        if (width>610) {
            context.drawText(client.textRenderer,"FABRIC 1.21.11",width-114,43,0xFFB6A47B,false);
            context.drawText(client.textRenderer,
                "LEFT CLICK  Toggle     RIGHT CLICK  Settings     SCROLL  More modules",
                20,height-21,0xFFC5BFB3,false);
        }
        else {
            context.drawText(client.textRenderer,
                "LEFT CLICK Toggle    RIGHT CLICK Settings",
                20,height-21,0xFFC5BFB3,false);
        }
    }

    @Override
    protected void init() {
        super.init();
        controller.refresh();
    }''')

change('''        w.view.hasScrollBar = false;
        w.view.spacing = 0;
        // The old full-height categories made second-row windows disappear''',
'''        // Keep visible scroll handles in AnkHack's compact module view.
        // An invisible scrollbar gave the impression the list just ended.
        w.view.hasScrollBar = true;
        w.view.spacing = 0;
        // The old full-height categories made second-row windows disappear''')

change('''        w.view.maxHeight = Math.min(w.view.maxHeight,
            Math.max(theme.scale(120), (getWindowHeight() - theme.scale(130)) / 2));''',
'''        w.view.maxHeight = Math.min(w.view.maxHeight,
            Math.max(theme.scale(150), (getWindowHeight() - theme.scale(130)) / 2));''')
change('''        w.view.hasScrollBar = false;
        w.view.maxHeight -= 20;''',
'''        w.view.hasScrollBar = true;
        w.view.maxHeight = Math.min(w.view.maxHeight,
            Math.max(theme.scale(145),getWindowHeight()-theme.scale(145)));''')
change('''        WTextBox text = w.add(theme.textBox("")).minWidth(140).expandX().widget();''',
'''        WTextBox text = w.add(theme.textBox("", "Search modules")).minWidth(140).expandX().widget();''')
change('''                windows.get(0).id = "ank-compact-search";''',
'''                // New layout IDs ensure old dragged/minimized window positions
                // cannot spoil this layout. The old GUI remains recoverable.
                windows.get(0).id = "ank-compact-search-v2";''')
change('''                picker.id = "ank-category-picker";''',
'''                picker.id = "ank-category-picker-v2";''')
change('''                picker.view.maxHeight = Math.min(theme.scale(300), getWindowHeight() - theme.scale(120));
                picker.view.scrollOnlyWhenMouseOver = true;''',
'''                picker.view.maxHeight = Math.max(theme.scale(135),
                    Math.min(theme.scale(330),getWindowHeight()-theme.scale(142)));
                picker.view.scrollOnlyWhenMouseOver = true;
                picker.view.hasScrollBar = true;''')
change('''                    categoryWindow.id = "ank-compact-" + category.name;''',
'''                    categoryWindow.id = "ank-compact-v2-" + category.name;''')
change('''                    categoryWindow.view.maxHeight = Math.min(theme.scale(300), getWindowHeight() - theme.scale(120));''',
'''                    // Fill the useful viewport but leave the bottom help strip free.
                    categoryWindow.view.maxHeight = Math.max(theme.scale(160),
                        Math.min(theme.scale(440),getWindowHeight()-theme.scale(138)));
                    categoryWindow.view.hasScrollBar = true;''')
change('''                    WButton button = picker.add(theme.button(category.name.toUpperCase(Locale.ROOT))).expandX().widget();''',
'''                    // Real custom navigation with selected state and per-category count.
                    WButton button = picker.add(new AnkCategoryNavButton(
                        category.name.toUpperCase(Locale.ROOT),
                        categoryModules.size(),() -> categoryWindow.visible)).expandX().widget();''')

# Avoid an invisible window if all configured categories were hidden.
change('''                return;
            }
            for (Category category : Modules.loopCategories()) {''',
'''                if(compactWindows.isEmpty())
                    picker.add(theme.label("No visible modules"));
                return;
            }
            for (Category category : Modules.loopCategories()) {''')
change('''                        categoryWindow.visible = true;
                        invalidate();''',
'''                        categoryWindow.visible = true;
                        System.out.println("[AnkHack] Category selected: " + category.name);
                        invalidate();''')
screen.write_text(source,encoding="utf-8")
src=Path("unified/native/AnkCategoryNavButton.java")
dst=base/"gui/screens/AnkCategoryNavButton.java"
if not src.is_file() or "class AnkCategoryNavButton" not in src.read_text():
    raise RuntimeError("Native category control missing")
copy2(src,dst)

# Tighter module rows for more functional entries at 854x480; do NOT change
# settings screens or break left/right click handlers.
row=base/"gui/themes/meteor/widgets/WMeteorModule.java"
r=row.read_text(encoding="utf-8")
old='''        height = pad + theme.textHeight() + pad + theme.scale(8);'''
new='''        height = pad + theme.textHeight() + pad + theme.scale(3);'''
if r.count(old)!=1: raise RuntimeError("Module row geometry changed unexpectedly")
r=r.replace(old,new,1)
row.write_text(r,encoding="utf-8")
print("AnkHack compact UI: unified frame, active category, live counts, scroll bars, longer lists, stable window IDs.")
