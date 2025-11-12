package top.fpsmaster.features.impl.utility;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.network.login.server.S02PacketLoginSuccess;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.world.World;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventPacket;
import top.fpsmaster.event.events.EventTick;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.api.ProviderManager;
import top.fpsmaster.modules.account.AccountManager;

import static top.fpsmaster.utils.Utility.mc;

public class SkinChanger extends Module {

    private final TextSetting skinName = new TextSetting("Skin", "");
    private Thread updateThread = new Thread(() -> {
        while (true) {
            update();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    });

    public static boolean using = false;
    private WorldClient world;

    public SkinChanger() {
        super("SkinChanger", Category.Utility);
        addSettings(skinName);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
        if (ProviderManager.mcProvider.getPlayer() != null) {
            if (!updateThread.isAlive()) {
                updateThread = new Thread(this::update);
                updateThread.start();
            }
        }
    }


    @Subscribe
    public void onTick(EventTick e) {
        if (ProviderManager.mcProvider.getPlayer() != null && ProviderManager.mcProvider.getPlayer().ticksExisted % 30 == 0) {
            if (AccountManager.skin.equals(skinName.getValue()) && world == mc.theWorld)
                return;
            world = mc.theWorld;
            AccountManager.skin = skinName.getValue();
            FPSMaster.async.runnable(this::update);
        }
    }

    public void update() {
        ProviderManager.skinProvider.updateSkin(
                ProviderManager.mcProvider.getPlayer().getName(),
                ProviderManager.mcProvider.getPlayer().getUniqueID().toString(),
                skinName.getValue()
        );
    }

    @Override
    public void onDisable() {
        super.onDisable();
        FPSMaster.async.runnable(() -> ProviderManager.skinProvider.updateSkin(
                ProviderManager.mcProvider.getPlayer().getName(),
                ProviderManager.mcProvider.getPlayer().getUniqueID().toString(),
                ProviderManager.mcProvider.getPlayer().getName()
        ));
        using = false;
    }
}