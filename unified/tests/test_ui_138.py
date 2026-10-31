"""Native GUI regression checks for AnkHack compact window rendering.

Static checks must pass *and* CI must actually launch Minecraft and capture
the GUI. A compile-only or decorative screenshot is not sufficient.
"""
import pathlib,unittest
SRC=pathlib.Path("upstream/src/main/java/meteordevelopment/meteorclient")

class AnkGui138(unittest.TestCase):
    def test_native_navigation_belongs_to_actual_gui(self):
        nav=(SRC/"gui/screens/AnkCategoryNavButton.java").read_text()
        screen=(SRC/"gui/screens/ModulesScreen.java").read_text()
        self.assertIn("extends WMeteorButton",nav)
        self.assertIn("BooleanSupplier selected",nav)
        self.assertIn("moduleCount",nav)
        self.assertIn("getAsBoolean()",nav)
        self.assertIn("theme.scale(27)",nav)
        self.assertIn("new AnkCategoryNavButton(",screen)
        self.assertIn("categoryModules.size(),() -> categoryWindow.visible",screen)
        self.assertIn("button.action = () ->",screen)
        self.assertIn("categoryWindow.visible = true",screen)
        self.assertIn("for (WWindow window : compactWindows) window.visible = false",screen)
        self.assertIn("invalidate();",screen)
    def test_visual_frame_is_real_draw_code(self):
        screen=(SRC/"gui/screens/ModulesScreen.java").read_text()
        for expected in ["protected void onRenderBefore(DrawContext context, float delta)",
                        "context.fill(0,0,width,height,0xC307090D)",
                        "context.drawText(client.textRenderer",
                        '"ANKHACK"',
                        '"MODULE CONTROL"',
                        "context.fill(8,height-27,width-8,height-8,0xFF171A20)"]:
            self.assertIn(expected,screen)
        self.assertNotIn("ImageIO",screen)
        self.assertNotIn("ProcessBuilder",screen)
    def test_controls_scroll_and_old_config_isolated(self):
        screen=(SRC/"gui/screens/ModulesScreen.java").read_text()
        for expected in ['"ank-category-picker-v2"',
                         '"ank-compact-search-v2"',
                         '"ank-compact-v2-" + category.name',
                         "categoryWindow.view.hasScrollBar = true;",
                         "getWindowHeight()-theme.scale(138)",
                         "w.view.hasScrollBar = true;",
                         'theme.textBox("", "Search modules")']:
            self.assertIn(expected,screen)
    def test_toggle_and_settings_still_function(self):
        row=(SRC/"gui/themes/meteor/widgets/WMeteorModule.java").read_text()
        self.assertIn("if (button == GLFW_MOUSE_BUTTON_LEFT) module.toggle();",row)
        self.assertIn("mc.setScreen(theme.moduleScreen(module))",row)
        self.assertIn("height = pad + theme.textHeight() + pad + theme.scale(3);",row)
        nav=(SRC/"gui/screens/AnkCategoryNavButton.java").read_text()
        self.assertNotIn("onPressed(",nav)
    def test_approved_palette_remains(self):
        theme=(SRC/"gui/themes/meteor/MeteorGuiTheme.java").read_text()
        self.assertIn("new SettingColor(255, 201, 55)",theme)
        self.assertIn("new SettingColor(7, 8, 9, 251)",theme)

if __name__=="__main__":unittest.main()
