package top.fpsmaster.ui.minimap.interfaces;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public abstract class Interface
{
    public String iname;
    public int id;
    public int bx;
    public int by;
    public int x;
    public int y;
    public int actualx;
    public int actualy;
    public int w0;
    public int h0;
    public int w;
    public int h;
    public int wc;
    public int hc;
    public boolean multi;
    public boolean centered;
    public boolean bcentered;
    public boolean flipped;
    public boolean bflipped;
    public boolean cflipped;
    public boolean fromRight;
    public boolean bfromRight;
    
    public Interface(final String name, final int w, final int h) {
        this(name, w, h, w, h);
    }
    
    public Interface(final String name, final int w, final int h, final int wc, final int hc) {
        this.iname = name;
        this.w = w;
        this.w0 = w;
        this.h = h;
        this.h0 = h;
        this.wc = wc;
        this.hc = hc;
        this.multi = (wc != w || hc != h);
        final boolean b = false;
        this.flipped = b;
        this.cflipped = b;
        this.bflipped = false;
        InterfaceHandler.list.add(this);
        this.id = InterfaceHandler.list.indexOf(this);
        final Preset preset = InterfaceHandler.presets.get(0);
        final int bx = preset.coords[this.id][0];
        this.x = bx;
        this.actualx = bx;
        this.bx = bx;
        final int by = preset.coords[this.id][1];
        this.y = by;
        this.actualy = by;
        this.by = by;
        final boolean b2 = preset.types[this.id][0];
        this.centered = b2;
        this.bcentered = b2;
        final boolean b3 = preset.types[this.id][1];
        this.fromRight = b3;
        this.bfromRight = b3;
    }
    
    public int getW(final int scale) {
        return this.w;
    }
    
    public int getH(final int scale) {
        return this.h;
    }
    
    public int getWC(final int scale) {
        return this.wc;
    }
    
    public int getHC(final int scale) {
        return this.hc;
    }
    
    public int getW0(final int scale) {
        return this.w0;
    }
    
    public int getH0(final int scale) {
        return this.h0;
    }
    
    public int getSize() {
        return this.w * this.h;
    }
    
    public void drawInterface(int width, int height, float partial) {
        if (this.fromRight) {
            this.x = width - this.x;
        }
        GL11.glEnable(3008);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
    }
}
