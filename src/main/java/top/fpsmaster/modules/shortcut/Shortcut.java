package top.fpsmaster.modules.shortcut;

import lombok.AllArgsConstructor;
import net.minecraft.client.gui.GuiMultiplayer;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventKey;
import top.fpsmaster.ui.screens.mainmenu.MainMenu;
import top.fpsmaster.utils.core.Utility;

import java.util.ArrayList;

@AllArgsConstructor
public class Shortcut {
    public static final ArrayList<Shortcut> shortcuts = new ArrayList<>();
    public String name;
    public int key;
    public ArrayList<Action> actions;

    @AllArgsConstructor
    public static class Action {
        public Type type;
        public String context;

        public void run() {
            if (type == Type.SEND_MESSAGE) {
                Utility.mc.thePlayer.sendChatMessage(context);
            } else if (type == Type.QUIT_NETWORK) {
                Utility.mc.theWorld.sendQuittingDisconnectingPacket();
                Utility.mc.loadWorld(null);
                Utility.mc.displayGuiScreen(new GuiMultiplayer(new MainMenu()));
            }
        }

        public enum Type {
            SEND_MESSAGE,
            QUIT_NETWORK
        }
    }

    @Subscribe
    public static void onKey(EventKey e) {
        for (Shortcut shortcut : shortcuts) {
            if (e.key == shortcut.key) {
                shortcut.actions.forEach(Action::run);
            }
        }
    }
}
