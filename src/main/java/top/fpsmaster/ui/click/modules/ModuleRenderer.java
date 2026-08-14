package top.fpsmaster.ui.click.modules;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.*;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.MainPanel;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.ui.click.modules.impl.*;
import top.fpsmaster.utils.math.anim.AnimMath;
import top.fpsmaster.utils.math.anim.ColorAnimator;
import top.fpsmaster.utils.math.anim.Easings;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.util.ArrayList;
import java.util.Locale;

public class ModuleRenderer extends ValueRender {

    private static final float SETTINGS_GAP = 2f;
    private static final float COL_GAP = 10f;

    ArrayList<SettingRender<?>> settingsRenderers = new ArrayList<>();
    private float settingHeight = 0f;
    private boolean expand = false;
    public ColorAnimator content;
    private float knobT;

    public String highlight = null;
    public boolean searchMode = false;

    /** Dedicated white line icon under client/textures/modules/, or null → category icon. */
    private final net.minecraft.util.ResourceLocation moduleIcon;

    public ModuleRenderer(Module module) {
        this.mod = module;
        this.moduleIcon = resolveModuleIcon(module);
        this.knobT = module.isEnabled() ? 1f : 0f;
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
            } else if (setting instanceof MultipleItemSetting) {
                settingsRenderers.add(new MultipleItemSettingRender(module, (MultipleItemSetting) setting));
            } else if (setting instanceof AutoTextSetting) {
                settingsRenderers.add(new AutoTextSettingRender(module, (AutoTextSetting) setting));
            }
        });
    }

    @Override
    public void render(ScaledGuiScreen screen, float x, float y, float width, float height, float mouseX, float mouseY, boolean current) {
        content.update();
        boolean canToggle = !"ClientSettings".equals(mod.name);
        boolean hasSettings = hasVisibleSettings();
        boolean hovered = Hover.is(x, y, width, height + settingHeight, (int) mouseX, (int) mouseY);

        if (mod.isEnabled()) {
            content.animateTo(ClickGuiTheme.moduleContentEnabled(), 0.2f, Easings.QUAD_IN_OUT);
            knobT = (float) AnimMath.base(knobT, 1.0, 0.25f);
        } else {
            content.animateTo(ClickGuiTheme.moduleContentDisabled(), 0.2f, Easings.QUAD_IN_OUT);
            knobT = (float) AnimMath.base(knobT, 0.0, 0.25f);
        }

        UiChrome.card(x, y, width, height + settingHeight, hovered, expand);

        String name = FPSMaster.i18n.get(mod.name.toLowerCase(Locale.getDefault()));
        String desc = FPSMaster.i18n.get(mod.name.toLowerCase(Locale.getDefault()) + ".desc");
        String bindText = UiChrome.keyName(mod.key);
        float chipW = UiChrome.keyChipWidth(bindText);
        float chipH = 11.5f;
        float switchW = canToggle ? UiChrome.SWITCH_SM_W : 0f;
        float chevW = hasSettings ? 6f : 0f;
        float right = x + width - 7f;
        float chevX = hasSettings ? right - chevW : right;
        float switchX = chevX - (canToggle ? 5f + switchW : 0f);
        float chipX = switchX - 5f - chipW;
        float chipY = y + (height - chipH) / 2f;
        float switchY = y + (height - UiChrome.SWITCH_SM_H) / 2f;
        float chevY = y + (height - 6f) / 2f;

        // leading icon tile: accent-tinted when the module is on
        float tile = 15f;
        float tileX = x + 5f;
        float tileY = y + (height - tile) / 2f;
        boolean on = mod.isEnabled();
        Rects.rounded(tileX, tileY, tile, tile, 5,
                (on ? ClickGuiTheme.accentSoft() : ClickGuiTheme.mask(64)).getRGB(), false);
        int iconColor = (on ? ClickGuiTheme.accentText() : ClickGuiTheme.textSecondary()).getRGB();
        if (moduleIcon != null) {
            top.fpsmaster.utils.render.draw.Images.drawSmooth(moduleIcon, tileX + 3f, tileY + 3f, 9f, 9f, iconColor);
        } else {
            Icons.draw(categoryIconName(), tileX + 3.5f, tileY + 3.5f, 8f, iconColor);
        }

        float infoX = tileX + tile + 6f;
        float infoMax = chipX - 6f;
        drawHighlighted(name, FPSMaster.fontManager.s14, infoX, y + 7.5f, content.getColor().getRGB());
        float nameW = FPSMaster.fontManager.s14.getStringWidth(name);
        float descX = infoX + nameW + 5f;
        if (searchMode) {
            String categoryName = FPSMaster.i18n.get("category." + mod.category.name().toLowerCase(Locale.getDefault()));
            float tagW = FPSMaster.fontManager.getFont(12).getStringWidth(categoryName);
            if (descX + tagW + 4f < infoMax) {
                FPSMaster.fontManager.getFont(12).drawString(categoryName, descX, y + 8.5f, ClickGuiTheme.accent().getRGB());
                descX += tagW + 4f;
            }
        }
        if (descX + 12f < infoMax) {
            String trimmed = ellipsize(FPSMaster.fontManager.getFont(12), desc, infoMax - descX);
            drawHighlighted(trimmed, FPSMaster.fontManager.getFont(12), descX, y + 8.5f, ClickGuiTheme.textDescription().getRGB());
        }

        boolean chipHover = Hover.is(chipX, chipY, chipW, chipH, (int) mouseX, (int) mouseY);
        boolean bindActive = MainPanel.bindLock.equals(moduleBindLock());
        UiChrome.keyChip(chipX, chipY, chipW, chipH, bindText, bindActive, chipHover);
        if (canToggle) {
            UiChrome.drawSwitchSm(switchX, switchY, mod.isEnabled(), knobT);
        }
        if (hasSettings) {
            GL11.glPushMatrix();
            float cx = chevX + 3f;
            float cy = chevY + 3f;
            GL11.glTranslatef(cx, cy, 0f);
            GL11.glRotatef(expand ? 90f : 0f, 0f, 0f, 1f);
            GL11.glTranslatef(-cx, -cy, 0f);
            Icons.draw("chev-r", chevX, chevY, 6f, ClickGuiTheme.textDisabled().getRGB());
            GL11.glPopMatrix();
        }

        float settingsHeight = 0f;
        if (expand) {
            UiChrome.hairlineH(x + 6, y + height, width - 12);
            ArrayList<SettingRender<?>> visible = visibleSettings();
            float innerX = x + 6f;
            float innerW = width - 12f;
            float colW = (innerW - COL_GAP) / 2f;
            float cursorY = y + height + 3f;
            int i = 0;
            while (i < visible.size()) {
                SettingRender<?> first = visible.get(i);
                boolean pair = !first.isWide() && i + 1 < visible.size() && !visible.get(i + 1).isWide();
                if (pair) {
                    SettingRender<?> second = visible.get(i + 1);
                    first.render(screen, innerX, cursorY, colW, 19f, mouseX, mouseY, MainPanel.curModule == mod);
                    second.render(screen, innerX + colW + COL_GAP, cursorY, colW, 19f, mouseX, mouseY, MainPanel.curModule == mod);
                    float rowH = Math.max(first.height, second.height);
                    cursorY += rowH + SETTINGS_GAP;
                    settingsHeight += rowH + SETTINGS_GAP;
                    i += 2;
                } else {
                    first.render(screen, innerX, cursorY, innerW, 19f, mouseX, mouseY, MainPanel.curModule == mod);
                    cursorY += first.height + SETTINGS_GAP;
                    settingsHeight += first.height + SETTINGS_GAP;
                    i++;
                }
            }
            settingsHeight += 2f;
        }

        settingHeight = (float) AnimMath.base(settingHeight, settingsHeight, 0.2);
        this.height = settingHeight;

        if (canToggle && screen.consumePressInBounds(switchX, switchY, switchW, UiChrome.SWITCH_SM_H, 0) != null) {
            mod.toggle();
        } else if (screen.consumePressInBounds(chipX, chipY, chipW, chipH, 0) != null) {
            MainPanel.bindLock = bindActive ? "" : moduleBindLock();
        } else if (hasSettings && screen.consumePressAsHovered(mod, x, y, width, height, 0) != null) {
            expand = !expand;
            MainPanel.curModule = null;
        }
    }

    /** Screenshot-pipeline hook: opens this module's settings without a synthetic click. */
    public void expandForShot() {
        expand = true;
    }

    private String moduleBindLock() {
        return "mod:" + mod.name;
    }

    private static net.minecraft.util.ResourceLocation resolveModuleIcon(Module module) {
        net.minecraft.util.ResourceLocation icon = new net.minecraft.util.ResourceLocation(
                "client/textures/modules/" + module.name.toLowerCase(Locale.getDefault()) + ".png");
        try {
            net.minecraft.client.Minecraft.getMinecraft().getResourceManager().getResource(icon);
            return icon;
        } catch (Exception missing) {
            return null;
        }
    }

    /** Sidebar icon of the module's category, for modules without a dedicated icon. */
    private String categoryIconName() {
        switch (mod.category) {
            case OPTIMIZE:
                return "zap";
            case RENDER:
                return "sparkles";
            case Utility:
                return "wrench";
            default:
                return "grid";
        }
    }

    private boolean hasVisibleSettings() {
        for (SettingRender<?> renderer : settingsRenderers) {
            if (renderer.setting.getVisible()) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<SettingRender<?>> visibleSettings() {
        ArrayList<SettingRender<?>> visible = new ArrayList<>();
        for (SettingRender<?> renderer : settingsRenderers) {
            if (renderer.setting.getVisible()) {
                visible.add(renderer);
            }
        }
        return visible;
    }

    private String ellipsize(UFontRenderer font, String text, float maxWidth) {
        if (maxWidth <= 0f || font.getStringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        while (!text.isEmpty() && font.getStringWidth(text + ellipsis) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    private void drawHighlighted(String text, UFontRenderer font, float x, float y, int color) {
        if (highlight == null || highlight.isEmpty()) {
            font.drawString(text, x, y, color);
            return;
        }
        String lowerText = text.toLowerCase(Locale.getDefault());
        int index = lowerText.indexOf(highlight.toLowerCase(Locale.getDefault()));
        if (index < 0) {
            font.drawString(text, x, y, color);
            return;
        }
        int matchEnd = Math.min(index + highlight.length(), text.length());
        String before = text.substring(0, index);
        String match = text.substring(index, matchEnd);
        String after = text.substring(matchEnd);
        float cursor = x;
        if (!before.isEmpty()) {
            cursor += font.drawString(before, cursor, y, color);
        }
        font.drawString(match, cursor, y, ClickGuiTheme.accent().getRGB());
        cursor += font.getStringWidth(match);
        if (!after.isEmpty()) {
            font.drawString(after, cursor, y, color);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (MainPanel.bindLock.equals(moduleBindLock())) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                MainPanel.bindLock = "";
                return;
            }
            if (keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) {
                mod.key = 0;
            } else if (keyCode != Keyboard.KEY_NONE) {
                mod.key = keyCode;
            }
            MainPanel.bindLock = "";
            return;
        }
        if (expand) {
            for (SettingRender<?> settingsRenderer : settingsRenderers) {
                settingsRenderer.keyTyped(typedChar, keyCode);
            }
        }
    }
}
