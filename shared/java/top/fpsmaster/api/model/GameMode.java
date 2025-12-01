package top.fpsmaster.api.model;

/**
 * 游戏模式枚举
 */
public enum GameMode {
    /**
     * 生存模式
     */
    SURVIVAL(0, "survival"),
    
    /**
     * 创造模式
     */
    CREATIVE(1, "creative"),
    
    /**
     * 冒险模式
     */
    ADVENTURE(2, "adventure"),
    
    /**
     * 旁观者模式
     */
    SPECTATOR(3, "spectator");
    
    private final int id;
    private final String name;
    
    GameMode(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public static GameMode fromId(int id) {
        for (GameMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        return SURVIVAL;
    }
}
