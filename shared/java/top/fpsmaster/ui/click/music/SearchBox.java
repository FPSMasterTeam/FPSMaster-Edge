package top.fpsmaster.ui.click.music;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.utils.math.MathUtils;
import top.fpsmaster.utils.math.animation.ColorAnimation;
import top.fpsmaster.utils.render.Render2DUtils;
import java.awt.Color;

public class SearchBox extends Gui {
    private UFontRenderer font;

    private float width;
    private float height;
    private float xPosition;
    private float yPosition;

    private String text = "";
    private int maxStringLength = 1000;
    private int cursorCounter;
    private boolean enableBackgroundDrawing = true;
    private boolean canLoseFocus = true;
    private boolean isFocused;
    private boolean isEnabled = true;
    private int lineScrollOffset = 0;
    private int cursorPosition = 0;
    private int selectionEnd = 0;
    private Color enabledColor;
    private Color disabledColor;

    private Predicate<String> validator = Predicates.alwaysTrue();
    private String placeholder = "";
    private ColorAnimation btnColor = new ColorAnimation();

    private boolean visible = true;
    private Runnable runnable;

    public SearchBox(String placeholder, Color enable, Color disable, Color focus, Color hover, UFontRenderer fontrendererObj) {
        this.placeholder = placeholder;
        this.enabledColor = enable;
        this.disabledColor = disable;
        this.font = fontrendererObj;
    }

    public SearchBox(String placeholder, UFontRenderer fontrendererObj, Runnable runnable) {
        this.font = fontrendererObj;
        this.runnable = runnable;
        this.placeholder = placeholder;
        this.enabledColor = FPSMaster.theme.getTextboxEnabled();
        this.disabledColor = FPSMaster.theme.getTextboxDisabled();
    }

    public SearchBox(String placeholder) {
        this.font = FPSMaster.fontManager.s18;
        this.placeholder = placeholder;
        this.enabledColor = FPSMaster.theme.getTextboxEnabled();
        this.disabledColor = FPSMaster.theme.getTextboxDisabled();
    }

    public SearchBox(String s, Runnable runnable) {
        this.font = FPSMaster.fontManager.s18;
        this.placeholder = s;
        this.runnable = runnable;
        this.enabledColor = FPSMaster.theme.getTextboxEnabled();
        this.disabledColor = FPSMaster.theme.getTextboxDisabled();
    }

    public void updateCursorCounter() {
        ++this.cursorCounter;
    }

    public String getContent() {
        return this.text;
    }

    public void setContent(String content) {
        this.text = content;
        this.setCursorPositionEnd();
    }

    private String getSelectedText() {
        int i = Math.min(cursorPosition, selectionEnd);
        int j = Math.max(cursorPosition, selectionEnd);
        return text.substring(i, j);
    }

    private void writeText(String textToWrite) {
        String s = "";
        String s1 = ChatAllowedCharacters.filterAllowedCharacters(textToWrite);
        int i = Math.min(cursorPosition, selectionEnd);
        int j = Math.max(cursorPosition, selectionEnd);
        int k = this.maxStringLength - text.length() - (i - j);

        if (!text.isEmpty()) {
            s += text.substring(0, i);
        }

        int l;
        if (k < s1.length()) {
            s += s1.substring(0, k);
            l = k;
        } else {
            s += s1;
            l = s1.length();
        }

        if (!text.isEmpty() && j < text.length()) {
            s += text.substring(j);
        }

        if (validator.apply(s)) {
            this.text = s;
            this.moveCursorBy(i - this.selectionEnd + l);
        }
    }

    private void deleteWords(int num) {
        if (!text.isEmpty()) {
            if (this.selectionEnd != this.cursorPosition) {
                this.writeText("");
            } else {
                this.deleteFromCursor(this.getNthWordFromCursor(num) - this.cursorPosition);
            }
        }
    }

    private void deleteFromCursor(int num) {
        if (!text.isEmpty()) {
            if (this.selectionEnd != this.cursorPosition) {
                this.writeText("");
            } else {
                boolean flag = num < 0;
                int i = flag ? this.cursorPosition + num : this.cursorPosition;
                int j = flag ? this.cursorPosition : this.cursorPosition + num;
                String s = "";

                if (i >= 0) {
                    s = text.substring(0, i);
                }

                if (j < text.length()) {
                    s += text.substring(j);
                }

                if (validator.apply(s)) {
                    this.text = s;

                    if (flag) {
                        this.moveCursorBy(num);
                    }
                }
            }
        }
    }

    private int getNthWordFromCursor(int numWords) {
        return this.getNthWordFromPos(numWords, this.cursorPosition);
    }

    private int getNthWordFromPos(int n, int pos) {
        return this.getNthWordFromPosWS(n, pos, true);
    }

    private int getNthWordFromPosWS(int n, int pos, boolean skipWs) {
        int i = pos;
        boolean flag = n < 0;
        int j = Math.abs(n);

        for (int k = 0; k < j; k++) {
            if (!flag) {
                int l = text.length();
                i = text.indexOf(' ', i);

                if (i == -1) {
                    i = l;
                } else {
                    while (skipWs && i < l && text.charAt(i) == ' ') {
                        ++i;
                    }
                }
            } else {
                while (skipWs && i > 0 && text.charAt(i - 1) == ' ') {
                    --i;
                }

                while (i > 0 && text.charAt(i - 1) != ' ') {
                    --i;
                }
            }
        }

        return i;
    }

    private void moveCursorBy(int num) {
        this.cursorPosition = this.selectionEnd + num;
    }

    private void setCursorPositionZero() {
        this.cursorPosition = 0;
    }

    private void setCursorPositionEnd() {
        this.cursorPosition = text.length();
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (!this.isFocused) {
            return false;
        } else if (GuiScreen.isKeyComboCtrlA(keyCode)) {
            this.setCursorPositionEnd();
            this.setSelectionPos(0);
            return true;
        } else if (GuiScreen.isKeyComboCtrlC(keyCode)) {
            GuiScreen.setClipboardString(this.getSelectedText());
            return true;
        } else if (GuiScreen.isKeyComboCtrlV(keyCode)) {
            if (this.isEnabled) {
                this.writeText(GuiScreen.getClipboardString());
            }

            return true;
        } else if (GuiScreen.isKeyComboCtrlX(keyCode)) {
            GuiScreen.setClipboardString(this.getSelectedText());

            if (this.isEnabled) {
                this.writeText("");
            }

            return true;
        } else {
            switch (keyCode) {
                case 14:
                    if (GuiScreen.isCtrlKeyDown()) {
                        if (this.isEnabled) {
                            this.deleteWords(-1);
                        }
                    } else if (this.isEnabled) {
                        this.deleteFromCursor(-1);
                    }
                    return true;

                case 199:
                    if (GuiScreen.isShiftKeyDown()) {
                        this.setSelectionPos(0);
                    } else {
                        this.setCursorPositionZero();
                    }
                    return true;

                case 203:
                    if (GuiScreen.isShiftKeyDown()) {
                        if (GuiScreen.isCtrlKeyDown()) {
                            this.setSelectionPos(this.getNthWordFromPos(-1, selectionEnd));
                        } else {
                            this.setSelectionPos(this.selectionEnd - 1);
                        }
                    } else if (GuiScreen.isCtrlKeyDown()) {
                        this.cursorPosition = this.getNthWordFromCursor(-1);
                    } else {
                        this.moveCursorBy(-1);
                    }
                    return true;

                case 205:
                    if (GuiScreen.isShiftKeyDown()) {
                        if (GuiScreen.isCtrlKeyDown()) {
                            this.setSelectionPos(this.getNthWordFromPos(1, selectionEnd));
                        } else {
                            this.setSelectionPos(this.selectionEnd + 1);
                        }
                    } else if (GuiScreen.isCtrlKeyDown()) {
                        this.cursorPosition = this.getNthWordFromCursor(1);
                    } else {
                        this.moveCursorBy(1);
                    }
                    return true;

                case 207:
                    if (GuiScreen.isShiftKeyDown()) {
                        this.setSelectionPos(text.length());
                    } else {
                        this.setCursorPositionEnd();
                    }
                    return true;

                case 211:
                    if (GuiScreen.isCtrlKeyDown()) {
                        if (this.isEnabled) {
                            this.deleteWords(1);
                        }
                    } else if (this.isEnabled) {
                        this.deleteFromCursor(1);
                    }
                    return true;

                case 28:
                    if (runnable != null) {
                        runnable.run();
                    }
                    return true;

                default:
                    if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
                        if (this.isEnabled) {
                            this.writeText(String.valueOf(typedChar));
                        }
                        return true;
                    }
                    return false;
            }
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean flag = (mouseX >= this.xPosition && mouseX < xPosition + this.width && mouseY >= yPosition) && mouseY < this.yPosition + this.height;

        if (this.canLoseFocus) {
            this.isFocused = flag;
        }

        if (this.isFocused && flag && mouseButton == 0) {
            int i = (int) (mouseX - this.xPosition);

            if (this.enableBackgroundDrawing) {
                i -= 4;
            }

            String s = font.trimStringToWidth(text.substring(this.lineScrollOffset), this.getWidth());
            this.cursorPosition = font.trimStringToWidth(s, i).length() + this.lineScrollOffset;
            return true;
        } else {
            return false;
        }
    }

    public void render(float x, float y, float width, float height, int mouseX, int mouseY) {
        this.xPosition = x;
        this.yPosition = y;
        this.width = width;
        this.height = height;
        if (this.visible) {
            if (Render2DUtils.isHovered(x, y, width, height, mouseX, mouseY)) {
                if (isFocused) {
                    btnColor.base(FPSMaster.theme.getTextboxFocus());
                } else {
                    btnColor.base(FPSMaster.theme.getTextboxHover());
                }
            } else {
                btnColor.base(enabledColor);
            }
            Render2DUtils.drawOptimizedRoundedRect(xPosition, yPosition, width, height, btnColor.getColor());

            int j = this.cursorPosition - this.lineScrollOffset;
            int k = this.selectionEnd - this.lineScrollOffset;
            String s = font.trimStringToWidth(text.substring(this.lineScrollOffset), this.getWidth());
            boolean flag = j >= 0 && j <= s.length();
            boolean flag1 = this.isFocused && (this.cursorCounter / 6 % 2 == 0) && flag;
            float l = this.enableBackgroundDrawing ? this.xPosition + 4 : this.xPosition;
            float i1 = this.enableBackgroundDrawing ? this.yPosition + (this.height - 8) / 2 : this.yPosition;
            float j1 = l;

            if (k > s.length()) {
                k = s.length();
            }

            if (s.isEmpty() && !isFocused) {
                font.drawStringWithShadow(placeholder, xPosition + 4, i1, new Color(120, 120, 120).getRGB());
            } else {
                font.drawStringWithShadow(s.substring(0, k), l, i1, -1);
            }

            if (flag1) {
                Gui.drawRect((int) (j1 + font.getStringWidth(s.substring(0, j))), (int) (i1 - 1), (int) (j1 + font.getStringWidth(s.substring(0, j)) + 1), (int) (i1 + font.getHeight() - 1), -3092272);
            }
        }
    }

    public int getWidth() {
        return (int) this.width;
    }

    public void setSelectionPos(int pos) {
        if (pos > this.text.length()) {
            pos = this.text.length();
        }

        if (pos < 0) {
            pos = 0;
        }

        this.selectionEnd = pos;
        if (font != null) {
            int i = font.trimStringToWidth(text, this.getWidth()).length();
            if (i > 0) {
                this.selectionEnd = i;
            }
        }
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setCanLoseFocus(boolean canLoseFocus) {
        this.canLoseFocus = canLoseFocus;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }
}
