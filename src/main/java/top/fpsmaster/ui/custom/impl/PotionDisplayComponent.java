package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.interfaces.PotionDisplay;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.utils.core.Utility;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.Easings;
import top.fpsmaster.utils.render.draw.Rects;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static top.fpsmaster.utils.core.Utility.mc;

public class PotionDisplayComponent extends Component {

    private static final int EXIT_TICKS = 8;
    private static final float ENTER_SECONDS = 0.20f;
    private static final float EXIT_SECONDS = 0.12f;
    private final AnimClock animClock = new AnimClock();
    private final Map<String, Float> effectAnimations = new HashMap<>();

    public PotionDisplayComponent() {
        super(PotionDisplay.class);
        allowScale = true;
    }

    public static final float POTION_HEIGHT = 36f;

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        double dt = animClock.tick();
        float dY = y - mod.spacing.getValue().intValue();
        GlStateManager.pushMatrix();
        int index = 0;
        Set<String> activeEffects = new HashSet<>();
        for (PotionEffect effect : mc.thePlayer.getActivePotionEffects()) {
            String title = I18n.format(effect.getEffectName()) + " lv." + (effect.getAmplifier() + 1);
            String duration = formatDuration(effect.getDuration());
            float width = Math.max(getStringWidth(18, title), getStringWidth(16, duration)) + 36;
            float rowWidth = width + 10;
            float visible = getVisibleProgress(effect, dt);
            activeEffects.add(getEffectKey(effect));

            if (PotionDisplay.betterAnimation.getValue()) {
                drawAnimatedPotion(effect, title, duration, x, dY, width, rowWidth, visible);
            } else {
                drawPotion(effect, title, duration, x, dY, width);
            }

            dY += (index * mod.spacing.getValue().intValue() * 2 + POTION_HEIGHT) * scale;
            this.width = width + 12 * scale;
            index++;
        }
        effectAnimations.keySet().removeIf(key -> !activeEffects.contains(key));

        GlStateManager.popMatrix();
        height = index * (mod.spacing.getValue().intValue() + POTION_HEIGHT);
    }

    private void drawAnimatedPotion(PotionEffect effect, String title, String duration, float x, float y, float width, float rowWidth, float visible) {
        float scaledWidth = rowWidth * scale;
        float scaledHeight = 32f * scale;
        if (visible <= 0.01f) {
            return;
        }
        beginScissor(x, y, scaledWidth * visible, scaledHeight);
        GlStateManager.pushMatrix();
        GlStateManager.translate((1f - visible) * -6f * scale, 0f, 0f);
        drawPotion(effect, title, duration, x, y, width);
        drawAccent(effect, x, y);
        GlStateManager.popMatrix();
        endScissor();
    }

    private void drawPotion(PotionEffect effect, String title, String duration, float x, float y, float width) {
        drawRect(x, y, width + 10, 32f, mod.backgroundColor.getColor());
        drawString(18, title, x + 34 * scale, y + 5, -1);
        drawString(16, duration, x + 34 * scale, y + 5 + 13 * scale, getDurationColor(effect));

        GL11.glColor4f(1f, 1f, 1f, 1f);
        ResourceLocation res = new ResourceLocation("textures/gui/container/inventory.png");
        Utility.mc.getTextureManager().bindTexture(res);

        int potion = getPotionIconIndex(effect);

        GL11.glTranslatef((int) (x + 8), (int) (y + 8), 0);
        GL11.glScalef(scale, scale, 0);
        Gui.drawModalRectWithCustomSizedTexture(
                0,
                0,
                (potion % 8 * 18) + 1,
                (198 + (float)(potion / 8) * 18) + 1,
                16,
                16,
                256f,
                256f
        );
        GL11.glScalef(1 / scale, 1 / scale, 0);
        GL11.glTranslatef(-(int) (x + 8), -(int) (y + 8), 0);
    }

    private void drawAccent(PotionEffect effect, float x, float y) {
        Potion potion = Potion.potionTypes[effect.getPotionID()];
        if (potion == null || !mod.bg.getValue()) {
            return;
        }
        Color potionColor = new Color(potion.getLiquidColor());
        Rects.fill(x, y, Math.max(1.5f, 2f * scale), 32f * scale, new Color(potionColor.getRed(), potionColor.getGreen(), potionColor.getBlue(), 150));
    }

    private float getVisibleProgress(PotionEffect effect, double dt) {
        String key = getEffectKey(effect);
        boolean exiting = effect.getDuration() <= EXIT_TICKS;
        float current = effectAnimations.getOrDefault(key, exiting ? 0f : 1f);
        if (exiting) {
            current = Math.max(0f, current - (float) (dt / EXIT_SECONDS));
        } else {
            current = Math.min(1f, current + (float) (dt / ENTER_SECONDS));
        }
        effectAnimations.put(key, current);

        return (float) (exiting ? Easings.CUBIC_IN.ease(current) : Easings.CUBIC_OUT.ease(current));
    }

    private int getDurationColor(PotionEffect effect) {
        if (PotionDisplay.noticeableReminder.getValue()) {
            int seconds = effect.getDuration() / 20;
            if (seconds <= PotionDisplay.reminderTime.getValue().intValue()) {
                return new Color(255, 85, 85).getRGB();
            }
        }
        return new Color(200, 200, 200).getRGB();
    }

    private String formatDuration(int ticks) {
        int seconds = Math.max(0, ticks / 20);
        if (seconds < 60) {
            return seconds + "s";
        }
        return seconds / 60 + "min" + seconds % 60 + "s";
    }

    private String getEffectKey(PotionEffect effect) {
        return effect.getPotionID() + ":" + effect.getAmplifier();
    }

    private void beginScissor(float x, float y, float width, float height) {
        int sx = Math.round(x * 2f);
        int sy = Math.round(y * 2f);
        int sw = Math.max(0, Math.round(width * 2f));
        int sh = Math.max(0, Math.round(height * 2f));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, mc.displayHeight - (sy + sh), sw, sh);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private int getPotionIconIndex(PotionEffect effect) {
        Potion p = Potion.potionTypes[effect.getPotionID()];
        return p.getStatusIconIndex();
    }
}



