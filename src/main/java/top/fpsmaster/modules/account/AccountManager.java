package top.fpsmaster.modules.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import top.fpsmaster.forge.api.IMinecraft;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Local offline-account book for the main-menu account switcher.
 *
 * <p>The launcher owns real (Microsoft) authentication and passes its session in on launch; that
 * session is represented by {@link #launcherAccount()} and can be switched back to but never
 * removed. Offline accounts are plain usernames with a deterministic offline UUID, persisted to
 * {@code accounts.json} next to the client config.
 */
public final class AccountManager {
    public static final class Account {
        public String name;
        public String uuid;

        Account(String name, String uuid) {
            this.name = name;
            this.uuid = uuid;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AccountManager instance;

    private final List<Account> offlineAccounts = new ArrayList<Account>();
    /** Session the client was launched with; null when it was already an offline launch. */
    private final Session launcherSession;
    private final boolean launcherSessionOnline;

    private AccountManager() {
        Session session = Minecraft.getMinecraft().getSession();
        this.launcherSession = session;
        String token = session == null ? null : session.getToken();
        this.launcherSessionOnline = token != null && !token.isEmpty() && !"undefined".equals(token);
        load();
    }

    public static AccountManager get() {
        if (instance == null) {
            instance = new AccountManager();
        }
        return instance;
    }

    /** The account the launcher signed in with, or null for a bare offline launch. */
    public Account launcherAccount() {
        if (launcherSession == null) {
            return null;
        }
        return new Account(launcherSession.getUsername(), launcherSession.getPlayerID());
    }

    public boolean isLauncherAccountOnline() {
        return launcherSessionOnline;
    }

    public List<Account> getOfflineAccounts() {
        return offlineAccounts;
    }

    public String currentName() {
        Session session = Minecraft.getMinecraft().getSession();
        return session == null ? "" : session.getUsername();
    }

    /** Whether the active session is the one the launcher provided (as opposed to a local switch). */
    public boolean isCurrentLauncherAccount() {
        return launcherSession != null && launcherSession == Minecraft.getMinecraft().getSession();
    }

    public static boolean isValidUsername(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{3,16}");
    }

    /** Adds (or reuses) an offline account and switches to it. Returns false for a bad name. */
    public boolean addAndUse(String name) {
        if (!isValidUsername(name)) {
            return false;
        }
        Account existing = find(name);
        if (existing == null) {
            existing = new Account(name, offlineUuid(name));
            offlineAccounts.add(existing);
            save();
        }
        use(existing);
        return true;
    }

    public void use(Account account) {
        if (account == null) {
            return;
        }
        if (launcherSession != null && account.name.equals(launcherSession.getUsername())) {
            useLauncherAccount();
            return;
        }
        Session session = new Session(account.name, account.uuid, "undefined", "legacy");
        ((IMinecraft) Minecraft.getMinecraft()).arch$setSession(session);
    }

    public void useLauncherAccount() {
        if (launcherSession != null) {
            ((IMinecraft) Minecraft.getMinecraft()).arch$setSession(launcherSession);
        }
    }

    public void remove(Account account) {
        Account stored = account == null ? null : find(account.name);
        if (stored == null) {
            return;
        }
        boolean wasCurrent = stored.name.equals(currentName());
        offlineAccounts.remove(stored);
        save();
        if (wasCurrent) {
            if (launcherSession != null) {
                useLauncherAccount();
            } else if (!offlineAccounts.isEmpty()) {
                use(offlineAccounts.get(0));
            }
        }
    }

    private Account find(String name) {
        for (Account account : offlineAccounts) {
            if (account.name.equalsIgnoreCase(name)) {
                return account;
            }
        }
        return null;
    }

    private static String offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private File file() {
        return new File(FileUtils.dir, "accounts.json");
    }

    private void load() {
        File file = file();
        if (!file.isFile()) {
            return;
        }
        try {
            String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            List<Account> loaded = GSON.fromJson(json, new TypeToken<List<Account>>() {
            }.getType());
            if (loaded != null) {
                for (Account account : loaded) {
                    if (account != null && isValidUsername(account.name)) {
                        offlineAccounts.add(account);
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            ClientLogger.warn("Failed to load accounts.json: " + exception);
        }
    }

    private void save() {
        try {
            Files.write(file().toPath(), GSON.toJson(offlineAccounts).getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            ClientLogger.warn("Failed to save accounts.json: " + exception);
        }
    }
}
