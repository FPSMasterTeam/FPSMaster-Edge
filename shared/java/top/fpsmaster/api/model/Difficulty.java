package top.fpsmaster.api.model;

/**
 * 难度枚举
 */
public enum Difficulty {
    /**
     * 和平
     */
    PEACEFUL(0, "peaceful"),
    
    /**
     * 简单
     */
    EASY(1, "easy"),
    
    /**
     * 普通
     */
    NORMAL(2, "normal"),
    
    /**
     * 困难
     */
    HARD(3, "hard");
    
    private final int id;
    private final String name;
    
    Difficulty(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public static Difficulty fromId(int id) {
        for (Difficulty difficulty : values()) {
            if (difficulty.id == id) {
                return difficulty;
            }
        }
        return NORMAL;
    }
}
