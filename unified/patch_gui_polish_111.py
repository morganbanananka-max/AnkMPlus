#!/usr/bin/env python3
"""Substantial in-game GUI skin improvements on real Meteor widgets.
The original GUI keeps full functional module settings, right-click and scaling.
"""
from pathlib import Path
root=Path("upstream/src/main/java/meteordevelopment/meteorclient/gui/themes/meteor/widgets")
m=root/"WMeteorModule.java"
s=m.read_text()
def edit(old,new,expected=1):
 global s
 if s.count(old)!=expected:raise RuntimeError("Module widget anchor: "+old[:75])
 s=s.replace(old,new,expected)
edit("""        width = pad + titleWidth + pad;
        height = pad + theme.textHeight() + pad;""",
"""        // Reserve a consistent column width for status pills. Text does not jump
        // around when modules are toggled or names vary in length.
        width = Math.max(theme.scale(152), pad + titleWidth + theme.scale(60));
        height = pad + theme.textHeight() + pad + theme.scale(8);""")
edit("""        if (animationProgress1 > 0) {
            renderer.quad(x, y, width * animationProgress1, height, theme.moduleBackground.get());
        }
        if (animationProgress2 > 0) {
            renderer.quad(x, y + height * (1 - animationProgress2), theme.scale(2), height * animationProgress2, theme.accentColor.get());
        }""",
"""        // Quiet dark tiles with gold status, not a wall of bright yellow panels.
        renderer.quad(x, y, width, height, theme.backgroundColor.get());
        if (animationProgress1 > 0)
            renderer.quad(x, y, width * animationProgress1, height, theme.moduleBackground.get());
        if (animationProgress2 > 0)
            renderer.quad(x, y, theme.scale(3), height * animationProgress2, theme.accentColor.get());""")
edit("""        renderer.text(title, x, y + pad, theme.textColor.get(), false);""",
"""        double centerY = y + (height - theme.textHeight()) / 2;
        renderer.text(title, x, centerY, theme.textColor.get(), false);
        // Status follows the module state, and is not a decorative dead button:
        // clicking anywhere still invokes the real onPressed() module toggle.
        double pillWidth = theme.scale(33), pillHeight = theme.textHeight() + theme.scale(6);
        double pillX = this.x + width - pillWidth - theme.scale(5);
        double pillY = y + (height - pillHeight) / 2;
        renderer.quad(pillX, pillY, pillWidth, pillHeight,
            module.isActive() ? theme.accentColor.get() : theme.outlineColor.get());
        String status = module.isActive() ? "ON" : "OFF";
        renderer.text(status,
            pillX + (pillWidth-theme.textWidth(status))/2,
            pillY + (pillHeight-theme.textHeight())/2,
            module.isActive() ? theme.backgroundColor.get() : theme.textSecondaryColor.get(),false);""")
# Verify target theme has an outlineColor, otherwise fall back to an already-known color.
theme=Path("upstream/src/main/java/meteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme.java").read_text()
if "public final Setting<SettingColor> outlineColor" not in theme:
    s=s.replace("theme.outlineColor.get()","theme.moduleBackground.get()")
m.write_text(s)
w=root/"WMeteorWindow.java"
s=w.read_text()
old="""            renderer.quad(this, theme().accentColor.get());"""
new="""            // Dark title bar, gold underline and existing functional title/icons.
            renderer.quad(this, theme().backgroundColor.get());
            renderer.quad(x, y + height - theme().scale(2), width,
                theme().scale(2), theme().accentColor.get());"""
if s.count(old)!=1:raise RuntimeError("Window widget anchor missing")
w.write_text(s.replace(old,new))
print("AnkHack GUI now has uniform larger module rows, explicit ON/OFF pills and charcoal headers with fine gold accents.")
