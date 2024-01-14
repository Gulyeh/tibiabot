package events.abstracts;

import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;

import java.util.List;

public abstract class EmbeddableEvent extends EventsMethods {
    protected abstract <T> List<EmbedCreateFields.Field> createEmbedFields(T model);
    protected abstract <T> void sendMessage(GuildMessageChannel channel, T model);
}
