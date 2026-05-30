package top.fpsmaster.ui.click.modules.impl;

import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;

import net.minecraft.item.ItemStack;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.features.settings.impl.MultipleItemSetting;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.ui.common.binding.MultipleItemSettingBinding;
import top.fpsmaster.utils.core.Utility;
import top.fpsmaster.utils.world.ItemsUtil;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.*;
import java.util.Locale;

public class MultipleItemSettingRender extends SettingRender<MultipleItemSetting> {
    public static final int xOffset = 14;
    public static final int padding = 3;
    public static final int itemHeight = 21;
    public static final int buttonSize = 15;

    public MultipleItemSettingRender(Module module, MultipleItemSetting setting) {
        super(setting);
        this.mod = module;
        this.binding = new MultipleItemSettingBinding(setting);
    }
    private float itemWidth;
    private final MultipleItemSettingBinding binding;
    @Override
    public void render(ScaledGuiScreen screen, float x, float y, float width, float height, float mouseX, float mouseY, boolean custom) {
        FPSMaster.fontManager.s16.drawString(
                FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault())),
                x + xOffset, y + 1, ClickGuiTheme.textSecondary().getRGB()
        );
        Rects.rounded(Math.round(x + xOffset), Math.round(y + FPSMaster.fontManager.s16.getHeight() + 5), Math.round(itemWidth + padding), Math.round(this.height - 7), 3, ClickGuiTheme.itemContainerBg().getRGB());
        int textWidth = FPSMaster.fontManager.s14.getStringWidth(FPSMaster.i18n.get(FPSMaster.i18n.get("ItemsSetting.heldItem".toLowerCase(Locale.getDefault()))));
        FPSMaster.fontManager.s14.drawString(FPSMaster.i18n.get("ItemsSetting.heldItem".toLowerCase(Locale.getDefault())), x + xOffset + itemWidth - 30 - textWidth, y + 1, ClickGuiTheme.textPrimary().getRGB());
        FPSMaster.fontManager.s22.drawString("+", x + xOffset + itemWidth - 12, y + 1, ClickGuiTheme.textPrimary().getRGB());

        Rects.rounded(Math.round(x + xOffset), Math.round(y + FPSMaster.fontManager.s16.getHeight() + 5), Math.round(itemWidth + padding), Math.round(this.height - 7), 3, ClickGuiTheme.itemContainerBg().getRGB());

        int index = 0;
        this.itemWidth = width - (xOffset * 2);
        for (ItemStack itemStack : binding.get()) {
            Rects.rounded(Math.round(x + xOffset + padding), Math.round(y + FPSMaster.fontManager.s16.getHeight() + 5 + padding + (index * (itemHeight + padding))), Math.round(itemWidth - padding), itemHeight, ClickGuiTheme.itemBg());
            ItemsUtil.renderItem(itemStack, x + (padding * 2) + 20f, (y + FPSMaster.fontManager.s16.getHeight() + 5 + padding * 2) + (index * (itemHeight + padding)));
            renderButton(x + xOffset + itemWidth - (padding * 2) - buttonSize, (y + FPSMaster.fontManager.s16.getHeight() + 5 + padding * 2) + (index * (buttonSize + (padding * 3))), mouseX,mouseY ,"-");
            FPSMaster.fontManager.s14.drawString(itemStack.getDisplayName(), x + (padding * 2) + 45f, (y + FPSMaster.fontManager.s16.getHeight() + 5 + padding * 2) + (index * (buttonSize + (padding * 3))) + 5, ClickGuiTheme.textPrimary().getRGB());
            index++;
        }
        if (binding.get().isEmpty()) {
            this.height = itemHeight + 10;
            FPSMaster.fontManager.s14.drawString(FPSMaster.i18n.get("ItemsSetting.isEmpty".toLowerCase(Locale.getDefault())), x + ((itemWidth - (padding * 2)) / 2), (y + FPSMaster.fontManager.s16.getHeight() + 5 + padding * 2) + 5, ClickGuiTheme.textSecondary().getRGB());
        }else{
            this.height = (index * (itemHeight + padding)) + 10;
        }

        ScaledGuiScreen.PointerEvent addClick = screen.consumePressInBounds(x + 10 + xOffset + itemWidth - 15, y - 3, 10, 10, 0);
        if (addClick != null) {
            ItemStack heldItem = Utility.mc.thePlayer.getHeldItem();
            if (heldItem != null) {
                binding.addItem(heldItem);
                return;
            }
        }

        for (int i = 0; i < binding.get().size(); i++) {
            float rx = x + 10 + xOffset + itemWidth - (padding * 2) - buttonSize;
            float ry = (y + FPSMaster.fontManager.s16.getHeight() + 5 + padding * 2) + (i * (buttonSize + (padding * 3)));
            ScaledGuiScreen.PointerEvent removeClick = screen.consumePressInBounds(rx, ry, buttonSize, buttonSize, 0);
            if (removeClick != null) {
                binding.removeItem(i);
                break;
            }
        }
    }

    public void renderButton(float x, float y, float mouseX, float mouseY, String icon) {
        Color color = ClickGuiTheme.buttonBg();
        if(Hover.is(x,y,buttonSize,buttonSize,(int) mouseX,(int) mouseY)){
            color = ClickGuiTheme.buttonHoverBg();
        }
        Rects.rounded(Math.round(x), Math.round(y), buttonSize, buttonSize, color);
        FPSMaster.fontManager.s16.drawString(icon, x + (buttonSize / 2.0f) - 2, y + (buttonSize / 2.0f) - 6, ClickGuiTheme.textPrimary().getRGB());
    }

}




