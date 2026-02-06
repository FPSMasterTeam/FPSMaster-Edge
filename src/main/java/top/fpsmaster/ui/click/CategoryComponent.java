package top.fpsmaster.ui.click;

import top.fpsmaster.utils.render.draw.Images;

import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.utils.math.anim.ColorAnimator;
import top.fpsmaster.utils.math.anim.Easings;

import java.awt.*;
import java.util.Locale;

public class CategoryComponent {
    public Category category;
    private final ColorAnimator animationName = new ColorAnimator();
    public final ColorAnimator categorySelectionColor = new ColorAnimator();

    public CategoryComponent(Category category) {
        this.category = category;
        animationName.set(new Color(234, 234, 234));
    }

    public void render(float x, float y, float width, float height, float mouseX, float mouseY, boolean selected, double dt) {
        animationName.animateTo(
                selected ? new Color(0, 0, 0) : new Color(255, 255, 255),
                0.2f,
                Easings.QUAD_IN_OUT
        );
        animationName.update(dt);

        Images.draw(
            new ResourceLocation("client/gui/settings/icons/" + category.name().toLowerCase() + ".png"),
            x + 9,
            y,
            12f,
            12f,
            animationName.get()
        );

        FPSMaster.fontManager.s16.drawString(
            FPSMaster.i18n.get("category." + category.name().toLowerCase(Locale.getDefault())),
            x + 30,
            y,
            animationName.get().getRGB()
        );
    }
}




