package top.fpsmaster.ui.click.modules.impl;

import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.ui.common.binding.SettingBinding;
import top.fpsmaster.utils.math.anim.AnimMath;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.util.Locale;

public class ModeSettingRender extends SettingRender<ModeSetting> {
    private static final int SEGMENT_LIMIT = 3;

    private boolean expand = false;
    private float expandH = 0f;
    private final SettingBinding<Integer> binding;

    public ModeSettingRender(Module mod, ModeSetting setting) {
        super(setting);
        this.mod = mod;
        this.binding = new SettingBinding<>(setting);
    }

    @Override
    public boolean isWide() {
        return true;
    }

    @Override
    public void render(ScaledGuiScreen screen, float x, float y, float width, float height, float mouseX, float mouseY, boolean custom) {
        String label = FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault()));
        FPSMaster.fontManager.getFont(13).drawString(label, x + 5, y + 6, ClickGuiTheme.textPrimary().getRGB());

        if (setting.getModesSize() <= SEGMENT_LIMIT) {
            renderSegmented(screen, x, y, width, mouseX, mouseY);
            this.height = 19f;
            return;
        }

        float labelW = FPSMaster.fontManager.getFont(13).getStringWidth(label);
        String current = modeLabel(setting.getModeName());
        float chipW = Math.max(52f, FPSMaster.fontManager.getFont(12).getStringWidth(current) + 18f);
        float chipX = x + width - 5 - chipW;
        float chipY = y + 1.5f;
        float chipH = 16f;
        if (chipX < x + 10 + labelW) {
            chipX = x + 5;
            chipW = width - 10;
        }

        boolean hover = Hover.is(chipX, chipY, chipW, chipH, (int) mouseX, (int) mouseY);
        Rects.rounded(
                chipX - 0.5f, chipY - 0.5f, chipW + 1f, chipH + expandH + 1f,
                UiChrome.CTL_RADIUS + 1,
                (hover || expand ? ClickGuiTheme.strokeStrong() : ClickGuiTheme.stroke()).getRGB(), false
        );
        Rects.rounded(
                chipX, chipY, chipW, chipH + expandH,
                UiChrome.CTL_RADIUS,
                (hover || expand ? ClickGuiTheme.layerHover() : ClickGuiTheme.layer()).getRGB(), false
        );
        FPSMaster.fontManager.getFont(12).drawString(current, chipX + 6, chipY + 5, ClickGuiTheme.textPrimary().getRGB());

        Icons.draw(expand ? "chev-d" : "chev-r", chipX + chipW - 10.5f, chipY + 5.25f, 5.5f,
                ClickGuiTheme.textDisabled().getRGB());

        float target = expand ? setting.getModesSize() * 10f + 2f : 0f;
        expandH = (float) AnimMath.base(expandH, target, 0.2);
        if (expandH > 1f) {
            for (int i = 0; i < setting.getModesSize(); i++) {
                float itemY = chipY + chipH + i * 10f;
                String item = modeLabel(setting.getMode(i + 1));
                boolean itemHover = Hover.is(chipX, itemY, chipW, 10f, (int) mouseX, (int) mouseY);
                int color = setting.getMode() == i
                        ? ClickGuiTheme.accentText().getRGB()
                        : (itemHover ? ClickGuiTheme.textPrimary().getRGB() : ClickGuiTheme.textSecondary().getRGB());
                FPSMaster.fontManager.getFont(12).drawString(item, chipX + 6, itemY + 2.5f, color);
            }
        }

        if (screen.consumePressInBounds(chipX, chipY, chipW, chipH) != null) {
            expand = !expand;
        } else if (expand) {
            for (int i = 0; i < setting.getModesSize(); i++) {
                if (screen.consumePressInBounds(chipX, chipY + chipH + i * 10f, chipW, 10f) != null) {
                    binding.set(i);
                    expand = false;
                    break;
                }
            }
        }
        this.height = 19f + expandH;
    }

    private void renderSegmented(ScaledGuiScreen screen, float x, float y, float width, float mouseX, float mouseY) {
        int n = setting.getModesSize();
        float pad = 1.5f;
        float segH = 13f + pad * 2f;
        float minOpt = 24f;
        float segW = Math.min(width * 0.62f, Math.max(n * minOpt + pad * 2f, 60f));
        float segX = x + width - 5 - segW;
        float segY = y + (19f - segH) / 2f;
        UiChrome.seg(segX, segY, segW, segH);
        float optW = (segW - pad * 2f) / n;
        for (int i = 0; i < n; i++) {
            float ox = segX + pad + i * optW;
            boolean selected = setting.getMode() == i;
            boolean hover = Hover.is(ox, segY + pad, optW, segH - pad * 2f, (int) mouseX, (int) mouseY);
            UiChrome.segOption(ox, segY + pad, optW, segH - pad * 2f,
                    modeLabel(setting.getMode(i + 1)), selected, hover);
            if (screen.consumePressInBounds(ox, segY, optW, segH) != null) {
                binding.set(i);
            }
        }
    }

    private String modeLabel(String mode) {
        return FPSMaster.i18n.get((mod.name + "." + setting.name + "." + mode).toLowerCase(Locale.getDefault()));
    }
}
