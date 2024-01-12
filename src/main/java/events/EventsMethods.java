package events;

import discord.Connector;
import discord.messages.DeleteMessages;
import discord.messages.SendMessages;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandInteractionData;
import discord4j.discordjson.json.ApplicationCommandInteractionOptionData;
import discord4j.discordjson.json.EmbedData;
import discord4j.rest.util.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public abstract class EventsMethods {
    protected final static Logger logINFO = LoggerFactory.getLogger(EventsMethods.class);
    protected final SendMessages sendMessages;
    protected final DeleteMessages deleteMessages;

    protected EventsMethods() {
        sendMessages = new SendMessages();
        deleteMessages = new DeleteMessages();
        activateEvent();
    }

    protected Snowflake getChannelId(ChatInputInteractionEvent event) {
        Optional<ApplicationCommandInteractionOptionValue> value = event.getOptions().get(0).getValue();
        return value.map(ApplicationCommandInteractionOptionValue::asSnowflake).orElse(null);
    }

    protected abstract List<EmbedCreateFields.Field> createEmbedFields();

    protected abstract void activateEvent();

    protected void saveSetChannel(ChatInputInteractionEvent event) {
        //TODO SQL
    }
}
