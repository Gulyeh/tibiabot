package discord.messages;

import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.util.Color;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
