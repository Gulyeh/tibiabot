package utils;


import lombok.SneakyThrows;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public final class Image {
    @SneakyThrows
    public static String cropImage(String src, String dest, int start_x, int start_y, int width, int height) {
        File source = new File(src);
        BufferedImage image = ImageIO.read(source);
        BufferedImage crop = image.getSubimage(start_x, start_y, width, height);
        File file = new File(dest);
        ImageIO.write(crop, dest.split("\\.")[1], file);
        return file.getAbsolutePath();
    }
}
