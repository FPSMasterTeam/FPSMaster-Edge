package top.fpsmaster.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.fpsmaster.api.domain.client.IClientPlayer;
import top.fpsmaster.api.domain.client.IItemStack;
import top.fpsmaster.api.model.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 客户端玩家实现 (v1.8.9)
 * 将 Minecraft 的 EntityPlayerSP 封装为自定义接口
 */
public class ClientPlayerImpl implements IClientPlayer {
    
    private final EntityPlayerSP player;
    
    public ClientPlayerImpl(EntityPlayerSP player) {
        this.player = player;
    }
    
    @Override
    public String getName() {
        return player.getName();
    }
    
    @Override
    public UUID getUniqueId() {
        return player.getUniqueID();
    }
    
    @Override
    public String getUUID() {
        return player.getUniqueID().toString();
    }
    
    @Override
    public Vec3 getPosition() {
        return new Vec3(player.posX, player.posY, player.posZ);
    }
    
    @Override
    public float getYaw() {
        return player.rotationYaw;
    }
    
    @Override
    public float getPitch() {
        return player.rotationPitch;
    }
    
    @Override
    public float getHealth() {
        return player.getHealth();
    }
    
    @Override
    public float getMaxHealth() {
        return player.getMaxHealth();
    }
    
    @Override
    public int getFoodLevel() {
        return player.getFoodStats().getFoodLevel();
    }
    
    @Override
    public boolean isOnGround() {
        return player.onGround;
    }
    
    @Override
    public boolean isSneaking() {
        return player.isSneaking();
    }
    
    @Override
    public boolean isSprinting() {
        return player.isSprinting();
    }
    
    @Override
    @Nullable
    public IItemStack getHeldItem() {
        ItemStack mcStack = player.getHeldItem();
        if (mcStack == null) return null;
        return new ItemStackImpl(mcStack);
    }
    
    @Override
    public List<IItemStack> getArmorItems() {
        List<IItemStack> armorList = new ArrayList<>();
        ItemStack[] mcArmor = player.inventory.armorInventory;
        
        for (ItemStack stack : mcArmor) {
            armorList.add(stack == null ? null : new ItemStackImpl(stack));
        }
        return armorList;
    }
    
    @Override
    public int getPing() {
        NetworkPlayerInfo info = Minecraft.getMinecraft()
            .getNetHandler()
            .getPlayerInfo(player.getUniqueID());
        return info != null ? info.getResponseTime() : 0;
    }
    
    @Override
    public void sendChatMessage(String message) {
        player.sendChatMessage(message);
    }
    
    @Override
    public Vec3 getMotion() {
        return new Vec3(player.motionX, player.motionY, player.motionZ);
    }
    
    @Override
    public float getFallDistance() {
        return player.fallDistance;
    }
    
    @Override
    public int getEntityId() {
        return player.getEntityId();
    }
    
    @Override
    public Object getRawPlayer() {
        return player;
    }
}
