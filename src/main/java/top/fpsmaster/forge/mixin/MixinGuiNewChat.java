package top.fpsmaster.forge.mixin;

import top.fpsmaster.utils.render.draw.Colors;

import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.BetterChat;
import top.fpsmaster.features.impl.render.ChatAvatarCache;
import top.fpsmaster.features.impl.render.ChatAvatars;

import java.awt.*;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static top.fpsmaster.utils.core.Utility.mc;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {

    @Unique
    private boolean v1_8_9$isChatOpenAnimationNeed = true;

    @Shadow
    public abstract int getLineCount();

    @Final
    @Shadow
    private List<ChatLine> drawnChatLines;


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

    @Shadow @Final private List<ChatLine> chatLines;

    @Unique
    private static final int FPSMASTER_MAX_CHAT_MESSAGE_SOURCES = 512;

    @Unique
    private final Map<IChatComponent, IChatComponent> fpsmaster$chatMessageSources = new IdentityHashMap<>();

    /**
     * @author SuperSkidder
     * @reason betterchat
     */
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
                    int chatAvatarOffset = ChatAvatars.getChatOffset();
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(2.0F + chatAvatarOffset, 8.0F, 0.0F);
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
                                    Gui.drawRect(-2 - chatAvatarOffset, q - 9, k + 4, q, o / 2 << 24);
                                    drawChatAvatar(chatLine, -chatAvatarOffset + 1 + ChatAvatars.getOffsetX(), q - 8 + ChatAvatars.getOffsetY(), o);
                                    String string = chatLine.getChatComponent().getFormattedText();
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
                            Gui.drawRect(-chatAvatarOffset, -s, 2 - chatAvatarOffset, -s - t, p + (o << 24));
                            Gui.drawRect(2 - chatAvatarOffset, -s, 1 - chatAvatarOffset, -s - t, 13421772 + (o << 24));
                        }
                    }

                    GlStateManager.popMatrix();
                }
            } else {
                BetterChat module = (BetterChat) FPSMaster.moduleManager.getModule(BetterChat.class);
                int i = this.getLineCount();
                int j = drawnChatLines.size();
                float f = mc.gameSettings.chatOpacity * 0.9F + 0.1F;
                if (j > 0) {
                    boolean bl = this.getChatOpen();

                    float g = this.getChatScale();
                    int k = MathHelper.ceiling_float_int((float) this.getChatWidth() / g);
                    int chatAvatarOffset = ChatAvatars.getChatOffset();
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(2.0F + chatAvatarOffset, 8.0F, 0.0F);
                    GlStateManager.scale(g, g, 1.0F);

                    int m;
                    int n;
                    int o;
                    for (m = 0; m + this.scrollPos < drawnChatLines.size() && m < i; ++m) {
                        ChatLine chatLine = drawnChatLines.get(m + this.scrollPos);
                        if (chatLine != null) {
                            if (getChatOpen() && v1_8_9$isChatOpenAnimationNeed) {
                                v1_8_9$isChatOpenAnimationNeed = false;
                            }
                            if (!getChatOpen()) {
                                v1_8_9$isChatOpenAnimationNeed = true;
                            }

                            n = updateCounter - chatLine.getUpdatedCounter();
                            if (n < 200 || bl) {
                                int alpha = (int) (255f * f);

                                if (alpha > 3) {
                                    int q = -m * 9;
                                    int alpha1 = (int) ((alpha / 255f) * module.backgroundColor.getColor().getAlpha());
                                    Gui.drawRect(-2 - chatAvatarOffset, q - 8, k + 4, q + 1, Colors.alpha(module.backgroundColor.getColor(), alpha1).getRGB());
                                    drawChatAvatar(chatLine, -chatAvatarOffset + 1 + ChatAvatars.getOffsetX(), q - 8 + ChatAvatars.getOffsetY() + Math.round(6 - (alpha / 255f) * 6), alpha);
                                    String string = chatLine.getChatComponent().getFormattedText();
                                    GlStateManager.enableBlend();
                                    if (module.betterFont.getValue()) {
                                        FPSMaster.fontManager.s16.drawStringWithShadow(string, 0.0F, (float) (q - 8) + (6 - (alpha / 255f) * 6), Colors.alpha(new Color(16777215), alpha).getRGB());
                                    } else {
                                        mc.fontRendererObj.drawStringWithShadow(string, 0.0F, (float) (q - 8) + (6 - (alpha / 255f) * 6), Colors.alpha(new Color(16777215), alpha).getRGB());
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
                        if (r != 0) {
                            Gui.drawRect(-chatAvatarOffset, 0, 2 - chatAvatarOffset, 0, module.backgroundColor.getColor().getRGB());
                            Gui.drawRect(2 - chatAvatarOffset, 0, 1 - chatAvatarOffset, 0, module.backgroundColor.getColor().getRGB());
                        }
                    }

                    GlStateManager.popMatrix();
                }
            }
        }
    }

    /**
     * @author SuperSkidder
     * @reason handle click
     */
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
            j = MathHelper.floor_float((float) j / f) - ChatAvatars.getChatOffset();
            k = MathHelper.floor_float((float) k / f);
            if (j >= 0 && k >= 0) {
                int lineCount = Math.min(this.getLineCount(), this.drawnChatLines.size());
                int fontHeight = mc.fontRendererObj.FONT_HEIGHT;
                BetterChat module = (BetterChat) FPSMaster.moduleManager.getModule(BetterChat.class);

                if (BetterChat.using && module.betterFont.getValue()) {
                    fontHeight = FPSMaster.fontManager.s16.getHeight();
                }
                if (j <= MathHelper.floor_float((float) this.getChatWidth() / this.getChatScale()) && k < fontHeight * lineCount + lineCount) {
                    int m = k / fontHeight + this.scrollPos;
                    if (m >= 0 && m < this.drawnChatLines.size()) {
                        ChatLine chatLine = this.drawnChatLines.get(m);
                        int n = 0;

                        for (IChatComponent iTextComponent : chatLine.getChatComponent()) {
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

        int chatAvatarOffset = ChatAvatars.getChatOffset();
        int width = Math.max(20, i - chatAvatarOffset);
        List<IChatComponent> lines;
        if (BetterChat.using && module.betterFont.getValue()) {
            lines = GuiUtilRenderComponents.splitText(chatComponent, width, FPSMaster.fontManager.s16, false, false);
        } else {
            lines = GuiUtilRenderComponents.splitText(chatComponent, width, mc.fontRendererObj, false, false);
        }
        fpsmaster$rememberChatMessageSource(chatComponent, lines);
        return lines;
    }

    @Unique
    private void fpsmaster$rememberChatMessageSource(IChatComponent source, List<IChatComponent> lines) {
        if (fpsmaster$chatMessageSources.size() >= FPSMASTER_MAX_CHAT_MESSAGE_SOURCES) {
            fpsmaster$chatMessageSources.clear();
        }
        for (IChatComponent line : lines) {
            fpsmaster$chatMessageSources.put(line, source);
        }
    }

    @Unique
    private void drawChatAvatar(ChatLine chatLine, int x, int y, int alpha) {
        IChatComponent line = chatLine.getChatComponent();
        IChatComponent source = fpsmaster$chatMessageSources.get(line);
        ResourceLocation avatar = ChatAvatars.getAvatar(source != null ? source : line);
        if (avatar != null) {
            ChatAvatarCache.drawHead(avatar, x, y, ChatAvatars.getAvatarSize(), alpha);
        }
    }

    public List<ChatLine> getChatLines() {
        return chatLines;
    }

    public List<ChatLine> getDrawnChatLines() {
        return drawnChatLines;
    }
}




