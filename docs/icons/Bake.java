import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * 把 svg/ 目录下的图标烘焙成多尺寸透明 PNG，输出到 out/<size>/<name>.png。
 * 尺寸档位必须与运行时 top.fpsmaster.utils.render.draw.Icons.SIZES 保持一致。
 *
 * 用法见本目录 README.md。
 */
public class Bake {
    private static final int[] SIZES = {24, 48, 96};

    public static void main(String[] args) throws Exception {
        File svgDir = new File("svg");
        SVGUniverse universe = new SVGUniverse();
        for (File f : svgDir.listFiles((d, n) -> n.endsWith(".svg"))) {
            SVGDiagram diagram = universe.getDiagram(universe.loadSVG(f.toURI().toURL()));
            diagram.setIgnoringClipHeuristic(true);
            String name = f.getName().replace(".svg", ".png");
            for (int size : SIZES) {
                File outDir = new File("out/" + size);
                outDir.mkdirs();
                BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.scale(size / diagram.getWidth(), size / diagram.getHeight());
                diagram.render(g);
                g.dispose();
                ImageIO.write(img, "png", new File(outDir, name));
            }
            System.out.println("baked " + name + " @ 24/48/96");
        }
    }
}
