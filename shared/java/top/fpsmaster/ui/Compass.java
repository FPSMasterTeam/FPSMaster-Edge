package top.fpsmaster.ui;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;
import java.util.List;

public class Compass {

    public static List<Degree> degrees = Lists.newArrayList();
    public float innerWidth;
    public float outerWidth;
    public boolean shadow;
    public float scale;
    public int accuracy;

    public Compass(float i, float o, float s, int a, boolean sh) {
        innerWidth = i;
        outerWidth = o;
        scale = s;
        accuracy = a;
        shadow = sh;

        degrees.add(new Degree("N", 1));
        degrees.add(new Degree("195", 2));
        degrees.add(new Degree("210", 2));
        degrees.add(new Degree("NE", 3));
        degrees.add(new Degree("240", 2));
        degrees.add(new Degree("255", 2));
        degrees.add(new Degree("E", 1));
        degrees.add(new Degree("285", 2));
        degrees.add(new Degree("300", 2));
        degrees.add(new Degree("SE", 3));
        degrees.add(new Degree("330", 2));
        degrees.add(new Degree("345", 2));
        degrees.add(new Degree("S", 1));
        degrees.add(new Degree("15", 2));
        degrees.add(new Degree("30", 2));
        degrees.add(new Degree("SW", 3));
        degrees.add(new Degree("60", 2));
        degrees.add(new Degree("75", 2));
        degrees.add(new Degree("W", 1));
        degrees.add(new Degree("105", 2));
        degrees.add(new Degree("120", 2));
        degrees.add(new Degree("NW", 3));
        degrees.add(new Degree("150", 2));
        degrees.add(new Degree("165", 2));
    }

    public void draw(ScaledResolution sr) {
        if (ProviderManager.mcProvider.getPlayer() == null)
            return;
        preRender(sr);
        float center = sr.getScaledWidth() / 2f;
        int count = 0;
        float yaaahhrewindTime = (ProviderManager.mcProvider.getPlayer().rotationYaw % 360) * 2 + 360 * 3;
        GL11.glPushMatrix();
        GL11.glEnable(3089);
        int scaleFactor = Render2DUtils.fixScale();
        Render2DUtils.doGlScissor(sr.getScaledWidth() / 2f - 100, 25, 200, 25, scaleFactor);
        for (Degree d : degrees) {
            float location = center + (count * 30) - yaaahhrewindTime;
            float completeLocation = d.type == 1 ? (location - FPSMaster.fontManager.s28.getStringWidth(d.text) / 2f)
                    : d.type == 2 ? (location - FPSMaster.fontManager.s28.getStringWidth(d.text) / 2f)
                    : (location - FPSMaster.fontManager.s22.getStringWidth(d.text) / 2f);

            int opacity = opacity(sr, completeLocation);

            if (d.type == 1 && opacity != 16777215) {
                GlStateManager.color(1, 1, 1, 1);
                FPSMaster.fontManager.s28.drawString(d.text, completeLocation, -75 + 100, opacity(sr, completeLocation));
            }

            if (d.type == 2 && opacity != 16777215) {
                GlStateManager.color(1, 1, 1, 1);
                Gui.drawRect((int) (location - 0.5), -75 + 100 + 4, (int) (location + 0.5), -75 + 105 + 4,
                        opacity(sr, completeLocation));
                GlStateManager.color(1, 1, 1, 1);
                FPSMaster.fontManager.s14.drawString(d.text, completeLocation, -75 + 105 + 3.5f + 4, opacity(sr, completeLocation));
            }

            if (d.type == 3 && opacity != 16777215) {
                GlStateManager.color(1, 1, 1, 1);
                FPSMaster.fontManager.s22.drawString(d.text, completeLocation,
                        -75 + 100 + FPSMaster.fontManager.s28.getHeight() / 2 - FPSMaster.fontManager.s22.getHeight() / 2,
                        opacity(sr, completeLocation));
            }

            count++;
        }

        for (Degree d : degrees) {

            float location = center + (count * 30) - yaaahhrewindTime;
            float completeLocation = d.type == 1 ? (location - FPSMaster.fontManager.s28.getStringWidth(d.text) / 2f)
                    : d.type == 2 ? (location - FPSMaster.fontManager.s14.getStringWidth(d.text) / 2f)
                    : (location - FPSMaster.fontManager.s22.getStringWidth(d.text) / 2f);


            if (d.type == 1) {
                GlStateManager.color(1, 1, 1, 1);
                FPSMaster.fontManager.s28.drawString(d.text, completeLocation, -75 + 100, opacity(sr, completeLocation));
            }

            if (d.type == 2) {
                GlStateManager.color(1, 1, 1, 1);
                Gui.drawRect((int) (location - 0.5), -75 + 100 + 4, (int) (location + 0.5), -75 + 105 + 4,
                        opacity(sr, completeLocation));
                GlStateManager.color(1, 1, 1, 1);
                FPSMaster.fontManager.s14.drawString(d.text, completeLocation, -75 + 105 + 3.5f + 4, opacity(sr, completeLocation));
            }

            if (d.type == 3) {
                GlStateManager.color(1, 1, 1, 1);
                FPSMaster.fontManager.s22.drawString(d.text, completeLocation,
                        -75 + 100 + FPSMaster.fontManager.s28.getHeight() / 2 - FPSMaster.fontManager.s22.getHeight() / 2,
                        opacity(sr, completeLocation));
            }

            count++;
        }
        for (Degree d : degrees) {

            float location = center + (count * 30) - yaaahhrewindTime;
            float completeLocation = d.type == 1 ? (location - FPSMaster.fontManager.s28.getStringWidth(d.text) / 2f)
                    : d.type == 2 ? (location - FPSMaster.fontManager.s14.getStringWidth(d.text) / 2f)
                    : (location - FPSMaster.fontManager.s22.getStringWidth(d.text) / 2f);

            if (d.type == 1) {
                GlStateManager.color(1, 1, 1, 1);
                FPSMaster.fontManager.s28.drawString(d.text, completeLocation, -75 + 100, opacity(sr, completeLocation));
            }

            if (d.type == 2) {
                GlStateManager.color(1, 1, 1, 1);
                Gui.drawRect((int) (location - 0.5), -75 + 100 + 4, (int) (location + 0.5), -75 + 105 + 4,
                        opacity(sr, completeLocation));
                GlStateManager.color(1, 1, 1, 1);
                FPSMaster.fontManager.s14.drawString(d.text, completeLocation, -75 + 105 + 3.5f + 4, opacity(sr, completeLocation));
            }

            if (d.type == 3) {
                GlStateManager.color(1, 1, 1, 1);
                FPSMaster.fontManager.s22.drawString(d.text, completeLocation,
                        -75 + 100 + FPSMaster.fontManager.s28.getHeight() / 2 - FPSMaster.fontManager.s22.getHeight() / 2,
                        opacity(sr, completeLocation));
            }

            count++;
        }


        GL11.glDisable(3089);
        GL11.glPopMatrix();
    }

    public static void preRender(ScaledResolution sr) {
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();

    }

    public static int opacity(ScaledResolution sr, float offset) {
        int op = 0;
        float offs = 255 - Math.abs(sr.getScaledWidth() / 2f - offset) * 1.8f;
        Color c = new Color(255, 255, 255, (int) Math.min(Math.max(0, offs), 255));
        return c.getRGB();
    }

    public static class Degree {
        public String text;
        public int type;

        public Degree(String s, int t) {
            text = s;
            type = t;
        }

    }

}
