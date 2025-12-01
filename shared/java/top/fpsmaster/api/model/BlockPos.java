package top.fpsmaster.api.model;

import java.util.Objects;

/**
 * 方块位置（值对象）
 * 不可变对象，线程安全
 * 
 * 注意: 这是自定义的 BlockPos，不依赖 Minecraft 的 BlockPos
 */
public final class BlockPos {
    private final int x;
    private final int y;
    private final int z;
    
    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getZ() {
        return z;
    }
    
    /**
     * 创建新的偏移位置
     */
    public BlockPos add(int dx, int dy, int dz) {
        return new BlockPos(x + dx, y + dy, z + dz);
    }
    
    /**
     * 向指定方向偏移
     */
    public BlockPos offset(Direction direction) {
        return add(direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ());
    }
    
    /**
     * 向上偏移
     */
    public BlockPos up() {
        return add(0, 1, 0);
    }
    
    /**
     * 向下偏移
     */
    public BlockPos down() {
        return add(0, -1, 0);
    }
    
    /**
     * 向北偏移
     */
    public BlockPos north() {
        return add(0, 0, -1);
    }
    
    /**
     * 向南偏移
     */
    public BlockPos south() {
        return add(0, 0, 1);
    }
    
    /**
     * 向西偏移
     */
    public BlockPos west() {
        return add(-1, 0, 0);
    }
    
    /**
     * 向东偏移
     */
    public BlockPos east() {
        return add(1, 0, 0);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockPos blockPos = (BlockPos) o;
        return x == blockPos.x && y == blockPos.y && z == blockPos.z;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
    
    @Override
    public String toString() {
        return "BlockPos{x=" + x + ", y=" + y + ", z=" + z + "}";
    }
}
