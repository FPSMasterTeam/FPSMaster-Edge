package top.fpsmaster.features.command;


import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventSendChatMessage;
import top.fpsmaster.features.impl.interfaces.BlockIndicator;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.features.impl.interfaces.PlayTime;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.Setting;
import top.fpsmaster.features.settings.impl.BindSetting;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.modules.client.api.AuthService;
import top.fpsmaster.modules.client.api.FPSMasterApiClient;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.modules.shortcut.Shortcut;
import top.fpsmaster.replay.ReplayRecorder;
import top.fpsmaster.ui.PendingScreen;
import top.fpsmaster.ui.screens.replay.ReplayScreen;
import top.fpsmaster.utils.core.Utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static top.fpsmaster.utils.core.Utility.mc;

public class CommandManager {

    private final List<Command> commands = new ArrayList<Command>();

    public void init() {
        commands.clear();
        commands.add(new Command("toggle", "toggle <module>", "t") {
            public void execute(String[] args) throws CommandException {
                require(args.length == 1, "toggle <module>");
                Module module = requireModule(args[0]);
                module.toggle();
                save();
                sendModuleState(module);
            }
        });
        commands.add(new Command("set", "set <module> <setting> <value>", "s") {
            public void execute(String[] args) throws CommandException {
                require(args.length >= 3, "set <module> <setting> <value>");
                applySetting(requireModule(args[0]), args[1], joinFrom(args, 2));
            }
        });
        commands.add(new Command("bind", "bind <module> <key|none>", "b") {
            public void execute(String[] args) throws CommandException {
                require(args.length == 2, "bind <module> <key|none>");
                Module module = requireModule(args[0]);
                Integer key = CommandKeys.parse(args[1]);
                if (key == null) {
                    throw new CommandException("未知按键: " + args[1]);
                }
                module.key = key;
                save();
                Utility.sendClientNotify(module.name + " 绑定到 " + CommandKeys.format(key));
            }
        });
        commands.add(new Command("config", "config <save|load|list> [name]", "cfg") {
            public void execute(String[] args) throws CommandException {
                handleConfig(args);
            }
        });
        commands.add(new Command("help", "help [command]", "h", "?") {
            public void execute(String[] args) {
                if (args.length == 0) {
                    StringBuilder names = new StringBuilder();
                    for (Command command : commands) {
                        if (names.length() > 0) {
                            names.append(", ");
                        }
                        names.append(command.name);
                    }
                    Utility.sendClientNotify("命令: " + names);
                    return;
                }
                for (Command command : commands) {
                    if (command.matches(args[0])) {
                        Utility.sendClientNotify("用法: " + command.usage);
                        return;
                    }
                }
                Utility.sendClientNotify("未知命令: " + args[0]);
            }
        });
        commands.add(new Command("auth", "auth <status|login|logout> [user] [pass]", "account") {
            public void execute(String[] args) throws CommandException {
                handleAuth(args);
            }
        });
        commands.add(new Command("shortcut", "shortcut <list>", "sc") {
            public void execute(String[] args) throws CommandException {
                if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
                    if (Shortcut.shortcuts.isEmpty()) {
                        throw new CommandException("没有快捷键");
                    }
                    for (Shortcut shortcut : Shortcut.shortcuts) {
                        Utility.sendClientNotify(shortcut.name + " = " + CommandKeys.format(shortcut.key));
                    }
                    return;
                }
                throw new CommandException("用法: shortcut list");
            }
        });
        commands.add(new Command("telemetry", "telemetry <status>", "presence") {
            public void execute(String[] args) {
                Utility.sendClientNotify("匿名数据: "
                        + (FPSMaster.configManager.configure.anonymousDataEnabled ? "开启" : "关闭"));
            }
        });
        commands.add(new Command("replay", "replay [start [name]|stop|status]") {
            public void execute(String[] args) {
                handleReplay(args);
            }
        });
        commands.add(new Command("blockindicator", "blockindicator [on|off]", "bi") {
            public void execute(String[] args) throws CommandException {
                setModuleState(BlockIndicator.class, args);
            }
        });
        commands.add(new Command("playtime", "playtime [on|off]", "pt") {
            public void execute(String[] args) throws CommandException {
                setModuleState(PlayTime.class, args);
            }
        });
        EventDispatcher.registerListener(this);
    }

    @Subscribe
    public void onChat(EventSendChatMessage e) {
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

    private void runCommand(String input) {
        String[] parts = input.trim().split("\\s+");
        String name = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        for (Command command : commands) {
            if (command.matches(name)) {
                try {
                    command.execute(args);
                } catch (CommandException failure) {
                    Utility.sendClientNotify("§c" + failure.getMessage());
                } catch (Exception failure) {
                    Utility.sendClientNotify("§c命令失败: " + failure.getMessage());
                }
                return;
            }
        }
        Utility.sendClientMessage(FPSMaster.i18n.get("command.notfound"));
    }

    private static void require(boolean ok, String usage) throws CommandException {
        if (!ok) {
            throw new CommandException("用法: " + usage);
        }
    }

    private static Module requireModule(String raw) throws CommandException {
        Module module = FPSMaster.moduleManager.findByName(raw);
        if (module == null) {
            throw new CommandException("模块不存在: " + raw);
        }
        return module;
    }

    private static void save() {
        ConfigProfileUtils.saveActiveProfileQuietly();
    }

    private static String joinFrom(String[] args, int index) {
        StringBuilder joined = new StringBuilder();
        for (int i = index; i < args.length; i++) {
            if (joined.length() > 0) {
                joined.append(' ');
            }
            joined.append(args[i]);
        }
        return joined.toString();
    }

    private static void applySetting(Module module, String settingName, String raw) throws CommandException {
        Setting<?> found = null;
        String wanted = settingName.replace("-", "").toLowerCase(Locale.ROOT);
        for (Setting<?> setting : module.settings) {
            if (setting.name.replace("-", "").equalsIgnoreCase(wanted) || setting.name.equalsIgnoreCase(settingName)) {
                found = setting;
                break;
            }
        }
        if (found == null) {
            throw new CommandException("选项不存在: " + module.name + "." + settingName);
        }
        if (found instanceof BooleanSetting) {
            ((BooleanSetting) found).setValue(parseBoolean(raw));
        } else if (found instanceof NumberSetting) {
            NumberSetting number = (NumberSetting) found;
            double parsed;
            try {
                parsed = Double.parseDouble(raw);
            } catch (NumberFormatException ignored) {
                throw new CommandException("数字无效: " + raw);
            }
            if (parsed < number.min.doubleValue() || parsed > number.max.doubleValue()) {
                throw new CommandException("数字越界: " + raw + " 允许 "
                        + number.min + ".." + number.max);
            }
            number.setValue(parsed);
        } else if (found instanceof ModeSetting) {
            ModeSetting mode = (ModeSetting) found;
            int index = -1;
            for (int i = 0; i < mode.getModesSize(); i++) {
                if (mode.getMode(i + 1).equalsIgnoreCase(raw)) {
                    index = i;
                    break;
                }
            }
            if (index < 0) {
                try {
                    index = Integer.parseInt(raw);
                } catch (NumberFormatException ignored) {
                    throw new CommandException("非法选项: " + raw);
                }
                if (index < 0 || index >= mode.getModesSize()) {
                    throw new CommandException("非法选项: " + raw);
                }
            }
            mode.setValue(index);
        } else if (found instanceof TextSetting) {
            ((TextSetting) found).setValue(raw);
        } else if (found instanceof BindSetting) {
            Integer key = CommandKeys.parse(raw);
            if (key == null) {
                throw new CommandException("未知按键: " + raw);
            }
            ((BindSetting) found).setValue(key);
        } else {
            throw new CommandException("该选项不能用命令设置: " + found.name);
        }
        save();
        Utility.sendClientNotify(module.name + "." + found.name + " = " + raw);
    }

    private static boolean parseBoolean(String raw) throws CommandException {
        if ("true".equalsIgnoreCase(raw) || "on".equalsIgnoreCase(raw) || "1".equals(raw) || "开启".equals(raw)) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw) || "off".equalsIgnoreCase(raw) || "0".equals(raw) || "关闭".equals(raw)) {
            return false;
        }
        throw new CommandException("非法布尔值: " + raw);
    }

    private void handleConfig(String[] args) throws CommandException {
        require(args.length >= 1, "config <save|load|list> [name]");
        String action = args[0].toLowerCase(Locale.ROOT);
        try {
            if ("list".equals(action)) {
                Utility.sendClientNotify("当前: " + ConfigProfileUtils.getActiveProfileName());
                return;
            }
            if ("save".equals(action)) {
                String name = args.length > 1 ? args[1] : ConfigProfileUtils.getActiveProfileName();
                FPSMaster.configManager.saveConfig(name);
                Utility.sendClientNotify("已保存 " + name);
                return;
            }
            if ("load".equals(action)) {
                require(args.length == 2, "config load <name>");
                FPSMaster.configManager.loadConfig(args[1]);
                Utility.sendClientNotify("已加载 " + args[1]);
                return;
            }
        } catch (CommandException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new CommandException("配置失败: " + failure.getMessage());
        }
        throw new CommandException("用法: config <save|load|list> [name]");
    }

    private void handleAuth(String[] args) throws CommandException {
        require(args.length >= 1, "auth <status|login|logout> [user] [pass]");
        String action = args[0].toLowerCase(Locale.ROOT);
        AuthService auth = AuthService.getInstance();
        if ("status".equals(action)) {
            Utility.sendClientNotify(auth.isLoggedIn() ? "已登录" : "未登录");
            return;
        }
        if ("logout".equals(action)) {
            auth.clearTokens();
            Utility.sendClientNotify("已退出登录");
            return;
        }
        if ("login".equals(action)) {
            require(args.length >= 3, "auth login <user> <pass>");
            new FPSMasterApiClient().login(args[1], args[2], response -> {
                if (response != null && response.isSuccess() && response.getData() != null
                        && response.getData().getToken() != null) {
                    auth.saveTokens(response.getData().getToken(), null);
                    Utility.sendClientNotify("登录成功");
                } else {
                    Utility.sendClientNotify("§c登录失败");
                }
            });
            return;
        }
        throw new CommandException("用法: auth <status|login|logout>");
    }

    private void handleReplay(String[] args) {
        ReplayRecorder recorder = ReplayRecorder.instance();
        if (args.length == 0) {
            PendingScreen.open(new ReplayScreen(null));
            return;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
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

    private void setModuleState(Class<? extends Module> moduleClass, String[] args) throws CommandException {
        Module module = FPSMaster.moduleManager.getModule(moduleClass);
        if (args.length == 0) {
            module.toggle();
            save();
            sendModuleState(module);
            return;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if ("on".equals(action) || "enable".equals(action) || "true".equals(action) || "开启".equals(action)) {
            module.set(true);
        } else if ("off".equals(action) || "disable".equals(action) || "false".equals(action) || "关闭".equals(action)) {
            module.set(false);
        } else {
            throw new CommandException("用法: on|off");
        }
        save();
        sendModuleState(module);
    }

    private void sendModuleState(Module module) {
        Utility.sendClientNotify(FPSMaster.i18n.get(module.name.toLowerCase()) + ": "
                + FPSMaster.i18n.get(module.isEnabled() ? "command.module.enabled" : "command.module.disabled"));
    }
}
