package top.fpsmaster.ui.click.modules;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.*;
import top.fpsmaster.modules.lua.LuaModule;
import top.fpsmaster.ui.click.MainPanel;
import top.fpsmaster.ui.click.modules.impl.*;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.math.animation.ColorAnimation;
import top.fpsmaster.utils.math.animation.Type;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Locale;

public class ModuleRenderer extends ValueRender {
    ArrayList<SettingRender<?>> settingsRenderers = new ArrayList<>();
    private float settingHeight = 0f;
    private float border = 0f;
    private boolean expand = false;
    public ColorAnimation content;
    ColorAnimation background = new ColorAnimation();
    ColorAnimation option = new ColorAnimation();
    float optionX = 0;

    public ModuleRenderer(Module module) {
        this.mod = module;
        content = new ColorAnimation(module.isEnabled() ? new Color(66, 66, 66) : new Color(40, 40, 40));
        module.settings.forEach(setting -> {
            if (setting instanceof BooleanSetting) {
                settingsRenderers.add(new BooleanSettingRender(module, (BooleanSetting) setting));
            } else if (setting instanceof ModeSetting) {
                settingsRenderers.add(new ModeSettingRender(module, (ModeSetting) setting));
            } else if (setting instanceof TextSetting) {
                settingsRenderers.add(new TextSettingRender(module, (TextSetting) setting));
            } else if (setting instanceof NumberSetting) {
                settingsRenderers.add(new NumberSettingRender(module, (NumberSetting) setting));
            } else if (setting instanceof ColorSetting) {
                settingsRenderers.add(new ColorSettingRender(module, (ColorSetting) setting));
            } else if (setting instanceof BindSetting) {
                settingsRenderers.add(new BindSettingRender(module, (BindSetting) setting));
            } else if(setting instanceof MultipleItemSetting) {
                settingsRenderers.add(new MultipleItemSettingRender(module,(MultipleItemSetting)setting));
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
        option.update();

        if (mod.isEnabled()) {
            content.start(content.getColor(), new Color(255, 255, 255), 0.2f, Type.EASE_IN_OUT_QUAD);
            option.start(option.getColor(), new Color(89, 101, 241), 0.2f, Type.EASE_IN_OUT_QUAD);
            optionX = (float) AnimationUtils.base(optionX, 10, 0.2f);
        } else {
            content.start(content.getColor(), new Color(156, 156, 156), 0.2f, Type.EASE_IN_OUT_QUAD);
            option.start(option.getColor(), new Color(255, 255, 255), 0.2f, Type.EASE_IN_OUT_QUAD);
            optionX = (float) AnimationUtils.base(optionX, 0, 0.2f);
        }

        Render2DUtils.drawImage(
                new ResourceLocation("client/gui/settings/window/module.png"),
                x + 5,
                y,
                width - 10,
                40,
                -1
        );
        GlStateManager.disableBlend();

        Render2DUtils.drawOptimizedRoundedRect(
                x + 5,
                y + 40,
                width - 10,
                settingHeight,
                10,
                new Color(100, 100, 100, 60).getRGB()
        );

//        Render2DUtils.drawOptimizedRoundedBorderRect(
//                x + 5, y, width - 10, 37f, 0.5f, background.getColor(), Render2DUtils.reAlpha(
//                        FPSMaster.theme.getModuleBorder(), (int) border)
//        );

        Render2DUtils.drawImage(
                new ResourceLocation("client/gui/settings/window/option.png"),
                x + width - 40,
                y + 16,
                21,
                10,
                option.getColor()
        );

        Render2DUtils.drawImage(
                new ResourceLocation("client/gui/settings/window/option_circle.png"),
                x + width - 38 + optionX,
                y + 17.5f,
                7,
                7,
                -1
        );


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
                new Color(162, 162, 162).getRGB()
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
