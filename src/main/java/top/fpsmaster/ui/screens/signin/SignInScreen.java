package top.fpsmaster.ui.screens.signin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.client.api.AuthService;
import top.fpsmaster.modules.client.api.FPSMasterApiClient;
import top.fpsmaster.modules.client.api.model.ApiResponse;
import top.fpsmaster.modules.client.api.model.UserInfo;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.prism.screen.SharedSignIn;
import top.fpsmaster.prism.screen.SignInBridge;
import top.fpsmaster.ui.kit.EdgeUi;
import top.fpsmaster.utils.render.gui.Backgrounds;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FPSMaster 产品账号登录（不是 Minecraft 账号，那条在首页的账号胶囊里）。
 *
 * <p>入口有两处：首页账号浮层的「FPSMaster 账号」一行，以及饰品界面未登录时的购买按钮。
 * OOBE 里的那套登录表单原样保留，走的是同一个 {@link FPSMasterApiClient#login}。
 */
public final class SignInScreen extends ScaledGuiScreen {
    /** 和 Nova 的登录界面指的是同一个网页入口。 */
    private static final String WEBSITE_URL = "https://fpsmaster.top/login";

    /** 后端 {@code banReason} 为空时的兜底原话（AuthService.loginInternal / CurrentUser）。 */
    private static final String BACKEND_DEFAULT_BAN_REASON = "account is banned";

    private final GuiScreen parent;
    private final SharedSignIn gui = new SharedSignIn();
    private final SignInBridge bridge = new EdgeSignInBridge();

    /** 请求跑在网络线程上、渲染线程每帧读，所以都得是 volatile。 */
    private volatile boolean busy;
    private volatile String error = "";

    /** 同一个界面里可能连着登录好几次，用它把迟到的响应丢掉。 */
    private final AtomicInteger attempt = new AtomicInteger();

    private boolean initialised;

    public SignInScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        // `initGui()` 在 resize 时也会跑，这时候不能清掉玩家已经输了一半的账号。
        if (!initialised) {
            gui.reset();
            initialised = true;
        }
        Backgrounds.initGui();
        // token 还有效但本次会话没登录过时，缓存是空的，名字会显示成「未知账号」。
        // 这里异步补一次；渲染线程只读缓存，绝不在 paint 里发请求。
        FPSMasterApiClient.getInstance().refreshUserInfoAsync();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        // 和首页同一张背景；SharedSignIn 自己会压一层暗罩。
        Backgrounds.draw((int) guiWidth, (int) guiHeight, mouseX, mouseY, partialTicks, (int) zLevel);
        gui.paint(EdgeUi.frame(), bridge);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void back() {
        // 迟到的登录响应可能在玩家已经翻到别的界面之后才回来，这时候不能把人拽回去。
        if (mc.currentScreen != SignInScreen.this) {
            return;
        }
        // Edge 的 GuiScreen 切屏不像 Nova 那样会在渲染栈里重入，直接切即可。
        mc.displayGuiScreen(parent);
    }

    /**
     * 后端和网络层给的都是裸英文（{@code invalid credentials} / {@code Network error}），
     * 直接贴到界面上中文玩家看不懂，所以按状态码分流到 lang 文件里的文案。
     *
     * @param response 失败的响应，可能为 null
     */
    private String localizeLoginError(ApiResponse<?> response) {
        if (response == null) {
            return FPSMaster.i18n.get("signin.failed.network");
        }
        int code = response.getCode();
        if (code == ApiResponse.NETWORK_ERROR) {
            return FPSMaster.i18n.get("signin.failed.network");
        }
        if (code == 401) {
            return FPSMaster.i18n.get("signin.failed.credentials");
        }
        if (code == 403) {
            // 后端对被封账号返回 403，message 就是封禁原因（AuthService.loginInternal）。
            // 并进 401 的话玩家看到的是「账号或密码不正确」，会反复改密码而不是来申诉。
            return banMessage(response);
        }
        if (code == 429) {
            return FPSMaster.i18n.get("signin.failed.throttled");
        }
        if (code >= 500) {
            return FPSMaster.i18n.get("signin.failed.server");
        }
        String message = response.getMessage();
        return message == null || message.isEmpty()
                ? FPSMaster.i18n.get("signin.failed") : message;
    }

    /**
     * 403 的文案。
     *
     * <p>只有后端按契约给出的原话才直接显示：Cloudflare 之类挡在前面时正文是 HTML，
     * 解析失败后 message 会是 "Parse error"，贴上去就是一句英文技术黑话。后端自己那句
     * 兜底的 {@code account is banned}（{@code banReason} 为空时用）也要换成中文，
     * 否则这个 key 等于白加。
     */
    private String banMessage(ApiResponse<?> response) {
        String reason = response.getMessage();
        if (!response.hasServerMessage() || reason == null || reason.isEmpty()
                || BACKEND_DEFAULT_BAN_REASON.equals(reason)) {
            return FPSMaster.i18n.get("signin.failed.banned");
        }
        return reason;
    }

    private final class EdgeSignInBridge implements SignInBridge {
        @Override
        public String i18n(String key) {
            return FPSMaster.i18n.get(key);
        }

        @Override
        public boolean signedIn() {
            return AuthService.getInstance().isLoggedIn();
        }

        @Override
        public String accountName() {
            UserInfo user = FPSMasterApiClient.getInstance().cachedUser();
            if (user == null) {
                return "";
            }
            if (user.getUsername() != null && !user.getUsername().isEmpty()) {
                return user.getUsername();
            }
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                return user.getDisplayName();
            }
            return user.getEmail() == null ? "" : user.getEmail();
        }

        @Override
        public boolean busy() {
            return busy;
        }

        @Override
        public String error() {
            return error;
        }

        @Override
        public void submit(String account, String password) {
            if (busy) {
                return;
            }
            busy = true;
            error = "";
            final int generation = attempt.incrementAndGet();
            FPSMasterApiClient.getInstance().login(account, password)
                    .whenComplete(new java.util.function.BiConsumer<ApiResponse<?>, Throwable>() {
                        @Override
                        public void accept(final ApiResponse<?> response, final Throwable throwable) {
                            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                                @Override
                                public void run() {
                                    if (generation != attempt.get()) {
                                        return;
                                    }
                                    busy = false;
                                    if (throwable != null) {
                                        ClientLogger.warn("FPSMaster sign-in request failed: " + throwable);
                                        error = i18n("signin.failed.network");
                                    } else if (response != null && response.isSuccess()) {
                                        error = "";
                                        // 登录成功就直接回到来处，省一次点击。
                                        back();
                                    } else {
                                        error = localizeLoginError(response);
                                    }
                                }
                            });
                        }
                    });
        }

        @Override
        public void signOut() {
            if (busy) {
                return;
            }
            busy = true;
            error = "";
            final int generation = attempt.incrementAndGet();
            FPSMasterApiClient.getInstance().logout()
                    .whenComplete(new java.util.function.BiConsumer<ApiResponse<Void>, Throwable>() {
                        @Override
                        public void accept(final ApiResponse<Void> response, final Throwable throwable) {
                            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                                @Override
                                public void run() {
                                    if (generation != attempt.get()) {
                                        return;
                                    }
                                    busy = false;
                                    if (throwable != null) {
                                        ClientLogger.warn("FPSMaster sign-out request failed: " + throwable);
                                    }
                                    gui.reset();
                                }
                            });
                        }
                    });
        }

        @Override
        public void close() {
            back();
        }

        @Override
        public boolean canOpenWebsite() {
            return Desktop.isDesktopSupported();
        }

        @Override
        public void openWebsite() {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(WEBSITE_URL));
                }
            } catch (Exception exception) {
                ClientLogger.warn("Failed to open the FPSMaster website: " + exception);
            }
        }
    }
}
