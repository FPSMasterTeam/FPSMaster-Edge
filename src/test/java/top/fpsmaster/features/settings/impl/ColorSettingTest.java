package top.fpsmaster.features.settings.impl;

import org.junit.jupiter.api.Test;
import top.fpsmaster.features.settings.impl.utils.CustomColor;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorSettingTest {

    private static final CustomColor BASE = new CustomColor(0.5f, 0.7f, 1f, 0.8f);

    private static float hueOf(Color c) {
        return Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null)[0];
    }

    private static float brightnessOf(Color c) {
        return Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null)[2];
    }

    @Test
    void waveBrightnessWavesBrightnessOnly() {
        for (int i = 0; i < 20; i++) {
            Color c = ColorSetting.resolveColor(BASE, ColorSetting.ColorType.WAVE_BRIGHTNESS, 0f);
            assertEquals(0.8f, c.getAlpha() / 255f, 0.02f);
            assertEquals(0.5f, hueOf(c), 0.02f);
            float brightness = brightnessOf(c);
            assertTrue(brightness >= 0.35f * BASE.brightness - 0.02f, "brightness too low: " + brightness);
            assertTrue(brightness <= BASE.brightness + 0.02f, "brightness too high: " + brightness);
        }
    }

    @Test
    void waveWavesAlphaOnly() {
        for (int i = 0; i < 20; i++) {
            Color c = ColorSetting.resolveColor(BASE, ColorSetting.ColorType.WAVE, 0f);
            assertEquals(1f, brightnessOf(c), 0.02f);
            float alpha = c.getAlpha() / 255f;
            assertTrue(alpha >= 0.35f * BASE.alpha - 0.02f, "alpha too low: " + alpha);
            assertTrue(alpha <= BASE.alpha + 0.02f, "alpha too high: " + alpha);
        }
    }

    @Test
    void waveAlphaDependsOnOffset() {
        // Offsets 0 and 0.5 are opposite phases, so their alphas sum to a constant.
        Color a = ColorSetting.resolveColor(BASE, ColorSetting.ColorType.WAVE, 0f);
        Color b = ColorSetting.resolveColor(BASE, ColorSetting.ColorType.WAVE, 0.5f);
        float sum = (a.getAlpha() + b.getAlpha()) / 255f;
        assertEquals(1.35f * BASE.alpha, sum, 0.06f);
    }

    @Test
    void waveBrightnessDependsOnOffset() {
        Color a = ColorSetting.resolveColor(BASE, ColorSetting.ColorType.WAVE_BRIGHTNESS, 0f);
        Color b = ColorSetting.resolveColor(BASE, ColorSetting.ColorType.WAVE_BRIGHTNESS, 0.5f);
        float sum = brightnessOf(a) + brightnessOf(b);
        assertEquals(1.35f * BASE.brightness, sum, 0.08f);
    }

    @Test
    void staticIgnoresOffsetAndSpeed() {
        Color a = ColorSetting.resolveColor(BASE, ColorSetting.ColorType.STATIC, 0.5f, 5f);
        Color b = ColorSetting.resolveColor(BASE, ColorSetting.ColorType.STATIC, 0f, 0.5f);
        assertEquals(a, b);
        assertEquals(0.8f, a.getAlpha() / 255f, 0.01f);
    }

    @Test
    void rainbowAppliesPerRowOffsetWithinHueRange() {
        Color a = ColorSetting.resolveColor(BASE, ColorSetting.ColorType.RAINBOW, 0f);
        Color b = ColorSetting.resolveColor(BASE, ColorSetting.ColorType.RAINBOW, 0.5f);
        assertEquals(0.8f, a.getAlpha() / 255f, 0.01f);
        assertEquals(0.8f, b.getAlpha() / 255f, 0.01f);
        float hueA = hueOf(a);
        float hueB = hueOf(b);
        float diff = Math.min(Math.abs(hueA - hueB), 1f - Math.abs(hueA - hueB));
        assertTrue(diff > 0.3f, "offset should shift hue");
    }

    @Test
    void speedIsClamped() {
        ColorSetting setting = new ColorSetting("Test", BASE);
        setting.setSpeed(-5f);
        assertEquals(0.1f, setting.getSpeed(), 0.001f);
        setting.setSpeed(99f);
        assertEquals(10f, setting.getSpeed(), 0.001f);
        setting.setSpeed(2.5f);
        assertEquals(2.5f, setting.getSpeed(), 0.001f);
    }
}
