package top.fpsmaster.features.command.impl;

import top.fpsmaster.api.MinecraftAPI;
import top.fpsmaster.api.domain.client.IClientPlayer;
import top.fpsmaster.api.domain.client.IGameSettings;
import top.fpsmaster.api.domain.client.IItemStack;
import top.fpsmaster.api.domain.render.IRenderManager;
import top.fpsmaster.api.domain.world.IWorld;
import top.fpsmaster.api.model.BlockPos;
import top.fpsmaster.api.model.Vec3;
import top.fpsmaster.api.model.WeatherType;
import top.fpsmaster.features.command.Command;

/**
 * 测试新 API 的命令
 * 使用方法: #testnewapi
 */
public class TestNewAPICommand extends Command {
    
    public TestNewAPICommand() {
        super("testnewapi");
    }
    
    @Override
    public void execute(String[] args) {
        printMessage("§a========== 测试新 API ==========");
        
        // 测试客户端 API
        testClientAPI();
        
        // 测试世界 API
        testWorldAPI();
        
        // 测试渲染 API
        testRenderAPI();
        
        // 测试设置 API
        testSettingsAPI();
        
        // 测试粒子效果
        testParticles();
        
        // 测试光照系统
        testLightLevel();
        
        // 测试实体 API
        testEntityAPI();
        
        printMessage("§a========== 测试完成 ==========");
    }
    
    private void testClientAPI() {
        printMessage("§e[客户端 API 测试]");
        
        IClientPlayer player = MinecraftAPI.client().getPlayer();
        if (player == null) {
            printMessage("§c玩家为 null（未加入世界）");
            return;
        }
        
        // 测试玩家基本信息
        String name = player.getName();
        Vec3 pos = player.getPosition();
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        int foodLevel = player.getFoodLevel();
        int ping = player.getPing();
        
        printMessage(String.format("§7玩家: §f%s", name));
        printMessage(String.format("§7位置: §f%.2f, %.2f, %.2f", pos.x, pos.y, pos.z));
        printMessage(String.format("§7生命值: §f%.1f/%.1f", health, maxHealth));
        printMessage(String.format("§7饥饿值: §f%d", foodLevel));
        printMessage(String.format("§7延迟: §f%dms", ping));
        printMessage(String.format("§7在地面: §f%s", player.isOnGround()));
        printMessage(String.format("§7潜行: §f%s", player.isSneaking()));
        printMessage(String.format("§7疾跑: §f%s", player.isSprinting()));
        
        // 测试物品
        IItemStack heldItem = player.getHeldItem();
        if (heldItem != null && !heldItem.isEmpty()) {
            printMessage(String.format("§7手持物品: §f%s x%d", 
                heldItem.getDisplayName(), heldItem.getCount()));
            printMessage(String.format("§7物品ID: §f%s", heldItem.getItemId()));
            if (heldItem.isEnchanted()) {
                printMessage(String.format("§7附魔: §f%s", heldItem.getEnchantments()));
            }
        } else {
            printMessage("§7手持物品: §f空");
        }
        
        // 测试护甲
        java.util.List<IItemStack> armor = player.getArmorItems();
        printMessage(String.format("§7护甲装备: §f%d件", 
            armor.stream().filter(a -> a != null && !a.isEmpty()).count()));
    }
    
    private void testWorldAPI() {
        printMessage("§e[世界 API 测试]");
        
        IWorld world = MinecraftAPI.world().getWorld();
        if (world == null) {
            printMessage("§c世界为 null");
            return;
        }
        
        String worldName = world.getWorldName();
        long worldTime = world.getWorldTime();
        BlockPos spawnPoint = world.getSpawnPoint();
        WeatherType weather = world.getWeatherType();
        
        printMessage(String.format("§7世界名称: §f%s", worldName));
        printMessage(String.format("§7世界时间: §f%d", worldTime));
        printMessage(String.format("§7出生点: §f%d, %d, %d", 
            spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ()));
        printMessage(String.format("§7天气: §f%s", weather));
        
        // 测试方块检测
        IClientPlayer player = MinecraftAPI.client().getPlayer();
        if (player != null) {
            Vec3 pos = player.getPosition();
            BlockPos playerPos = new BlockPos((int)pos.x, (int)pos.y, (int)pos.z);
            BlockPos feetPos = playerPos.down();
            
            String blockId = world.getBlockId(feetPos);
            boolean isAir = world.isAirBlock(feetPos);
            
            printMessage(String.format("§7脚下方块: §f%s", blockId));
            printMessage(String.format("§7是否为空气: §f%s", isAir));
        }
    }
    
    private void testRenderAPI() {
        printMessage("§e[渲染 API 测试]");
        
        IRenderManager rm = MinecraftAPI.render().getRenderManager();
        float partialTicks = MinecraftAPI.render().getPartialTicks();
        
        printMessage(String.format("§7渲染偏移: §f%.2f, %.2f, %.2f", 
            rm.getRenderPosX(), rm.getRenderPosY(), rm.getRenderPosZ()));
        printMessage(String.format("§7部分刻: §f%.3f", partialTicks));
    }
    
    private void testSettingsAPI() {
        printMessage("§e[设置 API 测试]");
        
        IGameSettings settings = MinecraftAPI.client().getSettings();
        
        printMessage(String.format("§7视距: §f%d 区块", settings.getRenderDistance()));
        printMessage(String.format("§7鼠标灵敏度: §f%.2f", settings.getMouseSensitivity()));
        printMessage(String.format("§7GUI 缩放: §f%d", settings.getGuiScale()));
        printMessage(String.format("§7FOV: §f%.0f", settings.getFov()));
        printMessage(String.format("§7VSync: §f%s", settings.isVsyncEnabled()));
        printMessage(String.format("§7帧率限制: §f%d", settings.getFramerateLimit()));
        printMessage(String.format("§7调试信息: §f%s", settings.isShowDebugInfo()));
    }
    
    private void testParticles() {
        printMessage("§e[粒子效果测试]");
        
        IClientPlayer player = MinecraftAPI.client().getPlayer();
        if (player == null) {
            printMessage("§c玩家为 null");
            return;
        }
        
        Vec3 pos = player.getPosition();
        BlockPos particlePos = new BlockPos((int)pos.x, (int)pos.y + 2, (int)pos.z);
        
        // 添加红石粒子效果
        MinecraftAPI.render().getParticleManager().addRedstoneBreakParticle(particlePos);
        printMessage("§7已在头顶生成红石破碎粒子");
    }
    
    private void testLightLevel() {
        printMessage("§e[光照系统测试]");
        
        IClientPlayer player = MinecraftAPI.client().getPlayer();
        IWorld world = MinecraftAPI.world().getWorld();
        
        if (player == null || world == null) {
            printMessage("§c玩家或世界为 null");
            return;
        }
        
        Vec3 pos = player.getPosition();
        BlockPos playerPos = new BlockPos((int)pos.x, (int)pos.y, (int)pos.z);
        
        int lightLevel = world.getLightLevel(playerPos);
        int skyLight = world.getSkyLightLevel(playerPos);
        int blockLight = world.getBlockLightLevel(playerPos);
        float rainStrength = world.getRainStrength();
        
        printMessage(String.format("§7总光照: §f%d", lightLevel));
        printMessage(String.format("§7天空光照: §f%d", skyLight));
        printMessage(String.format("§7方块光照: §f%d", blockLight));
        printMessage(String.format("§7雨强度: §f%.2f", rainStrength));
    }
    
    private void testEntityAPI() {
        printMessage("§e[实体 API 测试]");
        
        // 获取所有实体
        java.util.List<top.fpsmaster.api.domain.entity.IEntity> allEntities = MinecraftAPI.entity().getAllEntities();
        printMessage(String.format("§7总实体数: §f%d", allEntities.size()));
        
        // 获取附近的实体 (10格内)
        java.util.List<top.fpsmaster.api.domain.entity.IEntity> nearbyEntities = MinecraftAPI.entity().getNearbyEntities(10.0);
        printMessage(String.format("§710格内实体数: §f%d", nearbyEntities.size()));
        
        // 显示最近3个附近实体的信息
        int count = 0;
        for (top.fpsmaster.api.domain.entity.IEntity entity : nearbyEntities) {
            if (count >= 3) break;
            
            Vec3 ePos = entity.getPosition();
            printMessage(String.format("§7  - %s (ID: %d) 位置: %.1f, %.1f, %.1f", 
                entity.getName(), entity.getEntityId(), ePos.x, ePos.y, ePos.z));
            count++;
        }
    }
    
    private void printMessage(String message) {
        MinecraftAPI.gui().getGuiIngame().printChatMessage(message);
    }
}
