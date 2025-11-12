package top.fpsmaster.api.provider.gui;

import net.minecraft.entity.Entity;
import top.fpsmaster.api.provider.IProvider;

public interface IGuiIngameProvider extends IProvider {
    void drawHealth(Entity entityIn);
}