"""Give AnkHack's real GUI a distinctive dark/amber visual identity.

Applies to Meteor's actual widgets after all functional GUI patches.
Preserves accessible labels, controls, settings and click handling.
"""
from pathlib import Path

root=Path("upstream/src/main/java/meteordevelopment/meteorclient")
theme=root/"gui/themes/meteor/MeteorGuiTheme.java"
source=theme.read_text(encoding="utf-8")
colors={
    "new SettingColor(10, 14, 21, 245)":"new SettingColor(7, 8, 9, 251)",
    "new SettingColor(17, 24, 34, 245)":"new SettingColor(16, 15, 12, 251)",
    "new SettingColor(29, 40, 55, 245)":"new SettingColor(26, 23, 16, 251)",
    "new SettingColor(43, 55, 69)":"new SettingColor(59, 44, 15)",
    "new SettingColor(39, 52, 68)":"new SettingColor(105, 75, 19)",
    "new SettingColor(52, 68, 86)":"new SettingColor(162, 119, 28)",
    "new SettingColor(74, 89, 109)":"new SettingColor(226, 169, 39)",
    "new SettingColor(150, 150, 150)":"new SettingColor(184, 172, 151)",
}
for old,new in colors.items():
    count=source.count(old)
    # A default may also occur in other theme settings: transform all
    # matching defaults consistently, but fail if upstream completely changed.
    if not count:raise RuntimeError("Missing baseline theme color: "+old)
    source=source.replace(old,new)
source=source.replace(
 'new SettingColor(255, 255, 255));\n    public final Setting<SettingColor> separatorCenter',
 'new SettingColor(255, 218, 139));\n    public final Setting<SettingColor> separatorCenter'
) if False else source
theme.write_text(source,encoding="utf-8")

widgets=root/"gui/themes/meteor/widgets/WMeteorModule.java"
source=widgets.read_text(encoding="utf-8")
old='renderer.text(title, x, centerY, theme.textColor.get(), false);'
new='''// Native AnkHack features use amber titles. Upstream third-party
        // modules keep high-contrast white labels and remain fully usable.
        boolean nativeAnk = module.getClass().getSimpleName().startsWith("Ank");
        renderer.text(title, x, centerY,
            nativeAnk ? theme.accentColor.get() : theme.textColor.get(), false);'''
if source.count(old)!=1:raise RuntimeError("Unexpected module renderer")
widgets.write_text(source.replace(old,new),encoding="utf-8")

window=root/"gui/themes/meteor/widgets/WMeteorWindow.java"
source=window.read_text(encoding="utf-8")
old='renderer.quad(x, y + height - theme().scale(2), width,\n                theme().scale(2), theme().accentColor.get());'
new='''renderer.quad(x, y + height - theme().scale(3), width,
                theme().scale(3), theme().accentColor.get());'''
if source.count(old)!=1:raise RuntimeError("Unexpected window header")
window.write_text(source.replace(old,new),encoding="utf-8")

screen=root/"gui/screens/ModulesScreen.java"
source=screen.read_text(encoding="utf-8")
changes={
    'WWindow picker = theme.window("AnkHack");':'WWindow picker = theme.window("ANKHACK");',
    'theme.button(category.name)':'theme.button(category.name.toUpperCase(Locale.ROOT))',
}
for old,new in changes.items():
    if source.count(old)!=1:raise RuntimeError("Unexpected navigation widget: "+old)
    source=source.replace(old,new)
screen.write_text(source,encoding="utf-8")
print("AnkHack black/amber GUI and native module title distinction applied.")
