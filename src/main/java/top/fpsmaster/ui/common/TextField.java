package top.fpsmaster.ui.common;

import top.fpsmaster.utils.render.draw.Rects;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ChatAllowedCharacters;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.utils.math.MathUtils;

import java.awt.*;
import java.util.Arrays;
import java.util.function.Predicate;

public class TextField extends Gui {
    private final UFontRenderer fontRendererInstance;
    public float xPosition;
    public float yPosition;

    /**
     * The width of this text field.
     */
    private float width;
    private float height;

    /**
     * Has the current text being edited on the textbox.
     */
    public String text = "";
    private int maxStringLength;
    private int cursorCounter;

    /**
     * if true the textbox can lose focus by clicking elsewhere on the screen
     */
    private boolean canLoseFocus = true;

    /**
     * If this value is true along with isEnabled, keyTyped will process the keys.
     */
    private boolean isFocused;

    /**
     * If this value is true along with isFocused, keyTyped will process the keys.
     */
    private boolean isEnabled = true;

    /**
     * The current character index that should be used as start of the rendered text.
     */
    private int lineScrollOffset = 0;
    private int cursorPosition;

    /**
     * other selection position, maybe the same as the cursor
     */
    private int selectionEnd;
    private boolean hideContent = false;

    /**
     * True if this textbox is visible
     */
    private boolean visible = true;
    private Predicate<String> validator = s -> true;
    public int backGroundColor;
    public int fontColor;
    public String placeHolder;
    private Runnable onEnter;


    public TextField(UFontRenderer fontrendererObj, String placeHolder, int color, int fontColor, int maxLength, Runnable onEnter) {
        this(fontrendererObj, placeHolder, color, fontColor, maxLength);
        this.onEnter = onEnter;
    }

    public TextField(UFontRenderer fontrendererObj, String placeHolder, int color, int fontColor, int maxLength) {
        this.backGroundColor = color;
        this.fontColor = fontColor;
        this.fontRendererInstance = fontrendererObj;
        this.maxStringLength = maxLength;
        this.placeHolder = placeHolder;
    }

    public TextField(UFontRenderer fontrendererObj, boolean hideContent, String placeHolder, int color,int fontColor, int maxLength) {
        this.backGroundColor = color;
        this.fontColor = fontColor;
        this.fontRendererInstance = fontrendererObj;
        this.maxStringLength = maxLength;
        this.hideContent = hideContent;
        this.placeHolder = placeHolder;
    }


    /**
     * Increments the cursor counter
     */
    public void updateCursorCounter() {
        ++this.cursorCounter;
    }

    /**
     * Sets the text of the textbox
     */
    public void setText(String text) {
        if (this.validator.test(text)) {
            if (text.length() > this.maxStringLength) {
                this.text = text.substring(0, this.maxStringLength);
            } else {
                this.text = text;
            }

            this.setCursorPositionZero();
        }
    }

    /**
     * Returns the contents of the textbox
     */
    public String getText() {
        return this.text;
    }

    /**
     * returns the text between the cursor and selectionEnd
     */
    public String getSelectedText() {
        int i = Math.min(this.cursorPosition, this.selectionEnd);
        int j = Math.max(this.cursorPosition, this.selectionEnd);
        return this.text.substring(i, j);
    }

    public void setValidator(Predicate<String> theValidator) {
        this.validator = theValidator;
    }

    /**
     * replaces selected text, or inserts text at the position on the cursor
     */
    public void writeText(String p_146191_1_) {
        String s = "";
        String s1 = ChatAllowedCharacters.filterAllowedCharacters(p_146191_1_);
        int i = Math.min(this.cursorPosition, this.selectionEnd);
        int j = Math.max(this.cursorPosition, this.selectionEnd);
        int k = this.maxStringLength - this.text.length() - (i - j);
        int l;

        if (!this.text.isEmpty()) {
            s = s + this.text.substring(0, i);
        }

        if (k < s1.length()) {
            s = s + s1.substring(0, k);
            l = k;
        } else {
            s = s + s1;
            l = s1.length();
        }

        if (!this.text.isEmpty() && j < this.text.length()) {
            s = s + this.text.substring(j);
        }

        if (this.validator.test(s)) {
            this.text = s;
            this.moveCursorBy(i - this.selectionEnd + l);
        }
    }

    /**
     * Deletes the specified number of words starting at the cursor position. Negative numbers will delete words left of
     * the cursor.
     */
    public void deleteWords(int num) {
        if (!this.text.isEmpty()) {
            if (this.selectionEnd != this.cursorPosition) {
                this.writeText("");
            } else {
                this.deleteFromCursor(this.getNthWordFromCursor(num) - this.cursorPosition);
            }
        }
    }

    /**
     * delete the selected text, otherwsie deletes characters from either side of the cursor. params: delete num
     */
    public void deleteFromCursor(int num) {
        if (!this.text.isEmpty()) {
            if (this.selectionEnd != this.cursorPosition) {
                this.writeText("");
            } else {
                boolean flag = num < 0;
                int i = flag ? this.cursorPosition + num : this.cursorPosition;
                int j = flag ? this.cursorPosition : this.cursorPosition + num;
                String s = "";

                if (i >= 0) {
                    s = this.text.substring(0, i);
                }

                if (j < this.text.length()) {
                    s = s + this.text.substring(j);
                }

                if (this.validator.test(s)) {
                    this.text = s;

                    if (flag) {
                        this.moveCursorBy(num);
                    }
                }
            }
        }
    }


    /**
     * see @getNthNextWordFromPos() params: N, position
     */
    public int getNthWordFromCursor(int position) {
        return this.getNthWordFromPos(position, this.getCursorPosition());
    }

    /**
     * gets the position of the nth word. N may be negative, then it looks backwards. params: N, position
     */
    public int getNthWordFromPos(int n, int position) {
        return this.getNthWordFromPos(n, position, true);
    }

    public int getNthWordFromPos(int n, int pos, boolean p_146197_3_) {
        int i = pos;
        boolean flag = n < 0;
        int j = Math.abs(n);

        for (int k = 0; k < j; ++k) {
            if (!flag) {
                int l = this.text.length();
                i = this.text.indexOf(32, i);

                if (i == -1) {
                    i = l;
                } else {
                    while (p_146197_3_ && i < l && this.text.charAt(i) == 32) {
                        ++i;
                    }
                }
            } else {
                while (p_146197_3_ && i > 0 && this.text.charAt(i - 1) == 32) {
                    --i;
                }

                while (i > 0 && this.text.charAt(i - 1) != 32) {
                    --i;
                }
            }
        }

        return i;
    }

    /**
     * Moves the text cursor by a specified number of characters and clears the selection
     */
    public void moveCursorBy(int p_146182_1_) {
        this.setCursorPosition(this.selectionEnd + p_146182_1_);
    }

    /**
     * sets the position of the cursor to the provided index
     */
    public void setCursorPosition(int p_146190_1_) {
        this.cursorPosition = p_146190_1_;
        int i = this.text.length();

        this.cursorPosition = MathUtils.clamp(cursorPosition, 0, i);
        this.setSelectionPos(this.cursorPosition);
    }

    /**
     * sets the cursors position to the beginning
     */
    public void setCursorPositionZero() {
        this.setCursorPosition(0);
    }

    /**
     * sets the cursors position to after the text
     */
    public void setCursorPositionEnd() {
        this.setCursorPosition(this.text.length());
    }

    /**
     * Call this method from your GuiScreen to process the keys into the textbox
     */
    public boolean textboxKeyTyped(char p_146201_1_, int p_146201_2_) {
        if (!this.isFocused) {
            return false;
        } else if (GuiScreen.isKeyComboCtrlA(p_146201_2_)) {
            this.setCursorPositionEnd();
            this.setSelectionPos(0);
            return true;
        } else if (GuiScreen.isKeyComboCtrlC(p_146201_2_)) {
            GuiScreen.setClipboardString(this.getSelectedText());
            return true;
        } else if (GuiScreen.isKeyComboCtrlV(p_146201_2_)) {
            if (this.isEnabled) {
                this.writeText(GuiScreen.getClipboardString());
            }

            return true;
        } else if (GuiScreen.isKeyComboCtrlX(p_146201_2_)) {
            GuiScreen.setClipboardString(this.getSelectedText());

            if (this.isEnabled) {
                this.writeText("");
            }

            return true;
        } else {
            switch (p_146201_2_) {
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
                            this.setSelectionPos(this.getNthWordFromPos(-1, this.getSelectionEnd()));
                        } else {
                            this.setSelectionPos(this.getSelectionEnd() - 1);
                        }
                    } else if (GuiScreen.isCtrlKeyDown()) {
                        this.setCursorPosition(this.getNthWordFromCursor(-1));
                    } else {
                        this.moveCursorBy(-1);
                    }

                    return true;

                case 205:
                    if (GuiScreen.isShiftKeyDown()) {
                        if (GuiScreen.isCtrlKeyDown()) {
                            this.setSelectionPos(this.getNthWordFromPos(1, this.getSelectionEnd()));
                        } else {
                            this.setSelectionPos(this.getSelectionEnd() + 1);
                        }
                    } else if (GuiScreen.isCtrlKeyDown()) {
                        this.setCursorPosition(this.getNthWordFromCursor(1));
                    } else {
                        this.moveCursorBy(1);
                    }

                    return true;

                case 207:
                    if (GuiScreen.isShiftKeyDown()) {
                        this.setSelectionPos(this.text.length());
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
                    if (onEnter != null) {
                        onEnter.run();
                    }
                    return true;
                default:
                    if (ChatAllowedCharacters.isAllowedCharacter(p_146201_1_)) {
                        if (this.isEnabled) {
                            this.writeText(Character.toString(p_146201_1_));
                        }

                        return true;
                    } else {
                        return false;
                    }
            }
        }
    }

    /**
     * Args: x, y, buttonClicked
     */
    public void mouseClicked(int mouseX, int mouseY, int btn) {
        boolean flag = mouseX >= this.xPosition && mouseX < this.xPosition + this.width && mouseY >= this.yPosition && mouseY < this.yPosition + this.height;

        if (this.canLoseFocus) {
            this.setFocused(flag);
        }

        if (this.isFocused && flag && btn == 0) {
            float i = mouseX - this.xPosition;

            String s = this.fontRendererInstance.trimStringToWidth(this.text.substring(this.lineScrollOffset), this.getWidth());
            this.setCursorPosition(this.fontRendererInstance.trimStringToWidth(s, i).length() + this.lineScrollOffset);
        }
    }

    /**
     * Draws the textbox
     */
    public void drawTextBox(float x, float y, float width, float height) {
        if (this.getVisible()) {
            this.width = width;
            this.height = height;
            this.xPosition = x;
            this.yPosition = y;
            Rects.rounded(this.xPosition, this.yPosition, this.width, this.height, backGroundColor);
            int i = fontColor;
            int j = this.cursorPosition - this.lineScrollOffset;
            int k = this.selectionEnd - this.lineScrollOffset;
            String s = this.fontRendererInstance.trimStringToWidth(this.text.substring(this.lineScrollOffset), this.getWidth());
            if (hideContent) {
                char[] stars = new char[s.length()];
                Arrays.fill(stars, '*');
                s = new String(stars);
            }

            boolean flag = j >= 0 && j <= s.length();
            boolean isFocus = this.isFocused && this.cursorCounter / 6 % 2 == 0 && flag;
            float l = this.xPosition + 4;
            float i1 = this.yPosition + (this.height - 8) / 2;
            float j1 = l;

            if (k > s.length()) {
                k = s.length();
            }

            if (!s.isEmpty()) {
                String s1 = flag ? s.substring(0, j) : s;
                j1 = l + this.fontRendererInstance.drawString(s1, l, i1, i);
            }

            boolean isSub = this.cursorPosition < this.text.length() || this.text.length() >= this.getMaxStringLength();
            float k1 = j1;

            if (!flag) {
                k1 = j > 0 ? l + this.width : l;
            } else if (isSub) {
                k1 = j1 - 1;
                --j1;
            }

            if (s.isEmpty()){
                this.fontRendererInstance.drawString(placeHolder, j1, i1, new Color(200,200,200).getRGB());
            }

            if (!s.isEmpty() && flag && j < s.length()) {
                this.fontRendererInstance.drawString(s.substring(j), j1, i1, i);
            }

            if (isFocus) {
                if (isSub) {
                    Rects.fill(k1, i1 - 1, 1, this.fontRendererInstance.getHeight(), -3092272);
                } else {
                    this.fontRendererInstance.drawString("_", k1, i1, i);
                }
            }

            if (k != j) {
                float l1 = l + this.fontRendererInstance.getStringWidth(s.substring(0, k));
                this.drawCursorVertical(k1, i1 - 1, l1 - 1, i1 + 1 + this.fontRendererInstance.getHeight());
            }
        }
    }

    /**
     * draws the vertical line cursor in the textbox
     */
    private void drawCursorVertical(float startX, float startY, float endX, float endY) {
        if (startX < endX) {
            float i = startX;
            startX = endX;
            endX = i;
        }

        if (startY < endY) {
            float j = startY;
            startY = endY;
            endY = j;
        }

        if (endX > this.xPosition + this.width) {
            endX = this.xPosition + this.width;
        }

        if (startX > this.xPosition + this.width) {
            startX = this.xPosition + this.width;
        }

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer bufferbuilder = tessellator.getWorldRenderer();
        GlStateManager.color(0.0F, 0.0F, 255.0F, 255.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.enableColorLogic();
        GlStateManager.colorLogicOp(5387);
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
        bufferbuilder.pos(startX, endY, 0.0D).endVertex();
        bufferbuilder.pos(endX, endY, 0.0D).endVertex();
        bufferbuilder.pos(endX, startY, 0.0D).endVertex();
        bufferbuilder.pos(startX, startY, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.disableColorLogic();
        GlStateManager.enableTexture2D();
    }

    public void setMaxStringLength(int p_146203_1_) {
        this.maxStringLength = p_146203_1_;

        if (this.text.length() > p_146203_1_) {
            this.text = this.text.substring(0, p_146203_1_);
        }
    }

    /**
     * returns the maximum number of character that can be contained in this textbox
     */
    public int getMaxStringLength() {
        return this.maxStringLength;
    }

    /**
     * returns the current position of the cursor
     */
    public int getCursorPosition() {
        return this.cursorPosition;
    }

    /**
     * Sets focus to this gui element
     */
    public void setFocused(boolean p_146195_1_) {
        if (p_146195_1_ && !this.isFocused) {
            this.cursorCounter = 0;
        }

        this.isFocused = p_146195_1_;
    }

    /**
     * Getter for the focused field
     */
    public boolean isFocused() {
        return this.isFocused;
    }

    public void setEnabled(boolean p_146184_1_) {
        this.isEnabled = p_146184_1_;
    }

    /**
     * the side of the selection that is not the cursor, may be the same as the cursor
     */
    public int getSelectionEnd() {
        return this.selectionEnd;
    }

    /**
     * returns the width of the textbox depending on if background drawing is enabled
     */
    public float getWidth() {
        return this.width;
    }

    /**
     * Sets the position of the selection anchor (i.e. position the selection was started at)
     */
    public void setSelectionPos(int pos) {
        int i = this.text.length();

        if (pos > i) {
            pos = i;
        }

        if (pos < 0) {
            pos = 0;
        }

        this.selectionEnd = pos;

        if (this.fontRendererInstance != null) {
            if (this.lineScrollOffset > i) {
                this.lineScrollOffset = i;
            }

            float j = this.getWidth();
            String s = this.fontRendererInstance.trimStringToWidth(this.text.substring(this.lineScrollOffset), j);
            int k = s.length() + this.lineScrollOffset;

            if (pos == this.lineScrollOffset) {
                this.lineScrollOffset -= this.fontRendererInstance.trimString(this.text, j, true).length();
            }

            if (pos > k) {
                this.lineScrollOffset += pos - k;
            } else if (pos <= this.lineScrollOffset) {
                this.lineScrollOffset -= this.lineScrollOffset - pos;
            }

            this.lineScrollOffset = MathUtils.clamp(this.lineScrollOffset, 0, i);
        }
    }

    /**
     * if true the textbox can lose focus by clicking elsewhere on the screen
     */
    public void setCanLoseFocus(boolean canLose) {
        this.canLoseFocus = canLose;
    }

    /**
     * returns true if this textbox is visible
     */
    public boolean getVisible() {
        return this.visible;
    }

    /**
     * Sets whether or not this textbox is visible
     */
    public void setVisible(boolean p_146189_1_) {
        this.visible = p_146189_1_;
    }
}




