"""Source-level checks for AnkHack's actual, functional GUI identity."""
from pathlib import Path
import unittest

ROOT=Path("upstream/src/main/java/meteordevelopment/meteorclient")
class Ui135Tests(unittest.TestCase):
    def test_amber_theme(self):
        s=(ROOT/"gui/themes/meteor/MeteorGuiTheme.java").read_text()
        for color in ["new SettingColor(7, 8, 9, 251)",
                      "new SettingColor(16, 15, 12, 251)",
                      "new SettingColor(105, 75, 19)",
                      "new SettingColor(226, 169, 39)"]:
            self.assertIn(color,s)
        self.assertNotIn("new SettingColor(39, 52, 68)",s)
    def test_native_modules_distinguished(self):
        s=(ROOT/"gui/themes/meteor/widgets/WMeteorModule.java").read_text()
        self.assertIn('getSimpleName().startsWith("Ank")',s)
        self.assertIn('nativeAnk ? theme.accentColor.get() : theme.textColor.get()',s)
        self.assertIn("onPressed(int button)",s)
        self.assertIn("module.toggle()",s)
        self.assertIn("theme.moduleScreen(module)",s)
    def test_compact_menu_still_works(self):
        s=(ROOT/"gui/screens/ModulesScreen.java").read_text()
        self.assertIn('theme.window("ANKHACK")',s)
        self.assertIn("category.name.toUpperCase(Locale.ROOT)",s)
        self.assertIn('picker.id = "ank-category-picker-v2"',s)
        self.assertIn("button.action = () ->",s)
        self.assertIn("categoryWindow.visible = true",s)
    def test_actual_gold_header_uses_existing_renderer(self):
        s=(ROOT/"gui/themes/meteor/widgets/WMeteorWindow.java").read_text()
        self.assertIn("theme().scale(3), theme().accentColor.get()",s)
        self.assertIn("protected void onRender",s)

if __name__ == "__main__":unittest.main()
