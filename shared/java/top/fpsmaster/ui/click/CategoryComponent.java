package top.fpsmaster.ui.click;

import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.utils.math.animation.ColorAnimation;
import top.fpsmaster.utils.math.animation.Type;
import top.fpsmaster.utils.render.Render2DUtils;

import java.util.Locale;

public class CategoryComponent {
    public Category category;
    private ColorAnimation animationName = new ColorAnimation();
    public ColorAnimation categorySelectionColor = new ColorAnimation();

    public CategoryComponent(Category category) {
        this.category = category;
        animationName.setColor(FPSMaster.theme.getCategoryText());
    }

    public void render(float x, float y, float width, float height, float mouseX, float mouseY, boolean selected) {
        animationName.start(
            animationName.getColor(),
            selected ? FPSMaster.theme.getCategoryTextSelected() : FPSMaster.theme.getCategoryText(),
            0.2f,
            Type.EASE_IN_OUT_QUAD
        );
        animationName.update();

        Render2DUtils.drawImage(
            new ResourceLocation("client/gui/settings/icons/" + category.name().toLowerCase() + ".png"),
            x + 12,
            y - 2,
            12f,
            12f,
            animationName.getColor()
        );

        FPSMaster.fontManager.s16.drawString(
            FPSMaster.i18n.get("category." + category.name().toLowerCase(Locale.getDefault())),
            x + 30,
            y,
            animationName.getColor().getRGB()
        );
    }
}
