package top.fpsmaster.forge.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.events.EventSendChatMessage;
import top.fpsmaster.features.impl.interfaces.BetterScreen;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.render.Render2DUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static top.fpsmaster.utils.Utility.mc;

@Mixin(GuiScreen.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiScreen extends Gui {

    @Shadow
    protected abstract void keyTyped(char typedChar, int keyCode);

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    public abstract void drawBackground(int tint);

    @Unique
    private Map<String, BlockComponent> blockComponents = new HashMap<>();

    @Unique
    private float arch$alpha = 0;

    /**
     * @author SuperSkidder
     * @reason 中文输入
     */
    @Overwrite
    public void handleKeyboardInput() throws IOException {
        char c0 = Keyboard.getEventCharacter();

        if (Keyboard.getEventKey() == 0 && c0 >= ' ' || Keyboard.getEventKeyState()) {
            this.keyTyped(c0, Keyboard.getEventKey());
        }

        mc.dispatchKeypresses();
    }

    /**
     * @author SuperSkidder
     * @reason 自定义背景
     */
    @Overwrite
    public void drawWorldBackground(int tint) {
        if (ProviderManager.mcProvider.getWorld() != null) {
            if (BetterScreen.using) {
                if (BetterScreen.useBG.getValue()) {
                    if (BetterScreen.backgroundAnimation.getValue()) {
                        arch$alpha = (float) AnimationUtils.base(arch$alpha, 170, 0.2f);
                    } else {
                        arch$alpha = 170;
                    }
                    this.drawGradientRect(0, 0, this.width, this.height, Render2DUtils.reAlpha(Render2DUtils.intToColor(-1072689136), ((int) arch$alpha)).getRGB(), Render2DUtils.reAlpha(Render2DUtils.intToColor(-804253680), ((int) arch$alpha)).getRGB());
                } else {
                    GlStateManager.disableBlend();
                    GlStateManager.enableAlpha();
                    GlStateManager.enableTexture2D();
                }
            } else {
                this.drawGradientRect(0, 0, this.width, this.height, -1072689136, -804253680);
            }
        } else {
            this.drawBackground(tint);
        }
    }

    @Inject(method = "sendChatMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    public void sendChat(String msg, boolean addToChat, CallbackInfo ci) {
        EventSendChatMessage eventSendChatMessage = new EventSendChatMessage(msg);
        EventDispatcher.dispatchEvent(eventSendChatMessage);
        if (eventSendChatMessage.isCanceled()) {
            ci.cancel();
        }
    }

    /**
     * @author shaziawa
     * @reason 方块指示器
     */
    @Inject(method = "drawScreen", at = @At("HEAD"))
    public void drawBlockIndicator(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (ProviderManager.mcProvider.getBlockIndicatorEnabled()) {
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            int scaledWidth = sr.getScaledWidth();
            int scaledHeight = sr.getScaledHeight();

            // 绘制方块指示器
            GlStateManager.pushMatrix();
            Render2DUtils.drawImage(new ResourceLocation("client/gui/settings/block_indicator.png"),
                    ProviderManager.mcProvider.getBlockIndicatorX(), ProviderManager.mcProvider.getBlockIndicatorY(), 
                    ProviderManager.mcProvider.getBlockIndicatorSize(), ProviderManager.mcProvider.getBlockIndicatorSize(), -1);
            GlStateManager.popMatrix();

            // 绘制方块信息
            String blockName = "Block Name";
            String version = "Minecraft";
            Minecraft.getMinecraft().fontRendererObj.drawString(blockName, 
                    ProviderManager.mcProvider.getBlockIndicatorX() + ProviderManager.mcProvider.getBlockIndicatorSize() + 5, 
                    ProviderManager.mcProvider.getBlockIndicatorY(), 0xFFFFFF);
            Minecraft.getMinecraft().fontRendererObj.drawString(version, 
                    ProviderManager.mcProvider.getBlockIndicatorX() + ProviderManager.mcProvider.getBlockIndicatorSize() + 5, 
                    ProviderManager.mcProvider.getBlockIndicatorY() + 10, 0xFFFFFF);
        }

        // 绘制方块组件
        for (BlockComponent blockComponent : blockComponents.values()) {
            GlStateManager.pushMatrix();
            Render2DUtils.drawImage(new ResourceLocation("client/gui/settings/block_component.png"),
                    blockComponent.getX(), blockComponent.getY(), 
                    blockComponent.getWidth(), blockComponent.getHeight(), -1);
            GlStateManager.popMatrix();

            Minecraft.getMinecraft().fontRendererObj.drawString(blockComponent.getBlockName(), 
                    blockComponent.getX() + blockComponent.getWidth() + 5, 
                    blockComponent.getY(), 0xFFFFFF);
        }
    }

    /**
     * @author SuperSkidder
     * @reason 添加方块组件
     */
    @Inject(method = "initGui", at = @At("HEAD"))
    public void initBlockComponents(CallbackInfo ci) {
        // 添加示例方块组件
        blockComponents.put("block1", new BlockComponent("Stone", 0, 0, 0, 32, 32));
        blockComponents.put("block2", new BlockComponent("Dirt", 1, 0, 0, 32, 32));
    }

    /**
     * @author SuperSkidder
     * @reason 更新方块组件
     */
    @Inject(method = "updateScreen", at = @At("HEAD"))
    public void updateBlockComponents(CallbackInfo ci) {
        for (BlockComponent blockComponent : blockComponents.values()) {
            blockComponent.update();
        }
    }

    /**
     * @author SuperSkidder
     * @reason 鼠标拖拽方块组件
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"))
    public void onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (mouseButton == 0) { // 左键
            for (BlockComponent blockComponent : blockComponents.values()) {
                if (mouseX >= blockComponent.getX() && mouseX <= blockComponent.getX() + blockComponent.getWidth() &&
                    mouseY >= blockComponent.getY() && mouseY <= blockComponent.getY() + blockComponent.getHeight()) {
                    blockComponent.setDragging(true);
                    blockComponent.setDragOffsetX(mouseX - blockComponent.getX());
                    blockComponent.setDragOffsetY(mouseY - blockComponent.getY());
                }
            }
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    public void onMouseReleased(int mouseX, int mouseY, int state, CallbackInfo ci) {
        for (BlockComponent blockComponent : blockComponents.values()) {
            blockComponent.setDragging(false);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"))
    public void onMouseDragged(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick, CallbackInfo ci) {
        if (mouseButton == 0) { // 左键
            for (BlockComponent blockComponent : blockComponents.values()) {
                if (blockComponent.isDragging()) {
                    blockComponent.setX(mouseX - blockComponent.getDragOffsetX());
                    blockComponent.setY(mouseY - blockComponent.getDragOffsetY());
                }
            }
        }
    }
}

class BlockComponent {
    private String blockName;
    private int x, y, z;
    private int width, height;
    private boolean dragging;
    private int dragOffsetX, dragOffsetY;

    public BlockComponent(String blockName, int x, int y, int z, int width, int height) {
        this.blockName = blockName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
    }

    public void update() {
        // 更新方块信息
        System.out.println("Block: " + blockName + " at (" + x + ", " + y + ", " + z + ")");
    }

    public String getBlockName() {
        return blockName;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    public int getDragOffsetX() {
        return dragOffsetX;
    }

    public void setDragOffsetX(int dragOffsetX) {
        this.dragOffsetX = dragOffsetX;
    }

    public int getDragOffsetY() {
        return dragOffsetY;
    }

    public void setDragOffsetY(int dragOffsetY) {
        this.dragOffsetY = dragOffsetY;
    }
}