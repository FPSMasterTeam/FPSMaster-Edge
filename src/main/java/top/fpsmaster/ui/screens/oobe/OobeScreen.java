package top.fpsmaster.ui.screens.oobe;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.impl.interfaces.CPSDisplay;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.features.impl.interfaces.ComboDisplay;
import top.fpsmaster.features.impl.interfaces.CoordsDisplay;
import top.fpsmaster.features.impl.interfaces.DirectionDisplay;
import top.fpsmaster.features.impl.interfaces.FPSDisplay;
import top.fpsmaster.features.impl.interfaces.InventoryDisplay;
import top.fpsmaster.features.impl.interfaces.Keystrokes;
import top.fpsmaster.features.impl.interfaces.PingDisplay;
import top.fpsmaster.features.impl.optimizes.OldAnimations;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.features.impl.render.ItemPhysics;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.ui.screens.mainmenu.MainMenu;
import top.fpsmaster.modules.client.api.FPSMasterApiClient;
import top.fpsmaster.modules.client.api.model.LoginResponse;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.AnimMath;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.utils.system.OSUtil;

import java.awt.Desktop;
import java.awt.Color;
import java.io.IOException;
import java.net.URI;

/**
 * OOBE wizard, visual twin of {@code docs/prototypes/oobe.html}: a centered glass card with a
 * step rail on the left, the page content on the right and a Back/Continue footer. All eight
 * wizard pages (and their state machine) are kept; only the drawing changed.
 */
public class OobeScreen extends ScaledGuiScreen {
    private static final int PAGE_COUNT = 8;
    private static final float RAIL_W = 110f;
    private static final String[] SCALE_LABELS = new String[]{"0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x", "2.5x", "3.0x"};
    private static final String[] GREETINGS = new String[]{"Hello, welcome.", "你好，欢迎使用。", "こんにちは、ようこそ。"};
    private static final String[] STEP_ICONS = new String[]{"languages", "monitor", "box", "sparkles", "user", "sliders", "chev-r", "grid"};
    private static final String[] STEP_KEYS = new String[]{
            "oobe.step.language", "oobe.step.display", "oobe.step.tutorial", "oobe.step.features",
            "oobe.step.account", "oobe.step.options", "oobe.step.guide", "oobe.step.qa"};
    private static final ResourceLocation PREVIEW_IMAGE = new ResourceLocation("client/background/panorama_1/panorama_0.png");
    private static final ResourceLocation PANORAMA_THREE = new ResourceLocation("client/background/panorama_3/panorama_0.png");
    private static final long TUTORIAL_SLIDE_DURATION_MS = 3000L;
    private static final long OOBE_INTRO_DURATION_MS = 3000L;
    private static final long GREETING_ROTATE_DURATION_MS = 2200L;
    private static boolean sessionStateInitialized;
    private static int savedPage;
    private static int savedLanguageValue;
    private static int savedTutorialIndex;
    private static long savedTutorialStartedAt = System.currentTimeMillis();
    private static boolean savedAntiCheatEnabled;
    private static boolean savedAnonymousDataEnabled;
    private static boolean savedEnterGuide = true;
    private static int savedQaStep;
    private static final int[] savedQaAnswers = new int[]{-1, -1, -1};
    private static String savedBackgroundChoice;
    private static boolean savedLoginSkipped = true;
    private static boolean savedIsLoggingIn = false;
    private static String savedLoginError = null;
    private static boolean savedLoginWelcomeShown = false;
    private static String savedWelcomeUsername = null;
    private static int savedWelcomeUserLevel = 0;
    private static int savedFeatureCount = 5;
    private static String savedAccountText = "";
    private static String savedPasswordText = "";

    private final AnimClock animClock = new AnimClock();
    private final float[] featureExpand = new float[]{0f, 0f, 0f, 0f};
    private final OobeDropdown scaleDropdown = new OobeDropdown();
    private final float[] qaOptionHover = new float[]{0f, 0f, 0f};
    private final float[] qaOptionPress = new float[]{0f, 0f, 0f};

    private int page;
    private int languageValue;
    private boolean followGameScaleEnabled;
    private int fixedScaleIndex;
    private int tutorialIndex;
    private int hoveredFeature = -1;
    private int expandedFeatureCard = -1;
    private float featureDetailExpand = 0f;
    private boolean antiCheatEnabled;
    private boolean anonymousDataEnabled;
    private boolean enterGuide = true;
    private int qaStep;
    private final int[] qaAnswers = new int[]{-1, -1, -1};
    private String backgroundChoice;
    private boolean loginSkipped = true;
    private boolean isLoggingIn = false;
    private String loginError = null;
    private boolean loginWelcomeShown = false;
    private String welcomeUsername = null;
    private int welcomeUserLevel = 0;
    private String loginSuccessMessage = null;
    private float pageMotion;
    private int pageMotionDirection = 1;
    private int featureCount = 5;
    private String hoveredBackgroundPreview;
    private String pendingBackgroundChoice;
    private boolean shaderWarningDialogVisible;
    private boolean shaderUnsupportedDialogVisible;
    private boolean shaderBenchmarkConfirmDialogVisible;
    private boolean shaderBenchmarkRunningDialogVisible;
    private boolean shaderBenchmarkResultDialogVisible;
    private double shaderBenchmarkScore;
    private float shaderBenchmarkProgress;
    private long shaderBenchmarkStartTime;

    // Benchmark state for frame-based execution
    private boolean benchmarkWarmupComplete;
    private int benchmarkProgramId;
    private long benchmarkElapsedNs;
    private int benchmarkIterations;
    private long benchmarkStartTime;
    private float forgotHoverAnim;
    private float registerHoverAnim;
    private boolean tutorialPlaybackComplete;
    private float tutorialSlideTransition;
    private int tutorialPrevSlide;
    private long introStartedAt;
    private float introProgress = 1f;
    private float greetingTransition = 1f;
    private String greetingCurrentText = "";
    private String greetingPreviousText = "";
    private int greetingIndex;
    private boolean canGoBackQa = false;
    private float followSwitchAnim;
    private float antiCheatSwitchAnim;
    private float anonymousSwitchAnim;

    // Card layout metrics, refreshed once per frame in render().
    private float cardX;
    private float cardY;
    private float cardW;
    private float cardH;
    private float contentX;
    private float contentW;

    private TextField accountField;
    private TextField passwordField;
    private boolean loginFieldsHaveFont;
    private OobeButton backButton;
    private OobeButton nextButton;
    private OobeButton tutorialPrevButton;
    private OobeButton tutorialNextButton;
    private OobeButton loginButton;
    private OobeButton skipLoginButton;
    private OobeButton shaderContinueButton;
    private OobeButton shaderCancelButton;
    private OobeButton shaderUnsupportedOkButton;
    private OobeButton shaderBenchmarkConfirmYesButton;
    private OobeButton shaderBenchmarkConfirmNoButton;
    private OobeButton shaderBenchmarkConfirmSkipButton;
    private OobeButton shaderBenchmarkResultOkButton;

    @Override
    public void initGui() {
        super.initGui();
        MainMenu.preloadPlayerSkinTexture();
        animClock.reset();
        introStartedAt = System.currentTimeMillis();
        introProgress = 0f;
        greetingIndex = (int) ((System.currentTimeMillis() / GREETING_ROTATE_DURATION_MS) % GREETINGS.length);

        backButton = new OobeButton("Back", false, () -> {
            if (page > 0) {
                page--;
                pageMotion = 1f;
                pageMotionDirection = -1;
            }
        });
        nextButton = new OobeButton("Next", true, this::onNext);
        tutorialPrevButton = new OobeButton("Prev", false, () -> tutorialIndex = (tutorialIndex + 2) % 3);
        tutorialNextButton = new OobeButton("Next", true, () -> tutorialIndex = (tutorialIndex + 1) % 3);
        loginButton = new OobeButton("Sign in", false, this::performLogin);
        skipLoginButton = new OobeButton("Skip", true, () -> {
            loginSkipped = true;
            loginError = null;
            onNext();
        });
        shaderContinueButton = new OobeButton("Continue", true, this::confirmShaderBackgroundSelection);
        shaderCancelButton = new OobeButton("Cancel", false, this::cancelShaderBackgroundSelection);
        shaderUnsupportedOkButton = new OobeButton("OK", true, () -> shaderUnsupportedDialogVisible = false);
        shaderBenchmarkConfirmYesButton = new OobeButton("Run Test", true, this::startShaderBenchmark);
        shaderBenchmarkConfirmNoButton = new OobeButton("Enable Anyway", false, this::enableShaderWithoutBenchmark);
        shaderBenchmarkConfirmSkipButton = new OobeButton("Skip", true, this::cancelShaderBackgroundSelection);
        shaderBenchmarkResultOkButton = new OobeButton("OK", true, this::confirmShaderBenchmarkResult);

        initSessionStateIfNeeded();
        restoreSessionState();

        setPreviewLanguage(languageValue);
        greetingCurrentText = animatedGreeting();
        greetingPreviousText = greetingCurrentText;
        greetingTransition = 1f;
        scaleDropdown.setItems(SCALE_LABELS).setSelectedIndex(fixedScaleIndex).setEnabled(true);

        accountField = new TextField(FPSMaster.fontManager.s18, key("oobe.login.account.placeholder"),
                new Color(255, 255, 255, 0).getRGB(), ClickGuiTheme.textFieldText().getRGB(), 32);
        passwordField = new TextField(FPSMaster.fontManager.s18, true, key("oobe.login.password.placeholder"),
                new Color(255, 255, 255, 0).getRGB(), ClickGuiTheme.textFieldText().getRGB(), 32);
        accountField.setText(savedAccountText);
        passwordField.setText(savedPasswordText);
        loginFieldsHaveFont = FPSMaster.fontManager.s18 != null;
    }

    /** Rebuild login fields once fonts are available (ctor may run before font load in edge cases). */
    private void ensureLoginFields() {
        if (loginFieldsHaveFont || FPSMaster.fontManager == null || FPSMaster.fontManager.s18 == null) {
            return;
        }
        accountField = new TextField(FPSMaster.fontManager.s18, key("oobe.login.account.placeholder"),
                new Color(255, 255, 255, 0).getRGB(), ClickGuiTheme.textFieldText().getRGB(), 32);
        passwordField = new TextField(FPSMaster.fontManager.s18, true, key("oobe.login.password.placeholder"),
                new Color(255, 255, 255, 0).getRGB(), ClickGuiTheme.textFieldText().getRGB(), 32);
        accountField.setText(savedAccountText);
        passwordField.setText(savedPasswordText);
        loginFieldsHaveFont = true;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        cardW = Math.min(380f, guiWidth - 24f);
        cardH = Math.min(250f, guiHeight - 32f);
        cardX = (guiWidth - cardW) / 2f;
        cardY = (guiHeight - cardH) / 2f;
        contentX = cardX + RAIL_W + 20f;
        contentW = cardW - RAIL_W - 40f;

        float dt = (float) animClock.tick();
        updateAnimations(dt);
        updateTutorialAutoplay();

        renderBackground();
        float introEased = easeOutCubic(introProgress);
        float introOffsetY = (1f - introEased) * 14f;

        GL11.glPushMatrix();
        GL11.glTranslatef(0f, introOffsetY, 0f);
        UiChrome.panel(cardX, cardY, cardW, cardH);
        renderRail(mouseX, mouseY);

        GL11.glPushMatrix();
        GL11.glTranslatef(pageMotionDirection * pageMotion * 17f, 0f, 0f);
        switch (page) {
            case 0:
                renderLanguagePage(mouseX, mouseY);
                break;
            case 1:
                renderScalePage(mouseX, mouseY);
                break;
            case 2:
                renderTutorialPage(mouseX, mouseY);
                break;
            case 3:
                renderFeaturesPage(mouseX, mouseY);
                break;
            case 4:
                renderLoginPage(mouseX, mouseY);
                break;
            case 5:
                renderOptionsPage(mouseX, mouseY);
                break;
            case 6:
                renderGuideEntryPage(mouseX, mouseY);
                break;
            case 7:
                renderQaPage(mouseX, mouseY);
                break;
            default:
                break;
        }
        GL11.glPopMatrix();

        renderFooter(mouseX, mouseY);
        GL11.glPopMatrix();
        renderShaderDialogs(mouseX, mouseY);
        renderIntroOverlay();
        syncSessionState();
    }

    private void updateAnimations(float dt) {
        float speed = Math.min(1f, dt * 6f);
        for (int i = 0; i < featureExpand.length; i++) {
            float target = (hoveredFeature == i && expandedFeatureCard == -1) ? 1f : 0f;
            featureExpand[i] += (target - featureExpand[i]) * speed;
        }
        float detailTarget = expandedFeatureCard >= 0 ? 1f : 0f;
        featureDetailExpand += (detailTarget - featureDetailExpand) * Math.min(1f, dt * 6f);
        for (int i = 0; i < qaOptionHover.length; i++) {
            qaOptionPress[i] = (float) AnimMath.base(qaOptionPress[i], 0.0, 0.25);
        }
        forgotHoverAnim = (float) AnimMath.base(forgotHoverAnim, 0.0, 0.25);
        registerHoverAnim = (float) AnimMath.base(registerHoverAnim, 0.0, 0.25);
        followSwitchAnim = (float) AnimMath.base(followSwitchAnim, followGameScaleEnabled ? 1.0 : 0.0, 0.3);
        antiCheatSwitchAnim = (float) AnimMath.base(antiCheatSwitchAnim, antiCheatEnabled ? 1.0 : 0.0, 0.3);
        anonymousSwitchAnim = (float) AnimMath.base(anonymousSwitchAnim, anonymousDataEnabled ? 1.0 : 0.0, 0.3);
        pageMotion += (0f - pageMotion) * Math.min(1f, dt * 8.5f);
        tutorialSlideTransition += (1f - tutorialSlideTransition) * Math.min(1f, dt * 8f);
        float introTarget = Math.min(1f, Math.max(0f, (System.currentTimeMillis() - introStartedAt) / (float) OOBE_INTRO_DURATION_MS));
        introProgress += (introTarget - introProgress) * Math.min(1f, dt * 4f);
        int nextGreetingIndex = (int) ((System.currentTimeMillis() / GREETING_ROTATE_DURATION_MS) % GREETINGS.length);
        if (nextGreetingIndex != greetingIndex) {
            greetingPreviousText = greetingCurrentText;
            greetingIndex = nextGreetingIndex;
            greetingCurrentText = animatedGreeting();
            greetingTransition = 0f;
        }
        if (greetingTransition < 1f) {
            greetingTransition += (1f - greetingTransition) * Math.min(1f, dt * 7f);
            if (greetingTransition > 0.995f) {
                greetingTransition = 1f;
                greetingPreviousText = greetingCurrentText;
            }
        }
        if (accountField != null) {
            accountField.updateCursorCounter();
        }
        if (passwordField != null) {
            passwordField.updateCursorCounter();
        }
    }

    private void renderBackground() {
        Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(12, 12, 12, 255));
    }

    private void renderIntroOverlay() {
        float eased = easeOutCubic(introProgress);
        int overlayAlpha = Math.min(255, Math.max(0, Math.round((1f - eased) * 255f)));
        if (overlayAlpha > 0) {
            Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(12, 12, 12, overlayAlpha));
        }
    }

    // ------------------------------------------------------------------
    // Left rail: brand, step list, "skip all"
    // ------------------------------------------------------------------

    private void renderRail(int mouseX, int mouseY) {
        Rects.fill(cardX + 1f, cardY + 1f, RAIL_W - 1f, cardH - 2f, ClickGuiTheme.categoryBg());
        UiChrome.hairlineV(cardX + RAIL_W, cardY + 1f, cardH - 2f);

        // Brand row
        float markX = cardX + 12f;
        float markY = cardY + 12f;
        Rects.rounded(markX, markY, 15f, 15f, 4, ClickGuiTheme.accent().getRGB(), false);
        UiChrome.boldCentered(FPSMaster.fontManager.getFont(14), "F", markX + 7.5f, markY + 4f, Color.WHITE.getRGB());
        UiChrome.boldString(FPSMaster.fontManager.getFont(14), "FPSMaster", markX + 20f, markY + 0.5f,
                ClickGuiTheme.textPrimary().getRGB());
        FPSMaster.fontManager.getFont(10).drawString(key("oobe.brand.sub"), markX + 20f, markY + 9.5f,
                ClickGuiTheme.textDisabled().getRGB());

        // Step list
        float stepX = cardX + 8f;
        float stepW = RAIL_W - 16f;
        float stepsTop = markY + 15f + 11f;
        float skipY = cardY + cardH - 19f;
        float stepGap = 1f;
        float avail = skipY - 4f - stepsTop;
        float stepH = Math.min(23f, Math.max(14f, (avail - (PAGE_COUNT - 1) * stepGap) / PAGE_COUNT));
        for (int i = 0; i < PAGE_COUNT; i++) {
            float y = stepsTop + i * (stepH + stepGap);
            boolean current = i == page;
            boolean done = i < page;
            if (current) {
                Rects.rounded(stepX, y, stepW, stepH, 6, ClickGuiTheme.layer().getRGB(), false);
            }
            float dotSize = 13f;
            float dotX = stepX + 5f;
            float dotY = y + (stepH - dotSize) / 2f;
            float iconInset = (dotSize - 6.5f) / 2f;
            if (current) {
                Rects.rounded(dotX, dotY, dotSize, dotSize, 6, ClickGuiTheme.accent().getRGB(), false);
                Icons.draw(STEP_ICONS[i], dotX + iconInset, dotY + iconInset, 6.5f, Color.WHITE.getRGB());
            } else if (done) {
                Rects.rounded(dotX, dotY, dotSize, dotSize, 6, ClickGuiTheme.accentSoft().getRGB(), false);
                Icons.draw("check", dotX + iconInset, dotY + iconInset, 6.5f, ClickGuiTheme.accentText().getRGB());
            } else {
                Rects.rounded(dotX - 0.5f, dotY - 0.5f, dotSize + 1f, dotSize + 1f, 7,
                        ClickGuiTheme.stroke().getRGB(), false);
                Rects.rounded(dotX, dotY, dotSize, dotSize, 6, ClickGuiTheme.layer().getRGB(), false);
                Icons.draw(STEP_ICONS[i], dotX + iconInset, dotY + iconInset, 6.5f, ClickGuiTheme.textDisabled().getRGB());
            }
            int nameColor = current
                    ? ClickGuiTheme.textPrimary().getRGB()
                    : (done ? ClickGuiTheme.textSecondary() : ClickGuiTheme.textDisabled()).getRGB();
            FPSMaster.fontManager.getFont(13).drawString(key(STEP_KEYS[i]),
                    dotX + dotSize + 6f, y + stepH / 2f - 3.25f, nameColor);
        }

        // Skip-all ghost button
        String skipLabel = key("oobe.skip.all");
        UFontRenderer skipFont = FPSMaster.fontManager.getFont(12);
        float skipW = skipFont.getStringWidth(skipLabel) + 8f;
        float skipH = 12f;
        float skipX = cardX + 8f;
        boolean skipHover = !hasActiveModal() && Hover.is(skipX, skipY, skipW, skipH, mouseX, mouseY);
        if (skipHover) {
            Rects.rounded(skipX, skipY, skipW, skipH, 4, ClickGuiTheme.layer().getRGB(), false);
        }
        skipFont.drawString(skipLabel, skipX + 4f, skipY + skipH / 2f - 3f,
                (skipHover ? ClickGuiTheme.textSecondary() : ClickGuiTheme.textDisabled()).getRGB());
        if (!hasActiveModal() && consumePressInBounds(skipX, skipY, skipW, skipH, 0) != null) {
            finishOobe();
        }
    }

    // ------------------------------------------------------------------
    // Shared content chrome
    // ------------------------------------------------------------------

    /** Title + lede at the top of the content column; returns the y where the page body starts. */
    private float renderPageHeader(String title, String lede) {
        float y = cardY + 18f;
        UiChrome.boldString(FPSMaster.fontManager.getFont(24), title, contentX, y, ClickGuiTheme.textPrimary().getRGB());
        int ledeLines = drawWrapped(FPSMaster.fontManager.getFont(13), lede, contentX, y + 16f, contentW, 8f, 2,
                ClickGuiTheme.textSecondary().getRGB());
        return y + 16f + ledeLines * 8f + 8f;
    }

    /** Prototype .choice card: big label + optional sub text; accent-soft when selected. */
    private void renderChoiceCard(float x, float y, float width, float height, boolean selected, boolean hovered,
                                  String big, String sub) {
        if (selected) {
            UiChrome.selectedSurface(x, y, width, height, UiChrome.CARD_RADIUS);
        } else {
            Color fill = hovered ? ClickGuiTheme.layerHover() : ClickGuiTheme.layer();
            Rects.rounded(x - 0.5f, y - 0.5f, width + 1f, height + 1f, UiChrome.CARD_RADIUS + 1,
                    ClickGuiTheme.stroke().getRGB(), false);
            Rects.rounded(x, y, width, height, UiChrome.CARD_RADIUS, fill.getRGB(), false);
        }
        UiChrome.boldString(FPSMaster.fontManager.getFont(15), big, x + 9f, y + 9f, ClickGuiTheme.textPrimary().getRGB());
        if (sub != null && !sub.isEmpty()) {
            drawWrapped(FPSMaster.fontManager.getFont(11), sub, x + 9f, y + 19f, width - 18f, 6f, 2,
                    ClickGuiTheme.textSecondary().getRGB());
        }
    }

    /** Prototype .row left side: name + one-line desc. Row height is 19 units. */
    private void renderSettingRow(float x, float y, String name, String desc) {
        FPSMaster.fontManager.getFont(13).drawString(name, x, y + 2f, ClickGuiTheme.textPrimary().getRGB());
        FPSMaster.fontManager.getFont(11).drawString(desc, x, y + 10.5f, ClickGuiTheme.textSecondary().getRGB());
    }

    // ------------------------------------------------------------------
    // Pages
    // ------------------------------------------------------------------

    private void renderLanguagePage(int mouseX, int mouseY) {
        float bodyY = renderPageHeader(key("oobe.language.title"), key("oobe.language.desc"));
        float gap = 6f;
        float w = (contentW - gap) / 2f;
        float h = 36f;
        float y = bodyY + 2f;
        boolean modal = hasActiveModal();
        boolean zhHover = !modal && Hover.is(contentX, y, w, h, mouseX, mouseY);
        boolean enHover = !modal && Hover.is(contentX + w + gap, y, w, h, mouseX, mouseY);
        renderChoiceCard(contentX, y, w, h, languageValue == 1, zhHover, key("oobe.language.zh"), key("oobe.language.zh.sub"));
        renderChoiceCard(contentX + w + gap, y, w, h, languageValue == 0, enHover, "English", key("oobe.language.en.sub"));
        if (!modal && consumePressInBounds(contentX, y, w, h, 0) != null) {
            switchLanguage(1);
        }
        if (!modal && consumePressInBounds(contentX + w + gap, y, w, h, 0) != null) {
            switchLanguage(0);
        }
    }

    private void renderScalePage(int mouseX, int mouseY) {
        float bodyY = renderPageHeader(key("oobe.scale.title"), key("oobe.scale.desc"));
        float rowH = 19f;
        boolean modal = hasActiveModal();

        // Follow game scale row + switch
        float y = bodyY + 2f;
        renderSettingRow(contentX, y, key("oobe.scale.follow"), key("oobe.scale.follow.desc"));
        UiChrome.drawSwitch(contentX + contentW - UiChrome.SWITCH_W, y + (rowH - UiChrome.SWITCH_H) / 2f,
                followGameScaleEnabled, followSwitchAnim);
        if (!modal && consumePressInBounds(contentX, y, contentW, rowH, 0) != null) {
            followGameScaleEnabled = !followGameScaleEnabled;
            applyLiveScaleSettings();
        }
        UiChrome.hairlineH(contentX, y + rowH + 1f, contentW);

        // Fixed multiplier row + segmented control
        float y2 = y + rowH + 4f;
        renderSettingRow(contentX, y2, key("oobe.scale.label"), key("oobe.scale.fixed.desc"));
        float segY = y2 + rowH + 3f;
        float segH = 14f;
        UiChrome.seg(contentX, segY, contentW, segH);
        float optW = (contentW - 3f) / SCALE_LABELS.length;
        for (int i = 0; i < SCALE_LABELS.length; i++) {
            float ox = contentX + 1.5f + i * optW;
            boolean hover = !modal && Hover.is(ox, segY + 1.5f, optW, segH - 3f, mouseX, mouseY);
            UiChrome.segOption(ox, segY + 1.5f, optW, segH - 3f, SCALE_LABELS[i], fixedScaleIndex == i, hover);
            if (!modal && consumePressInBounds(ox, segY + 1.5f, optW, segH - 3f, 0) != null) {
                fixedScaleIndex = i;
                scaleDropdown.setSelectedIndex(fixedScaleIndex);
                applyLiveScaleSettings();
            }
        }
    }

    private void renderTutorialPage(int mouseX, int mouseY) {
        float bodyY = renderPageHeader(key("oobe.tutorial.title"), key("oobe.tutorial.desc"));
        String[][] slides = new String[][]{
                {key("oobe.tutorial.1.title"), key("oobe.tutorial.1.desc")},
                {key("oobe.tutorial.2.title"), key("oobe.tutorial.2.desc")},
                {key("oobe.tutorial.3.title"), key("oobe.tutorial.3.desc")}
        };

        float y = bodyY + 2f;
        float h = Math.max(64f, cardY + cardH - 38f - y);
        Rects.rounded(contentX - 0.5f, y - 0.5f, contentW + 1f, h + 1f, UiChrome.CARD_RADIUS + 1,
                ClickGuiTheme.stroke().getRGB(), false);
        Rects.rounded(contentX, y, contentW, h, UiChrome.CARD_RADIUS, ClickGuiTheme.layer().getRGB(), false);
        FPSMaster.fontManager.getFont(11).drawString((tutorialIndex + 1) + " / " + slides.length,
                contentX + 9f, y + 8f, ClickGuiTheme.textDisabled().getRGB());

        // Alpha below four reads as opaque to the renderer (no alpha bits == opaque, as vanilla),
        // so both text passes bail out early instead of popping in solid.
        int alpha = Math.min(255, (int) (tutorialSlideTransition * 255f));
        if (alpha > 3) {
            GL11.glPushMatrix();
            GL11.glTranslatef(0f, (1f - tutorialSlideTransition) * 4f, 0f);
            Color titleColor = ClickGuiTheme.textPrimary();
            UiChrome.boldString(FPSMaster.fontManager.getFont(15), slides[tutorialIndex][0], contentX + 9f, y + 18f,
                    new Color(titleColor.getRed(), titleColor.getGreen(), titleColor.getBlue(), alpha).getRGB());
            Color bodyColor = ClickGuiTheme.textSecondary();
            drawWrapped(FPSMaster.fontManager.getFont(11),
                    extendTutorialDescription(slides[tutorialIndex][1], tutorialIndex),
                    contentX + 9f, y + 30f, contentW - 18f, 6.5f, 4,
                    new Color(bodyColor.getRed(), bodyColor.getGreen(), bodyColor.getBlue(), alpha).getRGB());
            GL11.glPopMatrix();
        }

        // Slide progress dots
        float dotW = 4f;
        float dotGap = 3f;
        float dotsX = contentX + (contentW - (slides.length * dotW + (slides.length - 1) * dotGap)) / 2f;
        for (int i = 0; i < slides.length; i++) {
            Rects.rounded(dotsX + i * (dotW + dotGap), y + h - 9f, dotW, dotW, 2,
                    (i == tutorialIndex ? ClickGuiTheme.accent() : ClickGuiTheme.layerActive()).getRGB(), false);
        }
    }

    private void renderFeaturesPage(int mouseX, int mouseY) {
        float bodyY = renderPageHeader(key("oobe.features.title"), key("oobe.features.desc"));
        String[][] cards = new String[][]{
                {key("oobe.features.performance.title"), key("oobe.features.performance.desc")},
                {key("oobe.features.animations.title"), key("oobe.features.animations.desc")},
                {key("oobe.features.hud.title"), key("oobe.features.hud.desc")},
                {key("oobe.features.background.title"), key("oobe.features.background.desc")}
        };
        String[] icons = new String[]{"zap", "sparkles", "grid", "image"};

        float gap = 5f;
        float w = (contentW - 2f * gap) / 3f;
        float h = 54f;
        hoveredFeature = -1;
        for (int i = 0; i < cards.length; i++) {
            int row = i / 3;
            int column = i % 3;
            float x = contentX + column * (w + gap);
            float y = bodyY + 2f + row * (h + gap);
            boolean hovered = Hover.is(x, y, w, h, mouseX, mouseY);
            if (hovered) {
                hoveredFeature = i;
            }
            Rects.rounded(x - 0.5f, y - 0.5f, w + 1f, h + 1f, UiChrome.CARD_RADIUS + 1,
                    ClickGuiTheme.stroke().getRGB(), false);
            Rects.rounded(x, y, w, h, UiChrome.CARD_RADIUS,
                    (hovered ? ClickGuiTheme.layerHover() : ClickGuiTheme.layer()).getRGB(), false);
            Rects.rounded(x + 7f, y + 7f, 16f, 16f, UiChrome.CTL_RADIUS, ClickGuiTheme.accentSoft().getRGB(), false);
            Icons.draw(icons[i], x + 7f + 4.25f, y + 7f + 4.25f, 7.5f, ClickGuiTheme.accentText().getRGB());
            UiChrome.boldString(FPSMaster.fontManager.getFont(12), cards[i][0], x + 7f, y + 27f,
                    ClickGuiTheme.textPrimary().getRGB());
            drawWrapped(FPSMaster.fontManager.getFont(11), cards[i][1], x + 7f, y + 35f, w - 14f, 6f, 2,
                    ClickGuiTheme.textSecondary().getRGB());
        }
    }

    private void renderLoginPage(int mouseX, int mouseY) {
        // Show welcome screen if login was successful
        if (loginWelcomeShown && welcomeUsername != null) {
            renderLoginWelcome();
            return;
        }

        float bodyY = renderPageHeader(key("oobe.login.title"), key("oobe.login.desc"));

        ensureLoginFields();
        if (accountField == null || passwordField == null) {
            return;
        }

        // Disable input while logging in
        boolean inputEnabled = !isLoggingIn;
        accountField.setEnabled(inputEnabled);
        passwordField.setEnabled(inputEnabled);

        float formW = 150f;
        float fieldH = 19f;
        float y = bodyY + 4f;
        drawTextField(accountField, contentX, y, formW, fieldH);
        drawTextField(passwordField, contentX, y + fieldH + 5f, formW, fieldH);

        float btnY = y + (fieldH + 5f) * 2f + 2f;
        String loginLabel = isLoggingIn ? (isChinese() ? "登录中..." : "Logging in...") : key("oobe.login.submit");
        if (isLoggingIn) {
            Rects.rounded(contentX, btnY, formW, fieldH, UiChrome.CTL_RADIUS, ClickGuiTheme.layerActive().getRGB(), false);
            FPSMaster.fontManager.s14.drawCenteredString(loginLabel, contentX + formW / 2f, btnY + fieldH / 2f - 3.5f,
                    ClickGuiTheme.textDisabled().getRGB());
        } else if (UiChrome.buttonClicked(this, contentX, btnY, formW, fieldH, null, loginLabel,
                UiChrome.Style.PRIMARY, mouseX, mouseY) && !hasActiveModal()) {
            performLogin();
        }

        float belowY = btnY + fieldH + 6f;
        if (loginError != null) {
            int errorLines = drawWrapped(FPSMaster.fontManager.getFont(11), loginError, contentX, belowY, contentW, 6f, 2,
                    ClickGuiTheme.danger().getRGB());
            belowY += errorLines * 6f + 4f;
        }

        UFontRenderer linkFont = FPSMaster.fontManager.getFont(11);
        String forgot = key("oobe.login.forgot");
        String register = key("oobe.login.register");
        float forgotW = linkFont.getStringWidth(forgot);
        float registerX = contentX + forgotW + 12f;
        float registerW = linkFont.getStringWidth(register);
        boolean forgotHovered = Hover.is(contentX, belowY - 1f, forgotW, 8f, mouseX, mouseY);
        boolean registerHovered = Hover.is(registerX, belowY - 1f, registerW, 8f, mouseX, mouseY);
        forgotHoverAnim = (float) AnimMath.base(forgotHoverAnim, forgotHovered ? 1.0 : 0.0, 0.24);
        registerHoverAnim = (float) AnimMath.base(registerHoverAnim, registerHovered ? 1.0 : 0.0, 0.24);
        linkFont.drawString(forgot, contentX, belowY,
                blendColor(ClickGuiTheme.textDisabled(), ClickGuiTheme.accentText(), forgotHoverAnim).getRGB());
        linkFont.drawString(register, registerX, belowY,
                blendColor(ClickGuiTheme.textDisabled(), ClickGuiTheme.accentText(), registerHoverAnim).getRGB());
        if (!hasActiveModal() && !isLoggingIn && consumePressInBounds(contentX, belowY - 1f, forgotW, 9f, 0) != null) {
            openLink("https://fpsmaster.top/forgot");
        }
        if (!hasActiveModal() && !isLoggingIn && consumePressInBounds(registerX, belowY - 1f, registerW, 9f, 0) != null) {
            openLink("https://fpsmaster.top/login");
        }
    }

    private void renderLoginWelcome() {
        float centerX = contentX + contentW / 2f;
        float top = cardY + 40f;

        float badge = 26f;
        Rects.rounded(centerX - badge / 2f, top, badge, badge, 13, ClickGuiTheme.accentSoft().getRGB(), false);
        Icons.draw("check", centerX - 6.5f, top + 6.5f, 13f, ClickGuiTheme.accentText().getRGB());

        UiChrome.boldCentered(FPSMaster.fontManager.getFont(18), isChinese() ? "登录成功" : "Login Successful",
                centerX, top + badge + 10f, ClickGuiTheme.textPrimary().getRGB());
        String welcomeUserText = isChinese()
                ? "欢迎回来, " + welcomeUsername + "!"
                : "Welcome back, " + welcomeUsername + "!";
        FPSMaster.fontManager.getFont(12).drawCenteredString(welcomeUserText, centerX, top + badge + 24f,
                ClickGuiTheme.textSecondary().getRGB());

        float infoY = top + badge + 36f;
        if (welcomeUserLevel > 0) {
            FPSMaster.fontManager.getFont(11).drawCenteredString((isChinese() ? "等级: " : "Level: ") + welcomeUserLevel,
                    centerX, infoY, ClickGuiTheme.textDisabled().getRGB());
            infoY += 10f;
        }
        UiChrome.hairlineH(contentX + 20f, infoY + 4f, contentW - 40f);
        String infoText = isChinese()
                ? "您现在可以继续完成客户端配置"
                : "You can continue with the client setup";
        FPSMaster.fontManager.getFont(11).drawCenteredString(infoText, centerX, infoY + 10f,
                ClickGuiTheme.textDisabled().getRGB());
    }

    private void performLogin() {
        if (isLoggingIn) {
            return;
        }

        String username = accountField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            loginError = isChinese() ? "请输入账号和密码" : "Please enter username and password";
            return;
        }

        isLoggingIn = true;
        loginError = null;

        FPSMasterApiClient.getInstance().login(username, password, response -> {
            isLoggingIn = false;

            if (response.isSuccess() && response.getData() != null) {
                LoginResponse loginResponse = response.getData();
                loginSkipped = false;
                loginWelcomeShown = true;
                welcomeUsername = loginResponse.getCurrentUserView() != null
                        ? loginResponse.getCurrentUserView().getUsername()
                        : username;
                welcomeUserLevel = loginResponse.getCurrentUserView() != null
                        ? loginResponse.getCurrentUserView().getLevel()
                        : 0;
                ClientLogger.info("Login successful: " + loginResponse);
            } else {
                loginError = response.getMessage() != null && !response.getMessage().isEmpty()
                        ? response.getMessage()
                        : (isChinese() ? "登录失败，请重试" : "Login failed, please try again");
                ClientLogger.warn("Login failed: " + loginError);
            }
        });
    }

    private void renderOptionsPage(int mouseX, int mouseY) {
        float bodyY = renderPageHeader(key("oobe.options.title"), key("oobe.options.desc"));
        float rowH = 19f;
        boolean modal = hasActiveModal();

        float y = bodyY + 2f;
        renderSettingRow(contentX, y, key("oobe.options.anticheat"), key("oobe.options.anticheat.sub"));
        UiChrome.drawSwitch(contentX + contentW - UiChrome.SWITCH_W, y + (rowH - UiChrome.SWITCH_H) / 2f,
                antiCheatEnabled, antiCheatSwitchAnim);
        if (!modal && consumePressInBounds(contentX, y, contentW, rowH, 0) != null) {
            antiCheatEnabled = !antiCheatEnabled;
        }
        UiChrome.hairlineH(contentX, y + rowH + 1f, contentW);

        float y2 = y + rowH + 4f;
        renderSettingRow(contentX, y2, key("oobe.options.anonymous"), key("oobe.options.anonymous.sub"));
        UiChrome.drawSwitch(contentX + contentW - UiChrome.SWITCH_W, y2 + (rowH - UiChrome.SWITCH_H) / 2f,
                anonymousDataEnabled, anonymousSwitchAnim);
        if (!modal && consumePressInBounds(contentX, y2, contentW, rowH, 0) != null) {
            anonymousDataEnabled = !anonymousDataEnabled;
        }
    }

    private void renderGuideEntryPage(int mouseX, int mouseY) {
        float bodyY = renderPageHeader(key("oobe.guide.title"), key("oobe.guide.desc"));
        float gap = 6f;
        float w = (contentW - gap) / 2f;
        float h = 40f;
        float y = bodyY + 2f;
        boolean modal = hasActiveModal();
        boolean enterHover = !modal && Hover.is(contentX, y, w, h, mouseX, mouseY);
        boolean skipHover = !modal && Hover.is(contentX + w + gap, y, w, h, mouseX, mouseY);
        renderChoiceCard(contentX, y, w, h, enterGuide, enterHover,
                key("oobe.guide.result.enter"), key("oobe.guide.enter"));
        renderChoiceCard(contentX + w + gap, y, w, h, !enterGuide, skipHover,
                key("oobe.guide.result.skip"), key("oobe.guide.skip"));
        if (!modal && consumePressInBounds(contentX, y, w, h, 0) != null) {
            enterGuide = true;
        }
        if (!modal && consumePressInBounds(contentX + w + gap, y, w, h, 0) != null) {
            enterGuide = false;
        }
    }

    private void renderQaPage(int mouseX, int mouseY) {
        float bodyY = renderPageHeader(key("oobe.qa.title"), key("oobe.qa.desc"));
        String[][] questions = new String[][]{
                {key("oobe.qa.1.question"), key("oobe.qa.1.a"), key("oobe.qa.1.b"), key("oobe.qa.1.c")},
                {key("oobe.qa.2.question"), key("oobe.qa.2.a"), key("oobe.qa.2.b"), key("oobe.qa.2.c")},
                {key("oobe.qa.3.question"), key("oobe.qa.3.a"), key("oobe.qa.3.b"), key("oobe.qa.3.c")}
        };

        FPSMaster.fontManager.getFont(11).drawString((qaStep + 1) + " / " + questions.length,
                contentX, bodyY, ClickGuiTheme.textDisabled().getRGB());

        // Back to previous question
        if (qaStep > 0) {
            String backLabel = isChinese() ? "← 上一题" : "← Back";
            UFontRenderer backFont = FPSMaster.fontManager.getFont(11);
            float backW = backFont.getStringWidth(backLabel);
            float backX = contentX + contentW - backW;
            boolean backHovered = Hover.is(backX - 4f, bodyY - 2f, backW + 8f, 10f, getMouseX(), getMouseY());
            backFont.drawString(backLabel, backX, bodyY,
                    (backHovered ? ClickGuiTheme.accent() : ClickGuiTheme.accentText()).getRGB());
            if (backHovered && !hasActiveModal() && consumePressInBounds(backX - 4f, bodyY - 2f, backW + 8f, 10f, 0) != null) {
                qaStep--;
                // Clear the answer for the current step when going back
                qaAnswers[qaStep + 1] = -1;
            }
        }

        float qY = bodyY + 10f;
        UiChrome.boldString(FPSMaster.fontManager.getFont(15), questions[qaStep][0], contentX, qY,
                ClickGuiTheme.textPrimary().getRGB());

        float optTop = qY + 14f;
        if (qaStep == 1) {
            renderBackgroundPreviewChoices(contentX, optTop, contentW, questions[qaStep], mouseX, mouseY);
        } else {
            for (int i = 1; i <= 3; i++) {
                float optionY = optTop + (i - 1) * 23f;
                boolean selected = qaAnswers[qaStep] == i - 1;
                boolean hovered = Hover.is(contentX, optionY, contentW, 19f, getMouseX(), getMouseY());
                qaOptionHover[i - 1] = (float) AnimMath.base(qaOptionHover[i - 1], hovered ? 1.0 : 0.0, 0.22);
                renderQaOption(contentX, optionY, contentW, 19f, selected, hovered, qaOptionPress[i - 1],
                        questions[qaStep][i], 9f);
                if (!hasActiveModal() && consumePressInBounds(contentX, optionY, contentW, 19f, 0) != null) {
                    qaOptionPress[i - 1] = 1.0f;
                    qaAnswers[qaStep] = i - 1;
                    applyQaAnswer();
                    if (qaStep < questions.length - 1) {
                        qaStep++;
                    }
                }
            }
        }
    }

    private void renderQaOption(float x, float y, float width, float height, boolean selected, boolean hovered,
                                float pressAnim, String label, float textIndent) {
        float inset = pressAnim * 0.8f;
        float drawX = x + inset;
        float drawY = y + inset;
        float drawWidth = width - inset * 2f;
        float drawHeight = height - inset * 2f;
        if (selected) {
            UiChrome.selectedSurface(drawX, drawY, drawWidth, drawHeight, UiChrome.CARD_RADIUS);
        } else {
            Color fill = hovered ? ClickGuiTheme.layerHover() : ClickGuiTheme.layer();
            Rects.rounded(drawX - 0.5f, drawY - 0.5f, drawWidth + 1f, drawHeight + 1f, UiChrome.CARD_RADIUS + 1,
                    ClickGuiTheme.stroke().getRGB(), false);
            Rects.rounded(drawX, drawY, drawWidth, drawHeight, UiChrome.CARD_RADIUS, fill.getRGB(), false);
        }
        FPSMaster.fontManager.getFont(13).drawString(label, drawX + textIndent, drawY + drawHeight / 2f - 3.25f,
                (selected ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB());
    }

    private void renderBackgroundPreviewChoices(float x, float y, float width, String[] question, int mouseX, int mouseY) {
        String[] previewIds = new String[]{"classic", "shader", "panorama_3"};
        UFontRenderer tagFont = FPSMaster.fontManager.getFont(11);
        for (int i = 0; i < 3; i++) {
            float optionY = y + i * 23f;
            boolean selected = qaAnswers[qaStep] == i;
            boolean hovered = Hover.is(x, optionY, width, 19f, mouseX, mouseY);
            qaOptionHover[i] = (float) AnimMath.base(qaOptionHover[i], hovered ? 1.0 : 0.0, 0.22);
            renderQaOption(x, optionY, width, 19f, selected, hovered, qaOptionPress[i], question[i + 1], 27f);
            renderMiniBackgroundPreview(x + 6f, optionY + 4.5f, 16f, 10f, previewIds[i]);
            if ("shader".equals(previewIds[i]) && !isShaderBackgroundSupported()) {
                String tag = isChinese() ? "当前设备不支持" : "Unsupported";
                tagFont.drawString(tag, x + width - tagFont.getStringWidth(tag) - 9f, optionY + 7f,
                        ClickGuiTheme.danger().getRGB());
                continue;
            }
            if (!hasActiveModal() && consumePressInBounds(x, optionY, width, 19f, 0) != null) {
                qaOptionPress[i] = 1.0f;
                if ("shader".equals(previewIds[i]) && !ensureShaderBackgroundConfirmed()) {
                    return;
                }
                qaAnswers[qaStep] = i;
                applyQaAnswer();
                if (qaStep < 2) {
                    qaStep++;
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Footer: Back (ghost) + Continue/Finish (primary), right-aligned
    // ------------------------------------------------------------------

    private void renderFooter(int mouseX, int mouseY) {
        UFontRenderer font = FPSMaster.fontManager.s14;
        float btnH = 18f;
        float btnY = cardY + cardH - 12f - btnH;
        boolean allowNext = !hasActiveModal() && (page != 2 || tutorialPlaybackComplete);

        String nextLabel = getNextLabel();
        float nextW = Math.max(46f, font.getStringWidth(nextLabel) + UiChrome.BTN_PAD_X * 2f);
        float nextX = cardX + cardW - 12f - nextW;
        if (allowNext) {
            if (UiChrome.buttonClicked(this, nextX, btnY, nextW, btnH, null, nextLabel,
                    UiChrome.Style.PRIMARY, mouseX, mouseY)) {
                onNext();
            }
        } else {
            Rects.rounded(nextX, btnY, nextW, btnH, UiChrome.CTL_RADIUS, ClickGuiTheme.layerActive().getRGB(), false);
            font.drawCenteredString(nextLabel, nextX + nextW / 2f, btnY + btnH / 2f - 3.5f,
                    ClickGuiTheme.textDisabled().getRGB());
        }

        if (page > 0 && !hasActiveModal()) {
            String backLabel = key("oobe.back");
            float backW = Math.max(46f, font.getStringWidth(backLabel) + UiChrome.BTN_PAD_X * 2f);
            float backX = nextX - 6f - backW;
            if (UiChrome.buttonClicked(this, backX, btnY, backW, btnH, null, backLabel,
                    UiChrome.Style.GHOST, mouseX, mouseY)) {
                page--;
                pageMotion = 1f;
                pageMotionDirection = -1;
            }
        }
    }

    private void onNext() {
        if (page == 6 && !enterGuide) {
            finishOobe();
            return;
        }
        if (page >= PAGE_COUNT - 1) {
            finishOobe();
            return;
        }
        page++;
        pageMotion = 1f;
        pageMotionDirection = 1;
    }

    private void finishOobe() {
        applySelections();
        mc.displayGuiScreen(new MainMenu());
    }

    private void applySelections() {
        ClientSettings.language.setValue(languageValue);
        ClientSettings.followGameScale.setValue(followGameScaleEnabled);
        ClientSettings.fixedScale.setValue(fixedScaleIndex);

        FPSMaster.configManager.configure.background = backgroundChoice;
        FPSMaster.configManager.configure.antiCheatEnabled = antiCheatEnabled;
        FPSMaster.configManager.configure.anonymousDataEnabled = anonymousDataEnabled;
        FPSMaster.configManager.configure.oobeCompleted = true;
        System.out.println("[OOBE] Set oobeCompleted = true");

        applyDefaultModules();
        if (enterGuide) {
            applyQaModules();
        }

        try {
            FPSMaster.configManager.saveConfig(top.fpsmaster.modules.config.ConfigProfileUtils.getActiveProfileName());
        } catch (FileException e) {
            e.printStackTrace();
            System.err.println("Failed to save OOBE configuration: " + e.getMessage());
        }
    }

    private void applyDefaultModules() {
        setModuleEnabled(Performance.class, true);
        setModuleEnabled(OldAnimations.class, true);
        setModuleEnabled(ItemPhysics.class, true);
        setModuleEnabled(FPSDisplay.class, true);
        setModuleEnabled(Keystrokes.class, true);
        setModuleEnabled(CPSDisplay.class, true);
        setModuleEnabled(ComboDisplay.class, false);
        setModuleEnabled(PingDisplay.class, false);
        setModuleEnabled(DirectionDisplay.class, false);
        setModuleEnabled(CoordsDisplay.class, false);
        setModuleEnabled(InventoryDisplay.class, false);
    }

    private void applyQaModules() {
        if (qaAnswers[0] == 2) {
            setModuleEnabled(FPSDisplay.class, false);
            setModuleEnabled(Keystrokes.class, false);
            setModuleEnabled(CPSDisplay.class, false);
            setModuleEnabled(ComboDisplay.class, false);
            setModuleEnabled(PingDisplay.class, false);
            setModuleEnabled(DirectionDisplay.class, true);
        }

        if (qaAnswers[1] == 0) {
            backgroundChoice = "classic";
        } else if (qaAnswers[1] == 1) {
            backgroundChoice = "shader";
        } else if (qaAnswers[1] == 2) {
            backgroundChoice = "panorama_3";
        }

        if (qaAnswers[2] == 0) {
            setModuleEnabled(FPSDisplay.class, true);
            setModuleEnabled(Keystrokes.class, true);
            setModuleEnabled(CPSDisplay.class, true);
            setModuleEnabled(ComboDisplay.class, true);
            setModuleEnabled(PingDisplay.class, false);
            setModuleEnabled(CoordsDisplay.class, false);
            setModuleEnabled(InventoryDisplay.class, false);
        } else if (qaAnswers[2] == 1) {
            setModuleEnabled(FPSDisplay.class, false);
            setModuleEnabled(Keystrokes.class, false);
            setModuleEnabled(CPSDisplay.class, false);
            setModuleEnabled(ComboDisplay.class, false);
            setModuleEnabled(PingDisplay.class, true);
            setModuleEnabled(CoordsDisplay.class, true);
            setModuleEnabled(InventoryDisplay.class, true);
        } else if (qaAnswers[2] == 2) {
            setModuleEnabled(FPSDisplay.class, false);
            setModuleEnabled(Keystrokes.class, false);
            setModuleEnabled(CPSDisplay.class, false);
            setModuleEnabled(ComboDisplay.class, false);
            setModuleEnabled(PingDisplay.class, false);
            setModuleEnabled(CoordsDisplay.class, false);
            setModuleEnabled(InventoryDisplay.class, false);
            setModuleEnabled(DirectionDisplay.class, false);
        }
    }

    private void setModuleEnabled(Class<?> type, boolean enabled) {
        try {
            Module module = FPSMaster.moduleManager.getModule(type, true);
            module.set(enabled);
        } catch (IllegalStateException ignored) {
        }
    }

    private void applyQaAnswer() {
        if (qaStep == 0) {
            if (qaAnswers[0] == 0) {
                setFeatureCount(4);
            } else if (qaAnswers[0] == 1) {
                setFeatureCount(5);
            } else if (qaAnswers[0] == 2) {
                setFeatureCount(3);
            }
        }
        if (qaStep == 1) {
            if (qaAnswers[1] == 0) {
                backgroundChoice = "classic";
            } else if (qaAnswers[1] == 1) {
                backgroundChoice = "shader";
            } else if (qaAnswers[1] == 2) {
                backgroundChoice = "panorama_3";
            }
        }
    }

    private void setFeatureCount(int count) {
        featureCount = count;
    }

    private String getFeatureCountLabel() {
        return isChinese() ? featureCount + " " + key("oobe.features.count.unit") : String.valueOf(featureCount);
    }

    private String backgroundLabel() {
        if ("classic".equals(backgroundChoice)) {
            return key("oobe.background.classic");
        }
        if ("shader".equals(backgroundChoice)) {
            return key("oobe.background.shader");
        }
        if ("panorama_2".equals(backgroundChoice)) {
            return key("oobe.background.panorama2");
        }
        if ("panorama_3".equals(backgroundChoice)) {
            return key("oobe.background.panorama3");
        }
        return key("oobe.background.panorama1");
    }

    private String animatedGreeting() {
        return GREETINGS[greetingIndex];
    }

    private void switchLanguage(int newLanguage) {
        if (languageValue == newLanguage) {
            return;
        }
        languageValue = newLanguage;
        setPreviewLanguage(languageValue);
        updateTextFieldPlaceholders();
    }

    private String getNextLabel() {
        if (page == PAGE_COUNT - 1 || (page == 6 && !enterGuide)) {
            return key("oobe.finish");
        }
        return key("oobe.next");
    }

    // ------------------------------------------------------------------
    // Shader background confirmation + benchmark state machine
    // ------------------------------------------------------------------

    private boolean ensureShaderBackgroundConfirmed() {
        if (!isShaderBackgroundSupported()) {
            shaderUnsupportedDialogVisible = true;
            pendingBackgroundChoice = null;
            return false;
        }
        // Show benchmark confirmation dialog
        shaderBenchmarkConfirmDialogVisible = true;
        pendingBackgroundChoice = "shader";
        return false;
    }

    private void startShaderBenchmark() {
        shaderBenchmarkConfirmDialogVisible = false;
        shaderBenchmarkRunningDialogVisible = true;
        shaderBenchmarkProgress = 0f;
        shaderBenchmarkStartTime = System.nanoTime();

        // Initialize benchmark state
        benchmarkWarmupComplete = false;
        benchmarkElapsedNs = 0;
        benchmarkIterations = 0;
        benchmarkProgramId = createBenchmarkShaderProgram();
    }

    private void enableShaderWithoutBenchmark() {
        shaderBenchmarkConfirmDialogVisible = false;
        shaderBenchmarkScore = 0;
        confirmShaderBackgroundSelection();
    }

    private void confirmShaderBenchmarkResult() {
        shaderBenchmarkResultDialogVisible = false;
        confirmShaderBackgroundSelection();
    }

    private void cancelShaderBackgroundSelection() {
        shaderBenchmarkConfirmDialogVisible = false;
        shaderBenchmarkRunningDialogVisible = false;
        shaderBenchmarkResultDialogVisible = false;
        pendingBackgroundChoice = null;
    }

    private void confirmShaderBackgroundSelection() {
        shaderWarningDialogVisible = false;
        shaderBenchmarkResultDialogVisible = false;
        pendingBackgroundChoice = null;
        qaAnswers[qaStep] = 1; // shader option
        applyQaAnswer();
        if (qaStep < 2) {
            qaStep++;
        }
    }

    private void runWarmupPass() {
        // Warmup using actual shader rendering to stabilize GPU
        runShaderBenchmarkPass(30); // 30 iterations for warmup
        // Force GPU to complete all work
        GL11.glFinish();
        // Small delay to let GPU stabilize
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Run shader-based benchmark test
     * Runs for exactly 5 seconds to ensure stable, consistent results
     */
    private double runBenchmarkTest() {
        final long TEST_TIME_NS = 5_000_000_000L; // 5 seconds in nanoseconds
        final int BATCH_SIZE = 10; // Run in batches to track progress

        long startTime = System.nanoTime();
        long elapsed = 0;
        int totalIterations = 0;

        // Run benchmark for exactly 5 seconds
        while (elapsed < TEST_TIME_NS) {
            // Run a batch of shader renderings
            runShaderBenchmarkPass(BATCH_SIZE);
            GL11.glFinish(); // Wait for GPU to complete

            // Check elapsed time
            elapsed = System.nanoTime() - startTime;
            totalIterations += BATCH_SIZE;

            // Safety check - don't run forever if something goes wrong
            if (elapsed > TEST_TIME_NS * 2) {
                break;
            }
        }

        // Calculate score based on iterations per second
        double elapsedSec = elapsed / 1_000_000_000.0;
        double iterationsPerSec = totalIterations / elapsedSec;

        // Normalize to a reasonable score range
        // ~60 iter/sec at 5sec = 300 iter total ≈ score 25
        // ~100 iter/sec at 5sec = 500 iter total ≈ score 50
        return iterationsPerSec * 0.5;
    }

    /**
     * Run shader rendering benchmark pass
     * Uses a compute-intensive fragment shader that simulates shader background load
     */
    private void runShaderBenchmarkPass(int iterations) {
        // Use a simplified shader-based test that mimics the actual shader background
        // This creates a custom shader program for benchmarking
        int benchmarkProgram = createBenchmarkShaderProgram();

        float testWidth = 400f;
        float testHeight = 300f;
        float x = -testWidth - 10f; // Off-screen left (not visible)
        float y = -testHeight - 10f; // Off-screen top (not visible)

        // Save current OpenGL state
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        try {
            for (int i = 0; i < iterations; i++) {
                // Setup viewport for test rendering
                org.lwjgl.opengl.GL20.glUseProgram(benchmarkProgram);

                // Set uniforms
                int resolutionLoc = org.lwjgl.opengl.GL20.glGetUniformLocation(benchmarkProgram, "resolution");
                int timeLoc = org.lwjgl.opengl.GL20.glGetUniformLocation(benchmarkProgram, "time");
                int iterationLoc = org.lwjgl.opengl.GL20.glGetUniformLocation(benchmarkProgram, "iteration");

                if (resolutionLoc >= 0) {
                    org.lwjgl.opengl.GL20.glUniform2f(resolutionLoc, testWidth, testHeight);
                }
                if (timeLoc >= 0) {
                    org.lwjgl.opengl.GL20.glUniform1f(timeLoc, i * 0.1f);
                }
                if (iterationLoc >= 0) {
                    org.lwjgl.opengl.GL20.glUniform1i(iterationLoc, i);
                }

                // Render a full-screen quad that will invoke the fragment shader for every pixel
                // This is GPU-intensive as it runs the fragment shader for each pixel
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2f(0, 0);
                GL11.glVertex2f(x, y);
                GL11.glTexCoord2f(0, 1);
                GL11.glVertex2f(x, y + testHeight);
                GL11.glTexCoord2f(1, 1);
                GL11.glVertex2f(x + testWidth, y + testHeight);
                GL11.glTexCoord2f(1, 0);
                GL11.glVertex2f(x + testWidth, y);
                GL11.glEnd();
            }
        } finally {
            // Restore OpenGL state
            org.lwjgl.opengl.GL20.glUseProgram(0);
            GL11.glPopAttrib();
        }

        // Clean up the test program
        org.lwjgl.opengl.GL20.glDeleteProgram(benchmarkProgram);
    }

    /**
     * Run shader benchmark pass using a pre-created shader program.
     * More efficient when running multiple iterations.
     */
    private void runShaderBenchmarkPassOnProgram(int program, int iterations) {
        if (program == 0) {
            return;
        }

        float testWidth = 400f;
        float testHeight = 300f;
        float x = -testWidth - 10f; // Off-screen left (not visible)
        float y = -testHeight - 10f; // Off-screen top (not visible)

        // Save current OpenGL state
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        try {
            for (int i = 0; i < iterations; i++) {
                org.lwjgl.opengl.GL20.glUseProgram(program);

                int resolutionLoc = org.lwjgl.opengl.GL20.glGetUniformLocation(program, "resolution");
                int timeLoc = org.lwjgl.opengl.GL20.glGetUniformLocation(program, "time");
                int iterationLoc = org.lwjgl.opengl.GL20.glGetUniformLocation(program, "iteration");

                if (resolutionLoc >= 0) {
                    org.lwjgl.opengl.GL20.glUniform2f(resolutionLoc, testWidth, testHeight);
                }
                if (timeLoc >= 0) {
                    org.lwjgl.opengl.GL20.glUniform1f(timeLoc, i * 0.1f);
                }
                if (iterationLoc >= 0) {
                    org.lwjgl.opengl.GL20.glUniform1i(iterationLoc, i);
                }

                GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2f(0, 0);
                GL11.glVertex2f(x, y);
                GL11.glTexCoord2f(0, 1);
                GL11.glVertex2f(x, y + testHeight);
                GL11.glTexCoord2f(1, 1);
                GL11.glVertex2f(x + testWidth, y + testHeight);
                GL11.glTexCoord2f(1, 0);
                GL11.glVertex2f(x + testWidth, y);
                GL11.glEnd();
            }
        } finally {
            org.lwjgl.opengl.GL20.glUseProgram(0);
            GL11.glPopAttrib();
        }
    }

    /**
     * Create a compute-intensive shader program for benchmarking
     * This shader performs per-pixel calculations similar to shader backgrounds
     */
    private int createBenchmarkShaderProgram() {
        try {
            int program = org.lwjgl.opengl.GL20.glCreateProgram();

            // Simple vertex shader (passthrough) - no #version directive for compatibility
            String vertexShaderSource =
                    "attribute vec2 position;\n" +
                    "attribute vec2 texcoord;\n" +
                    "varying vec2 v_texcoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = vec4(position, 0.0, 1.0);\n" +
                    "  v_texcoord = texcoord;\n" +
                    "}\n";

            // Compute-intensive fragment shader that mimics shader background effects
            // Includes: sine wave calculations, color mixing, noise simulation
            String fragmentShaderSource =
                    "varying vec2 v_texcoord;\n" +
                    "uniform vec2 resolution;\n" +
                    "uniform float time;\n" +
                    "uniform int iteration;\n" +
                    "\n" +
                    "void main() {\n" +
                    "  vec2 uv = v_texcoord;\n" +
                    "  vec2 p = uv * 2.0 - 1.0;\n" +
                    "  \n" +
                    "  float t = time * 0.5 + float(iteration) * 0.01;\n" +
                    "  \n" +
                    "  for(int i = 0; i < 3; i++) {\n" +
                    "    p.x += sin(p.y * 3.0 + t) * 0.1;\n" +
                    "    p.y += cos(p.x * 2.5 + t * 0.8) * 0.1;\n" +
                    "  }\n" +
                    "  \n" +
                    "  vec3 col = vec3(0.0);\n" +
                    "  for(int i = 0; i < 2; i++) {\n" +
                    "    col.r += sin(p.x + t + float(i)) * 0.3;\n" +
                    "    col.g += cos(p.y + t * 0.7 + float(i)) * 0.3;\n" +
                    "    col.b += sin(p.x + p.y + t * 1.3) * 0.3;\n" +
                    "  }\n" +
                    "  \n" +
                    "  float d = length(p);\n" +
                    "  col += vec3(sin(d * 5.0 - t), sin(d * 5.0 - t + 2.0), sin(d * 5.0 - t + 4.0)) * 0.2;\n" +
                    "  \n" +
                    "  col = abs(col) * 0.3 + 0.1;\n" +
                    "  \n" +
                    "  gl_FragColor = vec4(col, 1.0);\n" +
                    "}\n";

            int vertexShader = compileShader(vertexShaderSource, org.lwjgl.opengl.GL20.GL_VERTEX_SHADER);
            int fragmentShader = compileShader(fragmentShaderSource, org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER);

            if (vertexShader == 0 || fragmentShader == 0) {
                return 0;
            }

            org.lwjgl.opengl.GL20.glAttachShader(program, vertexShader);
            org.lwjgl.opengl.GL20.glAttachShader(program, fragmentShader);
            org.lwjgl.opengl.GL20.glLinkProgram(program);

            int linked = org.lwjgl.opengl.GL20.glGetProgrami(program, org.lwjgl.opengl.GL20.GL_LINK_STATUS);
            if (linked == 0) {
                String log = org.lwjgl.opengl.GL20.glGetProgramInfoLog(program, 512);
                ClientLogger.error("Shader program link failed: " + log);
                org.lwjgl.opengl.GL20.glDeleteProgram(program);
                return 0;
            }

            // Clean up shader objects (they're now attached to the program)
            org.lwjgl.opengl.GL20.glDeleteShader(vertexShader);
            org.lwjgl.opengl.GL20.glDeleteShader(fragmentShader);

            return program;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Compile a GLSL shader
     */
    private int compileShader(String source, int type) {
        int shader = org.lwjgl.opengl.GL20.glCreateShader(type);
        org.lwjgl.opengl.GL20.glShaderSource(shader, source);
        org.lwjgl.opengl.GL20.glCompileShader(shader);

        int compiled = org.lwjgl.opengl.GL20.glGetShaderi(shader, org.lwjgl.opengl.GL20.GL_COMPILE_STATUS);
        if (compiled == 0) {
            String typeName = type == org.lwjgl.opengl.GL20.GL_VERTEX_SHADER ? "vertex" : "fragment";
            String log = org.lwjgl.opengl.GL20.glGetShaderInfoLog(shader, 512);
            ClientLogger.error("Shader compilation failed (" + typeName + "): " + log);
            ClientLogger.error("Shader source: " + source);
            org.lwjgl.opengl.GL20.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    /**
     * Compile a GLSL shader
     */
    private int compileShaderOld(String source, int type) {
        int shader = org.lwjgl.opengl.GL20.glCreateShader(type);
        org.lwjgl.opengl.GL20.glShaderSource(shader, source);
        org.lwjgl.opengl.GL20.glCompileShader(shader);

        int compiled = org.lwjgl.opengl.GL20.glGetShaderi(shader, org.lwjgl.opengl.GL20.GL_COMPILE_STATUS);
        if (compiled == 0) {
            org.lwjgl.opengl.GL20.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    private boolean isShaderBackgroundSupported() {
        return OSUtil.supportShader() && OpenGlHelper.shadersSupported && OpenGlHelper.framebufferSupported;
    }

    private boolean hasActiveModal() {
        return shaderWarningDialogVisible
                || shaderUnsupportedDialogVisible
                || shaderBenchmarkConfirmDialogVisible
                || shaderBenchmarkRunningDialogVisible
                || shaderBenchmarkResultDialogVisible;
    }

    // ------------------------------------------------------------------
    // Modal dialogs
    // ------------------------------------------------------------------

    private void renderShaderDialogs(int mouseX, int mouseY) {
        if (!hasActiveModal()) {
            return;
        }

        Rects.fill(0f, 0f, guiWidth, guiHeight, ClickGuiTheme.veil());

        // Render Benchmark Confirmation Dialog
        if (shaderBenchmarkConfirmDialogVisible) {
            renderBenchmarkConfirmDialog(mouseX, mouseY);
            return;
        }

        // Render Benchmark Running Dialog
        if (shaderBenchmarkRunningDialogVisible) {
            renderBenchmarkRunningDialog();
            return;
        }

        // Render Benchmark Result Dialog
        if (shaderBenchmarkResultDialogVisible) {
            renderBenchmarkResultDialog(mouseX, mouseY);
            return;
        }

        // Render Unsupported Dialog
        if (shaderUnsupportedDialogVisible) {
            renderUnsupportedDialog(mouseX, mouseY);
            return;
        }

        // Render Low Performance Warning Dialog (legacy, for old code path)
        if (shaderWarningDialogVisible) {
            renderLowPerformanceDialog(mouseX, mouseY);
        }
    }

    private void renderBenchmarkConfirmDialog(int mouseX, int mouseY) {
        float width = clamp(guiWidth * 0.38f, 180f, 230f);
        float height = 110f;
        float x = (guiWidth - width) / 2f;
        float y = (guiHeight - height) / 2f;

        UiChrome.panel(x, y, width, height);

        String title = isChinese() ? "GPU 性能测试" : "GPU Benchmark";
        String body = isChinese()
                ? "Shader 背景需要较高的 GPU 性能。建议进行性能测试以确定您的设备是否适合使用。"
                : "Shader backgrounds require higher GPU performance. We recommend running a benchmark to check if your device is suitable.";

        UiChrome.boldString(FPSMaster.fontManager.getFont(15), title, x + 10f, y + 10f,
                ClickGuiTheme.textPrimary().getRGB());
        drawWrapped(FPSMaster.fontManager.getFont(11), body, x + 10f, y + 26f, width - 20f, 6.5f, 5,
                ClickGuiTheme.textSecondary().getRGB());

        float btnY = y + height - 25f;
        shaderBenchmarkConfirmYesButton.setText(isChinese() ? "运行测试" : "Run Test")
                .renderInScreen(this, x + width - 156f, btnY, 48f, 15f, mouseX, mouseY);
        shaderBenchmarkConfirmNoButton.setText(isChinese() ? "直接开启" : "Enable Anyway")
                .renderInScreen(this, x + width - 104f, btnY, 56f, 15f, mouseX, mouseY);
        shaderBenchmarkConfirmSkipButton.setText(isChinese() ? "取消" : "Cancel")
                .renderInScreen(this, x + width - 44f, btnY, 34f, 15f, mouseX, mouseY);
    }

    private void renderBenchmarkRunningDialog() {
        float width = clamp(guiWidth * 0.32f, 150f, 200f);
        float height = 90f;
        float x = (guiWidth - width) / 2f;
        float y = (guiHeight - height) / 2f;

        UiChrome.panel(x, y, width, height);

        String title = isChinese() ? "正在测试..." : "Running Benchmark...";
        UiChrome.boldString(FPSMaster.fontManager.getFont(15), title, x + 10f, y + 10f,
                ClickGuiTheme.textPrimary().getRGB());

        float progressY = y + 38f;
        float progressWidth = width - 20f;
        float progressHeight = 4f;
        float progressX = x + 10f;

        // Progress bar background
        Rects.rounded(progressX, progressY, progressWidth, progressHeight, 2,
                ClickGuiTheme.layerActive().getRGB(), false);

        // Execute a small chunk of benchmark each frame (on main thread with OpenGL context)
        executeBenchmarkChunk();

        // Determine current phase and status text based on progress
        String statusText;
        float fillWidth;

        if (shaderBenchmarkProgress < 0.3f) {
            // Warmup phase
            float warmupProgress = shaderBenchmarkProgress / 0.3f;
            fillWidth = progressWidth * warmupProgress * 0.5f;
            statusText = isChinese() ? "正在预热 GPU..." : "Warming up GPU...";
        } else {
            // Actual benchmark phase (30%-100%)
            float benchmarkProgress = (shaderBenchmarkProgress - 0.3f) / 0.7f;
            fillWidth = progressWidth * (0.5f + benchmarkProgress * 0.5f);
            statusText = isChinese() ? "正在运行着色器测试..." : "Running shader test...";
        }

        // Add subtle animation to the progress bar
        float animatedWidth = fillWidth + (float) Math.sin(System.currentTimeMillis() / 100.0) * 1f;
        animatedWidth = Math.max(2f, Math.min(animatedWidth, progressWidth));

        Rects.rounded(progressX, progressY, animatedWidth, progressHeight, 2,
                ClickGuiTheme.accent().getRGB(), false);

        FPSMaster.fontManager.getFont(11).drawCenteredString(statusText, x + width / 2f, progressY + 10f,
                ClickGuiTheme.textSecondary().getRGB());
    }

    /**
     * Execute a small chunk of the benchmark on each frame.
     * This runs on the main thread with OpenGL context available.
     */
    private void executeBenchmarkChunk() {
        final long TEST_TIME_NS = 5_000_000_000L; // 5 seconds
        final int WARMUP_ITERATIONS = 20;
        final int BATCHES_PER_FRAME = 5; // Execute 5 batches per frame to keep UI responsive

        if (benchmarkProgramId == 0) {
            // Shader compilation failed, abort with zero score
            ClientLogger.error("Benchmark shader program failed to compile");
            shaderBenchmarkScore = 0;
            shaderBenchmarkRunningDialogVisible = false;
            shaderBenchmarkResultDialogVisible = true;
            return;
        }

        // Phase 1: Warmup (0-30% of progress)
        if (!benchmarkWarmupComplete) {
            runShaderBenchmarkPassOnProgram(benchmarkProgramId, WARMUP_ITERATIONS);
            GL11.glFinish();
            benchmarkWarmupComplete = true;
            shaderBenchmarkProgress = 0.3f;
            benchmarkElapsedNs = 0;
            benchmarkIterations = 0;
            benchmarkStartTime = System.nanoTime();
            return;
        }

        // Phase 2: Actual benchmark (30-100% of progress)
        long frameStartNs = System.nanoTime();
        for (int i = 0; i < BATCHES_PER_FRAME && benchmarkElapsedNs < TEST_TIME_NS; i++) {
            runShaderBenchmarkPassOnProgram(benchmarkProgramId, 10);
            benchmarkIterations += 10;
        }
        GL11.glFinish();

        benchmarkElapsedNs = System.nanoTime() - benchmarkStartTime;

        // Update progress (30% to 100% during actual test)
        double progressRatio = (double) benchmarkElapsedNs / TEST_TIME_NS;
        shaderBenchmarkProgress = (float) (0.3 + Math.min(progressRatio, 1.0) * 0.7);

        // Check if benchmark is complete
        if (benchmarkElapsedNs >= TEST_TIME_NS) {
            // Calculate final score
            double elapsedSec = benchmarkElapsedNs / 1_000_000_000.0;
            double iterationsPerSec = benchmarkIterations / elapsedSec;
            shaderBenchmarkScore = iterationsPerSec * 0.5;

            // Clean up shader program
            org.lwjgl.opengl.GL20.glDeleteProgram(benchmarkProgramId);
            benchmarkProgramId = 0;

            // Show result
            shaderBenchmarkRunningDialogVisible = false;
            shaderBenchmarkResultDialogVisible = true;
            shaderBenchmarkProgress = 1.0f;
        }
    }

    private void renderBenchmarkResultDialog(int mouseX, int mouseY) {
        float width = clamp(guiWidth * 0.36f, 170f, 220f);
        float height = 120f;
        float x = (guiWidth - width) / 2f;
        float y = (guiHeight - height) / 2f;

        // Determine if score is good enough (threshold: 25)
        boolean isGoodScore = shaderBenchmarkScore >= 25.0;
        Color titleColor = isGoodScore ? ClickGuiTheme.ok() : new Color(240, 160, 100);

        UiChrome.panel(x, y, width, height);

        String title = isChinese()
                ? (isGoodScore ? "GPU 性能良好" : "GPU 性能较低")
                : (isGoodScore ? "GPU Performance Good" : "GPU Performance Low");
        String scoreText = isChinese()
                ? ("测试分数: " + formatBenchmarkScore(shaderBenchmarkScore))
                : ("Benchmark Score: " + formatBenchmarkScore(shaderBenchmarkScore));

        UiChrome.boldString(FPSMaster.fontManager.getFont(15), title, x + 10f, y + 10f, titleColor.getRGB());

        String formattedScore = String.format("%.1f", shaderBenchmarkScore);
        UiChrome.boldCentered(FPSMaster.fontManager.getFont(18), formattedScore, x + width / 2f, y + 28f,
                titleColor.getRGB());
        FPSMaster.fontManager.getFont(11).drawCenteredString(scoreText, x + width / 2f, y + 42f,
                ClickGuiTheme.textDisabled().getRGB());

        String body;
        if (isGoodScore) {
            body = isChinese()
                    ? "您的 GPU 性能足以流畅运行 Shader 背景效果。"
                    : "Your GPU performance is sufficient for smooth shader background effects.";
        } else {
            body = isChinese()
                    ? "您的 GPU 性能可能不足以流畅运行 Shader 背景，开启后可能出现卡顿。是否仍要开启？"
                    : "Your GPU may not handle shader backgrounds smoothly. You may experience stuttering. Continue anyway?";
        }

        drawWrapped(FPSMaster.fontManager.getFont(11), body, x + 10f, y + 54f, width - 20f, 6.5f, 4,
                ClickGuiTheme.textSecondary().getRGB());

        float btnY = y + height - 25f;
        if (isGoodScore) {
            shaderBenchmarkResultOkButton.setText(isChinese() ? "开启 Shader 背景" : "Enable Shader")
                    .renderInScreen(this, x + width - 72f, btnY, 62f, 15f, mouseX, mouseY);
        } else {
            shaderCancelButton.setText(isChinese() ? "取消" : "Cancel")
                    .renderInScreen(this, x + width - 100f, btnY, 34f, 15f, mouseX, mouseY);
            shaderContinueButton.setText(isChinese() ? "仍要开启" : "Enable Anyway")
                    .renderInScreen(this, x + width - 62f, btnY, 52f, 15f, mouseX, mouseY);
        }
    }

    private void renderUnsupportedDialog(int mouseX, int mouseY) {
        float width = clamp(guiWidth * 0.32f, 150f, 200f);
        float height = 80f;
        float x = (guiWidth - width) / 2f;
        float y = (guiHeight - height) / 2f;

        UiChrome.panel(x, y, width, height);

        String title = isChinese() ? "Shader 背景不可用" : "Shader background unsupported";
        String body = isChinese()
                ? "检测到当前设备不支持 shader 背景，已禁用该选项。"
                : "Your device does not support shader backgrounds. This option has been disabled.";

        UiChrome.boldString(FPSMaster.fontManager.getFont(15), title, x + 10f, y + 10f,
                ClickGuiTheme.textPrimary().getRGB());
        drawWrapped(FPSMaster.fontManager.getFont(11), body, x + 10f, y + 26f, width - 20f, 6.5f, 3,
                ClickGuiTheme.textSecondary().getRGB());

        shaderUnsupportedOkButton.setText(isChinese() ? "知道了" : "OK")
                .renderInScreen(this, x + width - 50f, y + height - 25f, 40f, 15f, mouseX, mouseY);
    }

    private void renderLowPerformanceDialog(int mouseX, int mouseY) {
        float width = clamp(guiWidth * 0.34f, 160f, 210f);
        float height = 88f;
        float x = (guiWidth - width) / 2f;
        float y = (guiHeight - height) / 2f;

        UiChrome.panel(x, y, width, height);

        String title = isChinese() ? "是否继续使用？" : "Continue anyway?";
        String body = isChinese()
                ? "您的 GPU 性能较低，使用本背景样式可能引起卡顿，是否继续？基准分数: " + formatBenchmarkScore(shaderBenchmarkScore)
                : "Your GPU appears to be low performance. This background may cause stutter. Benchmark score: " + formatBenchmarkScore(shaderBenchmarkScore);

        UiChrome.boldString(FPSMaster.fontManager.getFont(15), title, x + 10f, y + 10f,
                ClickGuiTheme.textPrimary().getRGB());
        drawWrapped(FPSMaster.fontManager.getFont(11), body, x + 10f, y + 26f, width - 20f, 6.5f, 4,
                ClickGuiTheme.textSecondary().getRGB());

        shaderCancelButton.setText(isChinese() ? "取消" : "Cancel")
                .renderInScreen(this, x + width - 96f, y + height - 25f, 34f, 15f, mouseX, mouseY);
        shaderContinueButton.setText(isChinese() ? "继续" : "Continue")
                .renderInScreen(this, x + width - 58f, y + height - 25f, 48f, 15f, mouseX, mouseY);
    }

    private String formatBenchmarkScore(double score) {
        int integer = (int) Math.round(score * 10.0);
        return String.valueOf(integer / 10.0);
    }

    private void renderMiniBackgroundPreview(float x, float y, float width, float height, String id) {
        if ("classic".equals(id)) {
            Rects.rounded(x, y, width, height, 3, new Color(43, 50, 65).getRGB(), false);
            return;
        }
        if ("shader".equals(id)) {
            Rects.rounded(x, y, width, height, 3, new Color(46, 71, 173).getRGB(), false);
            Rects.fill(x, y, width, height, new Color(143, 160, 255, 72));
            return;
        }
        Images.draw(PANORAMA_THREE, x, y, width, height, -1);
    }

    // ------------------------------------------------------------------
    // Text + widget helpers
    // ------------------------------------------------------------------

    private void drawTextField(TextField field, float x, float y, float width, float height) {
        UiChrome.inputBox(x, y, width, height, field.isFocused());
        field.drawTextBox(x, y, width, height);
        PointerEvent outsideClick = peekAnyPress();
        if (outsideClick != null && !Hover.is(x, y, width, height, outsideClick.x, outsideClick.y)) {
            field.setFocused(false);
        }
        PointerEvent click = consumePressInBounds(x, y, width, height, 0);
        if (click != null) {
            field.mouseClicked(click.x, click.y, click.button);
        }
    }

    /**
     * Word/segment wrapped text. Splits after CJK punctuation and before spaces like the old
     * body-text helper did. Returns the number of lines drawn (at most {@code maxLines}).
     */
    private int drawWrapped(UFontRenderer font, String text, float x, float y, float width, float lineHeight,
                            int maxLines, int color) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        String[] segments = text.split("(?<=[，。：；！？、 ])|(?= )");
        StringBuilder line = new StringBuilder();
        int lineIndex = 0;
        for (int i = 0; i < segments.length && lineIndex < maxLines; i++) {
            String candidate = line.toString() + segments[i];
            if (font.getStringWidth(candidate) > width && line.length() > 0) {
                font.drawString(line.toString(), x, y + lineIndex * lineHeight, color);
                line = new StringBuilder(segments[i]);
                lineIndex++;
            } else {
                line.append(segments[i]);
            }
        }
        if (lineIndex < maxLines && line.length() > 0) {
            font.drawString(line.toString(), x, y + lineIndex * lineHeight, color);
            lineIndex++;
        }
        return lineIndex;
    }

    private void updateTutorialAutoplay() {
        if (page != 2) {
            tutorialPlaybackComplete = true;
            return;
        }
        long now = System.currentTimeMillis();
        long elapsed = Math.max(0L, now - savedTutorialStartedAt);
        int slide = (int) ((elapsed / TUTORIAL_SLIDE_DURATION_MS) % 3L);
        if (slide != tutorialIndex) {
            tutorialPrevSlide = tutorialIndex;
            tutorialSlideTransition = 0f;
        }
        tutorialIndex = slide;
        if (elapsed >= TUTORIAL_SLIDE_DURATION_MS * 3L) {
            tutorialPlaybackComplete = true;
        }
    }

    private String extendTutorialDescription(String base, int index) {
        if (isChinese()) {
            if (index == 0) {
                return base + " 你可以在这里开启、关闭或调整客户端的大部分功能，并快速完成最常用的配置。";
            }
            if (index == 1) {
                return base + " 常见 HUD 组件都可以自由摆放到更适合自己的位置，打造更顺手的游戏界面。";
            }
            return base + " 调整到舒服的尺寸后，游戏内阅读信息会更自然，也能减少不必要的视觉干扰。";
        }
        if (index == 0) {
            return base + " Most client features can be enabled, disabled or configured from there with only a few clicks.";
        }
        if (index == 1) {
            return base + " Common HUD widgets can be moved into positions that better match your play style.";
        }
        return base + " Once resized to a comfortable scale, the HUD will be easier to read and less distracting in game.";
    }

    private String pageStepLabel() {
        return isChinese() ? "第 " + (page + 1) + " 步" : "Step " + (page + 1);
    }

    private void updateTextFieldPlaceholders() {
        accountField.placeHolder = key("oobe.login.account.placeholder");
        passwordField.placeHolder = key("oobe.login.password.placeholder");
    }

    private void applyLiveScaleSettings() {
        ClientSettings.followGameScale.setValue(followGameScaleEnabled);
        ClientSettings.fixedScale.setValue(fixedScaleIndex);
    }

    private void initSessionStateIfNeeded() {
        if (sessionStateInitialized) {
            return;
        }
        savedPage = 0;
        savedLanguageValue = ClientSettings.language.getValue();
        savedTutorialIndex = 0;
        savedAntiCheatEnabled = FPSMaster.configManager.configure.antiCheatEnabled;
        savedAnonymousDataEnabled = FPSMaster.configManager.configure.anonymousDataEnabled;
        savedEnterGuide = true;
        savedQaStep = 0;
        savedQaAnswers[0] = -1;
        savedQaAnswers[1] = -1;
        savedQaAnswers[2] = -1;
        savedBackgroundChoice = FPSMaster.configManager.configure.background == null ? "panorama_1" : FPSMaster.configManager.configure.background;
        savedLoginSkipped = true;
        savedIsLoggingIn = false;
        savedLoginError = null;
        savedLoginWelcomeShown = false;
        savedWelcomeUsername = null;
        savedWelcomeUserLevel = 0;
        savedFeatureCount = 5;
        savedAccountText = "";
        savedPasswordText = "";
        sessionStateInitialized = true;
    }

    private void restoreSessionState() {
        page = savedPage;
        languageValue = savedLanguageValue;
        followGameScaleEnabled = ClientSettings.followGameScale.getValue();
        fixedScaleIndex = ClientSettings.fixedScale.getValue();
        tutorialIndex = savedTutorialIndex;
        antiCheatEnabled = savedAntiCheatEnabled;
        anonymousDataEnabled = savedAnonymousDataEnabled;
        enterGuide = savedEnterGuide;
        qaStep = savedQaStep;
        qaAnswers[0] = savedQaAnswers[0];
        qaAnswers[1] = savedQaAnswers[1];
        qaAnswers[2] = savedQaAnswers[2];
        backgroundChoice = savedBackgroundChoice;
        loginSkipped = savedLoginSkipped;
        isLoggingIn = savedIsLoggingIn;
        loginError = savedLoginError;
        loginWelcomeShown = savedLoginWelcomeShown;
        welcomeUsername = savedWelcomeUsername;
        welcomeUserLevel = savedWelcomeUserLevel;
        featureCount = savedFeatureCount;
        // Reset temporary UI states
        expandedFeatureCard = -1;
        featureDetailExpand = 0f;
    }

    private void syncSessionState() {
        savedPage = page;
        savedLanguageValue = languageValue;
        savedTutorialIndex = tutorialIndex;
        savedAntiCheatEnabled = antiCheatEnabled;
        savedAnonymousDataEnabled = anonymousDataEnabled;
        savedEnterGuide = enterGuide;
        savedQaStep = qaStep;
        savedQaAnswers[0] = qaAnswers[0];
        savedQaAnswers[1] = qaAnswers[1];
        savedQaAnswers[2] = qaAnswers[2];
        savedBackgroundChoice = backgroundChoice;
        savedLoginSkipped = loginSkipped;
        savedIsLoggingIn = isLoggingIn;
        savedLoginError = loginError;
        savedLoginWelcomeShown = loginWelcomeShown;
        savedWelcomeUsername = welcomeUsername;
        savedWelcomeUserLevel = welcomeUserLevel;
        savedFeatureCount = featureCount;
        if (accountField != null) {
            savedAccountText = accountField.getText();
        }
        if (passwordField != null) {
            savedPasswordText = passwordField.getText();
        }
    }

    private Color blendColor(Color from, Color to, float progress) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        int r = (int) (from.getRed() + (to.getRed() - from.getRed()) * clamped);
        int g = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * clamped);
        int b = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * clamped);
        int a = (int) (from.getAlpha() + (to.getAlpha() - from.getAlpha()) * clamped);
        return new Color(r, g, b, a);
    }

    private float easeOutCubic(float progress) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        float inverse = 1f - clamped;
        return 1f - inverse * inverse * inverse;
    }

    private void openLink(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {
        }
    }

    private void setPreviewLanguage(int value) {
        try {
            FPSMaster.i18n.read(value == 1 ? "zh_cn" : "en_us");
        } catch (FileException ignored) {
        }
    }

    private String key(String key) {
        return FPSMaster.i18n.get(key);
    }

    private boolean isChinese() {
        return languageValue == 1;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            return;
        }
        accountField.textboxKeyTyped(typedChar, keyCode);
        passwordField.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
