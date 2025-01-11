package discord.messages;

import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.TextChannel;
import lombok.SneakyThrows;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

public final class SendMessages {
    @SneakyThrows
    public static void sendImageMessage(Channel channel, String imageSrc) {
        BufferedImage buffer = ImageIO.read(new File(imageSrc));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(buffer, "png", outputStream);
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        ((TextChannel)channel).createMessage(messageCreateSpec -> messageCreateSpec.addFile("events.png", inputStream)).block();
    }
}
