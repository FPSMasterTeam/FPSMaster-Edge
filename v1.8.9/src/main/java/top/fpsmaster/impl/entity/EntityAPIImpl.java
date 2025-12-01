package top.fpsmaster.impl.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import top.fpsmaster.api.domain.entity.IEntity;
import top.fpsmaster.api.domain.entity.IEntityAPI;
import top.fpsmaster.api.model.Vec3;

import java.util.ArrayList;
import java.util.List;

public class EntityAPIImpl implements IEntityAPI {
    
    @Override
    public List<IEntity> getAllEntities() {
        List<IEntity> entities = new ArrayList<>();
        
        if (Minecraft.getMinecraft().theWorld != null) {
            for (Object obj : Minecraft.getMinecraft().theWorld.loadedEntityList) {
                if (obj instanceof Entity) {
                    entities.add(new EntityImpl((Entity) obj));
                }
            }
        }
        
        return entities;
    }
    
    @Override
    public IEntity getEntityById(int entityId) {
        if (Minecraft.getMinecraft().theWorld != null) {
            Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(entityId);
            if (entity != null) {
                return new EntityImpl(entity);
            }
        }
        return null;
    }
    
    @Override
    public List<IEntity> getNearbyEntities(double radius) {
        List<IEntity> entities = new ArrayList<>();
        
        if (Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().theWorld == null) {
            return entities;
        }
        
        Entity player = Minecraft.getMinecraft().thePlayer;
        Vec3 playerPos = new Vec3(player.posX, player.posY, player.posZ);
        
        for (Object obj : Minecraft.getMinecraft().theWorld.loadedEntityList) {
            if (obj instanceof Entity) {
                Entity entity = (Entity) obj;
                double distance = player.getDistanceToEntity(entity);
                if (distance <= radius) {
                    entities.add(new EntityImpl(entity));
                }
            }
        }
        
        return entities;
    }
    
    @Override
    public List<IEntity> getNearbyEntities(Vec3 pos, double radius) {
        List<IEntity> entities = new ArrayList<>();
        
        if (Minecraft.getMinecraft().theWorld == null) {
            return entities;
        }
        
        for (Object obj : Minecraft.getMinecraft().theWorld.loadedEntityList) {
            if (obj instanceof Entity) {
                Entity entity = (Entity) obj;
                double dx = entity.posX - pos.x;
                double dy = entity.posY - pos.y;
                double dz = entity.posZ - pos.z;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                
                if (distance <= radius) {
                    entities.add(new EntityImpl(entity));
                }
            }
        }
        
        return entities;
    }
}
