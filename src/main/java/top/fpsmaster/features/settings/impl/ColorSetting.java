package top.fpsmaster.features.settings.impl;

import top.fpsmaster.features.settings.Setting;
import top.fpsmaster.features.settings.impl.utils.CustomColor;

import java.awt.*;
import java.util.Arrays;

public class ColorSetting extends Setting<CustomColor> {

    public enum ColorType {
        STATIC("colorsetting.type.static"),
        WAVE("colorsetting.type.breath"),
        CHROMA("colorsetting.type.chroma"),
        RAINBOW("colorsetting.type.rainbow");

        public final String i18nKey;

        ColorType(String i18nKey) {
            this.i18nKey = i18nKey;
        }
    }

    private ColorType colorType = ColorType.STATIC;
    private final ColorType[] availableTypes;
    private Color latestResolvedColor;

    public ColorSetting(String name, CustomColor value, VisibleCondition visible) {
        super(name, value, visible);
        this.availableTypes = new ColorType[] {ColorType.STATIC};
    }

    public ColorSetting(String name, Color value, VisibleCondition visible) {
        super(name, new CustomColor(value), visible);
        this.availableTypes = new ColorType[] {ColorType.STATIC};
    }

    public ColorSetting(String name, CustomColor value) {
        super(name, value);
        this.availableTypes = new ColorType[] {ColorType.STATIC};
    }

    public ColorSetting(String name, Color value) {
        super(name, new CustomColor(value));
        this.availableTypes = new ColorType[] {ColorType.STATIC};
    }

    public ColorSetting(String name, CustomColor value, VisibleCondition visible, ColorType... availableTypes) {
        super(name, value, visible);
        this.availableTypes = normalizeTypes(availableTypes);
    }

    public ColorSetting(String name, Color value, VisibleCondition visible, ColorType... availableTypes) {
        super(name, new CustomColor(value), visible);
        this.availableTypes = normalizeTypes(availableTypes);
    }

    public ColorSetting(String name, CustomColor value, ColorType... availableTypes) {
        super(name, value);
        this.availableTypes = normalizeTypes(availableTypes);
    }

    public ColorSetting(String name, Color value, ColorType... availableTypes) {
        super(name, new CustomColor(value));
        this.availableTypes = normalizeTypes(availableTypes);
    }

    public int getRGB() {
        return updateAndGetColor().getRGB();
    }

    public int getRGB(float chromaOffset) {
        return updateAndGetColor(chromaOffset).getRGB();
    }

    public Color getColor() {
        return updateAndGetColor(0f);
    }

    public Color getColor(float chromaOffset) {
        return updateAndGetColor(chromaOffset);
    }

    public Color updateAndGetColor() {
        return updateAndGetColor(0f);
    }

    public Color updateAndGetColor(float chromaOffset) {
        latestResolvedColor = resolveColor(getValue(), colorType, chromaOffset);
        return latestResolvedColor;
    }

    public int updateAndGetRGB() {
        return updateAndGetColor().getRGB();
    }

    public int updateAndGetRGB(float chromaOffset) {
        return updateAndGetColor(chromaOffset).getRGB();
    }

    public Color getLatestResolvedColor() {
        return latestResolvedColor == null ? updateAndGetColor() : latestResolvedColor;
    }

    public static Color resolveColor(CustomColor value, ColorType type, float chromaOffset) {
        long now = System.nanoTime();
        float dynamicHue = (float) ((now / 1_000_000_000.0 / 6.0) % 1.0);
        float normalizedOffset = chromaOffset - (float) Math.floor(chromaOffset);

        switch (type) {
            case WAVE:
                float alphaWave = value.alpha * (0.35f + 0.65f * (float) ((Math.sin(now / 450_000_000.0) + 1.0) * 0.5));
                return new CustomColor(value.hue, value.brightness, value.saturation, alphaWave).getColor();
            case CHROMA:
                return new CustomColor((value.hue + dynamicHue + normalizedOffset) % 1.0f, value.brightness, value.saturation, value.alpha).getColor();
            case RAINBOW:
                return new CustomColor((dynamicHue + normalizedOffset) % 1.0f, value.brightness, value.saturation, value.alpha).getColor();
            case STATIC:
            default:
                return value.getColor();
        }
    }

    public ColorType[] getAvailableTypes() {
        return Arrays.copyOf(availableTypes, availableTypes.length);
    }

    public ColorType getColorType() {
        return colorType;
    }

    public boolean supportsType(ColorType type) {
        for (ColorType availableType : availableTypes) {
            if (availableType == type) {
                return true;
            }
        }
        return false;
    }

    public void setColorType(ColorType type) {
        if (type == null || !supportsType(type) || colorType == type) {
            return;
        }
        CustomColor current = getValue();
        CustomColor oldSnapshot = new CustomColor(current.hue, current.brightness, current.saturation, current.alpha);
        if (!fireValueChangeEvent(oldSnapshot, oldSnapshot)) {
            return;
        }
        colorType = type;
        notifyChangeListeners(oldSnapshot, oldSnapshot);
    }

    public void cycleColorType() {
        int index = 0;
        for (int i = 0; i < availableTypes.length; i++) {
            if (availableTypes[i] == colorType) {
                index = i;
                break;
            }
        }
        setColorType(availableTypes[(index + 1) % availableTypes.length]);
    }

    public void setColor(float hue, float saturation, float brightness, float alpha) {
        CustomColor v = getValue();
        CustomColor oldSnapshot = new CustomColor(v.hue, v.brightness, v.saturation, v.alpha);
        CustomColor newSnapshot = new CustomColor(hue, brightness, saturation, alpha);
        if (!fireValueChangeEvent(oldSnapshot, newSnapshot)) {
            return;
        }
        v.setColor(hue, saturation, brightness, alpha);
        notifyChangeListeners(oldSnapshot, newSnapshot);
    }

    public void setColor(Color color) {
        CustomColor v = getValue();
        CustomColor oldSnapshot = new CustomColor(v.hue, v.brightness, v.saturation, v.alpha);
        CustomColor newSnapshot = new CustomColor(color);
        if (!fireValueChangeEvent(oldSnapshot, newSnapshot)) {
            return;
        }
        v.setColor(color);
        notifyChangeListeners(oldSnapshot, newSnapshot);
    }

    private ColorType[] normalizeTypes(ColorType... modes) {
        if (modes == null || modes.length == 0) {
            return new ColorType[] {ColorType.STATIC};
        }

        ColorType[] unique = new ColorType[modes.length];
        int size = 0;
        for (ColorType mode : modes) {
            if (mode == null) {
                continue;
            }
            boolean exists = false;
            for (int i = 0; i < size; i++) {
                if (unique[i] == mode) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                unique[size++] = mode;
            }
        }

        if (size == 0) {
            return new ColorType[] {ColorType.STATIC};
        }

        ColorType[] result = Arrays.copyOf(unique, size);
        if (!supportsStatic(result)) {
            ColorType[] withStatic = new ColorType[size + 1];
            withStatic[0] = ColorType.STATIC;
            System.arraycopy(result, 0, withStatic, 1, size);
            return withStatic;
        }
        return result;
    }

    private boolean supportsStatic(ColorType[] types) {
        for (ColorType type : types) {
            if (type == ColorType.STATIC) {
                return true;
            }
        }
        return false;
    }
}



