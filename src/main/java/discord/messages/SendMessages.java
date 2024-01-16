package discord.messages;

import discord4j.core.object.entity.channel.Channel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.util.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static List<EmbedData> splitEmbeddedMessage(List<EmbedCreateFields.Field> fields, EmbedCreateSpec embedData) {
        int maxIndex, minIndex, maxFields = 20;
        int iterations = (int) Math.ceil((double) fields.size() / maxFields);
        List<EmbedData> listOfMessages = new ArrayList<>();
        boolean isFirstMessage = true;

        for(int i = 0; i < iterations; i++) {
            minIndex = maxFields * i;
            maxIndex = minIndex + maxFields;
            if(maxIndex > fields.size()) maxIndex = fields.size();

            List<EmbedCreateFields.Field> fieldsSplitted = fields.subList(minIndex, maxIndex);
            EmbedCreateSpec builder = generateEmbed(embedData, i, iterations);
            if(isFirstMessage) isFirstMessage = false;

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
