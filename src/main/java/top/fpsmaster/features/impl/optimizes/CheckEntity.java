package top.fpsmaster.features.impl.optimizes;

import net.minecraft.entity.Entity;

public class CheckEntity {

    private Entity entity;
    private long lastTimeChecked = 0;
    private boolean isLastCullingVisible = false;

    public CheckEntity(Entity entity) {
        this.entity = entity;
    }

    public double getMinX() {
        return entity.getEntityBoundingBox().minX;
    }

    public double getMinY() {
        return entity.getEntityBoundingBox().minY;
    }

    public double getMinZ() {
        return entity.getEntityBoundingBox().minZ;
    }

    public double getMaxX() {
        return entity.getEntityBoundingBox().maxX;
    }

    public double getMaxY() {
        return entity.getEntityBoundingBox().maxY;
    }

    public double getMaxZ() {
        return entity.getEntityBoundingBox().maxZ;
    }

    // Getters and setters for other fields
    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public long getLastTimeChecked() {
        return lastTimeChecked;
    }

    public void setLastTimeChecked(long lastTimeChecked) {
        this.lastTimeChecked = lastTimeChecked;
    }

    public boolean isLastCullingVisible() {
        return isLastCullingVisible;
    }

    public void setLastCullingVisible(boolean lastCullingVisible) {
        isLastCullingVisible = lastCullingVisible;
    }
}



