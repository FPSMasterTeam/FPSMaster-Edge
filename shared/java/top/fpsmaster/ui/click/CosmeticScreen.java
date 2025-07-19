package top.fpsmaster.ui.click;

import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.AccountException;
import top.fpsmaster.modules.account.AccountManager;
import top.fpsmaster.modules.account.Cosmetic;
import top.fpsmaster.ui.click.component.ScrollContainer;
import top.fpsmaster.utils.render.Render2DUtils;
import top.fpsmaster.utils.render.ScaledGuiScreen;

import java.awt.*;
import java.io.IOException;

public class CosmeticScreen extends ScaledGuiScreen {
    ScrollContainer container = new ScrollContainer();

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);
        String[] split = AccountManager.cosmeticsHeld.split(",");
        Render2DUtils.drawRoundedRectImage(width / 2f - 200, height / 2f - 130, 400, 260, 4, new Color(0, 0, 0, 100));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Render2DUtils.doGlScissor(width / 2f - 200, height / 2f - 130, 400, 260, scaleFactor);
        container.draw(width / 2f - 200, height / 2f - 130, 400, 260, mouseX, mouseY, () -> {
            int y = 0;
            for (String id : split) {
                if (id.isEmpty())
                    continue;
                Cosmetic cosmetic = AccountManager.cosmetics.get(Integer.parseInt(id));
                if (!cosmetic.loaded) {
                    cosmetic.load();
                }
                FPSMaster.fontManager.s18.drawString(cosmetic.name, width / 2f - 190, height / 2f - 120 + y + container.getScroll(), id.equals(AccountManager.cosmeticsUsing) ? Color.GREEN.getRGB() : Color.WHITE.getRGB());
                y += 20;
            }
            container.setHeight(y);
        });
        GL11.glDisable(GL11.GL_SCISSOR_TEST);


    }

    @Override
    public void initGui() {
        super.initGui();
        FPSMaster.async.runnable(() -> {
            try {
                if (AccountManager.cosmetics.isEmpty())
                    FPSMaster.accountManager.refreshCosmetics();
                FPSMaster.accountManager.refreshUserData();
            } catch (AccountException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        String[] split = AccountManager.cosmeticsHeld.split(",");
        int y = 0;
        for (String id : split) {
            if (id.isEmpty())
                continue;
            Cosmetic cosmetic = AccountManager.cosmetics.get(Integer.parseInt(id));
            if (!cosmetic.loaded)
                cosmetic.load();
            if (Render2DUtils.isHovered(width / 2f - 200, height / 2f - 120 + y, 400, 20, mouseX, mouseY)) {
                String cosmeticsUsing = String.valueOf(cosmetic.id);
                if (AccountManager.cosmeticsUsing.equals(cosmeticsUsing)) {
                    AccountManager.cosmeticsUsing = "";
                } else {
                    AccountManager.cosmeticsUsing = cosmeticsUsing;
                }
            }
            y += 20;
        }
    }
}
