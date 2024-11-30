package top.fpsmaster.features.impl.optimizes

import net.minecraft.client.Minecraft
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import top.fpsmaster.event.Subscribe
import top.fpsmaster.event.events.EventTick
import top.fpsmaster.features.manager.Category
import top.fpsmaster.features.manager.Module
import top.fpsmaster.features.settings.impl.BooleanSetting
import top.fpsmaster.features.settings.impl.NumberSetting
import top.fpsmaster.interfaces.ProviderManager

class OldAnimations : Module("OldAnimations", Category.OPTIMIZE) {
    init {
        addSettings(
            noShield,
            animationSneak,
            oldRod,
            oldBow,
            oldSwing,
            oldDamage,
            oldUsing,
            blockHit,
            oldBlock,
            x,
            y,
            z
        )
    }

    override fun onEnable() {
        super.onEnable()
        using = true
    }

    override fun onDisable() {
        super.onDisable()
        using = false
    }

    @Subscribe
    fun onTick(event: EventTick) {
        if (!this.isEnabled)
            return
        lastEyeHeight = eyeHeight

        val player = ProviderManager.mcProvider.getPlayer() ?: run {
            eyeHeight = START_HEIGHT
            return
        }

        when {
            player.isSneaking -> {
                eyeHeight = END_HEIGHT
            }

            !animationSneak.value -> {
                eyeHeight = START_HEIGHT
            }

            eyeHeight < START_HEIGHT -> {
                var delta = START_HEIGHT - eyeHeight
                delta *= 0.4f
                eyeHeight = START_HEIGHT - delta
            }
        }
    }

    companion object {
        @JvmField
        var noShield = BooleanSetting("NoShield", true)

        @JvmField
        var animationSneak = BooleanSetting("AnimationSneak", true)

        @JvmField
        var oldBlock = BooleanSetting("OldBlock", true)

        @JvmField
        var oldRod = BooleanSetting("OldRod", true)

        @JvmField
        var oldBow = BooleanSetting("OldBow", true)

        @JvmField
        var oldSwing = BooleanSetting("OldSwing", true)

        @JvmField
        var oldUsing = BooleanSetting("OldUsing", true)

        @JvmField
        var oldDamage = BooleanSetting("OldDamage", true)

        @JvmField
        var blockHit = BooleanSetting("BlockHit", true)

        @JvmField
        var x = NumberSetting("X", 0, -1, 1, 0.01)

        @JvmField
        var y = NumberSetting("Y", 0, -1, 1, 0.01)

        @JvmField
        var z = NumberSetting("Z", 0, -1, 1, 0.01)

        @JvmField
        var using = false


        var eyeHeight: Float = 0f
        var lastEyeHeight: Float = 0f
        private const val START_HEIGHT = 1.62f
        private const val END_HEIGHT = 1.54f

        fun getClientEyeHeight(partialTicks: Float): Float {
            if (!animationSneak.value) {
                return eyeHeight
            }

            return lastEyeHeight + (eyeHeight - lastEyeHeight) * partialTicks
        }
    }
}
