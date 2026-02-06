package top.fpsmaster.utils.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import top.fpsmaster.utils.core.Utility;

public class Render3DUtils extends Utility {
    private static void posBoundingBoxLeftHalf(AxisAlignedBB boundingBox, WorldRenderer bufferBuilder) {
        bufferBuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
    }
    public static void drawBoundingBoxOutline(AxisAlignedBB boundingBox) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer bufferBuilder = tessellator.getWorldRenderer();
        bufferBuilder.begin(3, DefaultVertexFormats.POSITION);
        posBoundingBoxHalf(boundingBox, bufferBuilder);
        tessellator.draw();
        bufferBuilder.begin(3, DefaultVertexFormats.POSITION);
        posBoundingBoxLeftHalf(boundingBox, bufferBuilder);
        tessellator.draw();
        bufferBuilder.begin(1, DefaultVertexFormats.POSITION);
        posBoundingBoxSquare(boundingBox, tessellator, bufferBuilder);
    }

    private static void posBoundingBoxHalf(AxisAlignedBB boundingBox, WorldRenderer bufferBuilder) {
        bufferBuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
    }
    private static void posBoundingBoxSquare(AxisAlignedBB boundingBox, Tessellator tessellator, WorldRenderer bufferBuilder) {
        bufferBuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        tessellator.draw();
    }

    public static void drawBoundingBox(AxisAlignedBB boundingBox) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer bufferBuilder = tessellator.getWorldRenderer();
        bufferBuilder.begin(7, DefaultVertexFormats.POSITION);
        posBoundingBoxSquare(boundingBox, tessellator, bufferBuilder);
        bufferBuilder.begin(7, DefaultVertexFormats.POSITION);
        bufferBuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        tessellator.draw();
        bufferBuilder.begin(7, DefaultVertexFormats.POSITION);
        posBoundingBoxLeftHalf(boundingBox, bufferBuilder);
        bufferBuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        tessellator.draw();
        bufferBuilder.begin(7, DefaultVertexFormats.POSITION);
        posBoundingBoxHalf(boundingBox, bufferBuilder);
        bufferBuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        tessellator.draw();
        bufferBuilder.begin(7, DefaultVertexFormats.POSITION);
        bufferBuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        tessellator.draw();
        bufferBuilder.begin(7, DefaultVertexFormats.POSITION);
        bufferBuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        bufferBuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        tessellator.draw();
    }
}


