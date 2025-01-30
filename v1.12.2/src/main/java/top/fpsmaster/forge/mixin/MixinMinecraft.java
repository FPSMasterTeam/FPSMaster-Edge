package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.advancements.GuiScreenAdvancements;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.forge.api.IMinecraft;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.events.EventKey;
import top.fpsmaster.event.events.EventMouseClick;
import top.fpsmaster.event.events.EventTick;
import top.fpsmaster.features.impl.optimizes.OldAnimations;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.utils.render.Render2DUtils;

import javax.annotation.Nullable;
import java.awt.*;

import static top.fpsmaster.FPSMaster.getClientTitle;

@Mixin(Minecraft.class)
@Implements(@Interface(iface = IMinecraft.class, prefix = "fpsmaster$"))
public abstract class MixinMinecraft implements IMinecraft {
    @Final
    @Shadow
    private net.minecraft.util.Timer timer;

    @Shadow
    public GameSettings gameSettings;

    @Shadow
    public EntityRenderer entityRenderer;

    @Shadow
    public abstract Entity getRenderViewEntity();

    @Shadow
    public RenderGlobal renderGlobal;
    @Shadow
    public EntityPlayerSP player;

    @Shadow
    public GuiIngame ingameGUI;
    @Shadow
    public GuiScreen currentScreen;

    @Shadow
    public PlayerControllerMP playerController;

    @Shadow
    public abstract void displayGuiScreen(@Nullable GuiScreen guiScreenIn);

    @Final
    @Shadow
    private Tutorial tutorial;

    @Shadow
    public abstract NetHandlerPlayClient getConnection();

    @Shadow
    private int rightClickDelayTimer;
    @Shadow
    public boolean inGameHasFocus;

    @Final
    @Mutable
    @Shadow
    private Session session;

    @Override
    public Session arch$getSession() {
        return session;
    }


    @Override
    public void arch$setSession(Session session) {
        this.session = session;
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    public void shutdown(CallbackInfo ci) {
        FPSMaster.INSTANCE.shutdown();
    }

    @Override
    public void arch$setLeftClickCounter(int c) {
        this.leftClickCounter = c;
    }

    @Override
    public void arch$setRightClickDelayTimer(int c) {
        this.rightClickDelayTimer = c;
    }

    @Override
    public Timer arch$getTimer() {
        return timer;
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        EventDispatcher.dispatchEvent(new EventTick());
    }



    /**
     * @author SuperSkidder
     * @reason fps Limit
     */
    @Overwrite
    public int getLimitFramerate(){
        return (Display.isActive()) ? this.gameSettings.limitFramerate : Performance.using ? Performance.fpsLimit.getValue().intValue() : this.gameSettings.limitFramerate;
    }

    /**
     *  FastLoad
     */
    @Redirect(method = "launchIntegratedServer",at = @At(value = "INVOKE", target = "Ljava/lang/System;gc()V"))
    public void gc1() {
        if (!Performance.using || !Performance.fastLoad.getValue()){
            System.gc();
        }
    }

    @Redirect(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V",at = @At(value = "INVOKE", target = "Ljava/lang/System;gc()V"))
    public void gc2() {
        if (!Performance.using || !Performance.fastLoad.getValue()){
            System.gc();
        }
    }


    @Shadow
    public abstract void updateDisplay();
    /**
     * @author SuperSkidder
     * @reason splash
     */
    @Overwrite
    public void drawSplashScreen(TextureManager textureManagerInstance){
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int i = sr.getScaleFactor();
        Framebuffer framebuffer = new Framebuffer(sr.getScaledWidth() * i, sr.getScaledHeight() * i, true);
        framebuffer.bindFramebuffer(false);
        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0D, sr.getScaledWidth(), sr.getScaledHeight(), 0.0D, 1000.0D, 3000.0D);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0F, 0.0F, -2000.0F);
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.resetColor();

        Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), new Color(20,20,20).getRGB());

        float width = 400;
        float height = 250;
        Render2DUtils.drawImage(new ResourceLocation("client/textures/ui/splash.png"), sr.getScaledWidth() / 2f - width / 2, sr.getScaledHeight() / 2f - height / 2, width, height, -1);
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        framebuffer.unbindFramebuffer();
        framebuffer.framebufferRender(sr.getScaledWidth() * i, sr.getScaledHeight() * i);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        updateDisplay();
    }

    /**
     * @author SuperSkidder
     * @reason Fix for blockhit
     */
    @Overwrite
    private void processKeyBinds() {
        for (; this.gameSettings.keyBindTogglePerspective.isPressed(); this.renderGlobal.setDisplayListEntitiesDirty()) {
            ++this.gameSettings.thirdPersonView;

            if (this.gameSettings.thirdPersonView > 2) {
                this.gameSettings.thirdPersonView = 0;
            }

            if (this.gameSettings.thirdPersonView == 0) {
                this.entityRenderer.loadEntityShader(this.getRenderViewEntity());
            } else if (this.gameSettings.thirdPersonView == 1) {
                this.entityRenderer.loadEntityShader(null);
            }
        }

        while (this.gameSettings.keyBindSmoothCamera.isPressed()) {
            this.gameSettings.smoothCamera = !this.gameSettings.smoothCamera;
        }

        for (int i = 0; i < 9; ++i) {
            boolean flag = this.gameSettings.keyBindSaveToolbar.isKeyDown();
            boolean flag1 = this.gameSettings.keyBindLoadToolbar.isKeyDown();

            if (this.gameSettings.keyBindsHotbar[i].isPressed()) {
                if (this.player.isSpectator()) {
                    this.ingameGUI.getSpectatorGui().onHotbarSelected(i);
                } else if (!this.player.isCreative() || this.currentScreen != null || !flag1 && !flag) {
                    this.player.inventory.currentItem = i;
                } else {
                    GuiContainerCreative.handleHotbarSnapshots(Minecraft.getMinecraft(), i, flag1, flag);
                }
            }
        }

        while (this.gameSettings.keyBindInventory.isPressed()) {
            if (this.playerController.isRidingHorse()) {
                this.player.sendHorseInventory();
            } else {
                this.tutorial.openInventory();
                this.displayGuiScreen(new GuiInventory(this.player));
            }
        }

        while (this.gameSettings.keyBindAdvancements.isPressed()) {
            this.displayGuiScreen(new GuiScreenAdvancements(this.player.connection.getAdvancementManager()));
        }

        while (this.gameSettings.keyBindSwapHands.isPressed()) {
            if (!this.player.isSpectator()) {
                this.getConnection().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.SWAP_HELD_ITEMS, BlockPos.ORIGIN, EnumFacing.DOWN));
            }
        }

        while (this.gameSettings.keyBindDrop.isPressed()) {
            if (!this.player.isSpectator()) {
                this.player.dropItem(GuiScreen.isCtrlKeyDown());
            }
        }

        boolean flag2 = this.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN;

        if (flag2) {
            while (this.gameSettings.keyBindChat.isPressed()) {
                this.displayGuiScreen(new GuiChat());
            }

            if (this.currentScreen == null && this.gameSettings.keyBindCommand.isPressed()) {
                this.displayGuiScreen(new GuiChat("/"));
            }
        }

        if (this.player.isHandActive()) {

            if (!this.gameSettings.keyBindUseItem.isKeyDown()) {
                this.playerController.onStoppedUsingItem(this.player);
            }

            if (this.gameSettings.keyBindAttack.isPressed()) {
                if (OldAnimations.using && OldAnimations.blockHit.getValue()) {
                    ((EntityLivingBase) Minecraft.getMinecraft().player).swingArm(EnumHand.MAIN_HAND);
                }
                return;
            }

            if (this.gameSettings.keyBindUseItem.isPressed()) {
                return;
            }

            if (this.gameSettings.keyBindPickBlock.isPressed()) {
                return;
            }

        } else {
            while (this.gameSettings.keyBindAttack.isPressed()) {
                EventDispatcher.dispatchEvent(new EventMouseClick(0));
                this.clickMouse();
            }

            while (this.gameSettings.keyBindUseItem.isPressed()) {
                EventDispatcher.dispatchEvent(new EventMouseClick(1));
                this.rightClickMouse();
            }

            while (this.gameSettings.keyBindPickBlock.isPressed()) {
                EventDispatcher.dispatchEvent(new EventMouseClick(2));
                this.middleClickMouse();
            }
        }

        if (this.gameSettings.keyBindUseItem.isKeyDown() && this.rightClickDelayTimer == 0 && !this.player.isHandActive()) {
            this.rightClickMouse();
        }

        this.sendClickBlockToController(this.currentScreen == null && this.gameSettings.keyBindAttack.isKeyDown() && this.inGameHasFocus);
    }


    @Shadow
    protected abstract void sendClickBlockToController(boolean leftClick);

    @Shadow
    protected abstract void clickMouse();

    @Shadow
    protected abstract void rightClickMouse();




    @Inject(method = "rightClickMouse", at = @At("HEAD"), cancellable = true)
    public void preventBanning(CallbackInfo ci) {
        if (PreventBanning.using && !PreventBanning.canBlock())
            ci.cancel();
    }

    @Shadow
    protected abstract void middleClickMouse();

    @Shadow private int leftClickCounter;

    @Inject(method="clickMouse",at = @At("RETURN"))
    public void onClickMouse(CallbackInfo ci){
        leftClickCounter = 0;
    }
    @Inject(method = "runTickKeyboard", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;dispatchKeypresses()V", shift = At.Shift.AFTER))
    public void keyEvent(CallbackInfo ci) {
        EventKey key = new EventKey(Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey());
        EventDispatcher.dispatchEvent(key);
    }
    @Inject(method = "createDisplay", at = @At(value = "RETURN"))
    public void setTitle(CallbackInfo ci){
        Display.setTitle(getClientTitle());
    }
}
