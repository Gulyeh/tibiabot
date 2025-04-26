package handlers;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.ImmutableMessageCreateRequest;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.util.Color;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
public class EmbeddedHandler {

    public Color getRandomColor() {
        Random rand = new Random();
        return Color.of(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
    }

    public List<MessageData> sendEmbeddedMessages(Channel channel, List<EmbedCreateFields.Field> fields, String title, String description, String imageUrl, String thumbnailUrl, Color color) {
        return sendEmbeddedMessages(channel, fields, title, description, imageUrl, thumbnailUrl, color, null, null);
    }

    public List<MessageData> sendEmbeddedMessages(Channel channel, List<EmbedCreateFields.Field> fields, String title, String description, String imageUrl, String thumbnailUrl, Color color, EmbedCreateFields.Footer footer) {
        return sendEmbeddedMessages(channel, fields, title, description, imageUrl, thumbnailUrl, color, footer, null);
    }

    public List<MessageData> sendEmbeddedMessages(Channel channel,
                                                     List<EmbedCreateFields.Field> fields,
                                                     String title,
                                                     String description,
                                                     String imageUrl,
                                                     String thumbnailUrl,
                                                     Color color,
                                                     EmbedCreateFields.Footer footer,
                                                     ActionRow components) {
        List<MessageData> sentMessages = new ArrayList<>();

        try {
            List<EmbedCreateSpec> messages = createEmbeddedMessages(fields, title, description, imageUrl, thumbnailUrl, color, footer);

            for (EmbedCreateSpec msg : messages) {
                EmbedData embedData = msg.asRequest();

                MessageCreateRequest request;
                ImmutableMessageCreateRequest.Builder builder = MessageCreateRequest.builder()
                        .embed(embedData);
                if(components != null) builder.addComponent(components.getData());
                request = builder.build();

                MessageData msgData = channel.getRestChannel().createMessage(request).block();
                if (msgData != null)
                    sentMessages.add(msgData);
            }
        } catch (Exception e) {
            log.warn("Could not send embedded messages - {}", e.getMessage());
        }

        return sentMessages;
    }

    public List<EmbedCreateSpec> createEmbeddedMessages(List<EmbedCreateFields.Field> fields, String title, String description,
                                                           String imageUrl, String thumbnailUrl, Color color, EmbedCreateFields.Footer footer) {
        EmbedCreateSpec template = buildEmbedTemplate(title, description, imageUrl, thumbnailUrl, color, footer);
        return new ArrayList<>(splitEmbeddedMessage(fields, template));
    }

    public EmbedCreateFields.Field emptyField(boolean inline) {
        return EmbedCreateFields.Field.of("\t", "\t", inline);
    }


    private List<EmbedCreateSpec> splitEmbeddedMessage(List<EmbedCreateFields.Field> fields, EmbedCreateSpec embedData) {
        List<EmbedCreateSpec> listOfMessages = new ArrayList<>();

        if(fields == null || fields.isEmpty()) {
            EmbedCreateSpec builder = generateEmbed(embedData, 0, 0);
            listOfMessages.add(builder);
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
            listOfMessages.add(builder.withFields(fieldsSplitted));
        }

        return listOfMessages;
    }

    private EmbedCreateSpec generateEmbed(EmbedCreateSpec embedData, int currentIndex, int lastIndex) {
        EmbedCreateSpec.Builder copy = EmbedCreateSpec.builder()
                .title(embedData.title().get())
                .description(embedData.description().get())
                .image(embedData.image().get())
                .thumbnail(embedData.thumbnail().get())
                .color(embedData.color().get())
                .footer(embedData.footer());

        if(currentIndex != 0)
            copy.title("").description("").image("").thumbnail("");

        if(embedData.footer() == null && currentIndex == lastIndex - 1)
            copy.footer("Last updated", "").timestamp(Instant.now());

        return copy.build();
    }

    private EmbedCreateSpec buildEmbedTemplate(String title, String description, String imageUrl, String thumbnailUrl, Color color, EmbedCreateFields.Footer footer) {
        return EmbedCreateSpec.builder()
                .title(title)
                .description(description)
                .image(imageUrl)
                .thumbnail(thumbnailUrl)
                .color(color)
                .footer(footer)
                .build();
    }
}
