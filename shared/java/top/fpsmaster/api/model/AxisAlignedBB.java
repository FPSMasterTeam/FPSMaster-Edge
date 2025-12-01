package top.fpsmaster.api.model;

import java.util.Objects;

/**
 * 轴对齐包围盒（值对象）
 * 不可变对象，线程安全
 * 
 * 注意: 这是自定义的 AxisAlignedBB，不依赖 Minecraft 的 AxisAlignedBB
 */
public final class AxisAlignedBB {
    public final double minX;
    public final double minY;
    public final double minZ;
    public final double maxX;
    public final double maxY;
    public final double maxZ;
    
    public AxisAlignedBB(double minX, double minY, double minZ,
                         double maxX, double maxY, double maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }
    
    /**
     * 从方块位置创建
     */
    public static AxisAlignedBB fromBlock(BlockPos pos) {
        return new AxisAlignedBB(
            pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
        );
    }
    
    /**
     * 从两个点创建
     */
    public static AxisAlignedBB fromPoints(Vec3 pos1, Vec3 pos2) {
        return new AxisAlignedBB(pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z);
    }
    
    /**
     * 扩展包围盒
     */
    public AxisAlignedBB expand(double x, double y, double z) {
        return new AxisAlignedBB(
            minX - x, minY - y, minZ - z,
            maxX + x, maxY + y, maxZ + z
        );
    }
    
    /**
     * 统一扩展
     */
    public AxisAlignedBB expand(double value) {
        return expand(value, value, value);
    }
    
    /**
     * 收缩包围盒
     */
    public AxisAlignedBB contract(double x, double y, double z) {
        return new AxisAlignedBB(
            minX + x, minY + y, minZ + z,
            maxX - x, maxY - y, maxZ - z
        );
    }
    
    /**
     * 偏移包围盒
     */
    public AxisAlignedBB offset(double x, double y, double z) {
        return new AxisAlignedBB(
            minX + x, minY + y, minZ + z,
            maxX + x, maxY + y, maxZ + z
        );
    }
    
    /**
     * 偏移包围盒
     */
    public AxisAlignedBB offset(Vec3 vec) {
        return offset(vec.x, vec.y, vec.z);
    }
    
    /**
     * 检查是否相交
     */
    public boolean intersects(AxisAlignedBB other) {
        return maxX > other.minX && minX < other.maxX &&
               maxY > other.minY && minY < other.maxY &&
               maxZ > other.minZ && minZ < other.maxZ;
    }
    
    /**
     * 检查是否包含点
     */
    public boolean contains(Vec3 point) {
        return point.x >= minX && point.x <= maxX &&
               point.y >= minY && point.y <= maxY &&
               point.z >= minZ && point.z <= maxZ;
    }
    
    /**
     * 检查是否包含点（带容差）
     */
    public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }
    
    /**
     * 获取中心点
     */
    public Vec3 getCenter() {
        return new Vec3(
            (minX + maxX) / 2.0,
            (minY + maxY) / 2.0,
            (minZ + maxZ) / 2.0
        );
    }
    
    /**
     * 获取体积
     */
    public double getVolume() {
        return (maxX - minX) * (maxY - minY) * (maxZ - minZ);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AxisAlignedBB that = (AxisAlignedBB) o;
        return Double.compare(that.minX, minX) == 0 &&
               Double.compare(that.minY, minY) == 0 &&
               Double.compare(that.minZ, minZ) == 0 &&
               Double.compare(that.maxX, maxX) == 0 &&
               Double.compare(that.maxY, maxY) == 0 &&
               Double.compare(that.maxZ, maxZ) == 0;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    @Override
    public String toString() {
        return String.format("AABB{min=(%.2f,%.2f,%.2f), max=(%.2f,%.2f,%.2f)}", 
            minX, minY, minZ, maxX, maxY, maxZ);
    }
}
