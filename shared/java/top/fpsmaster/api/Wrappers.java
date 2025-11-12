package top.fpsmaster.api;

import top.fpsmaster.api.wrapper.*;
import top.fpsmaster.api.wrapper.packets.*;
import top.fpsmaster.wrapper.*;
import top.fpsmaster.wrapper.packets.*;
import top.fpsmaster.wrapper.sound.SoundProvider;
import top.fpsmaster.wrapper.SkinProvider;

public final class Wrappers {
    private Wrappers() {}

    public static MinecraftWrap minecraft() { return new MinecraftProvider(); }
    public static WorldWrap world() { return new WorldClientProvider(); }
    public static RenderManagerWrap renderManager() { return new RenderManagerProvider(); }
    public static TimerWrap timer() { return new TimerProvider(); }
    public static GameSettingsWrap gameSettings() { return new GameSettingsProvider(); }
    public static GuiIngameWrap guiIngame() { return new GuiIngameProvider(); }
    public static MainMenuWrap mainMenu() { return new GuiMainMenuProvider(); }
    public static SoundWrap sound() { return new SoundProvider(); }
    public static EffectRendererWrap effects() { return new EffectRendererProvider(); }
    public static SkinWrap skin() { return new SkinProvider(); }

    public static ChatPacketWrap chatPacket() { return new SPacketChatProvider(); }
    public static PlayerListItemWrap playerListPacket() { return new SPacketPlayerListProvider(); }
    public static TimeUpdatePacketWrap timeUpdatePacket() { return new SPacketTimeUpdateProvider(); }
}