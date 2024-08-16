package events.abstracts;

import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.rest.util.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class EmbeddableEvent extends ProcessEvent {
    protected abstract <T> List<EmbedCreateFields.Field> createEmbedFields(T model);

    protected abstract <T> void processData(GuildMessageChannel channel, T model);

    protected Color getRandomColor() {
        Random rand = new Random();
        return Color.of(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
    }
}
