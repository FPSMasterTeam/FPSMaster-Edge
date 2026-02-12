package top.fpsmaster.ui.screens.mainmenu;

import net.minecraft.util.ResourceLocation;
import top.fpsmaster.ui.common.GuiButton;

import java.awt.Color;

public class MenuButton extends GuiButton {
    public MenuButton(String text, Runnable runnable) {
        super(text, runnable, new Color(0, 0, 0, 100), new Color(0, 0, 0, 180));
        setBackgroundColors(new Color(0, 0, 0, 100), new Color(0, 0, 0, 180), new Color(0, 0, 0, 220));
        setRoundRadius(4);
    }

    public MenuButton(String text, Runnable runnable, ResourceLocation icon, float iconWidth, float iconHeight) {
        this(text, runnable);
        setIcon(icon, iconWidth, iconHeight);
    }
}
