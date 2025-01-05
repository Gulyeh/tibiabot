package events.abstracts;

import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.util.Color;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class EmbeddableEvent extends ProcessEvent {
    protected Color getRandomColor() {
        Random rand = new Random();
        return Color.of(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
    }

    protected List<MessageData> sendEmbeddedMessages(Channel channel, List<EmbedCreateFields.Field> fields, String title, String description, String imageUrl, String thumbnailUrl, Color color) {
        return sendEmbeddedMessages(channel, fields, title, description, imageUrl, thumbnailUrl, color, null);
    }

    protected List<MessageData> sendEmbeddedMessages(Channel channel, List<EmbedCreateFields.Field> fields, String title, String description,
                                                         String imageUrl, String thumbnailUrl, Color color, EmbedCreateFields.Footer footer) {
        try {
            List<EmbedCreateSpec> messages = createEmbeddedMessages(fields, title, description, imageUrl, thumbnailUrl, color, footer);
            List<MessageData> sentMessages = new ArrayList<>();

            for (EmbedCreateSpec msg : messages) {
                MessageData msgData = channel.getRestChannel().createMessage(msg.asRequest()).block();
                sentMessages.add(msgData);
            }

            return sentMessages;
        } catch (Exception msg) {
            logINFO.info("Could not send embedded messages - " + msg);
            return new ArrayList<>();
        }
    }

    protected List<EmbedCreateSpec> createEmbeddedMessages(List<EmbedCreateFields.Field> fields, String title, String description,
                                                     String imageUrl, String thumbnailUrl, Color color, EmbedCreateFields.Footer footer) {

        EmbedCreateSpec template = buildEmbedTemplate(title, description, imageUrl, thumbnailUrl, color, footer);
        return new ArrayList<>(splitEmbeddedMessage(fields, template));
    }

    protected EmbedCreateFields.Field emptyField(boolean inline) {
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

    private EmbedCreateSpec buildEmbedTemplate(String title, String description, String imageUrl, String thumbnailUrl, Color color, EmbedCreateFields.Footer footer)
    {
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
