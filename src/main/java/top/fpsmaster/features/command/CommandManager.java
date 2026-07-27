package top.fpsmaster.features.command;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.replay.ReplayRecorder;
import top.fpsmaster.ui.PendingScreen;
import top.fpsmaster.ui.screens.replay.ReplayScreen;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventSendChatMessage;
import top.fpsmaster.features.impl.interfaces.BlockIndicator;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.features.impl.interfaces.PlayTime;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.utils.core.Utility;

import java.util.ArrayList;
import java.util.List;

import static top.fpsmaster.utils.core.Utility.mc;

public class CommandManager {

    private final List<Command> commands = new ArrayList<>();

    public void init() {
        // add commands
        commands.add(new Command("blockindicator") {
            @Override
            public void execute(String[] args) {
                setModuleState(BlockIndicator.class, args);
            }
        });
        commands.add(new Command("bi") {
            @Override
            public void execute(String[] args) {
                setModuleState(BlockIndicator.class, args);
            }
        });
        commands.add(new Command("playtime") {
            @Override
            public void execute(String[] args) {
                setModuleState(PlayTime.class, args);
            }
        });
        commands.add(new Command("pt") {
            @Override
            public void execute(String[] args) {
                setModuleState(PlayTime.class, args);
            }
        });
        commands.add(new Command("replay") {
            @Override
            public void execute(String[] args) {
                handleReplay(args);
            }
        });
        EventDispatcher.registerListener(this);
    }

    /**
     * {@code .replay} opens the browser; {@code .replay start [name] | stop | status}
     *
     * <p>A chat command rather than a keybind because recording is a deliberate act with a real
     * cost — it writes to disk for as long as it runs — and should not be one mistyped key away.
     */
    private void handleReplay(String[] args) {
        ReplayRecorder recorder = ReplayRecorder.instance();
        if (args.length == 0) {
            // Next tick, not now: the chat GUI closes itself once the message is handled, and
            // closing a screen sets the current screen to null, which would take this one with it.
            PendingScreen.open(new ReplayScreen(null));
            return;
        }
        String action = args[0].toLowerCase();

        if ("start".equals(action)) {
            if (recorder.isRecording()) {
                Utility.sendClientNotify("Already recording: " + recorder.currentFile().getName());
                return;
            }
            String name = args.length > 1 ? args[1] : "replay-" + System.currentTimeMillis();
            recorder.start(name);
            Utility.sendClientNotify(recorder.isRecording()
                    ? "Recording to " + recorder.currentFile().getName()
                    : "Could not start recording, see the log");
        } else if ("stop".equals(action)) {
            if (!recorder.isRecording()) {
                Utility.sendClientNotify("Not recording");
                return;
            }
            String name = recorder.currentFile().getName();
            long seconds = recorder.elapsedMillis() / 1000L;
            int packets = recorder.packetsRecorded();
            int dropped = recorder.packetsDropped();
            recorder.stop();
            Utility.sendClientNotify("Saved " + name + " - " + seconds + "s, " + packets
                    + " packets, " + recorder.bytesWritten() / 1024L + " KiB"
                    + (dropped > 0 ? ", " + dropped + " dropped" : ""));
        } else {
            Utility.sendClientNotify(recorder.isRecording()
                    ? "Recording " + recorder.currentFile().getName() + " - "
                      + recorder.elapsedMillis() / 1000L + "s, " + recorder.packetsRecorded()
                      + " packets, " + recorder.bytesWritten() / 1024L + " KiB"
                    : "Not recording. Use .replay start [name]");
        }
    }

    @Subscribe
    public void onChat(EventSendChatMessage e) throws Exception {
        String prefix = ClientSettings.prefix.getValue();
        if (ClientSettings.clientCommand.getValue() && e.msg.startsWith(prefix)) {
            e.cancel();
            mc.ingameGUI.getChatGUI().addToSentMessages(e.msg);
            if (e.msg.length() <= prefix.length()) {
                return;
            }
            runCommand(e.msg.substring(prefix.length()));

        }
    }

    private void runCommand(String command) throws Exception {
        String[] args = command.split(" ");
        String cmd = args[0];
        if (args.length == 1) {
            for (Command commandItem : commands) {
                if (commandItem.name.equals(cmd)) {
                    commandItem.execute(new String[]{});
                    return;
                }
            }
            Utility.sendClientMessage(FPSMaster.i18n.get("command.notfound"));
            return;
        }
        String[] cmdArgs = new String[args.length - 1];
        System.arraycopy(args, 1, cmdArgs, 0, cmdArgs.length);
        for (Command commandItem : commands) {
            if (commandItem.name.equals(cmd)) {
                commandItem.execute(cmdArgs);
                return;
            }
        }
        Utility.sendClientMessage(FPSMaster.i18n.get("command.notfound"));
    }

    private void setModuleState(Class<? extends Module> moduleClass, String[] args) {
        if (args.length == 0) {
            toggleModule(moduleClass);
            return;
        }
        Module module = FPSMaster.moduleManager.getModule(moduleClass);
        String action = args[0].toLowerCase();
        if ("on".equals(action) || "enable".equals(action) || "true".equals(action) || "开启".equals(action)) {
            module.set(true);
            sendModuleState(module);
            return;
        }
        if ("off".equals(action) || "disable".equals(action) || "false".equals(action) || "关闭".equals(action)) {
            module.set(false);
            sendModuleState(module);
            return;
        }
        Utility.sendClientMessage(FPSMaster.i18n.get("command.notfound"));
    }

    private void toggleModule(Class<? extends Module> moduleClass) {
        Module module = FPSMaster.moduleManager.getModule(moduleClass);
        module.toggle();
        sendModuleState(module);
    }

    private void sendModuleState(Module module) {
        Utility.sendClientNotify(FPSMaster.i18n.get(module.name.toLowerCase()) + ": " +
                FPSMaster.i18n.get(module.isEnabled() ? "command.module.enabled" : "command.module.disabled"));
    }
}



