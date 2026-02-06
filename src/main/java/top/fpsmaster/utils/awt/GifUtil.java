package top.fpsmaster.utils.awt;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GifUtil {
    public static class FrameData {
        public BufferedImage image;
        public int delay;

        public FrameData(BufferedImage image, int delay) {
            this.image = image;
            this.delay = delay;
        }
    }

    public static List<FrameData> convertGifToPng(InputStream stream) throws IOException {
        // 读取GIF
        ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
        ImageInputStream in = ImageIO.createImageInputStream(stream);
        reader.setInput(in);

        // 获取GIF的帧数
        int numImages = reader.getNumImages(true);
        List<FrameData> frames = new ArrayList<>();

        BufferedImage master = null;

        // 遍历每一帧
        for(int i = 0; i < numImages; i++) {
            // 获取总的元数据
            IIOMetadata metadata = reader.getImageMetadata(i);

            BufferedImage image = reader.read(i);

            if(i==0) {
                master = image;
            } else {
                Node tree = metadata.getAsTree("javax_imageio_gif_image_1.0");
                int x = Integer.parseInt(tree.getChildNodes().item(0).getAttributes().getNamedItem("imageLeftPosition").getNodeValue());
                int y = Integer.parseInt(tree.getChildNodes().item(0).getAttributes().getNamedItem("imageTopPosition").getNodeValue());

                BufferedImage combined = new BufferedImage(master.getWidth(), master.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics g = combined.getGraphics();

                g.drawImage(master,0,0,null);
                g.drawImage(image,x,y,null);

                master = combined;
                image = combined;
            }

            // 保存帧与帧之间的延迟时间
            String[] metadataNames = metadata.getMetadataFormatNames();
            int delay = -1;

            for(String name: metadataNames){
                Node node = metadata.getAsTree(name);
                if(node instanceof IIOMetadataNode){
                    NodeList listChildNodes = node.getChildNodes();
                    for (int j = 0; j < listChildNodes.getLength(); j++){
                        if(listChildNodes.item(j).getNodeName().equals("GraphicControlExtension")){
                            NamedNodeMap attr = listChildNodes.item(j).getAttributes();
                            delay = Integer.parseInt(attr.getNamedItem("delayTime").getNodeValue())*10;
                            break;
                        }
                    }
                }
            }

            // 将图像和延迟时间添加到帧列表中
            frames.add(new FrameData(image, delay));
        }

        return frames;
    }

}
