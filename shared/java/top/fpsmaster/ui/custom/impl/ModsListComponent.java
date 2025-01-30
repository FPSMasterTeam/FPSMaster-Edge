package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.ModsList;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.utils.render.Render2DUtils;
import top.fpsmaster.interfaces.ProviderManager;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ModsListComponent extends Component {

    private List<Module> modules;

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

        if (((ModsList) mod).getShowLogo().value) {
            drawString(36, "FPS V3", x + 0.5f, y + 0.5f, new Color(0, 0, 0, 150).getRGB());
            drawString(36, "FPS V3", x, y, FPSMaster.theme.getPrimary().getRGB());
            modY = 20f;
        }

        float width2 = 40f;
        x += this.width;

        if (modules.isEmpty()) {
            modules = FPSMaster.moduleManager.modules.stream()
                    .sorted((m1, m2) -> {
                        float w1 = (mod).betterFont.value
                                ? font.getStringWidth(((ModsList) mod).getEnglish().value ? m1.name : FPSMaster.i18n.get(m1.name.toLowerCase()))
                                : ProviderManager.mcProvider.getFontRenderer().getStringWidth(((ModsList) mod).getEnglish().value ? m1.name : FPSMaster.i18n.get(m1.name.toLowerCase()));
                        float w2 = (mod.betterFont.value
                                ? font.getStringWidth(((ModsList) mod).getEnglish().value ? m2.name : FPSMaster.i18n.get(m2.name.toLowerCase()))
                                : ProviderManager.mcProvider.getFontRenderer().getStringWidth(((ModsList) mod).getEnglish().value ? m2.name : FPSMaster.i18n.get(m2.name.toLowerCase())));
                        return Float.compare(w2, w1);
                    }).collect(Collectors.toList());
        }

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
            if (((ModsList) mod).getEnglish().value) {
                name = module.name;
            }

            float width = mod.betterFont.value
                    ? font.getStringWidth(name)
                    : ProviderManager.mcProvider.getFontRenderer().getStringWidth(name);

            if (width2 < width) {
                width2 = width + 5;
            }

            Render2DUtils.drawRect(x - width - 4, y + modY, width + 4, 14f, ((ModsList) mod).backgroundColor.getColor());
            Color color = ((ModsList) mod).getColor().getColor();
            if (((ModsList) mod).getRainbow().value) {
                color = col;
            }

            if (mod.betterFont.value) {
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
