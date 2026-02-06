package top.fpsmaster.ui.minimap.minimap;

import java.util.Random;

public class MinimapChunk
{
    public static Random rand = new Random();
    public int[][] colors;
    public boolean chunkGrid;
    public int[] lastHeights;
    
    public MinimapChunk(final int X, final int Z) {
        this.colors = new int[16][16];
        this.lastHeights = new int[16];
        this.chunkGrid = ((X & 0x1) == (Z & 0x1));
    }
}



