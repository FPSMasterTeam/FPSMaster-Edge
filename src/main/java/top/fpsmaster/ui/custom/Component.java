package top.fpsmaster.ui.custom;

import top.fpsmaster.utils.render.draw.Colors;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.ui.click.MainPanel;
import top.fpsmaster.utils.core.Utility;
import top.fpsmaster.utils.render.gui.GuiOcclusion;
import top.fpsmaster.utils.render.gui.GuiScale;
import top.fpsmaster.utils.render.state.Alpha;
import top.fpsmaster.utils.math.anim.AnimMath;

import java.awt.*;

public class Component {
    private static final Color STENCIL_MASK_COLOR = new Color(255, 255, 255, 255);

    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 4.5f;
    private static final float HANDLE_SIZE = 6f;
    /** Cursor travel, in screen pixels, needed to change the scale by 1x. */
    private static final float RESIZE_SENSITIVITY = 1f / 60f;

    /**
     * Alignment threshold in component space units (1 unit = 2 real pixels): an edge closer than
     * this to a candidate edge engages the snap and draws a guide line.
     */
    private static final float ALIGN_SNAP_DIST = 5f;

    /** Guide line width in component space units. */
    private static final float GUIDE_THICKNESS = 1.5f;

    private static final Color GUIDE_ACTIVE_COLOR = new Color(89, 101, 241, 230);
    private static final Color GUIDE_PASSIVE_COLOR = new Color(89, 101, 241, 70);

    /**
     * Inverse-verify tolerance in component space units: a snap whose target the position formula
     * cannot reproduce within this, or which sits more than this outside the drag envelope, is skipped.
     */
    private static final float SNAP_EXPRESSIBLE_TOLERANCE = 1f;

    /** Max guide lines per frame: the two screen-centre cross lines plus one matched edge per axis. */
    private static final int MAX_GUIDE_LINES = 4;

    private float dragX = 0f;

    private float dragY = 0f;

    private float resizeStartScale = 1f;

    private int resizeStartMouseX;

    private int resizeStartMouseY;

    /** Guide lines to draw this frame while this component is being dragged, or {@code null}. */
    private GuideLine[] guides;

    public InterfaceModule mod;

    public float x = 0f;

    public float y = 0f;

    public float width = 0f;

    public float height = 0f;

    public float scale = 1f;

    public boolean allowScale = false;

    /** Consecutive render failures; reset on any successful frame. See ComponentsManager. */
    public int renderFailures = 0;

    public Position position = Position.LT;

    @SuppressWarnings("unchecked")
    public Component(Class<?> clazz) {
        Module module;
        try {
            module = FPSMaster.moduleManager.getModule((Class<? extends Module>) clazz);
        } catch (IllegalStateException exception) {
            ClientLogger.warn("Missing interface module for component: " + clazz.getName());
            this.mod = new InterfaceModule(clazz.getSimpleName(), Category.Interface);
            this.mod.set(false);
            return;
        }
        if (module instanceof InterfaceModule) {
            this.mod = (InterfaceModule) module;
            return;
        }

        ClientLogger.warn("Missing interface module for component: " + clazz.getName());
        this.mod = new InterfaceModule(clazz.getSimpleName(), Category.Interface);
        this.mod.set(false);
    }

    public void draw(float x, float y) {
    }

    /**
     * Computes {@link #width}/{@link #height} for this frame, before anything reads them.
     *
     * <p>Historically every component assigned its size inside {@code draw()}, but anchoring
     * ({@link #getRealPosition}), hover testing, drag clamping and the blur mask all run <em>before</em>
     * {@code draw()} — so they used the previous frame's size. A component whose size changes each
     * frame (FPS going {@code 99fps} → {@code 100fps}, a potion expiring) visibly jitters, and on its
     * very first frame {@code width} is still 0.
     *
     * <p>Default implementation does nothing, so components that have not been migrated keep their old
     * behaviour. Override it, move the sizing math here, and {@code draw()} becomes pure rendering.
     */
    public void measure() {
    }

    /** Receives the rectangles that make up a component's background. */
    public interface ShapeSink {
        /**
         * @param x absolute left edge, already scaled by the caller (same convention as
         *          {@link #drawRect}: positions are the caller's job, sizes are the base class's)
         * @param y absolute top edge
         * @param width  logical width; the base class multiplies by {@link #scale}
         * @param height logical height
         */
        void rect(float x, float y, float width, float height);
    }

    /**
     * Declares the geometry of this component's background so the blur mask can reproduce it exactly
     * instead of guessing.
     *
     * <p>The mask used to assume every component draws one rectangle at {@code (rX - 2, rY)} sized
     * {@code width × height}. That holds for about two thirds of them; the rest either use a different
     * origin (Keystrokes, PotionDisplay) or draw several disjoint boxes (ArmorDisplay), so the blur
     * leaked into the gaps or sat a couple of pixels off.
     *
     * <p>Whatever a component declares here must match what it actually paints in {@code draw()}.
     */
    public void backgroundShape(ShapeSink sink, float originX, float originY) {
        sink.rect(originX - 2f, originY, width, height);
    }

    public float alpha = 0f;

    public boolean shouldDisplay() {
        return mod.isEnabled();
    }

    /**
     * Whether this component currently has geometry another component may align to.
     *
     * <p>Some enabled HUD modules are conditional: a target HUD has no box without a target, for
     * example. Those components override this rather than letting their last non-zero width/height
     * become an invisible snap target.
     */
    public boolean isVisibleForAlignment() {
        return shouldDisplay() && width > 0f && height > 0f;
    }

    public float[] getRealPosition() {
        return getRealPosition(new ScaledResolution(Minecraft.getMinecraft()));
    }

    public float[] getRealPosition(ScaledResolution sr) {
        float rX = 0f;
        float rY = 0f;
        x = Math.max(0f, Math.min(1f, x));
        y = Math.max(0f, Math.min(1f, y));

        // The space these coordinates live in is set by ComponentsManager, which converts the mouse
        // by sr.getScaleFactor() and has GuiScale scale the matrix by 2 / sr.getScaleFactor(). This
        // has to read the same factor. Reading the client's own UI scale instead - which is 1 unless
        // the interface is set to follow the game's - made the usable area half as wide and half as
        // tall, so a component could only be dragged around the top-left quarter of the screen.
        float scaleFactor = sr.getScaleFactor();
        float guiWidth = sr.getScaledWidth() / 2f * scaleFactor;
        float guiHeight = sr.getScaledHeight() / 2f * scaleFactor;

        // Anchors offset by the component's *rendered* size. width/height are logical units; everything
        // that touches the drawn box (drawRect, hover, drag clamping, blur mask) multiplies by scale,
        // so these must too — otherwise a scaled-up right-anchored component overflows the screen edge
        // by width * (scale - 1).
        float scaledWidth = width * scale;
        float scaledHeight = height * scale;

        switch (position) {
            case LT:
                rX = x * guiWidth / 2f;
                rY = y * guiHeight / 2f;
                break;
            case RT:
                rX = guiWidth - (x * guiWidth / 2f + scaledWidth);
                rY = y * guiHeight / 2f;
                break;
            case LB:
                rX = x * guiWidth / 2f;
                rY = guiHeight - (y * guiHeight / 2f + scaledHeight);
                break;
            case RB:
                rX = guiWidth - (x * guiWidth / 2f + scaledWidth);
                rY = guiHeight - (y * guiHeight / 2f + scaledHeight);
                break;
            case CT:
                rX = guiWidth / 2f - scaledWidth / 2f;
                rY = y * guiHeight / 2f;
                break;
        }
        return new float[]{rX, rY};
    }

    public void drawBlurMask(ScaledResolution sr) {
        if (!hasBackground() || width <= 0f || height <= 0f) {
            return;
        }
        float[] pos = getRealPosition(sr);
        boolean round = mod.rounded.getValue();
        int radius = mod.roundRadius.getValue().intValue();
        // Same geometry the component paints, so the mask can never drift from the background.
        backgroundShape((rectX, rectY, rectWidth, rectHeight) -> {
            float scaledWidth = rectWidth * scale;
            float scaledHeight = rectHeight * scale;
            if (scaledWidth <= 0f || scaledHeight <= 0f) {
                return;
            }
            if (round) {
                Rects.roundedImage(Math.round(rectX), Math.round(rectY), Math.round(scaledWidth), Math.round(scaledHeight), radius, STENCIL_MASK_COLOR);
            } else {
                Rects.fill(rectX, rectY, scaledWidth, scaledHeight, STENCIL_MASK_COLOR);
            }
        }, pos[0], pos[1]);
    }

    public void display(ScaledResolution sr, int mouseX, int mouseY) {
        float[] pos = getRealPosition(sr);
        float rX = pos[0];
        float rY = pos[1];
        if ((Utility.mc.currentScreen instanceof GuiChat || Utility.mc.currentScreen instanceof MainPanel)) {
            guides = null;
            float scaledWidth = width * scale;
            float scaledHeight = height * scale;
            ComponentsManager manager = FPSMaster.componentsManager;
            boolean drag = manager.dragTarget == this;
            boolean hovered = Hover.is(rX, rY, scaledWidth, scaledHeight, mouseX, mouseY);
            boolean overHandle = allowScale && isOverResizeHandle(rX, rY, scaledWidth, scaledHeight, mouseX, mouseY);
            boolean interactive = hovered || overHandle || drag;
            boolean occluded = false;

            alpha = (float) ((hovered || overHandle || drag) ?
                    AnimMath.base(alpha, 1f, 0.2f) : AnimMath.base(alpha, 0.0f, 0.2f));

            if (interactive) {
                // The HUD editor runs from EventRender2D, which 1.8.9 fires while drawing the in-game
                // overlay — before currentScreen.drawScreen(). Without this it happily drags components
                // that are sitting *underneath* an open panel. The previous guard only covered the case
                // where a ClickGUI slider already held a drag capture, which is a small fraction of the
                // panel's area.
                occluded = GuiOcclusion.covers(Mouse.getX(), Utility.mc.displayHeight - Mouse.getY());
                if (occluded) {
                    guides = null;
                } else {
                    if (allowScale && ClientSettings.isZoomBindDown()) {
                        int dWheel = Mouse.getDWheel();
                        if (dWheel > 0) scaleUp();
                        else if (dWheel < 0) scaleDown();
                    }

                    if (Mouse.isButtonDown(0)) {
                        if (manager.dragTarget == null) {
                            manager.dragTarget = this;
                            if (overHandle) {
                                manager.dragMode = ComponentsManager.DragMode.RESIZE;
                                resizeStartScale = scale;
                                resizeStartMouseX = mouseX;
                                resizeStartMouseY = mouseY;
                            } else {
                                manager.dragMode = ComponentsManager.DragMode.MOVE;
                                dragX = mouseX - rX;
                                dragY = mouseY - rY;
                            }
                        }

                        if (manager.dragTarget == this) {
                            if (manager.dragMode == ComponentsManager.DragMode.RESIZE) {
                                resizeTo(mouseX, mouseY);
                            } else {
                                move(sr, mouseX, mouseY);
                            }
                            alignDrag(sr);
                        }
                    }
                }
            }

            // Input above may have changed the anchor, normalised position or scale. Resolve all
            // render coordinates afterwards so the component and its guides describe the same frame.
            pos = getRealPosition(sr);
            rX = pos[0];
            rY = pos[1];
            scaledWidth = width * scale;
            scaledHeight = height * scale;
            drag = manager.dragTarget == this;
            overHandle = allowScale && isOverResizeHandle(rX, rY, scaledWidth, scaledHeight, mouseX, mouseY);

            Rects.fill(rX - 2, rY - 2, scaledWidth + 4, scaledHeight + 4, new Color(0, 0, 0, (int) (alpha * 80)));
            draw(rX, rY);
            GL11.glColor4f(1, 1, 1, 1);

            if (alpha > 0.01f && hasResizeHandle(scaledWidth, scaledHeight)) {
                drawResizeHandle(rX, rY, scaledWidth, scaledHeight,
                        overHandle || (drag && manager.dragMode == ComponentsManager.DragMode.RESIZE));
            }

            if (interactive && !occluded) {
                FPSMaster.fontManager.s14.drawString(FPSMaster.i18n.get(mod.name.toLowerCase()) + " "
                                + Math.round(scale * 10) / 10f + "x", rX, rY - 10,
                        new Color(255, 255, 255, (int) (alpha * 255)).getRGB());
            }
        } else {
            guides = null;
            draw(rX, rY);
        }
    }

    public void scaleUp() {
        if (scale < MAX_SCALE) scale = (int) (scale * 10 + 1) / 10f;
    }

    public void scaleDown() {
        if (scale > MIN_SCALE) scale = (int) (scale * 10 - 1) / 10f;
    }

    /**
     * The handle is suppressed on components smaller than this, where its grab area would swallow most
     * of the body and leave no room to start a move.
     */
    private boolean hasResizeHandle(float scaledWidth, float scaledHeight) {
        return allowScale && scaledWidth >= HANDLE_SIZE * 3f && scaledHeight >= HANDLE_SIZE * 3f;
    }

    /** Bottom-right corner box the user grabs to resize. Sized in screen pixels, deliberately not
     *  scaled — otherwise a component shrunk to 0.5x would have a handle too small to hit. */
    private boolean isOverResizeHandle(float rX, float rY, float scaledWidth, float scaledHeight, int mouseX, int mouseY) {
        if (!hasResizeHandle(scaledWidth, scaledHeight)) {
            return false;
        }
        return Hover.is(rX + scaledWidth - HANDLE_SIZE, rY + scaledHeight - HANDLE_SIZE,
                HANDLE_SIZE * 2f, HANDLE_SIZE * 2f, mouseX, mouseY);
    }

    private void drawResizeHandle(float rX, float rY, float scaledWidth, float scaledHeight, boolean active) {
        float handleX = rX + scaledWidth - HANDLE_SIZE;
        float handleY = rY + scaledHeight - HANDLE_SIZE;
        int shade = (int) (alpha * (active ? 255 : 160));
        Rects.rounded(handleX, handleY, HANDLE_SIZE, HANDLE_SIZE, 2, new Color(255, 255, 255, shade).getRGB());
        Rects.rounded(handleX + 1f, handleY + 1f, HANDLE_SIZE - 2f, HANDLE_SIZE - 2f, 1,
                new Color(0, 0, 0, (int) (alpha * 140)).getRGB());
    }

    /**
     * Resizes from how far the cursor has travelled since the grab, not from its distance to the
     * component's origin: for right/bottom-anchored components the origin itself moves as the scale
     * changes, which would feed back into the next frame's delta and run away.
     */
    private void resizeTo(int mouseX, int mouseY) {
        float delta = ((mouseX - resizeStartMouseX) + (mouseY - resizeStartMouseY)) / 2f;
        float target = resizeStartScale + delta * RESIZE_SENSITIVITY;
        // Quantise to the same 0.1 steps the scroll wheel uses so both paths agree.
        target = Math.round(target * 10f) / 10f;
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, target));
    }

    private void move(ScaledResolution sr, int x, int y) {
        float scaleFactor = sr.getScaleFactor();
        float guiWidth = sr.getScaledWidth() / 2f * scaleFactor;
        float guiHeight = sr.getScaledHeight() / 2f * scaleFactor;
        float changeX = 0f;
        float changeY = 0f;
        if (x > guiWidth / 2f) {
            if (y >= guiHeight / 2f)
                position = Position.RB;
            else if (y < guiHeight / 2f)
                position = Position.RT;
        } else {
            if (y >= guiHeight / 2f)
                position = Position.LB;
            else if (y < guiHeight / 2f)
                position = Position.LT;
        }

        switch (position) {
            case LT: {
                changeX = x - dragX;
                changeY = y - dragY;
                break;
            }
            case RT: {
                changeX = guiWidth - x - width * scale + dragX;
                changeY = y - dragY;
                break;
            }

            case LB: {
                changeX = x - dragX;
                changeY = guiHeight - y - height * scale + dragY;
                break;
            }

            case RB: {
                changeX = guiWidth - x - width * scale + dragX;
                changeY = guiHeight - y - height * scale + dragY;
                break;
            }

            case CT:
                position = Position.CT;
                break;
        }

        if (changeX < 0f || changeX + width * scale > guiWidth) {
            changeX = Math.min(Math.max(changeX, 0f), guiWidth - width * scale);
        }
        if (changeY < 0f || changeY + height * scale > guiHeight) {
            changeY = Math.min(Math.max(changeY, 0f), guiHeight - height * scale);
        }

        this.x = changeX / guiWidth * 2f;
        this.y = changeY / guiHeight * 2f;
    }

    /**
     * A module only has a background if its traits say so. Checking {@code bg} alone is not enough:
     * the field exists on every InterfaceModule and defaults to {@code true}, so a text-only module
     * such as Sprint would otherwise have a blur mask stamped for a panel it never draws.
     */
    public boolean hasBackground() {
        return mod.has(InterfaceModule.Trait.BACKGROUND) && mod.bg.getValue();
    }

    public void drawRect(float x, float y, float width, float height, Color color) {
        float scaledWidth = width * scale;
        float scaledHeight = height * scale;

        if (hasBackground()) {
            if (mod.rounded.getValue()) {
                Rects.roundedImage(Math.round(x), Math.round(y), Math.round(scaledWidth), Math.round(scaledHeight), mod.roundRadius.getValue().intValue(), color);
            } else {
                Rects.fill(x, y, scaledWidth, scaledHeight, color);
            }
        }
    }

    public void drawString(int fontSize, String text, float x, float y, int color) {
        drawString(fontSize, false, text, x, y, color);
    }

    /**
     * Draws a component's text.
     *
     * <p>Colours here come from {@link top.fpsmaster.features.settings.impl.ColorSetting}, whose
     * alpha slider reaches zero and whose Wave mode scales what it returns, so alpha means what it
     * says: below four there is nothing to draw. Both renderers underneath read it vanilla's way
     * instead — no alpha bits set means opaque — and would turn a string the user hid into a solid
     * one. Components must therefore pass a real alpha, not a bare {@code 0xRRGGBB}.
     *
     * <p>Only text is affected. Backgrounds and shapes go through {@code drawRect} and {@code Rects}
     * and never come through here, so a fully transparent background stays transparent.
     */
    public void drawString(int fontSize, boolean bold, String text, float x, float y, int color) {
        if (((color >>> 24) & 0xFF) <= 3) {
            return;
        }
        double scaled = (int) (scale * 100) / 100.0;
        fontSize = (int) (fontSize * scale);
        UFontRenderer font = FPSMaster.fontManager.getFont(fontSize);
        if (mod.betterFont.getValue()) {
            if (mod.fontShadow.getValue()) font.drawStringWithShadow(text, x, y, color);
            else font.drawString(text, x, y, color);
        } else {
            GL11.glPushMatrix();
            GL11.glTranslated(x, y, 0.0);
            GL11.glScaled(scaled, scaled, 1.0);
                if (mod.fontShadow.getValue()) {
                    Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, 0, 0, color);
                } else {
                    GL11.glColor4f(1, 1, 1, 1);
                    Minecraft.getMinecraft().fontRendererObj.drawString(text, 0, 0, color);
                }
            GL11.glPopMatrix();
        }
    }

    public float getStringWidth(int fontSize, String name) {
        UFontRenderer font = FPSMaster.fontManager.getFont(fontSize);
        return mod.betterFont.getValue() ? font.getStringWidth(name) : (Minecraft.getMinecraft().fontRendererObj.getStringWidth(name));
    }

    public float getStringHeight(int fontSize) {
        UFontRenderer font = FPSMaster.fontManager.getFont(fontSize);
        return mod.betterFont.getValue() ? font.getHeight() : (Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT);
    }

    /** Draws the drag target's alignment guides, on top of every component. Called once per frame by ComponentsManager. */
    void drawGuides() {
        if (guides == null) {
            return;
        }
        if (!(Utility.mc.currentScreen instanceof GuiChat || Utility.mc.currentScreen instanceof MainPanel)) {
            guides = null;
            return;
        }
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.enableAlpha();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        // Every guide is a thin full-span rectangle; grouping them by colour into two quad batches
        // costs a single texture switch instead of toggling GL state once per line.
        drawGuideBatch(GUIDE_PASSIVE_COLOR, false);
        drawGuideBatch(GUIDE_ACTIVE_COLOR, true);
        GlStateManager.enableTexture2D();
        // Restore the neutral state the HUD pass is assumed to leave behind.
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** Renders all guide lines of one activation state as a single quad batch. */
    private void drawGuideBatch(Color color, boolean active) {
        boolean begun = false;
        for (GuideLine line : guides) {
            if (line == null || line.active != active) {
                continue;
            }
            if (!begun) {
                Color c = Colors.toColor(Alpha.apply(color.getRGB()));
                GlStateManager.color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
                GL11.glBegin(GL11.GL_QUADS);
                begun = true;
            }
            GL11.glVertex2d(line.x, line.y);
            GL11.glVertex2d(line.x, line.y + line.height);
            GL11.glVertex2d(line.x + line.width, line.y + line.height);
            GL11.glVertex2d(line.x + line.width, line.y);
        }
        if (begun) {
            GL11.glEnd();
        }
    }

    /**
     * Lunar/Badlion-style alignment assistance for the HUD editor. While a component is dragged it
     * snaps to nearby edges — its own left/centre/right to a candidate's left/centre/right, and the
     * same for top/centre/bottom — and draws a single guide line at the matched candidate edge. The
     * screen centre is the only implicit candidate, so centring a component draws the classic
     * crosshair and nothing is ever drawn at the screen borders.
     *
     * <p>Everything lives in the shared component space ({@link GuiScale#getFixedBounds}) where
     * {@link #getRealPosition} expresses each box, so both axes are independent and can be active at
     * once. During RESIZE the box only changes via the quantised scale, which cannot express an exact
     * edge alignment, so resizing shows the guide lines but never snaps.
     *
     * <p>The x/y values a snap wants must be reproducible by the position formula without the
     * {@code [0,1]} clamp in {@link #getRealPosition} shifting them, and must survive {@code move()}'s
     * drag clamp, otherwise the box would jump across the screen and fight the cursor;
     * {@link #snapXTo}/{@link #snapYTo} verify the inverse before applying and skip the snap when it
     * cannot be expressed.
     */
    private void alignDrag(ScaledResolution sr) {
        guides = null;
        float[] bounds = GuiScale.getFixedBounds();
        float guiW = bounds[0];
        float guiH = bounds[1];
        float[] pos = getRealPosition(sr);
        float rX = pos[0];
        float rY = pos[1];
        float w = width * scale;
        float h = height * scale;
        if (w <= 0f || h <= 0f) {
            return;
        }

        boolean interactive = FPSMaster.componentsManager.dragMode == ComponentsManager.DragMode.MOVE;
        // CT is horizontally centred; its x coordinate cannot move, so x snapping is never applied.
        boolean movableX = interactive && position != Position.CT;

        // The screen centre is the only implicit candidate: centring a component draws the classic
        // crosshair. The screen edges are deliberately not candidates, so no border lines appear.
        Snap xSnap = bestSnap(boxEdges(rX, w), new float[]{guiW / 2f});
        Snap ySnap = bestSnap(boxEdges(rY, h), new float[]{guiH / 2f});

        for (Component candidate : FPSMaster.componentsManager.components) {
            if (candidate == this || !candidate.isVisibleForAlignment()) {
                continue;
            }
            float[] candidatePos = candidate.getRealPosition(sr);
            float cw = candidate.width * candidate.scale;
            float ch = candidate.height * candidate.scale;
            if (cw <= 0f || ch <= 0f) {
                continue;
            }
            Snap snap = bestSnap(boxEdges(rX, w), boxEdges(candidatePos[0], cw));
            if (snap != null && (xSnap == null || snap.diff < xSnap.diff)) {
                xSnap = snap;
            }
            snap = bestSnap(boxEdges(rY, h), boxEdges(candidatePos[1], ch));
            if (snap != null && (ySnap == null || snap.diff < ySnap.diff)) {
                ySnap = snap;
            }
        }

        applySnap(xSnap, rX, w, false, movableX, guiW, guiH);
        applySnap(ySnap, rY, h, true, interactive, guiH, guiW);
    }

    /**
     * Applies one axis's snap and draws a single guide line at the matched candidate edge.
     *
     * <p>{@code horizontal} selects the axis: horizontal lines mean a y snap (which spans
     * {@code axisLength} via {@link #snapYTo}) and vertical lines an x snap, which is why
     * {@code axisLength}/{@code guideSpan} swap between the two call sites.
     *
     * <p>No mouse-offset compensation is kept: the snap holds only while the dragged box stays within
     * the snap radius of the candidate edge, and the box follows the cursor again the moment the mouse
     * carries it beyond the radius. Compensating the mouse offset instead absorbs every slow mouse
     * movement while the box sits within the radius, pinning it to the candidate so it can never be
     * dragged away.
     */
    private void applySnap(Snap snap, float edge, float scaledLen, boolean horizontal,
                           boolean allowed, float axisLength, float guideSpan) {
        if (snap == null) {
            return;
        }
        boolean applied = allowed && (horizontal
                ? snapYTo(axisLength, edge + snap.delta, scaledLen)
                : snapXTo(axisLength, edge + snap.delta, scaledLen));
        addGuideLine(snap.matchedValue, guideSpan, horizontal, applied);
    }

    /** Left/centre/right (or top/centre/bottom) edges of a box spanning {@code min} to {@code min + span}. */
    private static float[] boxEdges(float min, float span) {
        return new float[]{min, min + span / 2f, min + span};
    }

    /**
     * Finds the closest pair between any of the dragged component's edges and any of a candidate's
     * edges. {@code null} when nothing is within {@link #ALIGN_SNAP_DIST}.
     */
    private static Snap bestSnap(float[] dragEdges, float[] candidateEdges) {
        Snap best = null;
        for (int i = 0; i < dragEdges.length; i++) {
            for (int j = 0; j < candidateEdges.length; j++) {
                float diff = Math.abs(dragEdges[i] - candidateEdges[j]);
                if (diff <= ALIGN_SNAP_DIST && (best == null || diff < best.diff)) {
                    best = new Snap(diff, candidateEdges[j] - dragEdges[i], candidateEdges[j]);
                }
            }
        }
        return best;
    }

    private void addGuideLine(float edge, float span, boolean horizontal, boolean active) {
        if (guides == null) {
            guides = new GuideLine[MAX_GUIDE_LINES];
        }
        for (int i = 0; i < guides.length; i++) {
            if (guides[i] == null) {
                guides[i] = horizontal
                        ? new GuideLine(0f, edge - GUIDE_THICKNESS / 2f, span, GUIDE_THICKNESS, active)
                        : new GuideLine(edge - GUIDE_THICKNESS / 2f, 0f, GUIDE_THICKNESS, span, active);
                return;
            }
        }
    }

    /** Moves the box so its left edge sits at {@code targetLeft}, if the current position can express it. */
    private boolean snapXTo(float guiW, float targetLeft, float scaledW) {
        float newX;
        switch (position) {
            case LT:
            case LB:
                newX = 2f * targetLeft / guiW;
                break;
            case RT:
            case RB:
                newX = 2f * (guiW - targetLeft - scaledW) / guiW;
                break;
            default:
                return false;
        }
        newX = Math.max(0f, Math.min(1f, newX));
        float rx = realX(newX, guiW, scaledW);
        if (Math.abs(rx - targetLeft) > SNAP_EXPRESSIBLE_TOLERANCE) {
            return false;
        }
        // move() clamps the dragged box to [0, guiW - scaledW]; a snap that lands outside that
        // envelope would be clamped back next frame and re-applied forever (drifting the mouse
        // offset), so it must sit inside. The box is already pinned there by move(), so rejecting
        // costs nothing.
        if (rx < 0f || rx > guiW - scaledW) {
            return false;
        }
        x = newX;
        return true;
    }

    /** Moves the box so its top edge sits at {@code targetTop}, if the current position can express it. */
    private boolean snapYTo(float guiH, float targetTop, float scaledH) {
        float newY;
        switch (position) {
            case LT:
            case RT:
            case CT:
                newY = 2f * targetTop / guiH;
                break;
            case LB:
            case RB:
                newY = 2f * (guiH - targetTop - scaledH) / guiH;
                break;
            default:
                return false;
        }
        newY = Math.max(0f, Math.min(1f, newY));
        float ry = realY(newY, guiH, scaledH);
        if (Math.abs(ry - targetTop) > SNAP_EXPRESSIBLE_TOLERANCE) {
            return false;
        }
        if (ry < 0f || ry > guiH - scaledH) {
            return false;
        }
        y = newY;
        return true;
    }

    /** Inverse of the x position formula: real left edge for a normalized {@code posX}. */
    private float realX(float posX, float guiW, float scaledW) {
        switch (position) {
            case LT:
            case LB:
                return posX * guiW / 2f;
            case RT:
            case RB:
                return guiW - (posX * guiW / 2f + scaledW);
            case CT:
                return guiW / 2f - scaledW / 2f;
            default:
                return 0f;
        }
    }

    /** Inverse of the y position formula: real top edge for a normalized {@code posY}. */
    private float realY(float posY, float guiH, float scaledH) {
        switch (position) {
            case LT:
            case RT:
            case CT:
                return posY * guiH / 2f;
            case LB:
            case RB:
                return guiH - (posY * guiH / 2f + scaledH);
            default:
                return 0f;
        }
    }

    /** A full-screen guide line (either axis) to draw while dragging. */
    private static final class GuideLine {
        private final float x;
        private final float y;
        private final float width;
        private final float height;
        private final boolean active;

        GuideLine(float x, float y, float width, float height, boolean active) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.active = active;
        }
    }

    /** The closest matching edge pair found for one axis. */
    private static final class Snap {
        private final float diff;
        private final float delta;
        private final float matchedValue;

        Snap(float diff, float delta, float matchedValue) {
            this.diff = diff;
            this.delta = delta;
            this.matchedValue = matchedValue;
        }
    }
}


