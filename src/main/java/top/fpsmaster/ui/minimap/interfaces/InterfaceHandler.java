package top.fpsmaster.ui.minimap.interfaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.MiniMap;
import top.fpsmaster.minimap.Minimap;
import top.fpsmaster.ui.custom.Component;

import java.util.ArrayList;

import static top.fpsmaster.utils.Utility.mc;

public class InterfaceHandler {
    public static int actionTimer;
    public static ArrayList<Preset> presets;
    public static ArrayList<Interface> list;
    public static final ResourceLocation invtextures;
    public static int selectedId;
    public static int draggingId;
    public static int draggingOffX;
    public static int draggingOffY;
    public static Minimap minimap;
    public static final ResourceLocation guiTextures;
    public static final ResourceLocation mapTextures;
    public static long lastFlip;

    public static void loadPresets() {
        new Preset("gui.xaero_preset_topleft", new int[][]{{0, 0}, {0, 10000}, {0, 0}, {0, 36}, {0, 0}}, new boolean[][]{{true, false}, {true, false}, {false, true}, {true, false}, {false, false}});
        new Preset("gui.xaero_preset_topright", new int[][]{{0, 0}, {0, 135}, {120, 0}, {0, 50}, {0, 0}}, new boolean[][]{{false, true}, {false, false}, {true, false}, {true, false}, {false, true}});
        new Preset("gui.xaero_preset_bottom_left", new int[][]{{0, 0}, {0, 135}, {120, 0}, {0, 50}, {0, 10000}}, new boolean[][]{{false, true}, {false, false}, {true, false}, {true, false}, {false, false}});
        new Preset("gui.xaero_preset_bottom_right", new int[][]{{0, 0}, {0, 135}, {120, 0}, {0, 50}, {0, 10000}}, new boolean[][]{{false, true}, {false, false}, {true, false}, {true, false}, {false, true}});
    }

    public static void load() {
        InterfaceHandler.minimap = new Minimap(new Interface("gui.xaero_minimap", 128, 128) {
            long lastFBOTry = 0L;
            int lastMinimapSize = 0;

            @Override
            public int getW(final int scale) {
                return this.getSize() / scale;
            }

            @Override
            public int getH(final int scale) {
                return this.getW(scale);
            }

            @Override
            public int getWC(final int scale) {
                return this.getW(scale);
            }

            @Override
            public int getHC(final int scale) {
                return this.getH(scale);
            }

            @Override
            public int getW0(final int scale) {
                return this.getW(scale);
            }

            @Override
            public int getH0(final int scale) {
                return this.getH(scale);
            }

            @Override
            public int getSize() {
                return InterfaceHandler.minimap.getMinimapWidth() + 36 + 2;
            }

            @Override
            public void drawInterface(final int width, final int height, final float partial) {

                if (Minimap.loadedFBO && !OpenGlHelper.isFramebufferEnabled()) {
                    Minimap.loadedFBO = false;
                    Minimap.scalingFrameBuffer.deleteFramebuffer();
                    Minimap.rotationFrameBuffer.deleteFramebuffer();
                    Minimap.resetImage();
                }

                if (System.currentTimeMillis() - this.lastFBOTry > 1000L) {
                    this.lastFBOTry = System.currentTimeMillis();
                    Minimap.loadFrameBuffer();
                }

                if (2 != this.lastMinimapSize) {
                    this.lastMinimapSize = 2;
                    Minimap.resetImage();
                    Minimap.frameUpdateNeeded = Minimap.usingFBO();
                }

                int bufferSize;

                if (Minimap.usingFBO()) {
                    bufferSize = InterfaceHandler.minimap.getFBOBufferSize();
                } else {
                    bufferSize = InterfaceHandler.minimap.getBufferSize();
                }

                final Minimap minimap = InterfaceHandler.minimap;
                final int minimapWidth = InterfaceHandler.minimap.getMinimapWidth();
                minimap.minimapWidth = minimapWidth;
                final Minimap minimap2 = InterfaceHandler.minimap;
                minimap2.minimapHeight = (minimapWidth);
                Minimap.frameUpdatePartialTicks = partial;
                InterfaceHandler.minimap.updateZoom();
                RenderHelper.disableStandardItemLighting();
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                float sizeFix = 512.0f;

                InterfaceHandler.minimap.renderFrameToFBO(bufferSize, minimapWidth, sizeFix, partial, true);
                Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
                Minimap.rotationFrameBuffer.bindFramebufferTexture();
                sizeFix = 1f;
                GL11.glEnable(3008);
                GlStateManager.enableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(770, 771);

                Component component = FPSMaster.componentsManager.getComponent(MiniMap.class);
                mc.ingameGUI.drawTexturedModalRect(((int) component.getRealPosition()[0]), ((int) component.getRealPosition()[1]), 0, 0, (int) ((minimapWidth / 2f + 1) / sizeFix), (int) ((minimapWidth / 2f + 1) / sizeFix));
                super.drawInterface(width, height, partial);
            }
        });
    }

    public static void drawInterfaces(float width, float height,float partial) {
        for (final Interface l : InterfaceHandler.list) {
            l.drawInterface((int) width, (int) height, partial);
        }
    }

    public static void confirm() {
        for (final Interface l : InterfaceHandler.list) {
            l.bx = l.actualx;
            l.by = l.actualy;
            l.bcentered = l.centered;
            l.bflipped = l.flipped;
            l.bfromRight = l.fromRight;
        }
    }

    public static void cancel() {
        for (final Interface l : InterfaceHandler.list) {
            l.actualx = l.bx;
            l.actualy = l.by;
            l.centered = l.bcentered;
            l.flipped = l.bflipped;
            l.fromRight = l.bfromRight;
        }
    }

    public static void applyPreset(final int id) {
        for (final Interface l : InterfaceHandler.list) {
            InterfaceHandler.actionTimer = 10;
            l.actualx = InterfaceHandler.presets.get(id).coords[l.id][0];
            l.actualy = InterfaceHandler.presets.get(id).coords[l.id][1];
            l.centered = InterfaceHandler.presets.get(id).types[l.id][0];
            l.flipped = l.cflipped;
            l.fromRight = InterfaceHandler.presets.get(id).types[l.id][1];
        }
    }

    static {
        InterfaceHandler.actionTimer = 0;
        InterfaceHandler.presets = new ArrayList<>();
        InterfaceHandler.list = new ArrayList<>();
        invtextures = new ResourceLocation("textures/gui/container/inventory.png");
        InterfaceHandler.selectedId = -1;
        InterfaceHandler.draggingId = -1;
        InterfaceHandler.draggingOffX = 0;
        InterfaceHandler.draggingOffY = 0;
        guiTextures = new ResourceLocation("xaerobetterpvp", "gui/guis.png");
        mapTextures = new ResourceLocation("xaeromaptexture");
        InterfaceHandler.lastFlip = 0L;
    }
}
