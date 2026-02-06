package top.fpsmaster.utils.imaging;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class KMeansUtil {
    static class ColorPoint {
        public int r, g, b;

        public ColorPoint(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public double distanceTo(ColorPoint other) {
            int dr = this.r - other.r;
            int dg = this.g - other.g;
            int db = this.b - other.b;
            return Math.sqrt(dr * dr + dg * dg + db * db);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ColorPoint that = (ColorPoint) o;
            return r == that.r && g == that.g && b == that.b;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(r, g, b);
        }

        @Override
        public String toString() {
            return "(" + r + ", " + g + ", " + b + ")";
        }
    }

    public static Color getOneDominantColor(BufferedImage image) {
        image = resizeImage(image,64);
        List<Color> dominantColors = getDominantColorsKMeans(image, 1, 100);
        return dominantColors.get(0);
    }

    // K-Means 聚类算法的核心实现
    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 如果目标宽度大于或等于原图宽度，则不进行缩放
        if (targetWidth >= originalWidth) {
            return originalImage;
        }

        double aspectRatio = (double) originalHeight / originalWidth;
        int targetHeight = (int) (targetWidth * aspectRatio);

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();

        // 设置渲染质量，这里使用高质量缩放
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return resizedImage;
    }

    // K-Means 聚类算法的核心实现，与之前版本相同，但使用 ColorPoint
    public static List<Color> getDominantColorsKMeans(BufferedImage image, int k, int maxIterations) {
        if (k <= 0) {
            throw new IllegalArgumentException("K must be a positive integer.");
        }

        List<ColorPoint> pixels = new ArrayList<>();
        int width = image.getWidth();
        int height = image.getHeight();

        // 1. 提取所有像素颜色数据
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                // 忽略 Alpha 通道，只取 RGB
                pixels.add(new ColorPoint((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
            }
        }

        // 如果图片像素点少于 K，则直接返回所有像素颜色
        if (pixels.size() < k) {
            System.out.println("Warning: Image has fewer pixels than K. Returning all unique pixels.");
            return pixels.stream()
                    .map(cp -> new Color(cp.r, cp.g, cp.b))
                    .distinct() // 移除重复颜色
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll); // Collectors.toList() 会生成不可变列表
        }


        // 2. 初始化 K 个随机质心
        List<ColorPoint> centroids = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < k; i++) {
            centroids.add(pixels.get(random.nextInt(pixels.size())));
        }

        List<ColorPoint> oldCentroids;
        for (int iter = 0; iter < maxIterations; iter++) {
            oldCentroids = new ArrayList<>(centroids);

            // 存储每个质心对应的像素列表
            Map<ColorPoint, List<ColorPoint>> clusters = new HashMap<>();
            // 为了解决HashMap键的引用问题，我们需要一个辅助映射来找到当前质心在clusters中的键
            Map<ColorPoint, ColorPoint> centroidToMapKey = new HashMap<>();
            for (ColorPoint centroid : centroids) {
                ColorPoint key = new ColorPoint(centroid.r, centroid.g, centroid.b); // 使用一个新实例作为Map的键
                clusters.put(key, new ArrayList<>());
                centroidToMapKey.put(centroid, key);
            }

            // 3. 分配阶段：将每个像素分配到最近的质心
            for (ColorPoint pixel : pixels) {
                ColorPoint closestCentroid = null;
                double minDistance = Double.MAX_VALUE;

                for (ColorPoint centroid : centroids) {
                    double distance = pixel.distanceTo(centroid);
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestCentroid = centroid;
                    }
                }
                if (closestCentroid != null) {
                    // 确保获取到的是clusters中实际的键引用
                    clusters.get(centroidToMapKey.get(closestCentroid)).add(pixel);
                }
            }

            // 4. 更新阶段：重新计算质心
            List<ColorPoint> newCentroids = new ArrayList<>();
            for (ColorPoint centroid : centroids) {
                // 查找对应的簇列表
                List<ColorPoint> clusterPixels = null;
                for(Map.Entry<ColorPoint, List<ColorPoint>> entry : clusters.entrySet()){
                    if(entry.getKey().equals(centroid)){
                        clusterPixels = entry.getValue();
                        break;
                    }
                }

                if (clusterPixels == null || clusterPixels.isEmpty()) {
                    newCentroids.add(new ColorPoint(centroid.r, centroid.g, centroid.b)); // 保持原质心，使用新实例
                    continue;
                }

                long sumR = 0, sumG = 0, sumB = 0;
                for (ColorPoint pixel : clusterPixels) {
                    sumR += pixel.r;
                    sumG += pixel.g;
                    sumB += pixel.b;
                }
                newCentroids.add(new ColorPoint(
                        (int) (sumR / clusterPixels.size()),
                        (int) (sumG / clusterPixels.size()),
                        (int) (sumB / clusterPixels.size())
                ));
            }
            centroids = newCentroids; // 更新质心

            // 5. 收敛判断：如果质心不再变化，则停止迭代
            boolean converged = true;
            for (int i = 0; i < k; i++) {
                // 确保索引不越界
                if (i >= centroids.size() || i >= oldCentroids.size() ||
                        centroids.get(i).distanceTo(oldCentroids.get(i)) > 0.1) {
                    converged = false;
                    break;
                }
            }
            if (converged) {
                System.out.println("K-Means converged at iteration: " + (iter + 1));
                break;
            }
        }

        // 6. 返回最终的主导色 (质心)
        List<Color> dominantColors = new ArrayList<>();
        for (ColorPoint centroid : centroids) {
            dominantColors.add(new Color(centroid.r, centroid.g, centroid.b));
        }
        return dominantColors;
    }

}



