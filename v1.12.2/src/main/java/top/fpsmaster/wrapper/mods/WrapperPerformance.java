package top.fpsmaster.wrapper.mods;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import top.fpsmaster.features.impl.optimizes.CheckEntity;
import top.fpsmaster.api.ProviderManager;
import top.fpsmaster.api.ProviderManager;
import top.fpsmaster.wrapper.WorldClientProvider;

public class WrapperPerformance {


    public static boolean isVisible(CheckEntity entity) {
        if (entity.getEntity() instanceof EntityPlayer)
            return true;
        long timePassedSinceLastCheck = System.currentTimeMillis() - entity.getLastTimeChecked();
        EntityPlayerSP player = ProviderManager.mcProvider.getPlayer();
        double cameraX = player.posX; // 获取玩家的 X 坐标
        double cameraY = player.posY + player.getEyeHeight(); // 获取玩家的 Y 坐标，并加上眼睛的高度
        double cameraZ = player.posZ; // 获取玩家的 Z 坐标
        if ((!entity.isLastCullingVisible() || timePassedSinceLastCheck >= 2000L)) {
            double distance = Math.max(Math.abs(entity.getMinX() - cameraX), Math.max(Math.abs(entity.getMinY() - cameraY), Math.abs(entity.getMinZ() - cameraZ)));
            distance = Math.max(0.0D, distance - 5.0D);
            int interval = 1500;
            long timeToCache = (long) ((double) interval * distance);
            if (timePassedSinceLastCheck < timeToCache) {
                return entity.isLastCullingVisible();
            }

            boolean visible = isVisible(ProviderManager.worldClientProvider.getWorld(), entity.getMinX(), entity.getMinY(), entity.getMinZ(), entity.getMaxX(), entity.getMaxY(), entity.getMaxZ(), cameraX, cameraY, cameraZ);
            entity.setLastCullingVisible(visible);
            return visible;
        } else {
            return true;
        }
    }

    public static boolean isVisible(World world, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double cameraX, double cameraY, double cameraZ) {
        if (Minecraft.getMinecraft().gameSettings.thirdPersonView != 0) {
            return true;
        }
        double distanceRay1X = minX - cameraX;
        double distanceRay1Y = minY - cameraY;
        double distanceRay1Z = minZ - cameraZ;
        double ray1X = cameraX;
        double ray1Y = cameraY;
        double ray1Z = cameraZ;
        double distanceRay1 = Math.sqrt(distanceRay1X * distanceRay1X + distanceRay1Y * distanceRay1Y + distanceRay1Z * distanceRay1Z);
        double step1X = distanceRay1X / distanceRay1;
        double step1Y = distanceRay1Y / distanceRay1;
        double step1Z = distanceRay1Z / distanceRay1;
        boolean ray1XPositive = step1X > 0.0D;
        boolean ray1YPositive = step1Y > 0.0D;
        boolean ray1ZPositive = step1Z > 0.0D;
        double distanceRay2X = maxX - cameraX;
        double distanceRay2Y = maxY - cameraY;
        double distanceRay2Z = maxZ - cameraZ;
        double ray2X = cameraX;
        double ray2Y = cameraY;
        double ray2Z = cameraZ;
        double distanceRay2 = Math.sqrt(distanceRay2X * distanceRay2X + distanceRay2Y * distanceRay2Y + distanceRay2Z * distanceRay2Z);
        double step2X = distanceRay2X / distanceRay2;
        double step2Y = distanceRay2Y / distanceRay2;
        double step2Z = distanceRay2Z / distanceRay2;
        boolean ray2XPositive = step2X > 0.0D;
        boolean ray2YPositive = step2Y > 0.0D;
        boolean ray2ZPositive = step2Z > 0.0D;
        double distanceRay3X = minX - cameraX;
        double distanceRay3Y = maxY - cameraY;
        double distanceRay3Z = minZ - cameraZ;
        double ray3X = cameraX;
        double ray3Y = cameraY;
        double ray3Z = cameraZ;
        double distanceRay3 = Math.sqrt(distanceRay3X * distanceRay3X + distanceRay3Y * distanceRay3Y + distanceRay3Z * distanceRay3Z);
        double step3X = distanceRay3X / distanceRay3;
        double step3Y = distanceRay3Y / distanceRay3;
        double step3Z = distanceRay3Z / distanceRay3;
        boolean ray3XPositive = step3X > 0.0D;
        boolean ray3YPositive = step3Y > 0.0D;
        boolean ray3ZPositive = step3Z > 0.0D;
        double distanceRay4X = maxX - cameraX;
        double distanceRay4Y = minY - cameraY;
        double distanceRay4Z = maxZ - cameraZ;
        double ray4X = cameraX;
        double ray4Y = cameraY;
        double ray4Z = cameraZ;
        double distanceRay4 = Math.sqrt(distanceRay4X * distanceRay4X + distanceRay4Y * distanceRay4Y + distanceRay4Z * distanceRay4Z);
        double step4X = distanceRay4X / distanceRay4;
        double step4Y = distanceRay4Y / distanceRay4;
        double step4Z = distanceRay4Z / distanceRay4;
        boolean ray4XPositive = step4X > 0.0D;
        boolean ray4YPositive = step4Y > 0.0D;
        boolean ray4ZPositive = step4Z > 0.0D;
        double maxDistance = Math.max(Math.max(Math.abs(distanceRay1), Math.abs(distanceRay2)), Math.max(Math.abs(distanceRay3), Math.abs(distanceRay4)));
        boolean ray1Hit = false;
        boolean ray2Hit = false;
        boolean ray3Hit = false;
        boolean ray4Hit = false;
        boolean ray1Free = false;
        boolean ray2Free = false;
        boolean ray3Free = false;
        boolean ray4Free = false;

        for (int i = 0; (double) i < Math.ceil(maxDistance); ++i) {
            IBlockState blockState = world.getBlockState(new BlockPos(MathHelper.floor(ray1X), MathHelper.floor(ray1Y), MathHelper.floor(ray1Z)));
            if (blockState.isFullBlock() && blockState.isFullCube()) {
                ray1Hit = ray1Free;
            } else {
                ray1Free = true;
            }

            IBlockState blockState1 = world.getBlockState(new BlockPos(MathHelper.floor(ray2X), MathHelper.floor(ray2Y), MathHelper.floor(ray2Z)));
            if (blockState1.isFullBlock() && blockState1.isFullCube()) {
                ray2Hit = ray2Free;
            } else {
                ray2Free = true;
            }

            IBlockState blockState2 = world.getBlockState(new BlockPos(MathHelper.floor(ray3X), MathHelper.floor(ray3Y), MathHelper.floor(ray3Z)));
            if (blockState2.isFullBlock() && blockState2.isFullCube()) {
                ray3Hit = ray3Free;
            } else {
                ray3Free = true;
            }

            IBlockState blockState3 = world.getBlockState(new BlockPos(MathHelper.floor(ray4X), MathHelper.floor(ray4Y), MathHelper.floor(ray4Z)));
            if (blockState3.isFullBlock() && blockState3.isFullCube()) {
                ray4Hit = ray4Free;
            } else {
                ray4Free = true;
            }

            if (ray1Hit && ray2Hit && ray3Hit && ray4Hit) {
                return false;
            }

            ray1X = ray1XPositive ? Math.min(ray1X + step1X, minX) : Math.max(ray1X + step1X, minX);
            ray1Y = ray1YPositive ? Math.min(ray1Y + step1Y, minY) : Math.max(ray1Y + step1Y, minY);
            ray1Z = ray1ZPositive ? Math.min(ray1Z + step1Z, minZ) : Math.max(ray1Z + step1Z, minZ);
            ray2X = ray2XPositive ? Math.min(ray2X + step2X, maxX) : Math.max(ray2X + step2X, maxX);
            ray2Y = ray2YPositive ? Math.min(ray2Y + step2Y, maxY) : Math.max(ray2Y + step2Y, maxY);
            ray2Z = ray2ZPositive ? Math.min(ray2Z + step2Z, maxZ) : Math.max(ray2Z + step2Z, maxZ);
            ray3X = ray3XPositive ? Math.min(ray3X + step3X, minX) : Math.max(ray3X + step3X, minX);
            ray3Y = ray3YPositive ? Math.min(ray3Y + step3Y, maxY) : Math.max(ray3Y + step3Y, minY);
            ray3Z = ray3ZPositive ? Math.min(ray3Z + step3Z, minZ) : Math.max(ray3Z + step3Z, minZ);
            ray4X = ray4XPositive ? Math.min(ray4X + step4X, maxX) : Math.max(ray4X + step4X, maxX);
            ray4Y = ray4YPositive ? Math.min(ray4Y + step4Y, minY) : Math.max(ray4Y + step4Y, minY);
            ray4Z = ray4ZPositive ? Math.min(ray4Z + step4Z, maxZ) : Math.max(ray4Z + step4Z, maxZ);
        }

        return true;
    }

}
