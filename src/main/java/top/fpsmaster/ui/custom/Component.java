package top.fpsmaster.ui.custom;

import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.font.impl.UFontRenderer;
import net.minecraft.client.Minecraft;
import top.fpsmaster.ui.click.MainPanel;
import top.fpsmaster.utils.core.Utility;
import top.fpsmaster.utils.math.animation.AnimationUtils;

import java.awt.*;

public class Component {
    private float dragX = 0f;

    private float dragY = 0f;

    public InterfaceModule mod;

    public float x = 0f;

    public float y = 0f;

    public float width = 0f;

    public float height = 0f;

    public float scale = 1f;

    public boolean allowScale = false;

    public Position position = Position.LT;

    public Component(Class<?> clazz) {
        this.mod = (InterfaceModule) FPSMaster.moduleManager.getModule(clazz);
    }

    public void draw(float x, float y) {
    }

    public float alpha = 0f;

    public boolean shouldDisplay() {
        return mod.isEnabled();
    }

    public float[] getRealPosition() {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        float rX = 0f;
        float rY = 0f;
        x = Math.max(0f, Math.min(1f, x));
        y = Math.max(0f, Math.min(1f, y));

        int scaleFactor = ClientSettings.fixedScale.getValue() ? sr.getScaleFactor() : 2;
        float guiWidth = sr.getScaledWidth() / 2f * scaleFactor;
        float guiHeight = sr.getScaledHeight() / 2f * scaleFactor;

        switch (position) {
            case LT:
                rX = x * guiWidth / 2f;
                rY = y * guiHeight / 2f;
                break;
            case RT:
                rX = guiWidth - (x * guiWidth / 2f + width);
                rY = y * guiHeight / 2f;
                break;
            case LB:
                rX = x * guiWidth / 2f;
                rY = guiHeight - (y * guiHeight / 2f + height);
                break;
            case RB:
                rX = guiWidth - (x * guiWidth / 2f + width);
                rY = guiHeight - (y * guiHeight / 2f + height);
                break;
            case CT:
                break;
        }
        return new float[]{rX, rY};
    }

    public void display(int mouseX, int mouseY) {
        float rX = getRealPosition()[0];
        float rY = getRealPosition()[1];
        if ((Utility.mc.currentScreen instanceof GuiChat || Utility.mc.currentScreen instanceof MainPanel)) {
            float scaledWidth = width * scale;
            float scaledHeight = height * scale;
            boolean drag = FPSMaster.componentsManager.dragLock.equals(mod.name);

            alpha = (float) ((Hover.is(rX, rY, scaledWidth, scaledHeight, mouseX, mouseY) || drag) ?
                    AnimationUtils.base(alpha, 1f, 0.2f) : AnimationUtils.base(alpha, 0.0f, 0.2f));

            Rects.fill(rX - 2, rY - 2, scaledWidth + 4, scaledHeight + 4, new Color(0, 0, 0, (int) (alpha * 80)));
            draw(rX, rY);
            GL11.glColor4f(1, 1, 1, 1);
            if (!Mouse.isButtonDown(0)) {
                FPSMaster.componentsManager.dragLock = "";
            }
            if (Hover.is(rX, rY, scaledWidth, scaledHeight, mouseX, mouseY) || drag) {
                if (!MainPanel.dragLock.equals("null"))
                    return;
                if (allowScale) {
                    int dWheel = Mouse.getDWheel();
                    if (dWheel > 0) scaleUp();
                    else if (dWheel < 0) scaleDown();
                }
                FPSMaster.fontManager.s14.drawString(FPSMaster.i18n.get(mod.name.toLowerCase()) + " " + (scale * 10) / 10f + "x", rX, rY - 10, new Color(255, 255, 255, (int) (alpha * 255)).getRGB());

                if (!Mouse.isButtonDown(0)) return;

                if (!drag && FPSMaster.componentsManager.dragLock.isEmpty()) {
                    dragX = mouseX - rX;
                    dragY = mouseY - rY;
                    FPSMaster.componentsManager.dragLock = mod.name;
                }

                if (FPSMaster.componentsManager.dragLock.equals(mod.name)) {
                    move(mouseX, mouseY);
                    FPSMaster.componentsManager.dragLock = mod.name;
                }
            }
        } else {
            draw(rX, rY);
        }
    }

    public void scaleUp() {
        if (scale < 2.5f) scale = (int) (scale * 10 + 1) / 10f;
    }

    public void scaleDown() {
        if (scale > 0.5f) scale = (int) (scale * 10 - 1) / 10f;
    }

    private void move(int x, int y) {
        ScaledResolution sr = new ScaledResolution(Utility.mc);
        int scaleFactor = 2;
        if (ClientSettings.fixedScale.getValue()) {
            scaleFactor = sr.getScaleFactor();
        }
        float guiWidth = sr.getScaledWidth() / 2f * scaleFactor;
        float guiHeight = sr.getScaledHeight() / 2f * scaleFactor;
        float changeX = 0f;
        float changeY = 0f;
        if (x > guiWidth / 2f) {
            if (y >= guiHeight / 2f)
                position = Position.RB;
            else if (y < guiHeight / 2f)
                position = Position.RT;
        } else {
            if (y >= guiHeight / 2f)
                position = Position.LB;
            else if (y < guiHeight / 2f)
                position = Position.LT;
        }

        switch (position) {
            case LT: {
                changeX = x - dragX;
                changeY = y - dragY;
                break;
            }
            case RT: {
                changeX = guiWidth - x - width + dragX;
                changeY = y - dragY;
                break;
            }

            case LB: {
                changeX = x - dragX;
                changeY = guiHeight - y - height + dragY;
                break;
            }

            case RB: {
                changeX = guiWidth - x - width + dragX;
                changeY = guiHeight - y - height + dragY;
                break;
            }

            case CT: {

            }
        }

        if (changeX < 0f || changeX + width * scale > guiWidth) {
            changeX = Math.min(Math.max(changeX, 0f), guiWidth - width * scale);
        }
        if (changeY < 0f || changeY + height * scale > guiHeight) {
            changeY = Math.min(Math.max(changeY, 0f), guiHeight - height * scale);
        }

        this.x = changeX / guiWidth * 2f;
        this.y = changeY / guiHeight * 2f;
    }

    public void drawRect(float x, float y, float width, float height, Color color) {
        float scaledWidth = width * scale;
        float scaledHeight = height * scale;

        if (mod.bg.getValue()) {
            if (mod.rounded.getValue()) {
                Rects.roundedImage(x, y, scaledWidth, scaledHeight, mod.roundRadius.getValue().intValue(), color);
            } else {
                Rects.fill(x, y, scaledWidth, scaledHeight, color);
            }
        }
    }

    public void drawString(int fontSize, String text, float x, float y, int color) {
        drawString(fontSize, false, text, x, y, color);
    }

    public void drawString(int fontSize, boolean bold, String text, float x, float y, int color) {
        double scaled = (int) (scale * 100) / 100.0;
        fontSize = (int) (fontSize * scale);
        UFontRenderer font = FPSMaster.fontManager.getFont(fontSize);
        if (mod.betterFont.getValue()) {
            if (mod.fontShadow.getValue()) font.drawStringWithShadow(text, x, y, color);
            else font.drawString(text, x, y, color);
        } else {
            GL11.glPushMatrix();
            GL11.glTranslated(x, y, 0.0);
            GL11.glScaled(scaled, scaled, 1.0);
                if (mod.fontShadow.getValue()) {
                    Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, 0, 0, color);
                } else {
                    GL11.glColor4f(1, 1, 1, 1);
                    Minecraft.getMinecraft().fontRendererObj.drawString(text, 0, 0, color);
                }
            GL11.glPopMatrix();
        }
    }

    public float getStringWidth(int fontSize, String name) {
        UFontRenderer font = FPSMaster.fontManager.getFont(fontSize);
        return mod.betterFont.getValue() ? font.getStringWidth(name) : Minecraft.getMinecraft().fontRendererObj.getStringWidth(name);
    }

    public float getStringHeight(int fontSize) {
        UFontRenderer font = FPSMaster.fontManager.getFont(fontSize);
        return mod.betterFont.getValue() ? font.getHeight() : Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
    }
}




