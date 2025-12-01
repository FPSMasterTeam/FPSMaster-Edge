package top.fpsmaster.api.model;

import java.util.Objects;

/**
 * 三维向量（值对象）
 * 不可变对象，线程安全
 * 
 * 注意: 这是自定义的 Vec3，不依赖 Minecraft 的 Vec3
 */
public final class Vec3 {
    public final double x;
    public final double y;
    public final double z;
    
    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    /**
     * 零向量
     */
    public static final Vec3 ZERO = new Vec3(0, 0, 0);
    
    /**
     * 向量加法
     */
    public Vec3 add(Vec3 other) {
        return new Vec3(x + other.x, y + other.y, z + other.z);
    }
    
    /**
     * 向量加法
     */
    public Vec3 add(double dx, double dy, double dz) {
        return new Vec3(x + dx, y + dy, z + dz);
    }
    
    /**
     * 向量减法
     */
    public Vec3 subtract(Vec3 other) {
        return new Vec3(x - other.x, y - other.y, z - other.z);
    }
    
    /**
     * 向量缩放
     */
    public Vec3 scale(double factor) {
        return new Vec3(x * factor, y * factor, z * factor);
    }
    
    /**
     * 计算到另一个向量的距离
     */
    public double distanceTo(Vec3 other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * 计算距离的平方（避免开方运算，性能更好）
     */
    public double distanceSquared(Vec3 other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
    
    /**
     * 向量长度
     */
    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }
    
    /**
     * 向量长度的平方
     */
    public double lengthSquared() {
        return x * x + y * y + z * z;
    }
    
    /**
     * 归一化（单位向量）
     */
    public Vec3 normalize() {
        double len = length();
        return len < 1e-4 ? ZERO : scale(1.0 / len);
    }
    
    /**
     * 点积
     */
    public double dot(Vec3 other) {
        return x * other.x + y * other.y + z * other.z;
    }
    
    /**
     * 叉积
     */
    public Vec3 cross(Vec3 other) {
        return new Vec3(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        );
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vec3 vec3 = (Vec3) o;
        return Double.compare(vec3.x, x) == 0 &&
               Double.compare(vec3.y, y) == 0 &&
               Double.compare(vec3.z, z) == 0;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
    
    @Override
    public String toString() {
        return String.format("Vec3{x=%.2f, y=%.2f, z=%.2f}", x, y, z);
    }
}
