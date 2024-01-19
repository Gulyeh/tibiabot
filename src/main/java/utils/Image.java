package utils;


import lombok.SneakyThrows;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public final class Image {
    @SneakyThrows
    public static String cropImage(String src) {
        File source = new File(src);
        BufferedImage image = ImageIO.read(source);
        BufferedImage crop = image.getSubimage(555, 265, 870, 580);
        File file = new File("crop.png");
        ImageIO.write(crop, "png", file);
        return file.getAbsolutePath();
    }
}
