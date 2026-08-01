package top.fpsmaster.ui.click.modules;

import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Colors;
import top.fpsmaster.utils.render.draw.Rects;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.*;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.MainPanel;
import top.fpsmaster.ui.click.modules.impl.*;
import top.fpsmaster.utils.math.anim.AnimMath;
import top.fpsmaster.utils.math.anim.ColorAnimator;
import top.fpsmaster.utils.math.anim.Easings;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.*;
import java.util.ArrayList;
import java.util.Locale;

public class ModuleRenderer extends ValueRender {

    private static final ResourceLocation INTERFACE_ICON =
            new ResourceLocation("client/textures/modules/interface.png");

    /**
     * Icon per module name, or null where the client ships no icon for it.
     *
     * <p>The path is derived from the module's name, so a module added without an icon
     * asked the texture manager for a file that is not there — which logs a
     * {@code FileNotFoundException} and then draws the missing-texture chequer in the
     * module list. Twenty-six of the sixty modules are in that state.
     *
     * <p>Resolved once per name rather than per frame: the lookup goes to the resource
     * manager, and this runs for every visible row of an open GUI. Cleared on a resource
     * reload, because a resource pack is allowed to supply an icon this build does not
     * have.
     */
    private static final Map<String, ResourceLocation> ICONS = new HashMap<>();

    private static ResourceLocation iconFor(String moduleName) {
        String key = moduleName.toLowerCase(Locale.getDefault());
        if (ICONS.containsKey(key)) {
            return ICONS.get(key);
        }
        ResourceLocation candidate = new ResourceLocation("client/textures/modules/" + key + ".png");
        ResourceLocation resolved = null;
        try {
            Minecraft.getMinecraft().getResourceManager().getResource(candidate);
            resolved = candidate;
        } catch (IOException missing) {
            ClientLogger.info("no icon for module " + key + ", drawing the row without one");
        }
        ICONS.put(key, resolved);
        return resolved;
    }

    /** Lets a resource pack's icons appear without a restart. */
    public static void clearIconCache() {
        ICONS.clear();
    }
    ArrayList<SettingRender<?>> settingsRenderers = new ArrayList<>();
    private float settingHeight = 0f;
    private float border = 0f;
    private boolean expand = false;
    public ColorAnimator content;
    ColorAnimator background = new ColorAnimator();
    ColorAnimator option = new ColorAnimator();
    float optionX = 0;

    public ModuleRenderer(Module module) {
        this.mod = module;
        content = new ColorAnimator(module.isEnabled() ? ClickGuiTheme.moduleContentEnabled() : ClickGuiTheme.moduleContentDisabled());
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
    public void render(ScaledGuiScreen screen, float x, float y, float width, float height, float mouseX, float mouseY, boolean current) {
        content.update();
        background.update();
        border = Hover.is(x + 5, y, width - 10, height, (int) mouseX, (int) mouseY)
                ? (float) AnimMath.base(border, 200.0, 0.3)
                : (float) AnimMath.base(border, 30.0, 0.3);
        option.update();

        if (mod.isEnabled()) {
            content.animateTo(ClickGuiTheme.moduleContentEnabled(), 0.2f, Easings.QUAD_IN_OUT);
            option.animateTo(ClickGuiTheme.toggleEnabled(), 0.2f, Easings.QUAD_IN_OUT);
            optionX = (float) AnimMath.base(optionX, 10, 0.2f);
        } else {
            content.animateTo(ClickGuiTheme.moduleContentDisabled(), 0.2f, Easings.QUAD_IN_OUT);
            option.animateTo(ClickGuiTheme.toggleDisabled(), 0.2f, Easings.QUAD_IN_OUT);
            optionX = (float) AnimMath.base(optionX, 0, 0.2f);
        }

        Images.draw(
                new ResourceLocation("client/gui/settings/window/module.png"),
                x + 5,
                y,
                width - 10,
                40,
                -1
        );
        GlStateManager.disableBlend();

        Rects.rounded(
                Math.round(x + 5),
                Math.round(y + 40),
                Math.round(width - 10),
                Math.round(settingHeight),
                10,
                ClickGuiTheme.settingsBg().getRGB()
        );

//        Rects.roundedBorder(
//                x + 5, y, width - 10, 37f, 0.5f, background.getColor(), Colors.alpha(
//                        FPSMaster.theme.getModuleBorder(), (int) border)
//        );

        Images.draw(
                new ResourceLocation("client/gui/settings/window/option.png"),
                x + width - 40,
                y + 16,
                21,
                10,
                option.getColor()
        );

        Images.draw(
                new ResourceLocation("client/gui/settings/window/option_circle.png"),
                x + width - 38 + optionX,
                y + 17.5f,
                7,
                7,
                -1
        );


        ResourceLocation icon = mod.category == Category.Interface
                ? INTERFACE_ICON
                : iconFor(mod.name);
        if (icon != null) {
            Images.draw(icon, x + 14, y + 10, 14f, 14f, content.getColor().getRGB());
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
                ClickGuiTheme.textDescription().getRGB()
        );

        float settingsHeight = 0f;
        if (expand) {
            for (SettingRender<?> settingsRenderer : settingsRenderers) {
                if (settingsRenderer.setting.getVisible()) {
                    settingsRenderer.render(
                            screen,
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

        settingHeight = (float) AnimMath.base(settingHeight, settingsHeight, 0.2);
        this.height = settingHeight;

        ScaledGuiScreen.PointerEvent click = screen.consumePressInBounds(x + 5, y, width - 10, 40f);
        if (click != null) {
            if (click.button == 0) {
                mod.toggle();
            } else if (click.button == 1) {
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




