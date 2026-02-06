package top.fpsmaster.features.command.impl;

import net.minecraft.client.Minecraft;
import top.fpsmaster.features.command.Command;
import top.fpsmaster.modules.dev.DevMode;
import top.fpsmaster.modules.lua.LuaManager;
import top.fpsmaster.ui.devspace.DevSpace;
import top.fpsmaster.utils.Utility;

public class Dev extends Command {

    public Dev() {
        super("dev");
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length == 0) {
            DevMode.INSTACE.setDev(!DevMode.INSTACE.dev);
            Utility.sendClientNotify("Dev mode is now " + (DevMode.INSTACE.dev ? "enabled" : "disabled"));
        } else if (args.length == 1 && DevMode.INSTACE.dev) {
            switch (args[0]) {
                case "hotswap":
                    DevMode.INSTACE.setHotswap(!DevMode.INSTACE.hotswap);
                    Utility.sendClientNotify("Hotswap is now " + (DevMode.INSTACE.hotswap ? "enabled" : "disabled"));
                    break;
                case "reload":
                    LuaManager.reload();
                    Utility.sendClientNotify(LuaManager.scripts.size() + " scripts reloaded");
                    break;
                case "list":
                    Utility.sendClientNotify("Scripts:");
                    LuaManager.scripts.forEach(script -> Utility.sendClientNotify(script.rawLua.filename));
                    break;
                case "ide":
                    Minecraft.getMinecraft().addScheduledTask(() -> Minecraft.getMinecraft().displayGuiScreen(new DevSpace()));
                    break;
                default:
                    Utility.sendClientNotify("Unknown command: " + args[0]);
                    break;
            }
        }
    }
}
