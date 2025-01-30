package top.fpsmaster.ui.click.modules.impl;

import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.render.Render2DUtils;

import java.util.Locale;

public class ModeSettingRender extends SettingRender<ModeSetting> {
    private boolean expand = false;
    private float expandH = 0f;

    public ModeSettingRender(Module mod, ModeSetting setting) {
        super(setting);
        this.mod = mod;
    }

    @Override
    public void render(float x, float y, float width, float height, float mouseX, float mouseY, boolean custom) {
        float fw = FPSMaster.fontManager.s16.drawString(
            FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault())),
            x + 10, y + 8, FPSMaster.theme.getTextColorDescription().getRGB()
        );
        float maxWidth = 80f;
        Render2DUtils.drawOptimizedRoundedBorderRect(
            x + 16 + fw,
            y + 4,
            maxWidth,
            16 + expandH,
            0.5f,
            FPSMaster.theme.getModeBox(),
            FPSMaster.theme.getModeBoxBorder()
        );
        FPSMaster.fontManager.s18.drawString(
            FPSMaster.i18n.get((mod.name + "." + setting.name + "." + setting.getModeName()).toLowerCase(Locale.getDefault())),
            x + 20 + fw, y + 7, FPSMaster.theme.getTextColorTitle().getRGB()
        );

        // Rotate this icon
        GL11.glPushMatrix();
        float rotatePercent = expandH / (setting.getModesSize() * 14);
        GL11.glTranslatef(x + 16 + fw + maxWidth - 12, y + 12, 0f);
        GL11.glRotatef(rotatePercent * 180, 0f, 0f, 1f);
        GL11.glTranslatef(-(x + 16 + fw + maxWidth - 12), -(y + 12), 0f);
        Render2DUtils.drawImage(
            new ResourceLocation("client/gui/settings/icons/arrow.png"),
            x + 16 + fw + maxWidth - 16,
            y + 8,
            8f,
            8f,
            FPSMaster.theme.getTextColorTitle()
        );
        GL11.glPopMatrix();
        if (expand) {
            expandH = (float) AnimationUtils.base(expandH, setting.getModesSize() * 14, 0.2);
            for (int i = 1; i <= setting.getModesSize(); i++) {
                if (Render2DUtils.isHovered(x + 20 + fw, y + 4 + i * 14, maxWidth, 16f, (int) mouseX, (int) mouseY)) {
                    FPSMaster.fontManager.s16.drawString(
                        FPSMaster.i18n.get((mod.name + "." + setting.name + "." + setting.getMode(i)).toLowerCase(Locale.getDefault())),
                        x + 20 + fw, y + 7 + i * 14, FPSMaster.theme.getTextColorDescriptionHover().getRGB()
                    );
                } else {
                    FPSMaster.fontManager.s16.drawString(
                        FPSMaster.i18n.get((mod.name + "." + setting.name + "." + setting.getMode(i)).toLowerCase(Locale.getDefault())),
                        x + 20 + fw, y + 7 + i * 14, FPSMaster.theme.getTextColorDescription().getRGB()
                    );
                }
            }
        } else {
            expandH = (float) AnimationUtils.base(expandH, 0.0, 0.2);
        }
        this.height = 24 + expandH;
    }

    @Override
    public void mouseClick(float x, float y, float width, float height, float mouseX, float mouseY, int btn) {
        float fw = FPSMaster.fontManager.s16.drawString(
            FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault())),
            x + 10, y + 8, FPSMaster.theme.getTextColorDescription().getRGB()
        );
        float maxWidth = 80f;
        if (Render2DUtils.isHovered(x + 16 + fw, y + 4, maxWidth, 16f, (int) mouseX, (int) mouseY)) {
            expand = !expand;
        }
        if (expand) {
            for (int i = 1; i <= setting.getModesSize(); i++) {
                if (Render2DUtils.isHovered(x + 20 + fw, y + 7 + i * 14, maxWidth, 16f, (int) mouseX, (int) mouseY)) {
                    setting.setValue(i - 1);
                    expand = false;
                }
            }
        }
    }
}
