package top.fpsmaster.ui.click.modules;

import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.Setting;
import top.fpsmaster.features.settings.impl.*;
import top.fpsmaster.modules.lua.LuaModule;
import top.fpsmaster.ui.click.MainPanel;
import top.fpsmaster.ui.click.modules.impl.*;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.math.animation.ColorAnimation;
import top.fpsmaster.utils.math.animation.Type;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Consumer;

public class ModuleRenderer extends ValueRender {
    ArrayList<SettingRender<?>> settingsRenderers = new ArrayList<>();
    private float settingHeight = 0f;
    private float border = 0f;
    private boolean expand = false;
    public ColorAnimation content;
    ColorAnimation background = new ColorAnimation();

    public ModuleRenderer(Module mod) {
        this.mod = mod;
        content = new ColorAnimation(mod.isEnabled() ? FPSMaster.theme.getModuleEnabled() : FPSMaster.theme.getModuleDisabled());
        mod.settings.forEach(new Consumer<Setting<?>>() {
            @Override
            public void accept(Setting<?> setting) {
                if (setting instanceof BooleanSetting) {
                    settingsRenderers.add(new BooleanSettingRender(mod, (BooleanSetting) setting));
                } else if (setting instanceof ModeSetting) {
                    settingsRenderers.add(new ModeSettingRender(mod, (ModeSetting) setting));
                } else if (setting instanceof TextSetting) {
                    settingsRenderers.add(new TextSettingRender(mod, (TextSetting) setting));
                } else if (setting instanceof NumberSetting) {
                    settingsRenderers.add(new NumberSettingRender(mod, (NumberSetting) setting));
                } else if (setting instanceof ColorSetting) {
                    settingsRenderers.add(new ColorSettingRender(mod, (ColorSetting) setting));
                } else if (setting instanceof BindSetting) {
                    settingsRenderers.add(new BindSettingRender(mod, (BindSetting) setting));
                }
            }
        });
    }

    @Override
    public void render(float x, float y, float width, float height, float mouseX, float mouseY, boolean current) {
        content.update();
        background.update();
        border = Render2DUtils.isHovered(x + 5, y, width - 10, height, (int) mouseX, (int) mouseY)
                ? (float) AnimationUtils.base(border, 200.0, 0.3)
                : (float) AnimationUtils.base(border, 30.0, 0.3);

        if (mod.isEnabled()) {
            content.start(content.getColor(), FPSMaster.theme.getModuleTextEnabled(), 0.2f, Type.EASE_IN_OUT_QUAD);
            background.start(background.getColor(), new Color(150,150,150,60), 0.2f, Type.EASE_IN_OUT_QUAD);
        } else {
            content.start(content.getColor(), FPSMaster.theme.getModuleTextDisabled(), 0.2f, Type.EASE_IN_OUT_QUAD);
            background.start(background.getColor(), new Color(100,100,100,60), 0.2f, Type.EASE_IN_OUT_QUAD);
        }

        Render2DUtils.drawOptimizedRoundedRect(
                x + 5,
                y,
                width - 10,
                settingHeight + 37f,
                10,
                new Color(100,100,100,60).getRGB()
        );

        Render2DUtils.drawOptimizedRoundedRect(
                x + 5,
                y,
                width - 10,
                37f,
                10,
                background.getColor().getRGB()
        );

//        Render2DUtils.drawOptimizedRoundedBorderRect(
//                x + 5, y, width - 10, 37f, 0.5f, background.getColor(), Render2DUtils.reAlpha(
//                        FPSMaster.theme.getModuleBorder(), (int) border)
//        );

        if (mod.category == Category.Interface) {
            Render2DUtils.drawImage(
                    new ResourceLocation("client/textures/modules/interface.png"),
                    x + 14,
                    y + 10,
                    14f,
                    14f,
                    content.getColor().getRGB()
            );
        } else if (mod instanceof LuaModule) {
            Render2DUtils.drawImage(
                    new ResourceLocation("client/textures/modules/lua.png"),
                    x + 14,
                    y + 10,
                    14f,
                    14f,
                    content.getColor().getRGB()
            );
        } else {
            Render2DUtils.drawImage(
                    new ResourceLocation("client/textures/modules/" + mod.name.toLowerCase(Locale.getDefault()) + ".png"),
                    x + 14,
                    y + 10,
                    14f,
                    14f,
                    content.getColor().getRGB()
            );
        }

        FPSMaster.fontManager.s18.drawString(
                FPSMaster.i18n.get(mod.name.toLowerCase(Locale.getDefault())),
                x + 40,
                y + 9,
                content.getColor().getRGB()
        );
        FPSMaster.fontManager.s16.drawString(
                FPSMaster.i18n.get(mod.name.toLowerCase(Locale.getDefault()) + ".desc"),
                x + 40,
                y + 20,
                FPSMaster.theme.getTextColorDescription().getRGB()
        );

        float settingsHeight = 0f;
        if (expand) {
            for (SettingRender<?> settingsRenderer : settingsRenderers) {
                if (settingsRenderer.setting.getVisible()) {
                    settingsRenderer.render(
                            x + 5,
                            y + 40 + settingsHeight,
                            width - 10,
                            12f,
                            mouseX,
                            mouseY,
                            MainPanel.curModule == mod
                    );
                    settingsHeight += settingsRenderer.height + 6;
                }
            }
        }

        settingHeight = (float) AnimationUtils.base(settingHeight, settingsHeight, 0.2);
        this.height = settingHeight;
    }

    @Override
    public void mouseClick(float x, float y, float width, float height, float mouseX, float mouseY, int btn) {
        float settingsHeight = 0f;
        if (expand) {
            for (SettingRender<?> settingsRenderer : settingsRenderers) {
                if (settingsRenderer.setting.getVisible()) {
                    settingsRenderer.mouseClick(x + 5, y + 40 + settingsHeight, width - 10, 12f, mouseX, mouseY, btn);
                    settingsHeight += settingsRenderer.height + 6;
                }
            }
        }

        if (Render2DUtils.isHovered(x + 5, y, width - 10, 40f, (int) mouseX, (int) mouseY)) {
            if (btn == 0) {
                mod.toggle();
            } else if (btn == 1) {
                expand = !expand;
                MainPanel.curModule = null;
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (expand) {
            for (SettingRender<?> settingsRenderer : settingsRenderers) {
                settingsRenderer.keyTyped(typedChar, keyCode);
            }
        }
    }
}
