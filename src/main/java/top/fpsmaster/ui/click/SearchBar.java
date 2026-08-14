package top.fpsmaster.ui.click;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.*;

// author:Serendisand
// reason:全局搜索
public class SearchBar {
    private static final int TRANSPARENT = new Color(0, 0, 0, 0).getRGB();
    private static final float ICON_SIZE = 6.5f;
    private static final float CLEAR_SIZE = 6f;

    private TextField field;

    private void ensureField() {
        if (field == null) {
            field = new TextField(
                    FPSMaster.fontManager.getFont(12),
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

        UiChrome.searchBox(x, y, width, height, isFocused());

        Icons.draw("search", x + 5, y + (height - ICON_SIZE) / 2f, ICON_SIZE, ClickGuiTheme.textDisabled().getRGB());

        boolean hasText = !field.getText().isEmpty();
        float clearZone = hasText ? 13f : 4f;
        field.drawTextBox(x + 14, y + 0.5f, width - 14 - clearZone, height - 1);

        if (hasText) {
            float clearX = x + width - 11;
            float clearY = y + (height - CLEAR_SIZE) / 2f;
            boolean clearHover = Hover.is(clearX - 2, clearY - 2, 10, 10, mouseX, mouseY);
            Icons.draw("close", clearX, clearY, CLEAR_SIZE,
                    clearHover ? ClickGuiTheme.textPrimary().getRGB() : ClickGuiTheme.textSecondary().getRGB());
            if (screen.consumePressInBounds(clearX - 2, clearY - 2, 10, 10) != null) {
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

    public void setQuery(String query) {
        ensureField();
        field.setText(query);
    }

    public void clear() {
        ensureField();
        field.setText("");
    }
}
