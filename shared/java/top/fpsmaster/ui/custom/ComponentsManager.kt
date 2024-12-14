package top.fpsmaster.ui.custom

import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.opengl.GL11
import top.fpsmaster.features.impl.InterfaceModule
import top.fpsmaster.features.impl.interfaces.ClientSettings
import top.fpsmaster.ui.custom.impl.*
import top.fpsmaster.utils.Utility
import java.util.function.Consumer

class ComponentsManager {
    var components = ArrayList<Component>()
    var dragLock = ""

    fun init() {
        components.add(FPSDisplayComponent())
        components.add(ArmorDisplayComponent())
        components.add(MusicComponent())
        components.add(ScoreboardComponent())
        components.add(PotionDisplayComponent())
        components.add(CPSDisplayComponent())
        components.add(KeystrokesComponent())
        components.add(ReachDisplayComponent())
        components.add(ComboDisplayComponent())
        components.add(LyricsComponent())
        components.add(InventoryDisplayComponent())
        components.add(TargetHUDComponent())
        components.add(PlayerDisplayComponent())
        components.add(PingDisplayComponent())
        components.add(CoordsDisplayComponent())
        components.add(ModsListComponent())
        components.add(MiniMapComponent())
    }

    fun getComponent(clazz: Class<out InterfaceModule>): Component {
        return components.stream().filter { component: Component -> component.mod.javaClass == clazz }
            .findFirst().orElse(null)
    }

    fun draw(mouseX: Int, mouseY: Int) {
        GL11.glPushMatrix();
        var mouseX = mouseX
        var mouseY = mouseY
        if (ClientSettings.fixedScale.value){
            val sr = ScaledResolution(Utility.mc)
            var scaleFactor: Int = if (ClientSettings.Companion.fixedScale.value) {
                sr.scaleFactor;
            } else {
                2;
            }
            val guiWidth = sr.scaledWidth / 2f * scaleFactor
            val guiHeight = sr.scaledHeight / 2f * scaleFactor
            mouseX = mouseX * scaleFactor / 2
            mouseY = mouseY * scaleFactor / 2

            GL11.glScaled(2.0 / scaleFactor, 2.0 / scaleFactor, 1.0);
        }
        components.forEach(Consumer { component: Component ->
            if (component.shouldDisplay()) component.display(
                mouseX,
                mouseY
            )
        })
        GL11.glPopMatrix();
    }
}
