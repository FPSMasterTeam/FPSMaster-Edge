package top.fpsmaster.ui.screens.oobe;

import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.AnimMath;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.Color;

public class OobeDropdown {
    private String label = "";
    private String[] items = new String[0];
    private int selectedIndex;
    private boolean open;
    private boolean enabled = true;
    private float openProgress;
    private final AnimClock clock = new AnimClock();
    private boolean selectionChanged;
    private float hoverAnim;
    private float pressAnim;

    public OobeDropdown setLabel(String label) {
        this.label = label;
        return this;
    }

    public OobeDropdown setItems(String[] items) {
        this.items = items == null ? new String[0] : items;
        if (selectedIndex >= this.items.length) {
            selectedIndex = Math.max(0, this.items.length - 1);
        }
        return this;
    }

    public OobeDropdown setSelectedIndex(int selectedIndex) {
        this.selectedIndex = Math.max(0, Math.min(selectedIndex, Math.max(0, items.length - 1)));
        return this;
    }

    public OobeDropdown setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            open = false;
        }
        return this;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public boolean consumeSelectionChanged() {
        boolean changed = selectionChanged;
        selectionChanged = false;
        return changed;
    }

    public void renderInScreen(ScaledGuiScreen screen, float x, float y, float width, float height, int mouseX, int mouseY) {
        float dt = (float) clock.tick();
        float target = open && enabled ? 1f : 0f;
        openProgress += (target - openProgress) * Math.min(1f, dt * 12f);
        boolean hovered = enabled && Hover.is(x, y, width, height, mouseX, mouseY);
        hoverAnim = (float) AnimMath.base(hoverAnim, hovered ? 1.0 : 0.0, 0.22);
        pressAnim = (float) AnimMath.base(pressAnim, 0.0, 0.28);

        Color headColor = enabled
                ? (hovered ? new Color(247, 249, 255, 252) : new Color(244, 247, 255, 248))
                : new Color(235, 239, 246, 205);
        Color labelColor = enabled ? new Color(110, 119, 136, 255) : new Color(146, 152, 164, 200);
        Color valueColor = enabled ? new Color(27, 35, 48, 255) : new Color(126, 132, 144, 200);

        float inset = pressAnim * 1.3f;
        float drawX = x + inset;
        float drawY = y + inset;
        float drawWidth = Math.max(4f, width - inset * 2f);
        float drawHeight = Math.max(4f, height - inset * 2f);

        Rects.rounded(Math.round(drawX), Math.round(drawY), Math.round(drawWidth), Math.round(drawHeight), 14, headColor.getRGB());
        FPSMaster.fontManager.s16.drawString(label, drawX + 14f, drawY + 10f, labelColor.getRGB());
        String selected = items.length == 0 ? "" : items[selectedIndex];
        float valueX = drawX + drawWidth - 20f - FPSMaster.fontManager.s16.getStringWidth(selected) - 16f;
        FPSMaster.fontManager.s16.drawString(selected, Math.max(drawX + 74f, valueX), drawY + 10f + pressAnim * 0.5f, valueColor.getRGB());
        renderArrow(drawX + drawWidth - 18f, drawY + drawHeight / 2f, openProgress, new Color(104, 117, 247, 230));

        if (enabled && screen.consumePressInBounds(x, y, width, height, 0) != null) {
            pressAnim = 1.0f;
            open = !open;
        }

        if (openProgress > 0.02f && enabled) {
            float optionHeight = 24f;
            float optionGap = 3f;
            float panelHeight = items.length * optionHeight + Math.max(0, items.length - 1) * optionGap + 10f;
            float panelY = y + height + 6f;
            int panelAlpha = Math.max(0, Math.min(255, (int) (openProgress * 255f)));

            Rects.rounded(Math.round(x), Math.round(panelY), Math.round(width), Math.round(panelHeight * openProgress), 10,
                    new Color(255, 255, 255, Math.min(244, panelAlpha)).getRGB());

            for (int i = 0; i < items.length; i++) {
                float optionY = panelY + 5f + i * (optionHeight + optionGap);
                int alpha = Math.max(0, Math.min(255, (int) (openProgress * 255f)));
                boolean optionHovered = Hover.is(x + 4f, optionY, width - 8f, optionHeight, mouseX, mouseY);
                Color optionColor = i == selectedIndex
                        ? new Color(104, 117, 247, Math.min(228, alpha))
                        : (optionHovered ? new Color(240, 244, 255, Math.min(236, alpha)) : new Color(255, 255, 255, 0));
                if (optionColor.getAlpha() > 0) {
                    Rects.rounded(Math.round(x + 4f), Math.round(optionY), Math.round(width - 8f), Math.round(optionHeight), 10, optionColor.getRGB());
                }
                FPSMaster.fontManager.s16.drawString(items[i], x + 12f, optionY + 6f,
                        (i == selectedIndex ? new Color(255, 255, 255, alpha) : new Color(78, 89, 108, alpha)).getRGB());
                if (openProgress > 0.95f && screen.consumePressInBounds(x, optionY, width, optionHeight, 0) != null) {
                    selectedIndex = i;
                    selectionChanged = true;
                    pressAnim = 1.0f;
                    open = false;
                }
            }

            ScaledGuiScreen.PointerEvent outside = screen.peekAnyPress();
            if (outside != null && openProgress > 0.95f && !Hover.is(x, y, width, height + panelHeight + 4f, outside.x, outside.y)) {
                open = false;
            }
        }
    }

    private void renderArrow(float centerX, float centerY, float progress, Color color) {
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 0f);
        GL11.glRotatef(progress * 180f, 0f, 0f, 1f);
        GL11.glTranslatef(-centerX, -centerY, 0f);
        Icons.draw("arrow", centerX - 4f, centerY - 4f, 8f, color.getRGB());
        GL11.glPopMatrix();
    }
}
