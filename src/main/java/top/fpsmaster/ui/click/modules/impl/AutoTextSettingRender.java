package top.fpsmaster.ui.click.modules.impl;

import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.AutoTextEntry;
import top.fpsmaster.features.settings.impl.AutoTextSetting;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.MainPanel;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * ClickGUI editor for {@link AutoTextSetting}.
 *
 * <p>Each row: a bind button, a message text field, and a delete (×) button.
 * A centered + button below the last row adds a new entry (up to 20).
 * Duplicate non-zero bindings are rejected with an inline warning.
 */
public class AutoTextSettingRender extends SettingRender<AutoTextSetting> {
    private static final int ROW_H = 13;
    private static final int CORNER = 3;

    private int capturingRow = -1;
    private String duplicateWarning = "";
    private final ArrayList<TextField> textFields = new ArrayList<>();

    public AutoTextSettingRender(Module module, AutoTextSetting setting) {
        super(setting);
        this.mod = module;
        rebuildTextFields();
    }

    @Override
    public boolean isWide() {
        return true;
    }

    private void rebuildTextFields() {
        textFields.clear();
        for (AutoTextEntry entry : setting.getValue()) {
            TextField tf = new TextField(FPSMaster.fontManager.getFont(12), false, "",
                    ClickGuiTheme.textFieldBg().getRGB(), ClickGuiTheme.textFieldText().getRGB(), 256);
            tf.setText(entry.message);
            textFields.add(tf);
        }
    }

    @Override
    public void render(ScaledGuiScreen screen, float x, float y, float width, float height, float mouseX, float mouseY, boolean custom) {
        ArrayList<AutoTextEntry> entries = setting.getValue();

        // Rebuild text fields if entry count changed externally
        while (textFields.size() < entries.size()) {
            TextField tf = new TextField(FPSMaster.fontManager.getFont(12), false, "",
                    ClickGuiTheme.textFieldBg().getRGB(), ClickGuiTheme.textFieldText().getRGB(), 256);
            tf.setText(entries.get(textFields.size()).message);
            textFields.add(tf);
        }
        while (textFields.size() > entries.size()) {
            textFields.remove(textFields.size() - 1);
        }

        // Sync text field contents back to entries only when the field is focused
        for (int i = 0; i < entries.size() && i < textFields.size(); i++) {
            TextField tf = textFields.get(i);
            if (tf.isFocused()) {
                String currentText = tf.getText();
                if (!currentText.equals(entries.get(i).message)) {
                    setting.editEntry(i, new AutoTextEntry(entries.get(i).keyCode, currentText));
                }
            }
        }

        duplicateWarning = "";

        // Detect duplicate keys
        Set<Integer> usedKeys = new HashSet<>();
        for (int i = 0; i < entries.size(); i++) {
            int k = entries.get(i).keyCode;
            if (k != 0) {
                if (!usedKeys.add(k)) {
                    duplicateWarning = "Duplicate key: " + Keyboard.getKeyName(k);
                }
            }
        }

        float rowX = x + 10;
        float rowY = y + 2;

        FPSMaster.fontManager.getFont(12).drawString(
                FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(java.util.Locale.getDefault())),
                rowX, rowY, ClickGuiTheme.textSecondary().getRGB()
        );
        rowY += 11;

        if (entries.isEmpty()) {
            FPSMaster.fontManager.getFont(12).drawString(
                    FPSMaster.i18n.get("autotext.empty"),
                    rowX + (width - 20 - FPSMaster.fontManager.getFont(12).getStringWidth(FPSMaster.i18n.get("autotext.empty"))) / 2,
                    rowY + 4, ClickGuiTheme.textSecondary().getRGB()
            );
            this.height = 38;
        } else {
            for (int i = 0; i < entries.size(); i++) {
                AutoTextEntry entry = entries.get(i);
                float rX = rowX + 8;
                float rY = rowY + i * (ROW_H + 3);

                // Bind button
                String keyName = entry.keyCode != 0 ? Keyboard.getKeyName(entry.keyCode) : "None";
                float bindW = FPSMaster.fontManager.getFont(12).getStringWidth(keyName) + 6;
                boolean isCapturing = capturingRow == i;
                Color bindBg = isCapturing
                        ? ClickGuiTheme.bindBgActive()
                        : (Hover.is(rX, rY, bindW, ROW_H, (int) mouseX, (int) mouseY)
                        ? ClickGuiTheme.bindBgInactive() : ClickGuiTheme.textFieldBg());
                Rects.rounded(Math.round(rX), Math.round(rY), Math.round(bindW), ROW_H, CORNER, bindBg);
                FPSMaster.fontManager.getFont(12).drawString(keyName, rX + 3, rY + 3, ClickGuiTheme.textPrimary().getRGB());

                ScaledGuiScreen.PointerEvent bindClick = screen.consumePressInBounds(rX, rY, bindW, ROW_H, 0);
                if (bindClick != null) {
                    capturingRow = (capturingRow == i) ? -1 : i;
                    MainPanel.bindLock = capturingRow >= 0 ? (setting.name + i) : "";
                }

                // Text field
                float tfX = rX + bindW + 4;
                float tfW = width - 20 - bindW - 4 - 14;
                TextField tf = textFields.get(i);
                tf.drawTextBox(tfX, rY, tfW, ROW_H);

                ScaledGuiScreen.PointerEvent tfClick = screen.consumePressInBounds(tfX, rY, tfW, ROW_H, 0);
                if (tfClick != null) {
                    tf.mouseClicked((int) tfClick.x, (int) tfClick.y, 0);
                    capturingRow = -1;
                }

                // Delete button
                float delX = tfX + tfW + 2;
                boolean delHover = Hover.is(delX, rY, 12, ROW_H, (int) mouseX, (int) mouseY);
                Rects.rounded(Math.round(delX), Math.round(rY), 12, ROW_H, CORNER,
                        delHover ? ClickGuiTheme.buttonHoverBg() : ClickGuiTheme.buttonBg());
                FPSMaster.fontManager.getFont(12).drawString("x", delX + 3, rY + 2, ClickGuiTheme.textPrimary().getRGB());

                ScaledGuiScreen.PointerEvent delClick = screen.consumePressInBounds(delX, rY, 12, ROW_H, 0);
                if (delClick != null) {
                    setting.removeEntry(i);
                    textFields.remove(i);
                    if (capturingRow == i) capturingRow = -1;
                    rebuildTextFields();
                    return;
                }
            }

            // Capacity indicator
            String capText = entries.size() + "/" + AutoTextSetting.MAX_CAPACITY;
            FPSMaster.fontManager.getFont(12).drawString(capText, x + width - 20 - 50, y + 2, ClickGuiTheme.textSecondary().getRGB());

            this.height = 16 + entries.size() * (ROW_H + 3) + 4;
        }

        // + button
        float addY = y + 16 + entries.size() * (ROW_H + 3) + 4;
        float addX = x + (width - 20 - 14) / 2;
        boolean canAdd = entries.size() < AutoTextSetting.MAX_CAPACITY;
        boolean addHover = Hover.is(addX, addY, 14, 14, (int) mouseX, (int) mouseY);
        Color addBg = canAdd && addHover ? ClickGuiTheme.buttonHoverBg() : (canAdd ? ClickGuiTheme.buttonBg() : ClickGuiTheme.textFieldBg());
        Rects.rounded(Math.round(addX), Math.round(addY), 14, 14, CORNER, addBg);
        FPSMaster.fontManager.getFont(12).drawString("+", addX + 3, addY + 2, canAdd ? ClickGuiTheme.textPrimary().getRGB() : ClickGuiTheme.textSecondary().getRGB());

        if (canAdd) {
            ScaledGuiScreen.PointerEvent addClick = screen.consumePressInBounds(addX, addY, 14, 14, 0);
            if (addClick != null) {
                // 新条目不自动绑键：keyCode 保持 0（None），避免误占快捷栏键位，
                // 由用户点击 Bind 按钮显式绑定。
                setting.addEntry(new AutoTextEntry(0, ""));
                rebuildTextFields();
                return;
            }
        }

        // Duplicate warning
        if (!duplicateWarning.isEmpty()) {
            FPSMaster.fontManager.getFont(12).drawString(duplicateWarning, x + 10, addY + 16, new Color(255, 80, 80).getRGB());
            this.height += 14;
        }

        this.height += 24;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (capturingRow >= 0) {
            ArrayList<AutoTextEntry> entries = setting.getValue();
            if (capturingRow < entries.size()) {
                boolean duplicate = false;
                for (int i = 0; i < entries.size(); i++) {
                    if (i != capturingRow && entries.get(i).keyCode == keyCode && keyCode != 0) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    setting.editEntry(capturingRow, new AutoTextEntry(keyCode, entries.get(capturingRow).message));
                }
            }
            capturingRow = -1;
            MainPanel.bindLock = "";
            return;
        }

        for (TextField tf : textFields) {
            tf.textboxKeyTyped(typedChar, keyCode);
        }
    }
}