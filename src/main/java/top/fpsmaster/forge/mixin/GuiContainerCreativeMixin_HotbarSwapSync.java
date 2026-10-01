package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 创造模式物品栏的热键栏换位只同步了一半：非「物品栏」页里悬停热键栏格按热键栏键（mode 2），
 * 原版走 {@code handleMouseClick} 末尾分支，本地两格都换了，却只给悬停格发
 * {@code sendSlotPacket}——目标热键栏格从没发给服务端，服务端那格还是旧物品，一放置就露馅。
 * 原版用数字键也一样，鼠标侧键绑热键栏后更容易撞上。
 *
 * <p>挂 TAIL：物品格（创造物品表）那支和丢弃等分支都中途 return 不会走到这里；「物品栏」页那支
 * 已经 detectAndSendChanges 过，这里再发一次同值包无害。
 */
@Mixin(GuiContainerCreative.class)
public class GuiContainerCreativeMixin_HotbarSwapSync {

    @Inject(method = "handleMouseClick", at = @At("TAIL"))
    private void edge$syncSwappedHotbarSlot(Slot slotIn, int slotId, int clickedButton, int clickType, CallbackInfo ci) {
        if (clickType != 2 || slotIn == null || clickedButton < 0 || clickedButton >= 9) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        // inventoryContainer 里热键栏 i 是 36 + i。
        mc.playerController.sendSlotPacket(mc.thePlayer.inventory.getStackInSlot(clickedButton), 36 + clickedButton);
    }
}
