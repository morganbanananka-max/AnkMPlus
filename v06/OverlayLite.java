package se.ankclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.util.*;

/** Minimal, opt-in HUD; never confuses overlay widgets with gameplay hacks. */
public final class OverlayLite {
    private OverlayLite(){}
    public static void render(DrawContext d,ModuleManager manager){
        MinecraftClient mc=MinecraftClient.getInstance();
        if(mc.player==null||mc.world==null||mc.options.hudHidden)return;
        int top=6,fg=0xFFFFCD42,muted=0xFFCAD4E0;
        Module array=manager.byId("arraylist"),fps=manager.byId("fps"),xyz=manager.byId("coords");
        if(array!=null&&array.enabled()){
            List<Module> active=new ArrayList<>();
            for(Module m:manager.all())if(m.enabled()&&m.category!=Module.Category.HUD
                &&!m.id.endsWith("_hud")&&!Set.of("mace_ready","shield_watch","player_radar","target_info").contains(m.id))active.add(m);
            int rx=mc.getWindow().getScaledWidth()-12;
            for(int i=0;i<Math.min(16,active.size());i++){
                String s=active.get(i).name;
                int ww=mc.textRenderer.getWidth(s);
                d.fill(rx-ww-9,top-2+i*13,rx+3,top+11+i*13,0xAB0D1219);
                d.drawTextWithShadow(mc.textRenderer,s,rx-ww,top+i*13,fg);
            }
        }
        int line=6;
        if(fps!=null&&fps.enabled()){
            String s="FPS "+mc.getCurrentFps();
            d.drawTextWithShadow(mc.textRenderer,s,8,line,muted);line+=13;
        }
        if(xyz!=null&&xyz.enabled()){
            var p=mc.player.getBlockPos();
            String s="XYZ "+p.getX()+" / "+p.getY()+" / "+p.getZ();
            d.drawTextWithShadow(mc.textRenderer,s,8,line,muted);
        }
    }
}
