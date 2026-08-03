package top.fpsmaster.ui.click;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.utils.math.anim.ColorAnimator;
import top.fpsmaster.utils.math.anim.Easings;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.*;

// author:Serendisand
// reason:全局搜索
public class SearchBar {
    private static final int TRANSPARENT = new Color(0, 0, 0, 0).getRGB();
    private static final float ICON_SIZE = 10f;
    private static final float CLEAR_SIZE = 10f;

    private TextField field;
    private final ColorAnimator bgAnim = new ColorAnimator(ClickGuiTheme.inputBg());

    private void ensureField() {
        if (field == null) {
            field = new TextField(
                    FPSMaster.fontManager.s16,
                    false,
                    FPSMaster.i18n.get("clickgui.search.placeholder"),
                    TRANSPARENT,
                    ClickGuiTheme.textFieldText().getRGB(),
                    64
            );
            field.setCanLoseFocus(false);
        }
    }

    public void draw(ScaledGuiScreen screen, float x, float y, float width, float height, int mouseX, int mouseY) {
        ensureField();
        field.backGroundColor = TRANSPARENT;
        field.fontColor = ClickGuiTheme.textFieldText().getRGB();
        field.placeHolder = FPSMaster.i18n.get("clickgui.search.placeholder");

        boolean hovered = Hover.is(x, y, width, height, mouseX, mouseY);
        bgAnim.animateTo(hovered ? ClickGuiTheme.sideBtnHoverBg() : ClickGuiTheme.inputBg(), 0.15, Easings.QUAD_OUT);
        bgAnim.update();
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 4, bgAnim.get().getRGB());

        Icons.draw("search", x + 6, y + (height - ICON_SIZE) / 2f, ICON_SIZE, ClickGuiTheme.textSecondary().getRGB());

        boolean hasText = !field.getText().isEmpty();
        float clearZone = hasText ? 22f : 6f;
        field.drawTextBox(x + 16, y + 1, width - 16 - clearZone, height - 2);

        if (hasText) {
            float clearX = x + width - 18;
            float clearY = y + (height - CLEAR_SIZE) / 2f;
            boolean clearHover = Hover.is(clearX, clearY, 12, 12, mouseX, mouseY);
            Icons.draw("close", clearX, clearY, CLEAR_SIZE,
                    clearHover ? ClickGuiTheme.textPrimary().getRGB() : ClickGuiTheme.textSecondary().getRGB());
            if (screen.consumePressInBounds(clearX, clearY, 12, 12) != null) {
                field.setText("");
                setFocused(true);
            }
        }

        ScaledGuiScreen.PointerEvent press = screen.consumePressInBounds(x, y, width, height);
        if (press != null) {
            setFocused(true);
            field.mouseClicked(press.x, press.y, 0);
        }
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (!isFocused()) {
            return false;
        }
        if (keyCode == 1) {
            setFocused(false);
            return true;
        }
        field.textboxKeyTyped(typedChar, keyCode);
        return true;
    }

    public String getQuery() {
        ensureField();
        return field.getText();
    }

    public boolean isFocused() {
        ensureField();
        return field.isFocused();
    }

    public void setFocused(boolean focused) {
        ensureField();
        field.setFocused(focused);
    }

    public void clear() {
        ensureField();
        field.setText("");
    }
}
