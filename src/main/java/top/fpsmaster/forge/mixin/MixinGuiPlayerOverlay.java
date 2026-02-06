package top.fpsmaster.forge.mixin;

import com.google.common.collect.Ordering;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import top.fpsmaster.features.impl.interfaces.TabOverlay;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Iterator;
import java.util.List;

import static top.fpsmaster.utils.core.Utility.mc;

@Mixin(GuiPlayerTabOverlay.class)
public abstract class MixinGuiPlayerOverlay {

    @Final
    @Shadow
    private static Ordering<NetworkPlayerInfo> field_175252_a;

    @Shadow
    public abstract String getPlayerName(NetworkPlayerInfo networkPlayerInfoIn);

    @Shadow
    private IChatComponent header;

    @Shadow
    private IChatComponent footer;

    @Shadow
    protected abstract void drawScoreboardValues(ScoreObjective objective, int i, String name, int j, int k, NetworkPlayerInfo info);

    @Shadow
    protected abstract void drawPing(int i, int j, int k, NetworkPlayerInfo networkPlayerInfoIn);


    /**
     * @author SuperSkidder
     * @reason BetterTabUI
     */
    @Overwrite
    public void renderPlayerlist(int width, Scoreboard scoreboardIn, ScoreObjective scoreObjectiveIn) {
        width += 10;
        NetHandlerPlayClient netHandlerPlayClient = mc.thePlayer.sendQueue;
        List<NetworkPlayerInfo> list = field_175252_a.sortedCopy(netHandlerPlayClient.getPlayerInfoMap());
        int i = 0;
        int j = 0;
        for (NetworkPlayerInfo networkPlayerInfo : list) {
            int k = mc.fontRendererObj.getStringWidth(this.getPlayerName(networkPlayerInfo));
            i = Math.max(i, k);
            if (scoreObjectiveIn != null && scoreObjectiveIn.getRenderType() != IScoreObjectiveCriteria.EnumRenderType.HEARTS) {
                k = mc.fontRendererObj.getStringWidth(" " + scoreboardIn.getValueFromObjective(networkPlayerInfo.getGameProfile().getName(), scoreObjectiveIn).getScorePoints());
                j = Math.max(j, k);
            }
        }

        list = list.subList(0, Math.min(list.size(), 80));
        int l = list.size();
        int m = l;

        int k;
        for (k = 1; m > 20; m = (l + k - 1) / k) {
            ++k;
        }

        boolean bl = mc.isIntegratedServerRunning() || mc.getNetHandler().getNetworkManager().getIsencrypted();
        int n;
        if (scoreObjectiveIn != null) {
            if (scoreObjectiveIn.getRenderType() == IScoreObjectiveCriteria.EnumRenderType.HEARTS) {
                n = 90;
            } else {
                n = j;
            }
        } else {
            n = 0;
        }

        int o = Math.min(k * ((bl ? 9 : 0) + i + n + 13), width - 50) / k;
        int p = width / 2 - (o * k + (k - 1) * 5) / 2;
        int q = 10;
        int r = o * k + (k - 1) * 5;
        List<String> list2 = null;
        List<String> list3 = null;
        if (this.header != null) {
            list2 = mc.fontRendererObj.listFormattedStringToWidth(this.header.getFormattedText(), width - 50);

            for (String string : list2) {
                r = Math.max(r, mc.fontRendererObj.getStringWidth(string));
            }
        }

        if (this.footer != null) {
            list3 = mc.fontRendererObj.listFormattedStringToWidth(this.footer.getFormattedText(), width - 50);

            for (String string : list3) {
                r = Math.max(r, mc.fontRendererObj.getStringWidth(string));
            }
        }

        if (list2 != null) {
            Gui.drawRect(width / 2 - r / 2 - 1, q - 1, width / 2 + r / 2 + 1, q + list2.size() * mc.fontRendererObj.FONT_HEIGHT, Integer.MIN_VALUE);

            for (String string : list2) {
                int s = mc.fontRendererObj.getStringWidth(string);
                mc.fontRendererObj.drawStringWithShadow(string, (float) (width / 2 - s / 2), (float) q, -1);
                q += mc.fontRendererObj.FONT_HEIGHT;
            }

            ++q;
        }

        Gui.drawRect(width / 2 - r / 2 - 1, q - 1, width / 2 + r / 2 + 1, q + m * 9, Integer.MIN_VALUE);

        for (int t = 0; t < l; ++t) {
            int u = t / m;
            int s = t % m;
            int v = p + u * o + u * 5;
            int w = q + s * 9;
            Gui.drawRect(v, w, v + o, w + 8, 553648127);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            if (t < list.size()) {
                NetworkPlayerInfo networkPlayerInfo2 = list.get(t);
                String string2 = this.getPlayerName(networkPlayerInfo2);
                GameProfile gameProfile = networkPlayerInfo2.getGameProfile();
                if (bl) {
                    EntityPlayer entityPlayer = mc.theWorld.getPlayerEntityByUUID(gameProfile.getId());
                    boolean bl2 = entityPlayer != null && entityPlayer.isWearing(EnumPlayerModelParts.CAPE) && (gameProfile.getName().equals("Dinnerbone") || gameProfile.getName().equals("Grumm"));
                    mc.getTextureManager().bindTexture(networkPlayerInfo2.getLocationSkin());
                    int x = 8 + (bl2 ? 8 : 0);
                    int y = 8 * (bl2 ? -1 : 1);
                    Gui.drawScaledCustomSizeModalRect(v, w, 8.0F, (float) x, 8, y, 8, 8, 64.0F, 64.0F);
                    if (entityPlayer != null && entityPlayer.isWearing(EnumPlayerModelParts.HAT)) {
                        int z = 8 + (bl2 ? 8 : 0);
                        int aa = 8 * (bl2 ? -1 : 1);
                        Gui.drawScaledCustomSizeModalRect(v, w, 40.0F, (float) z, 8, aa, 8, 8, 64.0F, 64.0F);
                    }

                    v += 9;
                }

                int clientOffset = 0;

                if (networkPlayerInfo2.getGameType() == WorldSettings.GameType.SPECTATOR) {
                    string2 = EnumChatFormatting.ITALIC + string2;
                    mc.fontRendererObj.drawStringWithShadow(string2, (float) v + clientOffset, (float) w, -1862270977);
                } else {
                    mc.fontRendererObj.drawStringWithShadow(string2, (float) v + clientOffset, (float) w, -1);
                }

                if (scoreObjectiveIn != null && networkPlayerInfo2.getGameType() != WorldSettings.GameType.SPECTATOR) {
                    int ab = v + i + 1;
                    int ac = ab + n;
                    if (ac - ab > 5) {
                        this.drawScoreboardValues(scoreObjectiveIn, w, gameProfile.getName(), ab, ac, networkPlayerInfo2);
                    }
                }

                if (TabOverlay.using && TabOverlay.showPing.getValue()) {
                    int responseTime = networkPlayerInfo2.getResponseTime();
                    String text = responseTime + "ms";
                    Color color = responseTime < 150 ? Color.GREEN : responseTime < 300 ? Color.YELLOW : Color.RED;
                    mc.fontRendererObj.drawStringWithShadow(text, o + v - (bl ? 9 : 0) - mc.fontRendererObj.getStringWidth(text), w, color.getRGB());
                } else {
                    this.drawPing(o, v - (bl ? 9 : 0), w, networkPlayerInfo2);
                }
            }
        }

        if (list3 != null) {
            q += m * 9 + 1;
            Gui.drawRect(width / 2 - r / 2 - 1, q - 1, width / 2 + r / 2 + 1, q + list3.size() * mc.fontRendererObj.FONT_HEIGHT, Integer.MIN_VALUE);

            for (String string : list3) {
                int s = mc.fontRendererObj.getStringWidth(string);
                mc.fontRendererObj.drawStringWithShadow(string, (float) (width / 2 - s / 2), (float) q, -1);
                q += mc.fontRendererObj.FONT_HEIGHT;
            }
        }

    }


}



