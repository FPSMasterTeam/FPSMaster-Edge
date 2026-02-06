package top.fpsmaster.features.settings.impl.utils;

import top.fpsmaster.utils.render.draw.Colors;


import java.awt.*;

public class CustomColor {

    public float hue;
    public float brightness;
    public float saturation;
    public float alpha;
    public Color color;

    public CustomColor(float hue, float brightness, float saturation, float alpha) {
        this.hue = hue;
        this.brightness = brightness;
        this.saturation = saturation;
        this.alpha = alpha;
        this.color = Colors.alpha(
            Color.getHSBColor(hue, saturation, brightness),
            Colors.clamp((alpha * 255))
        );
    }

    public CustomColor(Color color) {
        float[] col = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), col);
        this.hue = col[0];
        this.saturation = col[1];
        this.brightness = col[2];
        this.color = color;
        this.alpha = color.getAlpha() / 255f;
    }

    public int getRGB() {
        return color.getRGB();
    }

    public Color getColor() {
        return color;
    }

    public void setColor(float hue, float saturation, float brightness, float alpha) {
        this.hue = hue;
        this.saturation = saturation;
        this.brightness = brightness;
        this.alpha = alpha;
        this.color = Colors.alpha(
            Color.getHSBColor(hue, saturation, brightness),
            Colors.clamp((alpha * 255))
        );
    }

    public void setColor(Color color) {
        float[] col = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), col);
        this.hue = col[0];
        this.saturation = col[1];
        this.brightness = col[2];
        this.color = color;
        this.alpha = color.getAlpha() / 255f;
    }
}




