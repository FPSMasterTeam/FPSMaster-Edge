package top.fpsmaster.ui.screens.mainmenu;

import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Colors;
import top.fpsmaster.utils.render.draw.Rects;

import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.utils.math.animation.AnimationUtils;

import java.awt.*;

public class MenuButton {
    private final String text;
    private final Runnable runnable;
    private float x;
    private float y;
    private float width;
    private float height;
    private double alpha;

    public MenuButton(String text, Runnable runnable) {
        this.text = text;
        this.runnable = runnable;
        this.x = 0f;
        this.y = 0f;
        this.width = 0f;
        this.height = 0f;
        this.alpha = 100.0;
    }

    public void render(float x, float y, float width, float height, float mouseX, float mouseY) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        // Handle alpha change based on hover
        if (Hover.is(x, y, width, height, (int) mouseX, (int) mouseY)) {
            alpha = AnimationUtils.base(alpha, 200.0, 0.1);
        } else {
            alpha = AnimationUtils.base(alpha, 100.0, 0.1);
        }

        // Draw the button rectangle
        Rects.rounded(x, y, width, height, new Color(0, 0, 0, Colors.clamp(alpha)).getRGB());

        // Draw text or icon
        if (!text.equals("settings")) {
            FPSMaster.fontManager.s18.drawCenteredString(
                    FPSMaster.i18n.get(text),
                    x + width / 2,
                    y + height / 2 - 6,
                    new Color(255, 255, 255).getRGB()
            );
        } else {
            Images.draw(
                    new ResourceLocation("client/gui/screen/settings.png"),
                    x + width / 2 - 6,
                    y + height / 2 - 6,
                    12f,
                    12f,
                    new Color(255, 255, 255).getRGB()
            );
        }
    }

    public void mouseClick(float mouseX, float mouseY, int btn) {
        if (Hover.is(x, y, width, height, (int) mouseX, (int) mouseY) && btn == 0) {
            runnable.run();
        }
    }
}




