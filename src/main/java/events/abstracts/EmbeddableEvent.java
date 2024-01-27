package events.abstracts;

import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;

import java.util.List;

public abstract class EmbeddableEvent extends ProcessEvent {
    protected abstract <T> List<EmbedCreateFields.Field> createEmbedFields(T model);
    protected abstract <T> void processData(GuildMessageChannel channel, T model);
}
