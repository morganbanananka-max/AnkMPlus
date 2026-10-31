/*
 * AnkHack compact navigation: original widget, not just recolored Meteor buttons.
 * Maintains normal WPressable click, focus and keyboard behavior through the base class.
 */
package meteordevelopment.meteorclient.gui.screens;

import java.util.function.BooleanSupplier;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorButton;

public final class AnkCategoryNavButton extends WMeteorButton {
    private final BooleanSupplier selected;
    private final int moduleCount;

    public AnkCategoryNavButton(String label, int moduleCount, BooleanSupplier selected) {
        super(label, null);
        this.selected=selected;
        this.moduleCount=moduleCount;
    }

    @Override
    protected void onCalculateSize() {
        super.onCalculateSize();
        width = Math.max(width, theme.scale(150));
        height = Math.max(height, theme.scale(27));
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        MeteorGuiTheme t=theme();
        boolean active=selected.getAsBoolean();
        renderer.quad(x,y,width,height,
            active ? t.moduleBackground.get() : t.backgroundColor.get());
        if (active) {
            renderer.quad(x,y,t.scale(4),height,t.accentColor.get());
            renderer.quad(x,y+height-t.scale(1),width,t.scale(1),t.accentColor.get());
        } else if (mouseOver) {
            renderer.quad(x,y,t.scale(2),height,t.outlineColor.get(true,false));
        }

        double middle = y+(height-t.textHeight())/2.0;
        String label=getText();
        renderer.text(label,x+t.scale(13),middle,
            active ? t.accentColor.get() : t.textColor.get(),false);
        String count=Integer.toString(moduleCount);
        renderer.text(count,x+width-t.scale(11)-t.textWidth(count),middle,
            t.textSecondaryColor.get(),false);
    }
}
