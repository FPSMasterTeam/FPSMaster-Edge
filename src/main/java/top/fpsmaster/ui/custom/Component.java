package top.fpsmaster.ui.custom;

import top.fpsmaster.utils.render.draw.Rects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.modules.logger.ClientLogger;

import java.awt.*;

public class Component {
    private static final Color STENCIL_MASK_COLOR = new Color(255, 255, 255, 255);
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 4.5f;

    public InterfaceModule mod;

    public float x = 0f;

    public float y = 0f;

    public float width = 0f;

    public float height = 0f;

    public float scale = 1f;

    public boolean allowScale = false;

    /** Consecutive render failures; reset on any successful frame. See ComponentsManager. */
    public int renderFailures = 0;

    public Position position = Position.LT;

    @SuppressWarnings("unchecked")
    public Component(Class<?> clazz) {
        Module module;
        try {
            module = FPSMaster.moduleManager.getModule((Class<? extends Module>) clazz);
        } catch (IllegalStateException exception) {
            ClientLogger.warn("Missing interface module for component: " + clazz.getName());
            this.mod = new InterfaceModule(clazz.getSimpleName(), Category.Interface);
            this.mod.set(false);
            return;
        }
        if (module instanceof InterfaceModule) {
            this.mod = (InterfaceModule) module;
            return;
        }

        ClientLogger.warn("Missing interface module for component: " + clazz.getName());
        this.mod = new InterfaceModule(clazz.getSimpleName(), Category.Interface);
        this.mod.set(false);
    }

    public void draw(float x, float y) {
    }

    /**
     * Computes {@link #width}/{@link #height} for this frame, before anything reads them.
     *
     * <p>Historically every component assigned its size inside {@code draw()}, but anchoring
     * ({@link #getRealPosition}), hover testing, drag clamping and the blur mask all run <em>before</em>
     * {@code draw()} — so they used the previous frame's size. A component whose size changes each
     * frame (FPS going {@code 99fps} → {@code 100fps}, a potion expiring) visibly jitters, and on its
     * very first frame {@code width} is still 0.
     *
     * <p>Default implementation does nothing, so components that have not been migrated keep their old
     * behaviour. Override it, move the sizing math here, and {@code draw()} becomes pure rendering.
     */
    public void measure() {
    }

    /** Computes editor geometry using representative data when live data is absent. */
    public void measurePreview() {
        measure();
    }

    /** Draws the editor representation without mutating player or world state. */
    public void drawPreview(float x, float y) {
        draw(x, y);
    }

    /** Receives the rectangles that make up a component's background. */
    public interface ShapeSink {
        /**
         * @param x absolute left edge, already scaled by the caller (same convention as
         *          {@link #drawRect}: positions are the caller's job, sizes are the base class's)
         * @param y absolute top edge
         * @param width  logical width; the base class multiplies by {@link #scale}
         * @param height logical height
         */
        void rect(float x, float y, float width, float height);
    }

    /**
     * Declares the geometry of this component's background so the blur mask can reproduce it exactly
     * instead of guessing.
     *
     * <p>The mask used to assume every component draws one rectangle at {@code (rX - 2, rY)} sized
     * {@code width × height}. That holds for about two thirds of them; the rest either use a different
     * origin (Keystrokes, PotionDisplay) or draw several disjoint boxes (ArmorDisplay), so the blur
     * leaked into the gaps or sat a couple of pixels off.
     *
     * <p>Whatever a component declares here must match what it actually paints in {@code draw()}.
     */
    public void backgroundShape(ShapeSink sink, float originX, float originY) {
        sink.rect(originX - 2f, originY, width, height);
    }

    public float alpha = 0f;

    public boolean shouldDisplay() {
        return mod.isEnabled();
    }

    /**
     * Whether this component currently has geometry another component may align to.
     *
     * <p>Some enabled HUD modules are conditional: a target HUD has no box without a target, for
     * example. Those components override this rather than letting their last non-zero width/height
     * become an invisible snap target.
     */
    public boolean isVisibleForAlignment() {
        return shouldDisplay() && width > 0f && height > 0f;
    }

    public float[] getRealPosition() {
        return getRealPosition(new ScaledResolution(Minecraft.getMinecraft()));
    }

    public float[] getRealPosition(ScaledResolution sr) {
        float rX = 0f;
        float rY = 0f;
        x = Math.max(0f, Math.min(1f, x));
        y = Math.max(0f, Math.min(1f, y));

        // The space these coordinates live in is set by ComponentsManager, which converts the mouse
        // by sr.getScaleFactor() and has GuiScale scale the matrix by 2 / sr.getScaleFactor(). This
        // has to read the same factor. Reading the client's own UI scale instead - which is 1 unless
        // the interface is set to follow the game's - made the usable area half as wide and half as
        // tall, so a component could only be dragged around the top-left quarter of the screen.
        float scaleFactor = sr.getScaleFactor();
        float guiWidth = sr.getScaledWidth() / 2f * scaleFactor;
        float guiHeight = sr.getScaledHeight() / 2f * scaleFactor;

        // Anchors offset by the component's *rendered* size. width/height are logical units; everything
        // that touches the drawn box (drawRect, hover, drag clamping, blur mask) multiplies by scale,
        // so these must too — otherwise a scaled-up right-anchored component overflows the screen edge
        // by width * (scale - 1).
        float scaledWidth = width * scale;
        float scaledHeight = height * scale;

        switch (position) {
            case LT:
                rX = x * guiWidth / 2f;
                rY = y * guiHeight / 2f;
                break;
            case RT:
                rX = guiWidth - (x * guiWidth / 2f + scaledWidth);
                rY = y * guiHeight / 2f;
                break;
            case LB:
                rX = x * guiWidth / 2f;
                rY = guiHeight - (y * guiHeight / 2f + scaledHeight);
                break;
            case RB:
                rX = guiWidth - (x * guiWidth / 2f + scaledWidth);
                rY = guiHeight - (y * guiHeight / 2f + scaledHeight);
                break;
            case CT:
                rX = guiWidth / 2f - scaledWidth / 2f;
                rY = y * guiHeight / 2f;
                break;
        }
        return new float[]{rX, rY};
    }

    /** Converts a shared editor placement back into Edge's anchored config model. */
    public void setRealPosition(float realX, float realY, float guiWidth, float guiHeight, float newScale) {
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));
        float scaledWidth = width * scale;
        float scaledHeight = height * scale;
        boolean right = realX + scaledWidth / 2f > guiWidth / 2f;
        boolean bottom = realY + scaledHeight / 2f > guiHeight / 2f;
        position = right ? (bottom ? Position.RB : Position.RT) : (bottom ? Position.LB : Position.LT);
        x = right ? 2f * (guiWidth - realX - scaledWidth) / guiWidth : 2f * realX / guiWidth;
        y = bottom ? 2f * (guiHeight - realY - scaledHeight) / guiHeight : 2f * realY / guiHeight;
        x = Math.max(0f, Math.min(1f, x));
        y = Math.max(0f, Math.min(1f, y));
    }

    public void drawBlurMask(ScaledResolution sr) {
        if (!hasBackground() || width <= 0f || height <= 0f) {
            return;
        }
        float[] pos = getRealPosition(sr);
        boolean round = mod.rounded.getValue();
        int radius = mod.roundRadius.getValue().intValue();
        // Same geometry the component paints, so the mask can never drift from the background.
        backgroundShape((rectX, rectY, rectWidth, rectHeight) -> {
            float scaledWidth = rectWidth * scale;
            float scaledHeight = rectHeight * scale;
            if (scaledWidth <= 0f || scaledHeight <= 0f) {
                return;
            }
            if (round) {
                Rects.roundedImage(Math.round(rectX), Math.round(rectY), Math.round(scaledWidth), Math.round(scaledHeight), radius, STENCIL_MASK_COLOR);
            } else {
                Rects.fill(rectX, rectY, scaledWidth, scaledHeight, STENCIL_MASK_COLOR);
            }
        }, pos[0], pos[1]);
    }

    public void display(ScaledResolution sr, int mouseX, int mouseY) {
        float[] pos = getRealPosition(sr);
        draw(pos[0], pos[1]);
    }

    /**
     * A module only has a background if its traits say so. Checking {@code bg} alone is not enough:
     * the field exists on every InterfaceModule and defaults to {@code true}, so a text-only module
     * such as Sprint would otherwise have a blur mask stamped for a panel it never draws.
     */
    public boolean hasBackground() {
        return mod.has(InterfaceModule.Trait.BACKGROUND) && mod.bg.getValue();
    }

    public void drawRect(float x, float y, float width, float height, Color color) {
        float scaledWidth = width * scale;
        float scaledHeight = height * scale;

        if (hasBackground()) {
            if (mod.rounded.getValue()) {
                Rects.roundedImage(Math.round(x), Math.round(y), Math.round(scaledWidth), Math.round(scaledHeight), mod.roundRadius.getValue().intValue(), color);
            } else {
                Rects.fill(x, y, scaledWidth, scaledHeight, color);
            }
        }
    }

    public void drawString(int fontSize, String text, float x, float y, int color) {
        drawString(fontSize, false, text, x, y, color);
    }

    /**
     * Draws a component's text.
     *
     * <p>Colours here come from {@link top.fpsmaster.features.settings.impl.ColorSetting}, whose
     * alpha slider reaches zero and whose Wave mode scales what it returns, so alpha means what it
     * says: below four there is nothing to draw. Both renderers underneath read it vanilla's way
     * instead — no alpha bits set means opaque — and would turn a string the user hid into a solid
     * one. Components must therefore pass a real alpha, not a bare {@code 0xRRGGBB}.
     *
     * <p>Only text is affected. Backgrounds and shapes go through {@code drawRect} and {@code Rects}
     * and never come through here, so a fully transparent background stays transparent.
     */
    public void drawString(int fontSize, boolean bold, String text, float x, float y, int color) {
        if (((color >>> 24) & 0xFF) <= 3) {
            return;
        }
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
        return mod.betterFont.getValue() ? font.getStringWidth(name) : (Minecraft.getMinecraft().fontRendererObj.getStringWidth(name));
    }

    public float getStringHeight(int fontSize) {
        UFontRenderer font = FPSMaster.fontManager.getFont(fontSize);
        return mod.betterFont.getValue() ? font.getHeight() : (Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT);
    }

}
