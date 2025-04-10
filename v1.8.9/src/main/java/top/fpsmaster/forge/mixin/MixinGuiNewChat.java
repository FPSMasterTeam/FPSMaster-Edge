package top.fpsmaster.forge.mixin;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.forge.api.IChatLine;
import top.fpsmaster.features.impl.interfaces.BetterChat;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;
import java.util.Iterator;
import java.util.List;

import static top.fpsmaster.utils.Utility.mc;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {

    private boolean isChatOpenAnimationNeed = true;

    @Shadow
    public abstract int getLineCount();

    @Final
    @Shadow
    private List<ChatLine> drawnChatLines = Lists.newArrayList();

    @Shadow
    public abstract boolean getChatOpen();

    @Shadow
    public abstract float getChatScale();

    @Shadow
    public abstract int getChatWidth();

    @Shadow
    private int scrollPos;

    @Shadow
    private boolean isScrolled;

    @Overwrite
    public void drawChat(int updateCounter) {
        if (mc.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN) {
            if (!BetterChat.using) {
                int i = this.getLineCount();
                int j = this.drawnChatLines.size();
                float f = mc.gameSettings.chatOpacity * 0.9F + 0.1F;
                if (j > 0) {
                    boolean bl = this.getChatOpen();

                    float g = this.getChatScale();
                    int k = MathHelper.ceiling_float_int((float) this.getChatWidth() / g);
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(2.0F, 8.0F, 0.0F);
                    GlStateManager.scale(g, g, 1.0F);
                    int l = 0;

                    int m;
                    int n;
                    int o;
                    for (m = 0; m + this.scrollPos < this.drawnChatLines.size() && m < i; ++m) {
                        ChatLine chatLine = this.drawnChatLines.get(m + this.scrollPos);
                        if (chatLine != null) {
                            n = updateCounter - chatLine.getUpdatedCounter();
                            if (n < 200 || bl) {
                                double d = (double) n / 200.0;
                                d = 1.0 - d;
                                d *= 10.0;
                                d = MathHelper.clamp_double(d, 0.0, 1.0);
                                d *= d;
                                o = (int) (255.0 * d);
                                if (bl) {
                                    o = 255;
                                }

                                o = (int) ((float) o * f);
                                ++l;
                                if (o > 3) {
                                    int q = -m * 9;
                                    Gui.drawRect(-2, q - 9, k + 4, q, o / 2 << 24);
                                    String string = chatLine.getChatComponent().getFormattedText();
                                    // 替换提示信息中的 # 为当前命令前缀
                                    if (string.startsWith("#IRC聊天")) {
                                        string = string.replace("#", FPSMaster.getCommandPrefix());
                                        chatLine.getChatComponent().setFormattedText(string);
                                    }
                                    GlStateManager.enableBlend();
                                    mc.fontRendererObj.drawStringWithShadow(string, 0.0F, (float) (q - 8), 16777215 + (o << 24));
                                    GlStateManager.disableAlpha();
                                    GlStateManager.disableBlend();
                                }
                            }
                        }
                    }

                    if (bl) {
                        m = mc.fontRendererObj.FONT_HEIGHT;
                        GlStateManager.translate(-3.0F, 0.0F, 0.0F);
                        int r = j * m + j;
                        n = l * m + l;
                        int s = this.scrollPos * n / j;
                        int t = n * n / r;
                        if (r != n) {
                            o = s > 0 ? 170 : 96;
                            int p = this.isScrolled ? 13382451 : 3355562;
                            Gui.drawRect(0, -s, 2, -s - t, p + (o << 24));
                            Gui.drawRect(2, -s, 1, -s - t, 13421772 + (o << 24));
                        }
                    }

                    GlStateManager.popMatrix();
                }
            } else {
                BetterChat module = (BetterChat) FPSMaster.moduleManager.getModule(BetterChat.class);

                int i = this.getLineCount();
                int j = this.drawnChatLines.size();
                float f = mc.gameSettings.chatOpacity * 0.9F + 0.1F;
                if (j > 0) {
                    boolean bl = this.getChatOpen();

                    float g = this.getChatScale();
                    int k = MathHelper.ceiling_float_int((float) this.getChatWidth() / g);
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(2.0F, 8.0F, 0.0F);
                    GlStateManager.scale(g, g, 1.0F);
                    int l = 0;

                    int m;
                    int n;
                    int o;
                    for (m = 0; m + this.scrollPos < this.drawnChatLines.size() && m < i; ++m) {
                        ChatLine chatLine = this.drawnChatLines.get(m + this.scrollPos);
                        if (chatLine != null) {
                            if (getChatOpen() && isChatOpenAnimationNeed) {
                                for (int i1 = 0; i1 + this.scrollPos < this.drawnChatLines.size() && i1 < i; ++i1) {
                                    ChatLine chatline = this.drawnChatLines.get(i1 + this.scrollPos);
                                    ((IChatLine) chatline).setAnimation(100);
                                }
                                isChatOpenAnimationNeed = false;
                            }
                            if (!getChatOpen()) {
                                isChatOpenAnimationNeed = true;
                            }

                            n = updateCounter - chatLine.getUpdatedCounter();
                            if (n < 200 || bl) {
                                if (n < 150 || getChatOpen()) {
                                    ((IChatLine) chatLine).setAnimation((float) AnimationUtils.base(((IChatLine) chatLine).getAnimation(), 255, 0.1f));
                                } else {
                                    ((IChatLine) chatLine).setAnimation((float) AnimationUtils.base(((IChatLine) chatLine).getAnimation(), 0, 0.1f));
                                }
                                int alpha = (int) (((IChatLine) chatLine).getAnimation() * f);

                                if (alpha > 3) {
                                    int q = -m * 9;
                                    int alpha1 = (int) ((alpha / 255f) * module.backgroundColor.getColor().getAlpha());
                                    Gui.drawRect(-2, q - 9, k + 4, q, Render2DUtils.reAlpha(module.backgroundColor.getColor(), alpha1).getRGB());
                                    String string = chatLine.getChatComponent().getFormattedText();
                                    // 替换提示信息中的 # 为当前命令前缀
                                    if (string.startsWith("#IRC聊天")) {
                                        string = string.replace("#", FPSMaster.getCommandPrefix());
                                        chatLine.getChatComponent().setFormattedText(string);
                                    }
                                    GlStateManager.enableBlend();
                                    if (module.betterFont.getValue()) {
                                        FPSMaster.fontManager.s16.drawStringWithShadow(string, 0.0F, (float) (q - 8) + (6 - (alpha / 255f) * 6), Render2DUtils.reAlpha(new Color(16777215), alpha).getRGB());
                                    } else {
                                        mc.fontRendererObj.drawStringWithShadow(string, 0.0F, (float) (q - 8) + (6 - (alpha / 255f) * 6), Render2DUtils.reAlpha(new Color(16777215), alpha).getRGB());
                                    }
                                    GlStateManager.disableAlpha();
                                    GlStateManager.disableBlend();
                                }
                            }
                        }
                    }

                    if (bl) {
                        m = mc.fontRendererObj.FONT_HEIGHT;
                        GlStateManager.translate(-3.0F, 0.0F, 0.0F);
                        int r = j * m + j;
                        n = l * m + l;
                        int s = this.scrollPos * n / j;
                        int t = n * n / r;
                        if (r != n) {
                            o = s > 0 ? 170 : 96;
                            int p = this.isScrolled ? 13382451 : 3355562;
                            Gui.drawRect(0, -s, 2, -s - t, module.backgroundColor.getColor().getRGB());
                            Gui.drawRect(2, -s, 1, -s - t, module.backgroundColor.getColor().getRGB());
                        }
                    }

                    GlStateManager.popMatrix();
                }
            }
        }
    }

    @Overwrite
    public IChatComponent getChatComponent(int mouseX, int mouseY) {
        if (!this.getChatOpen()) {
            return null;
        } else {
            ScaledResolution scaledResolution = new ScaledResolution(mc);
            int i = scaledResolution.getScaleFactor();
            float f = this.getChatScale();
            int j = mouseX / i - 2;
            int k = mouseY / i - 40;
            j = MathHelper.floor_float((float) j / f);
            k = MathHelper.floor_float((float) k / f);
            if (j >= 0 && k >= 0) {
                int l = Math.min(this.getLineCount(), this.drawnChatLines.size());
                int fontHeight = mc.fontRendererObj.FONT_HEIGHT;
                BetterChat module = (BetterChat) FPSMaster.moduleManager.getModule(BetterChat.class);

                if (BetterChat.using && module.betterFont.getValue()) {
                    fontHeight = FPSMaster.fontManager.s16.getHeight();
                }
                if (j <= MathHelper.floor_float((float) this.getChatWidth() / this.getChatScale()) && k < fontHeight * l + l) {
                    int m = k / fontHeight + this.scrollPos;
                    if (m >= 0 && m < this.drawnChatLines.size()) {
                        ChatLine chatLine = this.drawnChatLines.get(m);
                        int n = 0;
                        Iterator var12 = chatLine.getChatComponent().iterator();

                        while (var12.hasNext()) {
                            IChatComponent iTextComponent = (IChatComponent) var12.next();
                            if (iTextComponent instanceof ChatComponentText) {
                                if (BetterChat.using && module.betterFont.getValue()) {
                                    n += FPSMaster.fontManager.s16.getStringWidth(GuiUtilRenderComponents.func_178909_a(((ChatComponentText) iTextComponent).getChatComponentText_TextValue(), false));
                                } else {
                                    n += mc.fontRendererObj.getStringWidth(GuiUtilRenderComponents.func_178909_a(((ChatComponentText) iTextComponent).getChatComponentText_TextValue(), false));
                                }
                                if (n > j) {
                                    return iTextComponent;
                                }
                            }
                        }
                    }

                    return null;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    @Redirect(method = "setChatLine", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiUtilRenderComponents;splitText(Lnet/minecraft/util/IChatComponent;ILnet/minecraft/client/gui/FontRenderer;ZZ)Ljava/util/List;"))
    public List<IChatComponent> spilt(IChatComponent chatComponent, int i, FontRenderer chatcomponenttext, boolean l, boolean chatcomponenttext2){
        BetterChat module = (BetterChat) FPSMaster.moduleManager.getModule(BetterChat.class);

        if (BetterChat.using && module.betterFont.getValue()) {
            return GuiUtilRenderComponents.splitText(chatComponent, i, FPSMaster.fontManager.s16, false, false);
        } else {
            return GuiUtilRenderComponents.splitText(chatComponent, i, mc.fontRendererObj, false, false);
        }
    }
}