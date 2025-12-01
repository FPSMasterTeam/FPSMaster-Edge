package top.fpsmaster.impl.entity;

import net.minecraft.entity.Entity;
import top.fpsmaster.api.domain.entity.IEntity;
import top.fpsmaster.api.model.Vec3;

public class EntityImpl implements IEntity {
    
    private final Entity entity;
    
    public EntityImpl(Entity entity) {
        this.entity = entity;
    }
    
    @Override
    public int getEntityId() {
        return entity.getEntityId();
    }
    
    @Override
    public String getUUID() {
        return entity.getUniqueID().toString();
    }
    
    @Override
    public Vec3 getPosition() {
        return new Vec3(entity.posX, entity.posY, entity.posZ);
    }
    
    @Override
    public String getName() {
        return entity.getName();
    }
    
    @Override
    public String getCustomName() {
        return entity.getCustomNameTag();
    }
    
    @Override
    public boolean hasCustomName() {
        return entity.hasCustomName();
    }
    
    @Override
    public float getYaw() {
        return entity.rotationYaw;
    }
    
    @Override
    public float getPitch() {
        return entity.rotationPitch;
    }
    
    @Override
    public Vec3 getMotion() {
        return new Vec3(entity.motionX, entity.motionY, entity.motionZ);
    }
    
    @Override
    public boolean isOnGround() {
        return entity.onGround;
    }
    
    @Override
    public boolean isInWater() {
        return entity.isInWater();
    }
    
    @Override
    public boolean isInLava() {
        return entity.isInLava();
    }
    
    @Override
    public boolean isBurning() {
        return entity.isBurning();
    }
    
    @Override
    public boolean isSneaking() {
        return entity.isSneaking();
    }
    
    @Override
    public boolean isSprinting() {
        return entity.isSprinting();
    }
    
    @Override
    public boolean isInvisible() {
        return entity.isInvisible();
    }
    
    @Override
    public double getDistanceTo(Vec3 pos) {
        double dx = entity.posX - pos.x;
        double dy = entity.posY - pos.y;
        double dz = entity.posZ - pos.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    @Override
    public float getEyeHeight() {
        return entity.getEyeHeight();
    }
    
    /**
     * 获取原始 Minecraft 实体对象（仅供内部使用）
     */
    public Entity getMinecraftEntity() {
        return entity;
    }
}
