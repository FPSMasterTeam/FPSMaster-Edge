package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.ModsList;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.utils.render.Render2DUtils;
import top.fpsmaster.interfaces.ProviderManager;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ModsListComponent extends Component {

    List<Module> modules = new ArrayList<>();
    
    public ModsListComponent() {
        super(ModsList.class);
        this.x = 1f;
        this.allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        UFontRenderer font = FPSMaster.fontManager.s18;
        float modY = 0f;

        ModsList modlist = (ModsList) mod;
        if (modlist.showLogo.getValue()) {
            drawString(36, modlist.text.getValue(), x + 0.5f, y + 0.5f, new Color(0, 0, 0, 100).getRGB());
            drawString(36, modlist.text.getValue(), x, y, FPSMaster.theme.getPrimary().getRGB());
            modY = 20f;
        }

        float width2 = 40f;
        x += this.width;

        if (ProviderManager.mcProvider.getPlayer().ticksExisted % 20 == 0)
            modules = FPSMaster.moduleManager.modules.stream()
                    .sorted((m1, m2) -> {
                        float w1 = (mod).betterFont.getValue()
                                ? font.getStringWidth(modlist.english.getValue() ? m1.name : FPSMaster.i18n.get(m1.name.toLowerCase()))
                                : ProviderManager.mcProvider.getFontRenderer().getStringWidth(modlist.english.getValue() ? m1.name : FPSMaster.i18n.get(m1.name.toLowerCase()));
                        float w2 = (mod.betterFont.getValue()
                                ? font.getStringWidth(modlist.english.getValue() ? m2.name : FPSMaster.i18n.get(m2.name.toLowerCase()))
                                : ProviderManager.mcProvider.getFontRenderer().getStringWidth(modlist.english.getValue() ? m2.name : FPSMaster.i18n.get(m2.name.toLowerCase())));
                        return Float.compare(w2, w1);
                    }).collect(Collectors.toList());

        int ls = 0;
        for (Module module : modules) {
            Color col = Color.getHSBColor(
                    ls / (float) modules.size() - ProviderManager.mcProvider.getPlayer().ticksExisted % 50 / 50f,
                    0.7f,
                    1f
            );
            if (!module.isEnabled() || module.category == Category.Interface) {
                continue;
            }

            String name = FPSMaster.i18n.get(module.name.toLowerCase());
            if (modlist.english.getValue()) {
                name = module.name;
            }

            float width = mod.betterFont.getValue()
                    ? font.getStringWidth(name)
                    : ProviderManager.mcProvider.getFontRenderer().getStringWidth(name);

            if (width2 < width) {
                width2 = width + 5;
            }

            Render2DUtils.drawRect(x - width - 4, y + modY, width + 4, 14f, modlist.backgroundColor.getColor());
            Color color = modlist.color.getColor();
            if (modlist.rainbow.getValue()) {
                color = col;
            }

            if (mod.betterFont.getValue()) {
                font.drawStringWithShadow(name, x - width - 2, y + modY + 2, color.getRGB());
            } else {
                ProviderManager.mcProvider.getFontRenderer().drawStringWithShadow(name, x - width - 2, y + modY, color.getRGB());
            }
            ls++;
            modY += 14f;
        }

        this.width = width2;
        height = modY;
    }
}
