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
    private static final Logger logINFO = LoggerFactory.getLogger(SendMessages.class);

    public static List<MessageData> sendEmbeddedMessages(Channel channel, List<EmbedCreateFields.Field> fields, String title, String description, String imageUrl, String thumbnailUrl, Color color) {
        try {
            EmbedCreateSpec template = buildEmbedTemplate(title, description, imageUrl, thumbnailUrl, color);
            List<EmbedData> messages = new ArrayList<>(splitEmbeddedMessage(fields, template));
            List<MessageData> sentMessages = new ArrayList<>();

            for (EmbedData msg : messages) {
                MessageData msgData = channel.getRestChannel().createMessage(msg).block();
                sentMessages.add(msgData);
            }

            return sentMessages;
        } catch (Exception ignore) {
            logINFO.info("Could not send embedded messages");
            return new ArrayList<>();
        }
    }

    @SneakyThrows
    public static void sendImageMessage(Channel channel, String imageSrc) {
        BufferedImage buffer = ImageIO.read(new File(imageSrc));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(buffer, "png", outputStream);
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        ((TextChannel)channel).createMessage(messageCreateSpec -> messageCreateSpec.addFile("events.png", inputStream)).block();
    }

    private static List<EmbedData> splitEmbeddedMessage(List<EmbedCreateFields.Field> fields, EmbedCreateSpec embedData) {
        List<EmbedData> listOfMessages = new ArrayList<>();

        if(fields == null || fields.isEmpty()) {
            EmbedCreateSpec builder = generateEmbed(embedData, 0, 0);
            listOfMessages.add(builder.asRequest());
            return listOfMessages;
        }

        int maxIndex, minIndex, maxFields = 20;
        int iterations = (int) Math.ceil((double) fields.size() / maxFields);

        for(int i = 0; i < iterations; i++) {
            minIndex = maxFields * i;
            maxIndex = minIndex + maxFields;
            if(maxIndex > fields.size()) maxIndex = fields.size();

            List<EmbedCreateFields.Field> fieldsSplitted = fields.subList(minIndex, maxIndex);
            EmbedCreateSpec builder = generateEmbed(embedData, i, iterations);

            listOfMessages.add(builder.withFields(fieldsSplitted).asRequest());
        }

        return listOfMessages;
    }

    private static EmbedCreateSpec generateEmbed(EmbedCreateSpec embedData, int currentIndex, int lastIndex) {
        EmbedCreateSpec.Builder copy = EmbedCreateSpec.builder()
                .title(embedData.title().get())
                .description(embedData.description().get())
                .image(embedData.image().get())
                .thumbnail(embedData.thumbnail().get())
                .color(embedData.color().get());

        if(currentIndex != 0) copy.title("").description("").image("").thumbnail("");
        if(currentIndex == lastIndex - 1) copy.footer("Last updated", "").timestamp(Instant.now());

        return copy.build();
    }

    private static EmbedCreateSpec buildEmbedTemplate(String title, String description, String imageUrl, String thumbnailUrl, Color color)
    {
        return EmbedCreateSpec.builder()
                .title(title)
                .description(description)
                .image(imageUrl)
                .thumbnail(thumbnailUrl)
                .color(color)
                .build();
    }
}
