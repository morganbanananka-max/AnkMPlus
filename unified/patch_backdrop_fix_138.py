"""Fix GUI z-order: draw AnkHack backdrop inside GUI batch, BEFORE controls.

The old DrawContext background was visually on top of interactive widgets
because GuiRenderer.begin() creates its own root layer after onRenderBefore().
This patch intentionally removes that faulty background. Fail closed if
the previous 1.0.38 UI patch did not apply.
"""
from pathlib import Path
from shutil import copy2

root=Path("upstream/src/main/java/meteordevelopment/meteorclient/gui/screens")
p=root/"ModulesScreen.java"
s=p.read_text(encoding="utf-8")
start='''    // One native AnkHack viewport behind the functional Meteor-derived'''
end='''    @Override
    protected void init() {
        super.init();
        controller.refresh();
    }'''
if s.count(start)!=1 or s.count(end)!=1:
    raise RuntimeError("Cannot safely locate old DrawContext overlay")
a=s.index(start)
b=s.index(end,a)
s=s[:a]+s[b:]
old='''        controller = add(new WCategoryController()).top().widget();'''
new='''        // Draw first, in the SAME native GuiRenderer layer as the widgets.
        // This guarantees all category controls remain visible above the frame.
        add(new AnkMenuBackdrop()).widget();
        controller = add(new WCategoryController()).top().widget();'''
if s.count(old)!=1:
    raise RuntimeError("Cannot safely locate control layout root")
s=s.replace(old,new,1)
s=s.replace('import net.minecraft.client.gui.DrawContext;\n','')
p.write_text(s,encoding="utf-8")
native=Path("unified/native/AnkMenuBackdrop.java")
if "class AnkMenuBackdrop" not in native.read_text(encoding="utf-8"):
    raise RuntimeError("Missing new GUI backdrop source")
copy2(native,root/"AnkMenuBackdrop.java")
print("Real GUI now draws black/amber frame before module controls in the same renderer batch.")
