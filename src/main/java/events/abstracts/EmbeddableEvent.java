package events.abstracts;

import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;

import java.util.List;

public abstract class EmbeddableEvent extends EventsMethods {
    protected abstract List<EmbedCreateFields.Field> createEmbedFields();
    protected abstract void sendMessage(GuildMessageChannel channel);
}
