package top.fpsmaster.ui.custom.impl

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import top.fpsmaster.FPSMaster
import top.fpsmaster.features.impl.interfaces.ArmorDisplay
import top.fpsmaster.ui.custom.Component
import top.fpsmaster.utils.Utility
import top.fpsmaster.interfaces.ProviderManager
import java.awt.Color

class ArmorDisplayComponent : Component(ArmorDisplay::class.java) {
    override fun draw(x: Float, y: Float) {
        super.draw(x, y)
        val armorInventory = ProviderManager.mcProvider.getArmorInventory()
        for (i in armorInventory.indices) {
            var itemStack = armorInventory[i]
            var x1 = (x + i * 18).toInt()
            var y1 = y.toInt()
            when (ArmorDisplay.mode.mode) {
                0 -> {itemStack = armorInventory[armorInventory.size - 1 - i]}
                1, 2 -> {
                    itemStack = armorInventory[armorInventory.size - 1 - i]
                    x1 = x.toInt()
                    y1 = y.toInt() + i * 18
                }
            }
            drawRect(x1.toFloat(), y1.toFloat(), 16f, 16f, mod.backgroundColor.color)
            if (itemStack == null) continue
            RenderHelper.enableGUIStandardItemLighting()
            Utility.mc.renderItem.renderItemAndEffectIntoGUI(itemStack, x1, y1)
            Utility.mc.renderItem.renderItemOverlays(ProviderManager.mcProvider.getFontRenderer(), itemStack, x1, y1)
            RenderHelper.disableStandardItemLighting()
            GlStateManager.enableAlpha()
            GlStateManager.disableCull()
            GlStateManager.disableBlend()
            GlStateManager.disableLighting()
            GlStateManager.clear(256)
            if (ArmorDisplay.mode.mode == 2) {
                // draw durability
                val durability = itemStack.maxDamage - itemStack.itemDamage
                val dura = durability.toFloat() / itemStack.maxDamage
                var color = -1
                if (dura < 0.5) color = if (dura < 0.2) Color(255, 20, 20).rgb else Color(255, 255, 20).rgb
                val durabilityString =
                    if (durability > 0) durability.toString() + "/" + itemStack.maxDamage else "0/" + itemStack.maxDamage
                drawRect(
                    (x1 + 18).toFloat(),
                    y1.toFloat(),
                    getStringWidth(16, durabilityString) + 4,
                    16f,
                    mod.backgroundColor.color
                )
                drawString(16, durabilityString, (x1 + 20).toFloat(), (y1 + 2).toFloat(), color)
            }
        }
        when (ArmorDisplay.mode.mode) {
            0 -> {
                width = 70f
                height = 18f
            }

            1 -> {
                width = 18f
                height = (4 + armorInventory.size * 16).toFloat()
            }

            2 -> {
                width = 70f
                height = (4 + armorInventory.size * 16).toFloat()
            }
        }
    }
}
