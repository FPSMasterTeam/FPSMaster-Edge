package top.fpsmaster.api.model;

/**
 * 方向枚举
 */
public enum Direction {
    DOWN(0, -1, 0, "down"),
    UP(0, 1, 0, "up"),
    NORTH(0, 0, -1, "north"),
    SOUTH(0, 0, 1, "south"),
    WEST(-1, 0, 0, "west"),
    EAST(1, 0, 0, "east");
    
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final String name;
    
    Direction(int offsetX, int offsetY, int offsetZ, String name) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.name = name;
    }
    
    public int getOffsetX() {
        return offsetX;
    }
    
    public int getOffsetY() {
        return offsetY;
    }
    
    public int getOffsetZ() {
        return offsetZ;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * 获取相反方向
     */
    public Direction getOpposite() {
        switch (this) {
            case DOWN: return UP;
            case UP: return DOWN;
            case NORTH: return SOUTH;
            case SOUTH: return NORTH;
            case WEST: return EAST;
            case EAST: return WEST;
            default: return this;
        }
    }
}
