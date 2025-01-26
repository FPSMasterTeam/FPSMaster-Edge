package top.fpsmaster.ui.custom.impl

import top.fpsmaster.FPSMaster
import top.fpsmaster.features.impl.interfaces.CPSDisplay
import top.fpsmaster.ui.custom.Component
import top.fpsmaster.wrapper.TextFormattingProvider

class CPSDisplayComponent : Component(CPSDisplay::class.java) {
    init {
        x = 0.05f
        y = 0.05f
        allowScale = true

    }

    override fun draw(x: Float, y: Float) {
        super.draw(x, y)
        val text =
            "CPS: " + CPSDisplay.lcps + TextFormattingProvider.getGray() + " | " + TextFormattingProvider.getReset() + CPSDisplay.rcps
        width = getStringWidth(16, text) + 4
        height = 14f
        drawRect(x - 2, y, width, height, mod.backgroundColor.color)
        drawString(16, text, x, y + 2, CPSDisplay.textColor.rGB)
    }
}
