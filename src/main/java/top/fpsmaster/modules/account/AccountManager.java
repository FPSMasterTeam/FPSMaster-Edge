package top.fpsmaster.modules.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.AccountException;
import top.fpsmaster.forge.api.IMinecraft;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.io.AtomicFiles;
import top.fpsmaster.utils.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main-menu account book.
 *
 * <p>Launch-session (from the launcher or Gradle) can be switched back to but not removed.
 * Local accounts — offline names and Microsoft profiles signed in from this client — live in
 * {@code accounts.json}. Microsoft login uses the same Azure client as the FPSMaster launcher.
 */
public final class AccountManager {
    public static final String TYPE_OFFLINE = "offline";
    public static final String TYPE_MICROSOFT = "microsoft";
    private static final long REFRESH_SKEW_MS = 5L * 60L * 1000L;

    public static final class Account {
        public String name;
        public String uuid;
        public String type;
        public String accessToken;
        public String refreshToken;
        public Long expiresAt;
        public String xuid;

        public Account() {
        }

        Account(String name, String uuid) {
            this.name = name;
            this.uuid = uuid;
            this.type = TYPE_OFFLINE;
        }

        public boolean isMicrosoft() {
            return TYPE_MICROSOFT.equalsIgnoreCase(type);
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AccountManager instance;

    private final List<Account> accounts = new ArrayList<Account>();
    /** Session the client was launched with; null when it was already an offline launch. */
    private final Session launcherSession;
    private final boolean launcherSessionOnline;
    private String selectedName;

    private AccountManager() {
        Session session = Minecraft.getMinecraft().getSession();
        this.launcherSession = session;
        String token = session == null ? null : session.getToken();
        this.launcherSessionOnline = isOnlineToken(token);
        load();
        restoreSelected();
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
        Account account = new Account(launcherSession.getUsername(), launcherSession.getPlayerID());
        account.type = launcherSessionOnline ? TYPE_MICROSOFT : TYPE_OFFLINE;
        account.accessToken = launcherSession.getToken();
        return account;
    }

    public boolean isLauncherAccountOnline() {
        return launcherSessionOnline;
    }

    public List<Account> getOfflineAccounts() {
        return accounts;
    }

    public String currentName() {
        Session session = Minecraft.getMinecraft().getSession();
        return session == null ? "" : session.getUsername();
    }

    public boolean isCurrentOnline() {
        Session session = Minecraft.getMinecraft().getSession();
        return session != null && isOnlineToken(session.getToken());
    }

    /** Whether the active session is the one the launcher provided (as opposed to a local switch). */
    public boolean isCurrentLauncherAccount() {
        Session current = Minecraft.getMinecraft().getSession();
        if (launcherSession == null || current == null) {
            return false;
        }
        if (launcherSession == current) {
            return true;
        }
        return namesEqual(launcherSession.getUsername(), current.getUsername())
                && !isStoredCurrent();
    }

    public static boolean isValidUsername(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{3,16}");
    }

    /** Adds (or reuses) an offline account and switches to it. Returns false for a bad name. */
    public boolean addAndUse(String name) {
        if (!isValidUsername(name)) {
            return false;
        }
        Account existing = findOffline(name);
        if (existing == null) {
            existing = new Account(name, offlineUuid(name));
            accounts.add(existing);
        }
        use(existing);
        return true;
    }

    public void addAndUseMicrosoft(MicrosoftAuth.MinecraftProfile profile) {
        if (profile == null || profile.name == null || profile.uuid == null) {
            return;
        }
        Account existing = findMicrosoft(profile.uuid);
        if (existing == null) {
            existing = new Account();
            accounts.add(existing);
        }
        existing.name = profile.name;
        existing.uuid = MicrosoftAuth.dashedUuid(profile.uuid);
        existing.type = TYPE_MICROSOFT;
        existing.accessToken = profile.accessToken;
        existing.refreshToken = profile.refreshToken;
        existing.expiresAt = profile.expiresAt;
        existing.xuid = profile.xuid;
        applySession(existing);
        selectedName = existing.name;
        save();
        ClientLogger.info("Signed in Microsoft account " + existing.name);
    }

    public void use(Account account) {
        if (account == null) {
            return;
        }
        if (account.isMicrosoft()) {
            Account stored = findMicrosoft(account.uuid);
            if (stored == null) {
                stored = account;
            }
            if (tokenUsable(stored)) {
                applySession(stored);
                selectedName = stored.name;
                save();
                return;
            }
            if (stored.refreshToken != null && !stored.refreshToken.isEmpty()) {
                refreshMicrosoft(stored);
                return;
            }
            ClientLogger.warn("Microsoft account " + stored.name + " has no usable token");
            return;
        }
        if (launcherSession != null && namesEqual(account.name, launcherSession.getUsername())
                && findOffline(account.name) == null) {
            useLauncherAccount();
            return;
        }
        applySession(account);
        selectedName = account.name;
        save();
    }

    public void useLauncherAccount() {
        if (launcherSession != null) {
            ((IMinecraft) Minecraft.getMinecraft()).arch$setSession(launcherSession);
            selectedName = null;
            save();
        }
    }

    public void remove(Account account) {
        Account stored = account == null ? null : findStored(account);
        if (stored == null) {
            return;
        }
        boolean wasCurrent = namesEqual(stored.name, currentName());
        accounts.remove(stored);
        if (wasCurrent) {
            selectedName = null;
            if (launcherSession != null) {
                useLauncherAccount();
            } else if (!accounts.isEmpty()) {
                use(accounts.get(0));
                return;
            }
        }
        save();
    }

    private void restoreSelected() {
        if (selectedName == null || selectedName.isEmpty()) {
            return;
        }
        Account selected = findByName(selectedName);
        if (selected == null) {
            return;
        }
        if (selected.isMicrosoft()) {
            if (tokenUsable(selected)) {
                applySession(selected);
            } else if (selected.refreshToken != null && !selected.refreshToken.isEmpty()) {
                refreshMicrosoft(selected);
            }
            return;
        }
        applySession(selected);
    }

    private void refreshMicrosoft(final Account account) {
        if (FPSMaster.async == null) {
            return;
        }
        FPSMaster.async.runnable(new Runnable() {
            @Override
            public void run() {
                try {
                    final MicrosoftAuth.MinecraftProfile profile = MicrosoftAuth.refresh(account.refreshToken);
                    Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            addAndUseMicrosoft(profile);
                        }
                    });
                } catch (IOException | AccountException exception) {
                    ClientLogger.warn("Failed to refresh Microsoft account " + account.name + ": "
                            + exception.getMessage());
                }
            }
        });
    }

    private void applySession(Account account) {
        boolean microsoft = account.isMicrosoft();
        String token = microsoft && account.accessToken != null && !account.accessToken.isEmpty()
                ? account.accessToken
                : "undefined";
        String uuid = microsoft ? MicrosoftAuth.dashedUuid(account.uuid) : account.uuid;
        Session session = new Session(account.name, uuid, token, microsoft ? "mojang" : "legacy");
        ((IMinecraft) Minecraft.getMinecraft()).arch$setSession(session);
    }

    private Account findStored(Account account) {
        if (account == null) {
            return null;
        }
        if (account.isMicrosoft()) {
            Account microsoft = findMicrosoft(account.uuid);
            return microsoft != null ? microsoft : findByName(account.name);
        }
        return findOffline(account.name);
    }

    private Account findOffline(String name) {
        return findOffline(accounts, name);
    }

    private static Account findOffline(List<Account> accounts, String name) {
        for (Account account : accounts) {
            if (!account.isMicrosoft() && namesEqual(account.name, name)) {
                return account;
            }
        }
        return null;
    }

    private Account findMicrosoft(String uuid) {
        return findMicrosoft(accounts, uuid);
    }

    private static Account findMicrosoft(List<Account> accounts, String uuid) {
        if (uuid == null) {
            return null;
        }
        String dashed = MicrosoftAuth.dashedUuid(uuid);
        for (Account account : accounts) {
            if (account.isMicrosoft() && dashed.equalsIgnoreCase(MicrosoftAuth.dashedUuid(account.uuid))) {
                return account;
            }
        }
        return null;
    }

    private Account findByName(String name) {
        for (Account account : accounts) {
            if (namesEqual(account.name, name)) {
                return account;
            }
        }
        return null;
    }

    private boolean isStoredCurrent() {
        return findByName(currentName()) != null;
    }

    private static boolean tokenUsable(Account account) {
        if (account == null || account.accessToken == null || account.accessToken.isEmpty()) {
            return false;
        }
        if (account.expiresAt == null) {
            return true;
        }
        return account.expiresAt - REFRESH_SKEW_MS > System.currentTimeMillis();
    }

    private static boolean isOnlineToken(String token) {
        return token != null && !token.isEmpty() && !"undefined".equals(token) && !"offline".equals(token);
    }

    private static boolean namesEqual(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private static String offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)).toString();
    }

    /**
     * Microsoft refresh tokens live here, so the file sits in the per-user FPSMaster directory, not in the game's "FPSMaster Edge" folder that players zip up and share with their
     * configs. Named for Edge so it never collides with another FPSMaster client's file.
     */
    private File file() {
        return top.fpsmaster.utils.io.UserDataDir.file("edge_accounts.json");
    }

    private File legacyFile() {
        return new File(FileUtils.dir, "accounts.json");
    }

    /**
     * Moves accounts.json out of the shareable game folder the first time this version runs. Every
     * game directory has its own legacy file but they all migrate into the one per-user file, so the
     * legacy accounts are merged into whatever is already there, and the legacy file is deleted only
     * after the merged list has been written.
     */
    private void migrateLegacyFile() {
        File legacy = legacyFile();
        if (!legacy.isFile()) {
            return;
        }
        List<Account> legacyAccounts = new ArrayList<Account>();
        String legacySelected;
        try {
            legacySelected = parse(read(legacy), legacyAccounts);
        } catch (IOException | RuntimeException exception) {
            ClientLogger.warn("Failed to read legacy accounts.json, leaving it in place: " + exception);
            return;
        }
        merge(accounts, legacyAccounts);
        if (selectedName == null || selectedName.isEmpty()) {
            selectedName = legacySelected;
        }
        if (!save()) {
            return;
        }
        try {
            Files.delete(legacy.toPath());
            ClientLogger.info("Merged accounts.json from the game folder into " + file().getAbsolutePath());
        } catch (IOException exception) {
            ClientLogger.warn("Merged accounts.json but failed to delete the legacy copy: " + exception);
        }
    }

    private void load() {
        File file = file();
        if (file.isFile()) {
            try {
                selectedName = parse(read(file), accounts);
            } catch (IOException | RuntimeException exception) {
                // Do not merge into (and overwrite) a target we could not read.
                ClientLogger.warn("Failed to load accounts.json: " + exception);
                return;
            }
        }
        migrateLegacyFile();
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    /** Appends the accounts in {@code json} to {@code out}; returns the stored selection, if any. */
    static String parse(String json, List<Account> out) {
        JsonElement root = new JsonParser().parse(json);
        if (root == null || root.isJsonNull()) {
            return null;
        }
        if (root.isJsonArray()) {
            readAccounts(root.getAsJsonArray(), out);
            return null;
        }
        String selected = null;
        if (root.isJsonObject()) {
            JsonObject object = root.getAsJsonObject();
            if (object.has("selected") && !object.get("selected").isJsonNull()) {
                selected = object.get("selected").getAsString();
            }
            if (object.has("accounts") && object.get("accounts").isJsonArray()) {
                readAccounts(object.getAsJsonArray("accounts"), out);
            }
        }
        return selected;
    }

    /** Adds the {@code extra} accounts {@code into} lacks; Microsoft by uuid, offline by name. */
    static void merge(List<Account> into, List<Account> extra) {
        for (Account account : extra) {
            Account existing = account.isMicrosoft()
                    ? findMicrosoft(into, account.uuid)
                    : findOffline(into, account.name);
            if (existing == null) {
                into.add(account);
            }
        }
    }

    private static void readAccounts(JsonArray array, List<Account> out) {
        for (JsonElement element : array) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            Account account = GSON.fromJson(element, Account.class);
            if (account == null || account.name == null || account.name.trim().isEmpty()) {
                continue;
            }
            if (account.type == null || account.type.trim().isEmpty()) {
                account.type = TYPE_OFFLINE;
            }
            if (account.isMicrosoft()) {
                account.uuid = MicrosoftAuth.dashedUuid(account.uuid);
                out.add(account);
            } else if (isValidUsername(account.name)) {
                if (account.uuid == null || account.uuid.isEmpty()) {
                    account.uuid = offlineUuid(account.name);
                }
                out.add(account);
            }
        }
    }

    private boolean save() {
        try {
            JsonObject root = new JsonObject();
            if (selectedName != null) {
                root.addProperty("selected", selectedName);
            }
            root.add("accounts", GSON.toJsonTree(accounts));
            AtomicFiles.write(file().toPath(), GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException exception) {
            ClientLogger.warn("Failed to save accounts.json: " + exception);
            return false;
        }
    }
}

