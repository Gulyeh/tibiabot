package events.abstracts;

import cache.CacheData;
import cache.enums.EventTypes;
import discord.messages.DeleteMessages;
import discord.messages.SendMessages;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.discordjson.json.ApplicationCommandInteractionOptionData;
import events.interfaces.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public abstract class EventsMethods implements EventListener {
    protected final static Logger logINFO = LoggerFactory.getLogger(EventsMethods.class);
    protected final SendMessages sendMessages;
    protected final DeleteMessages deleteMessages;

    protected EventsMethods() {
        sendMessages = new SendMessages();
        deleteMessages = new DeleteMessages();
        new Thread(this::activateEvent).start();
    }

    protected Snowflake getChannelId(ChatInputInteractionEvent event) {
        Optional<ApplicationCommandInteractionOptionValue> value = event.getOptions().get(0).getValue();
        return value.map(x -> Snowflake.of(x.getRaw().replaceAll("\\D", ""))).orElse(null);
    }

    protected Snowflake getGuildId(ChatInputInteractionEvent event) {
        return event.getInteraction().getGuildId().get();
    }

    protected String getTextParameter(ChatInputInteractionEvent event) {
        Optional<ApplicationCommandInteractionOptionValue> value = event.getOptions().get(0).getValue();
        return value.map(ApplicationCommandInteractionOptionValue::getRaw).orElse(null);
    }

    protected abstract void activateEvent();

    protected void saveSetChannel(ChatInputInteractionEvent event) {
//        Snowflake guildId = getGuildId(event);
//        Snowflake channelId = getChannelId(event);
//        EventTypes eventType = EventTypes.getEnum(getEventName());
//        boolean guildExists = CacheData.getChannelsCache().containsKey(guildId);
//        HashMap<EventTypes, Snowflake> mapChannels;
        //TODO SQL
    }
}
