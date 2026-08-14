package top.fpsmaster.ui.click;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.utils.math.anim.ColorAnimator;
import top.fpsmaster.utils.math.anim.Easings;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.util.Locale;

public class CategoryComponent {
    public Category category;
    private final ColorAnimator animationName = new ColorAnimator();
    public final ColorAnimator categorySelectionColor = new ColorAnimator();

    public CategoryComponent(Category category) {
        this.category = category;
        animationName.set(ClickGuiTheme.categoryTextUnselected());
    }

    public void render(
            ScaledGuiScreen screen,
            float x,
            float y,
            float width,
            float height,
            float mouseX,
            float mouseY,
            boolean selected,
            int count,
            double dt
    ) {
        boolean hover = Hover.is(x, y, width, height, (int) mouseX, (int) mouseY);
        UiChrome.navItem(x, y, width, height, selected, hover);
        animationName.animateTo(
                selected ? ClickGuiTheme.categoryTextSelected() : ClickGuiTheme.categoryTextUnselected(),
                0.15f,
                Easings.QUAD_OUT
        );
        animationName.update(dt);

        int color = animationName.get().getRGB();
        Icons.draw(iconName(), x + 6, y + (height - 7) / 2f, 7f, color);
        FPSMaster.fontManager.getFont(13).drawString(
                FPSMaster.i18n.get("category." + category.name().toLowerCase(Locale.getDefault())),
                x + 17.5f,
                y + height / 2f - 3f,
                color
        );
        if (count >= 0) {
            String n = String.valueOf(count);
            float nw = FPSMaster.fontManager.getFont(11).getStringWidth(n);
            FPSMaster.fontManager.getFont(11).drawString(
                    n,
                    x + width - 6 - nw,
                    y + height / 2f - 2.5f,
                    selected ? 0xB3FFFFFF : ClickGuiTheme.textDisabled().getRGB()
            );
        }
    }

    /** Prototype sidebar icons: 优化=zap, 渲染=sparkles, 实用工具=wrench, 界面=grid. */
    private String iconName() {
        switch (category) {
            case OPTIMIZE:
                return "zap";
            case RENDER:
                return "sparkles";
            case Utility:
                return "wrench";
            case Interface:
                return "grid";
            default:
                return category.name().toLowerCase(Locale.getDefault());
        }
    }
}
