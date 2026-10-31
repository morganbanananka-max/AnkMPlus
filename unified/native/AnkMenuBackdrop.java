/*
 * AnkHack viewport background, drawn as the FIRST normal GUI widget.
 * Do not use DrawContext.onRenderBefore: Fabric's GuiRenderer opens a
 * separate render layer and can place DrawContext fill above GUI controls.
 */
package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.render.color.Color;

import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public final class AnkMenuBackdrop extends WWidget {
    private static final Color DIM=new Color(5,7,11,166);
    private static final Color PANEL=new Color(12,16,21,246);
    private static final Color GOLD=new Color(255,198,60);
    private static final Color BORDER=new Color(83,64,30);
    private static final Color FOOTER=new Color(22,25,31);
    private static final Color TEXT=new Color(204,198,186);

    @Override
    protected void onCalculateSize() {
        width=getWindowWidth();
        height=getWindowHeight();
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double w=getWindowWidth(),h=getWindowHeight();
        if(w<320 || h<230)return;
        double left=theme.scale(8), top=theme.scale(31), bottom=theme.scale(8);
        renderer.quad(0,0,w,h,DIM);
        renderer.quad(left,top,w-left*2,h-top-bottom,PANEL);
        renderer.quad(left,top,w-left*2,theme.scale(2),GOLD);
        renderer.quad(left,top+theme.scale(2),theme.scale(2),h-top-bottom,BORDER);
        renderer.quad(left,h-theme.scale(27),w-left*2,theme.scale(19),FOOTER);
        renderer.quad(left,h-theme.scale(27),w-left*2,theme.scale(1),BORDER);
        renderer.text("ANKHACK",theme.scale(19),theme.scale(41),GOLD,false);
        renderer.text("MODULE CONTROL",theme.scale(105),theme.scale(41),TEXT,false);
        if(w>theme.scale(610)) {
            renderer.text("FABRIC 1.21.11",w-theme.scale(121),theme.scale(41),TEXT,false);
            renderer.text("LEFT CLICK  Toggle     RIGHT CLICK  Settings     SCROLL  More modules",
                theme.scale(19),h-theme.scale(23),TEXT,false);
        } else {
            renderer.text("LEFT CLICK  Toggle     RIGHT CLICK  Settings",
                theme.scale(19),h-theme.scale(23),TEXT,false);
        }
    }
}
