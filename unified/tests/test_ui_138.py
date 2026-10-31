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
    def test_backdrop_renders_before_controls_in_same_layer(self):
        screen=(SRC/"gui/screens/ModulesScreen.java").read_text()
        backdrop=(SRC/"gui/screens/AnkMenuBackdrop.java").read_text()
        self.assertIn("add(new AnkMenuBackdrop()).widget();",screen)
        self.assertLess(screen.index("add(new AnkMenuBackdrop()).widget();"),
                        screen.index("controller = add(new WCategoryController())"))
        self.assertNotIn("onRenderBefore(",screen)
        self.assertIn("extends WWidget",backdrop)
        self.assertIn("protected void onRender(GuiRenderer renderer",backdrop)
        self.assertIn("renderer.quad(0,0,w,h,DIM)",backdrop)
        self.assertIn("renderer.text(\"ANKHACK\"",backdrop)
        self.assertIn('renderer.text("ANKHACK",w-theme.scale(116),theme.scale(9),GOLD,false)',backdrop)
        self.assertNotIn("MODULE CONTROL",backdrop)
        self.assertNotIn("DrawContext",screen)
        self.assertNotIn("ProcessBuilder",backdrop)

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
    def test_ank_ui_is_shared_across_fullscreen_and_windowed(self):
        screen=(SRC/"gui/screens/ModulesScreen.java").read_text()
        self.assertIn("private boolean useAnkLayout() { return true; }",screen)
        self.assertIn("if (useAnkLayout()) {",screen)
        self.assertIn("if (useAnkLayout()) return;",screen)
        self.assertIn("Math.min(760, Math.max(180",screen)
        self.assertIn("Math.min(theme.scale(720),getWindowHeight()-theme.scale(138))",screen)
        self.assertIn("double combined=pad;",screen)
        self.assertIn("x = Math.max(pad,(getWindowWidth()-combined)/2.0);",screen)

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
