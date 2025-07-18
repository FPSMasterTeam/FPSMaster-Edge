package top.fpsmaster.utils;

import net.minecraft.client.Minecraft;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.modules.dev.DevMode;

import java.util.ArrayList;

public class Utility {

    public static Minecraft mc = Minecraft.getMinecraft();

    static ArrayList<String> messages = new ArrayList<>();

    public static void sendChatMessage(String message) {
        if (ProviderManager.mcProvider.getPlayer() == null) return;
        ProviderManager.mcProvider.getPlayer().sendChatMessage(message);
    }

    public static void sendClientMessage(String msg) {
        if (ProviderManager.mcProvider.getWorld() != null) {
            ProviderManager.mcProvider.printChatMessage(ProviderManager.utilityProvider.makeChatComponent(msg));
        } else {
            messages.add(msg);
        }
    }

    public static void sendClientNotify(String msg) {
        String msg1 = "§9[FPSMaster]§r " + msg;
        if (ProviderManager.mcProvider.getWorld() != null) {
            ProviderManager.mcProvider.printChatMessage(ProviderManager.utilityProvider.makeChatComponent(msg1));
        } else {
            messages.add(msg1);
        }
    }

    public static void sendClientDebug(String msg) {
        if (DevMode.INSTACE.dev) {
            String msg1 = "§9[FPSMaster]§r " + msg;
            if (ProviderManager.mcProvider.getWorld() != null) {
                ProviderManager.mcProvider.printChatMessage(ProviderManager.utilityProvider.makeChatComponent(msg1));
            } else {
                messages.add(msg1);
            }
        }
    }

    public static void flush() {
        for (String message : messages) {
            ProviderManager.mcProvider.printChatMessage(ProviderManager.utilityProvider.makeChatComponent(message));
        }
        messages.clear();
    }
}
