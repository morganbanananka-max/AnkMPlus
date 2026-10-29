from pathlib import Path
root = Path("ankhack")
source = root / "src/main/java/se/ankclient/AnkHack.java"
s=source.read_text()
assert "private int ticks;" in s
s=s[:s.index("    private int ticks;")]+"""    private int ticks;
    private int joinGraceTicks = 100;
    private boolean disableOverlay;
    private boolean disableWorldTick;
    private net.minecraft.client.world.ClientWorld lastWorld;

    @Override
    public void onInitializeClient() {
        Features.registerAll(MODULES);
        ExtraFeatures.registerAll(MODULES);
        Settings.load();
        AnkLog.event("startup","ankhack","version_0.3.1");
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("ankhack", "general"));
        KeyBinding openGui = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.ankclient.menu", GLFW.GLFW_KEY_RIGHT_SHIFT, category)
        );
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(client.world != lastWorld) {
                lastWorld=client.world;
                joinGraceTicks=100;
                disableOverlay=false;
                disableWorldTick=false;
                ticks=0;
            }
            while (openGui.wasPressed()) {
                if (client.currentScreen instanceof ClickGuiScreen) client.setScreen(null);
                else if(client.currentScreen == null) client.setScreen(new ClickGuiScreen("ALL", "", 0, null, 0));
            }
            if(client.world == null || client.player == null) return;
            if(joinGraceTicks > 0) { joinGraceTicks--; return; }
            if(disableWorldTick) return;
            try {
                if(++ticks % 20 == 0) {
                    ServerProfiles.detect(client);
                    PerformanceMonitor.sample(client);
                }
                // Never turn third-party features on/off automatically while joining an Essential world.
                MODULES.tick(client);
            } catch (RuntimeException | LinkageError error) {
                disableWorldTick = true;
                AnkLog.event("world_tick_error","ankhack",error.getClass().getSimpleName());
                System.err.println("[AnkHack] Disabled world updates: " + error);
            }
        });
        HudElementRegistry.addLast(Identifier.of("ankhack", "overlay"), (draw,tickCounter) -> {
            if(disableOverlay || joinGraceTicks > 0) return;
            try { Overlay.render(draw, MODULES); }
            catch(RuntimeException | LinkageError error) {
                disableOverlay = true;
                AnkLog.event("hud_error","ankhack",error.getClass().getSimpleName());
                System.err.println("[AnkHack] Disabled HUD for this world: " + error);
            }
        });
    }
}
"""
source.write_text(s)
for f in [root/"gradle.properties",root/"src/main/resources/fabric.mod.json",
          root/"src/main/java/se/ankclient/Overlay.java",root/"src/main/java/se/ankclient/ClickGuiScreen.java"]:
    t=f.read_text()
    if f.name=="gradle.properties":
        assert "mod_version=0.3.0" in t
        t=t.replace("mod_version=0.3.0","mod_version=0.3.1")
    elif f.name=="fabric.mod.json":
        assert '"version": "0.3.0"' in t
        t=t.replace('"version": "0.3.0"','"version": "0.3.1"')
    else:t=t.replace("v0.3","v0.3.1")
    f.write_text(t)
print("AnkHack 0.3.1 Essential world-load guard applied")
