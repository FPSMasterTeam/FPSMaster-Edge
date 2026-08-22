package top.fpsmaster.ui.click;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.modules.config.ConfigProfileUtils.ConfigProfile;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.prism.screen.ConfigProfilesBridge;
import top.fpsmaster.prism.screen.SharedConfigProfiles;
import top.fpsmaster.ui.kit.EdgeUi;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Edge host for the shared Prism config-profile screen. */
public class ConfigProfilesScreen extends ScaledGuiScreen {
    private final ScaledGuiScreen parent;
    private final SharedConfigProfiles profiles = new SharedConfigProfiles();
    private final ConfigProfilesBridge bridge = new EdgeConfigProfilesBridge();

    public ConfigProfilesScreen(ScaledGuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (profiles.draw(EdgeUi.frame(), bridge)) mc.displayGuiScreen(parent);
    }

    private final class EdgeConfigProfilesBridge implements ConfigProfilesBridge {
        @Override public String i18n(String key) { return FPSMaster.i18n.get(key); }
        @Override public String activeName() { return ConfigProfileUtils.getActiveProfileName(); }

        @Override
        public List<Profile> profiles() {
            List<Profile> result = new ArrayList<Profile>();
            for (ConfigProfile profile : ConfigProfileUtils.listConfigs()) {
                File file = profile.getFile();
                result.add(new Profile(profile.getName(), file.lastModified(), file.length()));
            }
            return result;
        }

        @Override
        public int enabledModules() {
            int count = 0;
            for (Module module : FPSMaster.moduleManager.modules) {
                if (module.isEnabled() && !"ClientSettings".equals(module.name)) count++;
            }
            return count;
        }

        @Override
        public int hudModules() {
            int count = 0;
            for (Module module : FPSMaster.moduleManager.modules) {
                if (module instanceof InterfaceModule && module.isEnabled()) count++;
            }
            return count;
        }

        @Override public long activeBytes() { File file = activeFile(); return file == null ? 0L : file.length(); }
        @Override public long activeModified() { File file = activeFile(); return file == null ? 0L : file.lastModified(); }
        @Override public boolean isDefault(String name) { return ConfigProfileUtils.CURRENT_CONFIG.equals(name); }

        @Override
        public String load(String name) {
            try {
                ConfigProfileUtils.loadProfile(name);
                return String.format(i18n("configprofiles.status.loaded"), name);
            } catch (Exception e) {
                return failure("load", "configprofiles.status.load_failed", e);
            }
        }

        @Override
        public String delete(String name) {
            try {
                ConfigProfileUtils.deleteProfile(name);
                if (name.equals(activeName())) ConfigProfileUtils.loadProfileWithoutSavingCurrent(ConfigProfileUtils.CURRENT_CONFIG);
                return String.format(i18n("configprofiles.status.deleted"), name);
            } catch (Exception e) {
                return failure("delete", "configprofiles.status.delete_failed", e);
            }
        }

        @Override
        public String rename(String from, String to) {
            try {
                String renamed = ConfigProfileUtils.renameProfile(from, to, "");
                if (from.equals(activeName())) ConfigProfileUtils.setActiveProfileName(renamed);
                return String.format(i18n("configprofiles.status.renamed"), renamed);
            } catch (FileException e) {
                return failure("rename", "configprofiles.status.rename_failed", e);
            }
        }

        @Override
        public String create(String name) {
            try {
                String created = ConfigProfileUtils.saveCurrentAs(name);
                return String.format(i18n("configprofiles.status.renamed"), created);
            } catch (FileException e) {
                return failure("create", "configprofiles.status.rename_failed", e);
            }
        }

        @Override
        public String exportActive() {
            FileDialog dialog = new FileDialog((Frame) null, i18n("configprofiles.filedialog.export"), FileDialog.SAVE);
            dialog.setDirectory(ConfigProfileUtils.getProfileDir().getAbsolutePath());
            dialog.setFile(activeName() + ".json");
            dialog.setVisible(true);
            if (dialog.getFile() == null) return "";
            File target = new File(dialog.getDirectory(), dialog.getFile());
            if (!target.getName().toLowerCase(Locale.ROOT).endsWith(".json")) {
                target = new File(target.getParentFile(), target.getName() + ".json");
            }
            try {
                ConfigProfileUtils.exportActiveProfile(target);
                return String.format(i18n("configprofiles.status.exported"), target.getName());
            } catch (FileException e) {
                return failure("export", "configprofiles.status.export_failed", e);
            }
        }

        @Override
        public String importFile() {
            FileDialog dialog = new FileDialog((Frame) null, i18n("configprofiles.filedialog.import"), FileDialog.LOAD);
            dialog.setDirectory(ConfigProfileUtils.getProfileDir().getAbsolutePath());
            dialog.setFilenameFilter((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json"));
            dialog.setVisible(true);
            if (dialog.getFile() == null) return "";
            try {
                String name = ConfigProfileUtils.importProfile(new File(dialog.getDirectory(), dialog.getFile()));
                ConfigProfileUtils.loadProfile(name);
                return String.format(i18n("configprofiles.status.imported"), name);
            } catch (Exception e) {
                return failure("import", "configprofiles.status.import_failed", e);
            }
        }

        @Override
        public String resetAllOff() {
            try {
                ConfigProfileUtils.resetActiveProfileToDefaults();
                return String.format(i18n("configprofiles.status.alloff"), activeName());
            } catch (FileException e) {
                return failure("reset", "configprofiles.status.alloff_failed", e);
            }
        }

        private File activeFile() {
            try {
                return ConfigProfileUtils.getProfileFile(activeName());
            } catch (FileException e) {
                return null;
            }
        }

        private String failure(String action, String key, Exception error) {
            ClientLogger.error("Failed to " + action + " config profile: " + error.getMessage());
            return i18n(key);
        }
    }
}
