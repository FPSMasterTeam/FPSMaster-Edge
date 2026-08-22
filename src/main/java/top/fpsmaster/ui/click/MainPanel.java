package top.fpsmaster.ui.click;

import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.Setting;
import top.fpsmaster.features.settings.impl.AutoTextEntry;
import top.fpsmaster.features.settings.impl.AutoTextSetting;
import top.fpsmaster.features.settings.impl.BindSetting;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.MultipleItemSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.features.settings.impl.utils.CustomColor;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.prism.screen.ClickGuiBridge;
import top.fpsmaster.prism.screen.SharedClickGui;
import top.fpsmaster.ui.kit.EdgeUi;
import top.fpsmaster.ui.hud.HudEditorScreen;
import top.fpsmaster.ui.screens.music.MusicScreen;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Edge host for the shared Prism ClickGUI. */
public class MainPanel extends ScaledGuiScreen {
    private final SharedClickGui gui = new SharedClickGui("optimize");
    private final ClickGuiBridge bridge = new EdgeClickGuiBridge();
    private boolean configSavedOnClose;

    @Override
    public void initGui() {
        super.initGui();
        configSavedOnClose = false;
        gui.onOpen();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (gui.draw(EdgeUi.frame(), bridge)) {
            mc.displayGuiScreen(null);
            if (mc.currentScreen == null) mc.setIngameFocus();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (!gui.cancelKeyCapture()) {
                saveConfigOnClose();
                gui.beginClose();
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        saveConfigOnClose();
        super.onGuiClosed();
    }

    /** Screenshot-pipeline hook for showing one expanded module. */
    public void showModuleForShot(String moduleName) {
        Module module = findModule(moduleName);
        if (module == null) return;
        gui.category = categoryId(module.category);
        gui.expandedId = module.name;
        gui.search.setText(moduleLabel(module));
    }

    private void saveConfigOnClose() {
        if (configSavedOnClose) return;
        try {
            FPSMaster.configManager.saveConfig(ConfigProfileUtils.getActiveProfileName());
            configSavedOnClose = true;
        } catch (FileException e) {
            ClientLogger.error("Failed to save config when closing MainPanel: " + e.getMessage());
        }
    }

    private final class EdgeClickGuiBridge implements ClickGuiBridge {
        @Override public String i18n(String key) { return FPSMaster.i18n.get(key); }
        @Override public String edition() { return FPSMaster.EDITION; }
        @Override public String version() { return FPSMaster.CLIENT_VERSION; }
        @Override public List<String> categories() { return Arrays.asList("optimize", "render", "utility", "interface"); }
        @Override public String categoryLabel(String id) { return i18n("category." + id); }

        @Override
        public String categoryIcon(String id) {
            if ("optimize".equals(id)) return "zap";
            if ("render".equals(id)) return "sparkles";
            if ("utility".equals(id)) return "wrench";
            if ("interface".equals(id)) return "grid";
            return "box";
        }

        @Override public int moduleCount(String categoryId) { return modulesOf(categoryId).size(); }

        @Override
        public int enabledCount(String categoryId) {
            int count = 0;
            for (Module module : modulesOf(categoryId)) if (module.isEnabled()) count++;
            return count;
        }

        @Override
        public List<ModInfo> modules(String categoryId, String query) {
            List<ModInfo> result = new ArrayList<ModInfo>();
            for (Module module : FPSMaster.moduleManager.modules) {
                boolean show = query == null || query.trim().isEmpty()
                        ? categoryId.equals(MainPanel.categoryId(module.category))
                        : matchesSearch(module, query);
                if (show) result.add(toInfo(module));
            }
            return result;
        }

        @Override public void toggle(String moduleId) { Module module = findModule(moduleId); if (module != null) module.toggle(); }
        @Override public void setModuleKey(String moduleId, int keyCode) { Module module = findModule(moduleId); if (module != null) module.key = normalizeKey(keyCode); }

        @Override
        public void setNumber(String moduleId, String settingId, double value) {
            Setting<?> setting = findSetting(moduleId, settingId);
            if (setting instanceof NumberSetting) ((NumberSetting) setting).setValue(value);
        }

        @Override
        public void setBool(String moduleId, String settingId, boolean value) {
            Setting<?> setting = findSetting(moduleId, settingId);
            if (setting instanceof BooleanSetting) ((BooleanSetting) setting).setValue(value);
        }

        @Override
        public void setText(String moduleId, String settingId, String value) {
            Setting<?> setting = findSetting(moduleId, settingId);
            if (setting instanceof TextSetting) ((TextSetting) setting).setValue(value);
        }

        @Override
        public void setChoice(String moduleId, String settingId, int index) {
            Setting<?> setting = findSetting(moduleId, settingId);
            if (setting instanceof ModeSetting) ((ModeSetting) setting).setValue(index);
        }

        @Override
        public void setColor(String moduleId, String settingId, float hue, float saturation,
                             float brightness, float alpha, String mode) {
            Setting<?> setting = findSetting(moduleId, settingId);
            if (!(setting instanceof ColorSetting)) return;
            ColorSetting color = (ColorSetting) setting;
            try {
                color.setColorType(ColorSetting.ColorType.valueOf(mode));
            } catch (IllegalArgumentException ignored) {
                ClientLogger.warn("Unknown color mode: " + mode);
            }
            color.setColor(hue, saturation, brightness, alpha);
        }

        @Override
        public void setKey(String moduleId, String settingId, int keyCode) {
            Setting<?> setting = findSetting(moduleId, settingId);
            if (setting instanceof BindSetting) ((BindSetting) setting).setValue(normalizeKey(keyCode));
        }

        @Override
        public void addListItem(String moduleId, String settingId) {
            Setting<?> setting = findSetting(moduleId, settingId);
            if (setting instanceof MultipleItemSetting) {
                if (mc.thePlayer != null) {
                    ItemStack held = mc.thePlayer.getHeldItem();
                    if (held != null) ((MultipleItemSetting) setting).addItemAndNotify(held);
                }
            } else if (setting instanceof AutoTextSetting) {
                ((AutoTextSetting) setting).addEntry(new AutoTextEntry(0, ""));
            }
        }

        @Override
        public void removeListItem(String moduleId, String settingId, int index) {
            Setting<?> setting = findSetting(moduleId, settingId);
            if (setting instanceof MultipleItemSetting) ((MultipleItemSetting) setting).removeItemAndNotify(index);
            else if (setting instanceof AutoTextSetting) ((AutoTextSetting) setting).removeEntry(index);
        }

        @Override
        public void setListItemText(String moduleId, String settingId, int index, String value) {
            Setting<?> setting = findSetting(moduleId, settingId);
            if (!(setting instanceof AutoTextSetting)) return;
            AutoTextSetting autoText = (AutoTextSetting) setting;
            if (index >= 0 && index < autoText.getValue().size()) {
                AutoTextEntry current = autoText.getValue().get(index);
                autoText.editEntry(index, new AutoTextEntry(current.keyCode, value));
            }
        }

        @Override
        public void setListItemKey(String moduleId, String settingId, int index, int keyCode) {
            Setting<?> setting = findSetting(moduleId, settingId);
            if (!(setting instanceof AutoTextSetting)) return;
            AutoTextSetting autoText = (AutoTextSetting) setting;
            int normalized = normalizeKey(keyCode);
            if (index < 0 || index >= autoText.getValue().size()) return;
            for (int i = 0; i < autoText.getValue().size(); i++) {
                if (i != index && normalized != 0 && autoText.getValue().get(i).keyCode == normalized) return;
            }
            AutoTextEntry current = autoText.getValue().get(index);
            autoText.editEntry(index, new AutoTextEntry(normalized, current.message));
        }

        @Override public boolean lightTheme() { return ClientSettings.theme.getValue() == 1; }
        @Override public void toggleTheme() { ClientSettings.theme.setValue(lightTheme() ? 0 : 1); }
        @Override public void openMusic() { mc.displayGuiScreen(new MusicScreen(MainPanel.this)); }
        @Override public void openProfiles() { mc.displayGuiScreen(new ConfigProfilesScreen(MainPanel.this)); }
        @Override public void openHudEditor() { mc.displayGuiScreen(new HudEditorScreen()); }
    }

    private ClickGuiBridge.ModInfo toInfo(Module module) {
        List<ClickGuiBridge.SettingInfo> settings = new ArrayList<ClickGuiBridge.SettingInfo>();
        for (Setting<?> setting : module.settings) {
            if (setting.getVisible()) {
                ClickGuiBridge.SettingInfo info = toInfo(module, setting);
                if (info != null) settings.add(info);
            }
        }
        return new ClickGuiBridge.ModInfo(module.name, moduleLabel(module), module.isEnabled(),
                !"ClientSettings".equals(module.name), settings, module.key, keyName(module.key));
    }

    private ClickGuiBridge.SettingInfo toInfo(Module module, Setting<?> setting) {
        String label = settingLabel(module, setting);
        if (setting instanceof BooleanSetting) return new ClickGuiBridge.SettingInfo(setting.name, label, ((BooleanSetting) setting).getValue());
        if (setting instanceof NumberSetting) {
            NumberSetting number = (NumberSetting) setting;
            return new ClickGuiBridge.SettingInfo(setting.name, label, number.getValue().doubleValue(), number.min.doubleValue(), number.max.doubleValue());
        }
        if (setting instanceof TextSetting) return new ClickGuiBridge.SettingInfo(setting.name, label, ((TextSetting) setting).getValue());
        if (setting instanceof ModeSetting) {
            ModeSetting mode = (ModeSetting) setting;
            List<String> names = new ArrayList<String>();
            for (int i = 0; i < mode.getModesSize(); i++) {
                String raw = mode.getMode(i + 1);
                String key = (module.name + "." + setting.name + "." + raw).toLowerCase(Locale.getDefault());
                String translated = FPSMaster.i18n.get(key);
                names.add(key.equals(translated) ? raw : translated);
            }
            return new ClickGuiBridge.SettingInfo(setting.name, label, names, mode.getMode());
        }
        if (setting instanceof ColorSetting) {
            ColorSetting color = (ColorSetting) setting;
            CustomColor value = color.getValue();
            List<String> modes = new ArrayList<String>();
            for (ColorSetting.ColorType type : color.getAvailableTypes()) modes.add(type.name());
            return new ClickGuiBridge.SettingInfo(setting.name, label, value.hue, value.saturation, value.brightness, value.alpha, color.getColorType().name(), modes);
        }
        if (setting instanceof BindSetting) {
            int key = ((BindSetting) setting).getValue();
            return new ClickGuiBridge.SettingInfo(setting.name, label, key, keyName(key));
        }
        if (setting instanceof MultipleItemSetting) {
            List<ClickGuiBridge.ListItem> items = new ArrayList<ClickGuiBridge.ListItem>();
            for (ItemStack item : ((MultipleItemSetting) setting).getValue()) items.add(new ClickGuiBridge.ListItem(item.getDisplayName()));
            return new ClickGuiBridge.SettingInfo(setting.name, label, items, MultipleItemSetting.MAX_CAPACITY, false);
        }
        if (setting instanceof AutoTextSetting) {
            List<ClickGuiBridge.ListItem> items = new ArrayList<ClickGuiBridge.ListItem>();
            for (AutoTextEntry entry : ((AutoTextSetting) setting).getValue()) items.add(new ClickGuiBridge.ListItem(entry.message, entry.keyCode, keyName(entry.keyCode)));
            return new ClickGuiBridge.SettingInfo(setting.name, label, items, AutoTextSetting.MAX_CAPACITY, true);
        }
        return null;
    }

    private static List<Module> modulesOf(String categoryId) {
        if (FPSMaster.moduleManager == null) return Collections.emptyList();
        List<Module> result = new ArrayList<Module>();
        for (Module module : FPSMaster.moduleManager.modules) if (categoryId.equals(categoryId(module.category))) result.add(module);
        return result;
    }

    private static Module findModule(String id) {
        if (FPSMaster.moduleManager == null || id == null) return null;
        for (Module module : FPSMaster.moduleManager.modules) if (module.name.equalsIgnoreCase(id)) return module;
        return null;
    }

    private static Setting<?> findSetting(String moduleId, String settingId) {
        Module module = findModule(moduleId);
        if (module == null) return null;
        for (Setting<?> setting : module.settings) if (setting.name.equals(settingId)) return setting;
        return null;
    }

    private static String categoryId(Category category) {
        if (category == Category.OPTIMIZE) return "optimize";
        if (category == Category.RENDER) return "render";
        if (category == Category.Utility) return "utility";
        return "interface";
    }

    private static String moduleLabel(Module module) {
        String key = module.name.toLowerCase(Locale.getDefault());
        String translated = FPSMaster.i18n.get(key);
        return key.equals(translated) ? module.name : translated;
    }

    private static String settingLabel(Module module, Setting<?> setting) {
        String key = (module.name + "." + setting.name).toLowerCase(Locale.getDefault());
        String translated = FPSMaster.i18n.get(key);
        return key.equals(translated) ? setting.name : translated;
    }

    private static boolean matchesSearch(Module module, String rawQuery) {
        String query = rawQuery.trim().toLowerCase(Locale.getDefault());
        String name = moduleLabel(module).toLowerCase(Locale.getDefault());
        String desc = FPSMaster.i18n.get(module.name.toLowerCase(Locale.getDefault()) + ".desc").toLowerCase(Locale.getDefault());
        return name.contains(query) || desc.contains(query)
                || (ClientSettings.language.getValue() != 1 && module.name.toLowerCase(Locale.getDefault()).contains(query));
    }

    private static String keyName(int keyCode) {
        String name = keyCode == 0 ? null : Keyboard.getKeyName(keyCode);
        return name == null || name.isEmpty() ? "None" : name;
    }

    private static int normalizeKey(int keyCode) {
        return keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE ? 0 : keyCode;
    }
}
